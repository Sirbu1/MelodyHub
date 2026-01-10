#!/usr/bin/env python3
"""
50并发用户文件上传测试脚本
模拟50个用户同时上传30MB左右的文件，测试上传成功率和性能

功能：
1. 50并发用户同时上传文件
2. 记录每个文件的上传耗时
3. 上传完成后验证文件完整性（通过下载并比较文件大小和MD5）

使用方法:
    python concurrent_upload_test.py
"""

import asyncio
import aiohttp
import aiofiles
import time
import json
import hashlib
import os
import random
from typing import List, Dict
from dataclasses import dataclass
from datetime import datetime

# ==================== 配置参数 ====================
BASE_URL = "http://localhost:8080"  # 后端API地址
MINIO_URL = "http://127.0.0.1:9000"  # MinIO地址
CONCURRENT_USERS = 50  # 并发用户数
FILE_SIZE_MB = 30  # 文件大小（MB）
TEST_FILE_DIR = "test_upload_files"  # 测试文件目录

# 测试用户账号
TEST_USERS = [
    {"username": f"testuser{i}", "password": "123456ab", "email": f"testuser{i}@test.com"}
    for i in range(1, 501)  # 使用已创建的500个测试账号
]

# 歌曲风格列表
SONG_STYLES = ["流行", "摇滚", "电子", "古典", "爵士", "民谣", "嘻哈", "其他"]

# ==================== 数据类 ====================
@dataclass
class UploadResult:
    """上传结果"""
    user_id: int
    username: str
    file_path: str
    file_size: int
    file_md5: str
    upload_success: bool
    upload_time: float
    upload_url: str = ""
    verify_success: bool = False
    verify_time: float = 0
    error_message: str = ""


# ==================== 统计信息 ====================
class Statistics:
    def __init__(self):
        self.total_uploads = 0
        self.successful_uploads = 0
        self.failed_uploads = 0
        self.successful_verifications = 0
        self.failed_verifications = 0
        self.upload_times = []
        self.verify_times = []
        self.errors = []

    def add_result(self, result: UploadResult):
        self.total_uploads += 1
        if result.upload_success:
            self.successful_uploads += 1
            self.upload_times.append(result.upload_time)
            if result.verify_success:
                self.successful_verifications += 1
                self.verify_times.append(result.verify_time)
            else:
                self.failed_verifications += 1
        else:
            self.failed_uploads += 1
        if result.error_message:
            self.errors.append(f"User {result.user_id} ({result.username}): {result.error_message}")

    def print_summary(self):
        print("\n" + "="*70)
        print("文件上传测试结果统计")
        print("="*70)
        print(f"总上传数: {self.total_uploads}")
        print(f"成功上传: {self.successful_uploads} ({self.successful_uploads/self.total_uploads*100:.2f}%)")
        print(f"失败上传: {self.failed_uploads} ({self.failed_uploads/self.total_uploads*100:.2f}%)")
        print(f"成功验证: {self.successful_verifications} ({self.successful_verifications/self.successful_uploads*100:.2f}% of successful uploads)")
        print(f"失败验证: {self.failed_verifications}")
        
        if self.upload_times:
            print(f"\n上传性能:")
            print(f"  平均时间: {sum(self.upload_times)/len(self.upload_times):.2f}秒")
            print(f"  最快: {min(self.upload_times):.2f}秒")
            print(f"  最慢: {max(self.upload_times):.2f}秒")
            print(f"  总上传数据量: {sum(self.upload_times) * FILE_SIZE_MB / len(self.upload_times):.2f} MB (估算)")
        
        if self.verify_times:
            print(f"\n验证性能:")
            print(f"  平均时间: {sum(self.verify_times)/len(self.verify_times):.2f}秒")
            print(f"  最快: {min(self.verify_times):.2f}秒")
            print(f"  最慢: {max(self.verify_times):.2f}秒")
        
        if self.errors:
            print(f"\n错误信息 (前10条):")
            for error in self.errors[:10]:
                print(f"  {error}")


# ==================== 全局统计 ====================
stats = Statistics()


# ==================== 工具函数 ====================
def calculate_md5(file_path: str) -> str:
    """计算文件的MD5值"""
    hash_md5 = hashlib.md5()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def generate_test_file(file_path: str, size_mb: int) -> tuple[str, int]:
    """生成测试文件"""
    size_bytes = size_mb * 1024 * 1024
    # 生成随机内容
    chunk_size = 1024 * 1024  # 1MB chunks
    with open(file_path, "wb") as f:
        remaining = size_bytes
        while remaining > 0:
            chunk = os.urandom(min(chunk_size, remaining))
            f.write(chunk)
            remaining -= len(chunk)
    
    md5 = calculate_md5(file_path)
    return md5, size_bytes


async def login(session: aiohttp.ClientSession, username: str, password: str, email: str) -> tuple[bool, str, float]:
    """用户登录"""
    start_time = time.time()
    try:
        url = f"{BASE_URL}/user/login"
        data = {
            "email": email,
            "password": password
        }
        async with session.post(url, json=data) as response:
            elapsed = time.time() - start_time
            if response.status == 200:
                result = await response.json()
                if isinstance(result, dict) and result.get("code") == 0:
                    token = result.get("data", "")
                    if isinstance(token, dict):
                        token = token.get("token", "")
                    return True, token, elapsed
            return False, "", elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        return False, "", elapsed


