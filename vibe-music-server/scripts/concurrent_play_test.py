#!/usr/bin/env python3
"""
500并发用户播放脚本
模拟500个用户同时播放歌曲，测试系统并发性能

使用方法:
    python concurrent_play_test.py

配置说明:
    - BASE_URL: 后端API地址
    - MINIO_URL: MinIO文件服务器地址
    - CONCURRENT_USERS: 并发用户数（默认500）
    - TEST_DURATION: 测试持续时间（秒）
"""

import asyncio
import aiohttp
import time
import json
import random
from typing import List, Dict
from dataclasses import dataclass
from datetime import datetime

# ==================== 配置参数 ====================
BASE_URL = "http://localhost:8080"  # 后端API地址
MINIO_URL = "http://127.0.0.1:9000"  # MinIO地址
CONCURRENT_USERS = 500  # 并发用户数
TEST_DURATION = 60  # 测试持续时间（秒）
SONG_ID = None  # 指定歌曲ID，None则随机选择

# 测试用户账号（需要提前创建）
# 注意：密码需要符合格式要求：8-18位数字、字母、符号的任意两种组合
# 使用 "123456ab" 作为测试密码（数字+字母）
TEST_USERS = [
    {"username": f"testuser{i}", "password": "123456ab", "email": f"testuser{i}@test.com"}
    for i in range(1, 501)  # 创建500个测试账号
]

# ==================== 数据类 ====================
@dataclass
class TestResult:
    """测试结果"""
    user_id: int
    login_success: bool
    login_time: float
    get_songs_success: bool
    get_songs_time: float
    play_success: bool
    play_time: float
    audio_download_success: bool
    audio_download_time: float
    total_time: float
    error_message: str = ""


# ==================== 统计信息 ====================
class Statistics:
    def __init__(self):
        self.total_users = 0
        self.successful_logins = 0
        self.successful_song_requests = 0
        self.successful_plays = 0
        self.successful_downloads = 0
        self.failed_requests = 0
        self.total_response_time = 0
        self.login_times = []
        self.song_request_times = []
        self.play_times = []
        self.download_times = []
        self.errors = []

    def add_result(self, result: TestResult):
        self.total_users += 1
        if result.login_success:
            self.successful_logins += 1
            self.login_times.append(result.login_time)
        if result.get_songs_success:
            self.successful_song_requests += 1
            self.song_request_times.append(result.get_songs_time)
        if result.play_success:
            self.successful_plays += 1
            self.play_times.append(result.play_time)
        if result.audio_download_success:
            self.successful_downloads += 1
            self.download_times.append(result.audio_download_time)
        if result.error_message:
            self.errors.append(f"User {result.user_id}: {result.error_message}")
            self.failed_requests += 1
        self.total_response_time += result.total_time

    def print_summary(self):
        print("\n" + "="*60)
        print("测试结果统计")
        print("="*60)
        print(f"总用户数: {self.total_users}")
        print(f"成功登录: {self.successful_logins} ({self.successful_logins/self.total_users*100:.2f}%)")
        print(f"成功获取歌曲: {self.successful_song_requests} ({self.successful_song_requests/self.total_users*100:.2f}%)")
        print(f"成功播放: {self.successful_plays} ({self.successful_plays/self.total_users*100:.2f}%)")
        print(f"成功下载音频: {self.successful_downloads} ({self.successful_downloads/self.total_users*100:.2f}%)")
        print(f"失败请求: {self.failed_requests}")
        print(f"平均总响应时间: {self.total_response_time/self.total_users:.2f}秒")
        
        if self.login_times:
            print(f"\n登录性能:")
            print(f"  平均时间: {sum(self.login_times)/len(self.login_times):.2f}秒")
            print(f"  最快: {min(self.login_times):.2f}秒")
            print(f"  最慢: {max(self.login_times):.2f}秒")
        
        if self.song_request_times:
            print(f"\n获取歌曲性能:")
            print(f"  平均时间: {sum(self.song_request_times)/len(self.song_request_times):.2f}秒")
            print(f"  最快: {min(self.song_request_times):.2f}秒")
            print(f"  最慢: {max(self.song_request_times):.2f}秒")
        
        if self.download_times:
            print(f"\n音频下载性能:")
            print(f"  平均时间: {sum(self.download_times)/len(self.download_times):.2f}秒")
            print(f"  最快: {min(self.download_times):.2f}秒")
            print(f"  最慢: {max(self.download_times):.2f}秒")
        
        if self.errors:
            print(f"\n错误信息 (前10条):")
            for error in self.errors[:10]:
                print(f"  {error}")


# ==================== 全局统计 ====================
stats = Statistics()


# ==================== API 函数 ====================
async def login(session: aiohttp.ClientSession, username: str, password: str, email: str, user_id: int = 0):
    """用户登录"""
    start_time = time.time()
    try:
        url = f"{BASE_URL}/user/login"
        data = {
            "email": email,  # 登录使用邮箱
            "password": password
        }
        async with session.post(url, json=data) as response:
            elapsed = time.time() - start_time
            if response.status == 200:
                try:
                    result = await response.json()
                    if isinstance(result, dict) and result.get("code") == 0:
                        token = result.get("data", "")
                        if isinstance(token, dict):
                            token = token.get("token", "")
                        return True, token, elapsed
                    else:
                        # 记录前5个失败的用户信息用于调试
                        if user_id <= 5 and isinstance(result, dict):
                            print(f"登录失败 - User {user_id} ({email}): {result.get('message', '未知错误')}")
                except Exception as json_error:
                    if user_id <= 5:
                        text = await response.text()
                        print(f"JSON解析错误 - User {user_id}: {str(json_error)} - Response: {text[:200]}")
            else:
                if user_id <= 5:
                    text = await response.text()
                    print(f"HTTP错误 - User {user_id}: {response.status} - {text[:100]}")
            return False, "", elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        if user_id <= 5:
            print(f"登录异常 - User {user_id} ({email}): {str(e)}")
        return False, "", elapsed