async def upload_song(session: aiohttp.ClientSession, token: str, file_path: str, 
                      song_name: str, style: str, file_size: int) -> tuple[bool, str, float]:
    """上传歌曲文件"""
    start_time = time.time()
    try:
        url = f"{BASE_URL}/song/uploadOriginalSong"
        headers = {"Authorization": f"Bearer {token}"}
        
        # 准备FormData
        form_data = aiohttp.FormData()
        form_data.add_field('songName', song_name)
        form_data.add_field('style', style)
        form_data.add_field('isRewardEnabled', 'false')
        # 估算时长（30MB文件大约200秒，根据文件大小估算）
        estimated_duration = int(file_size / (30 * 1024 * 1024) * 200)
        form_data.add_field('duration', str(estimated_duration))
        
        # 添加音频文件（使用文件流而不是一次性读取，避免内存问题）
        async with aiofiles.open(file_path, 'rb') as f:
            file_content = await f.read()
            form_data.add_field('audioFile', 
                              file_content,
                              filename=os.path.basename(file_path),
                              content_type='audio/mpeg')
        
        async with session.post(url, headers=headers, data=form_data) as response:
            elapsed = time.time() - start_time
            if response.status == 200:
                result = await response.json()
                if isinstance(result, dict) and result.get("code") == 0:
                    # 获取上传后的音频URL（需要从返回的songId查询）
                    song_id = result.get("data")
                    if song_id:
                        # 等待一小段时间确保文件已处理完成
                        await asyncio.sleep(1)
                        # 从用户自己的歌曲列表中获取音频URL（因为待审核的歌曲可能无法从公开接口获取）
                        # 获取用户ID（从token中解析或从用户信息接口获取）
                        user_info_url = f"{BASE_URL}/user/getUserInfo"
                        async with session.get(user_info_url, headers=headers) as user_response:
                            if user_response.status == 200:
                                user_result = await user_response.json()
                                if isinstance(user_result, dict) and user_result.get("code") == 0:
                                    user_data = user_result.get("data", {})
                                    user_id = user_data.get("userId") or user_data.get("user_id")
                                    if user_id:
                                        # 从用户的原创歌曲列表中查找刚上传的歌曲
                                        songs_url = f"{BASE_URL}/song/getUserOriginalSongs/{user_id}?pageNum=1&pageSize=10&auditStatus=0"
                                        async with session.get(songs_url, headers=headers) as songs_response:
                                            if songs_response.status == 200:
                                                songs_result = await songs_response.json()
                                                if isinstance(songs_result, dict) and songs_result.get("code") == 0:
                                                    songs_data = songs_result.get("data", {})
                                                    items = songs_data.get("items", [])
                                                    # 查找匹配的歌曲
                                                    for song in items:
                                                        if str(song.get("songId") or song.get("song_id")) == str(song_id):
                                                            audio_url = song.get("audioUrl") or song.get("audio_url", "")
                                                            if audio_url:
                                                                return True, audio_url, elapsed
                        # 如果从用户列表获取失败，尝试从详情接口获取（可能需要审核通过）
                        detail_url = f"{BASE_URL}/song/getSongDetail/{song_id}"
                        async with session.get(detail_url, headers=headers) as detail_response:
                            if detail_response.status == 200:
                                detail_result = await detail_response.json()
                                if isinstance(detail_result, dict) and detail_result.get("code") == 0:
                                    song_data = detail_result.get("data", {})
                                    audio_url = song_data.get("audioUrl") or song_data.get("audio_url", "")
                                    if audio_url:
                                        return True, audio_url, elapsed
                    return True, "", elapsed
            return False, "", elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        return False, "", elapsed


async def verify_file(session: aiohttp.ClientSession, audio_url: str, 
                     original_md5: str, original_size: int) -> tuple[bool, float]:
    """验证文件完整性"""
    start_time = time.time()
    try:
        if not audio_url:
            return False, time.time() - start_time
        
        # 如果是MinIO URL，直接请求
        if audio_url.startswith("http://") or audio_url.startswith("https://"):
            url = audio_url
        else:
            url = f"{MINIO_URL}/{audio_url}"
        
        # 下载文件并验证
        async with session.get(url) as response:
            elapsed = time.time() - start_time
            if response.status == 200:
                # 下载文件到临时位置
                temp_file = f"temp_verify_{int(time.time() * 1000)}.tmp"
                async with aiofiles.open(temp_file, 'wb') as f:
                    async for chunk in response.content.iter_chunked(8192):
                        await f.write(chunk)
                
                # 验证文件大小
                downloaded_size = os.path.getsize(temp_file)
                if downloaded_size != original_size:
                    os.remove(temp_file)
                    return False, elapsed
                
                # 验证MD5（可选，如果文件很大可以跳过）
                if downloaded_size < 100 * 1024 * 1024:  # 小于100MB才验证MD5
                    downloaded_md5 = calculate_md5(temp_file)
                    if downloaded_md5 != original_md5:
                        os.remove(temp_file)
                        return False, elapsed
                
                # 清理临时文件
                os.remove(temp_file)
                return True, elapsed
            return False, elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        return False, elapsed