async def get_songs(session: aiohttp.ClientSession, token: str, song_id: int = None):
    """获取歌曲列表或指定歌曲"""
    start_time = time.time()
    try:
        if song_id:
            # 获取指定歌曲详情
            url = f"{BASE_URL}/song/getSongDetail/{song_id}"
            headers = {"Authorization": f"Bearer {token}"}
            async with session.get(url, headers=headers) as response:
                elapsed = time.time() - start_time
                if response.status == 200:
                    result = await response.json()
                    if result.get("code") == 0:
                        return True, result.get("data", {}), elapsed
        else:
            # 获取推荐歌曲列表
            url = f"{BASE_URL}/song/getRecommendedSongs"
            headers = {"Authorization": f"Bearer {token}"}
            async with session.get(url, headers=headers) as response:
                elapsed = time.time() - start_time
                if response.status == 200:
                    result = await response.json()
                    if result.get("code") == 0:
                        songs = result.get("data", [])
                        if songs:
                            return True, random.choice(songs), elapsed
        return False, {}, elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        return False, {}, elapsed


async def download_audio(session: aiohttp.ClientSession, audio_url: str):
    """下载音频文件（模拟播放）"""
    start_time = time.time()
    try:
        # 如果是MinIO URL，直接请求
        if audio_url.startswith("http://") or audio_url.startswith("https://"):
            url = audio_url
        else:
            url = f"{MINIO_URL}/{audio_url}"
        
        # 只下载前1MB来模拟播放（Range请求）
        headers = {"Range": "bytes=0-1048575"}  # 1MB
        async with session.get(url, headers=headers) as response:
            elapsed = time.time() - start_time
            if response.status in [200, 206]:  # 200 OK 或 206 Partial Content
                # 读取部分数据即可
                await response.read()
                return True, elapsed
            return False, elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        return False, elapsed


# ==================== 用户模拟 ====================
async def simulate_user(user_id: int, username: str, password: str, email: str, song_id: int = None) -> TestResult:
    """模拟单个用户的操作流程（添加user_id到函数内部用于调试）"""
    """模拟单个用户的操作流程"""
    result = TestResult(
        user_id=user_id,
        login_success=False,
        login_time=0,
        get_songs_success=False,
        get_songs_time=0,
        play_success=False,
        play_time=0,
        audio_download_success=False,
        audio_download_time=0,
        total_time=0
    )
    
    total_start = time.time()
    
    try:
        async with aiohttp.ClientSession() as session:
            # 1. 登录（传递user_id用于调试）
            login_success, token, login_time = await login(session, username, password, email, user_id)
            result.login_success = login_success
            result.login_time = login_time
            
            if not login_success:
                result.error_message = "登录失败"
                result.total_time = time.time() - total_start
                return result
            
            # 2. 获取歌曲信息
            get_success, song_data, get_time = await get_songs(session, token, song_id)
            result.get_songs_success = get_success
            result.get_songs_time = get_time
            
            if not get_success or not song_data:
                result.error_message = "获取歌曲失败"
                result.total_time = time.time() - total_start
                return result
            
            # 3. 获取音频URL
            audio_url = song_data.get("audioUrl") or song_data.get("audio_url")
            if not audio_url:
                result.error_message = "音频URL为空"
                result.total_time = time.time() - total_start
                return result
            
            result.play_success = True
            result.play_time = 0.1  # 模拟播放操作时间
            
            # 4. 下载音频文件（模拟播放）
            download_success, download_time = await download_audio(session, audio_url)
            result.audio_download_success = download_success
            result.audio_download_time = download_time
            
            if not download_success:
                result.error_message = "音频下载失败"
            
    except Exception as e:
        result.error_message = f"异常: {str(e)}"
    
    result.total_time = time.time() - total_start
    return result


# ==================== 主函数 ====================
async def main():
    """主测试函数"""
    print("="*60)
    print("500并发用户播放测试")
    print("="*60)
    print(f"后端地址: {BASE_URL}")
    print(f"MinIO地址: {MINIO_URL}")
    print(f"并发用户数: {CONCURRENT_USERS}")
    print(f"测试时长: {TEST_DURATION}秒")
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    
    # 创建任务列表
    tasks = []
    for i in range(CONCURRENT_USERS):
        user = TEST_USERS[i % len(TEST_USERS)]
        task = simulate_user(i + 1, user["username"], user["password"], user["email"], SONG_ID)
        tasks.append(task)
    
    # 执行并发测试
    print(f"\n开始执行 {CONCURRENT_USERS} 个并发用户测试...")
    start_time = time.time()
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    elapsed_time = time.time() - start_time
    print(f"\n所有请求完成，耗时: {elapsed_time:.2f}秒")
    
    # 收集结果
    for result in results:
        if isinstance(result, TestResult):
            stats.add_result(result)
        else:
            print(f"任务异常: {result}")
    
    # 打印统计信息
    stats.print_summary()
    
    print(f"\n测试结束时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)


if __name__ == "__main__":
    # 检查依赖
    try:
        import aiohttp
    except ImportError:
        print("错误: 需要安装 aiohttp 库")
        print("安装命令: pip install aiohttp")
        exit(1)
    
    # 运行测试
    asyncio.run(main())