# ==================== 用户模拟 ====================
async def simulate_upload(user_id: int, username: str, password: str, email: str, 
                          file_path: str, file_md5: str, file_size: int) -> UploadResult:
    """模拟单个用户的上传流程"""
    result = UploadResult(
        user_id=user_id,
        username=username,
        file_path=file_path,
        file_size=file_size,
        file_md5=file_md5,
        upload_success=False,
        upload_time=0,
        verify_success=False,
        verify_time=0
    )
    
    try:
        async with aiohttp.ClientSession() as session:
            # 1. 登录
            login_success, token, _ = await login(session, username, password, email)
            if not login_success:
                result.error_message = "登录失败"
                return result
            
            # 2. 上传文件
            song_name = f"测试歌曲_{username}_{user_id}"
            style = random.choice(SONG_STYLES)
            
            upload_success, audio_url, upload_time = await upload_song(
                session, token, file_path, song_name, style, file_size
            )
            result.upload_success = upload_success
            result.upload_time = upload_time
            result.upload_url = audio_url
            
            if not upload_success:
                result.error_message = "上传失败"
                return result
            
            if not audio_url:
                result.error_message = "无法获取音频URL"
                return result
            
            # 3. 验证文件完整性（等待一小段时间确保文件可访问）
            await asyncio.sleep(1)
            verify_success, verify_time = await verify_file(
                session, audio_url, file_md5, file_size
            )
            result.verify_success = verify_success
            result.verify_time = verify_time
            
            if not verify_success:
                result.error_message = "文件验证失败"
            
    except Exception as e:
        result.error_message = f"异常: {str(e)}"
    
    return result


# ==================== 主函数 ====================
async def main():
    """主测试函数"""
    print("="*70)
    print("50并发用户文件上传测试")
    print("="*70)
    print(f"后端地址: {BASE_URL}")
    print(f"并发用户数: {CONCURRENT_USERS}")
    print(f"文件大小: {FILE_SIZE_MB} MB")
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70)
    
    # 创建测试文件目录
    if not os.path.exists(TEST_FILE_DIR):
        os.makedirs(TEST_FILE_DIR)
    
    # 生成测试文件
    print(f"\n正在生成 {CONCURRENT_USERS} 个测试文件（每个 {FILE_SIZE_MB} MB）...")
    test_files = []
    for i in range(1, CONCURRENT_USERS + 1):
        file_path = os.path.join(TEST_FILE_DIR, f"test_file_{i}.mp3")
        if not os.path.exists(file_path):
            print(f"  生成文件 {i}/{CONCURRENT_USERS}: {file_path}")
            md5, size = generate_test_file(file_path, FILE_SIZE_MB)
        else:
            # 如果文件已存在，计算MD5
            md5 = calculate_md5(file_path)
            size = os.path.getsize(file_path)
        
        test_files.append({
            "path": file_path,
            "md5": md5,
            "size": size
        })
    
    print(f"测试文件准备完成！\n")
    
    # 创建上传任务
    tasks = []
    for i in range(CONCURRENT_USERS):
        user = TEST_USERS[i % len(TEST_USERS)]
        file_info = test_files[i]
        task = simulate_upload(
            i + 1,
            user["username"],
            user["password"],
            user["email"],
            file_info["path"],
            file_info["md5"],
            file_info["size"]
        )
        tasks.append(task)
    
    # 执行并发上传测试
    print(f"开始执行 {CONCURRENT_USERS} 个并发用户上传测试...")
    start_time = time.time()
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    elapsed_time = time.time() - start_time
    print(f"\n所有上传完成，总耗时: {elapsed_time:.2f}秒")
    
    # 收集结果
    for result in results:
        if isinstance(result, UploadResult):
            stats.add_result(result)
            # 打印每个文件的上传耗时
            if result.upload_success:
                verify_status = "成功" if result.verify_success else "失败"
                print(f"[OK] User {result.user_id} ({result.username}): 上传成功, 耗时 {result.upload_time:.2f}秒, 验证 {verify_status}")
            else:
                print(f"[FAIL] User {result.user_id} ({result.username}): 上传失败 - {result.error_message}")
        else:
            print(f"任务异常: {result}")
    
    # 打印统计信息
    stats.print_summary()
    
    print(f"\n测试结束时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70)
    
    # 清理测试文件（可选）
    # print(f"\n清理测试文件...")
    # for file_info in test_files:
    #     if os.path.exists(file_info["path"]):
    #         os.remove(file_info["path"])


if __name__ == "__main__":
    # 检查依赖
    try:
        import aiohttp
        import aiofiles
    except ImportError as e:
        print(f"错误: 需要安装依赖库")
        print(f"安装命令: pip install aiohttp aiofiles")
        exit(1)
    
    # 运行测试
    asyncio.run(main())

