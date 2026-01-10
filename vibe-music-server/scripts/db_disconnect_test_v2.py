#!/usr/bin/env python3
"""
数据库断连测试脚本 V2
模拟100用户进行收藏、评论操作，测试数据库断连场景下的系统行为

测试流程：
1. 100用户进行收藏、评论操作
2. 在测试过程中手动断开数据库连接（需要管理员权限执行: net stop MySQL80）
3. 观察用户操作反馈
4. 手动恢复数据库连接（执行: net start MySQL80）
5. 检查断连期间的操作数据是否完整

使用方法:
    python db_disconnect_test_v2.py
    
注意：
    - 需要管理员权限来停止/启动MySQL服务
    - 或者可以手动在另一个终端执行: net stop MySQL80 和 net start MySQL80
"""

import asyncio
import aiohttp
import time
import json
import random
import subprocess
from typing import List, Dict
from dataclasses import dataclass
from datetime import datetime

# ==================== 配置参数 ====================
BASE_URL = "http://localhost:8080"  # 后端API地址
CONCURRENT_USERS = 100  # 并发用户数
DB_DISCONNECT_DURATION = 60  # 数据库断连持续时间（秒）
DB_HOST = "localhost"
DB_PORT = 3306
DB_USER = "root"
DB_PASSWORD = "3127"

# 测试用户账号
TEST_USERS = [
    {"username": f"testuser{i}", "password": "123456ab", "email": f"testuser{i}@test.com"}
    for i in range(1, 501)
]

# 测试歌曲ID（需要从数据库获取）
TEST_SONG_IDS = []

# ==================== 数据类 ====================
@dataclass
class OperationResult:
    """操作结果"""
    user_id: int
    username: str
    operation_type: str  # "collect" 或 "comment"
    operation_id: int  # song_id
    success: bool
    response_time: float
    error_message: str = ""
    timestamp: float = 0
    db_connected: bool = True  # 操作时数据库是否连接
    http_status: int = 200


# ==================== 统计信息 ====================
class Statistics:
    def __init__(self):
        self.total_operations = 0
        self.successful_operations = 0
        self.failed_operations = 0
        self.operations_during_disconnect = 0
        self.successful_during_disconnect = 0
        self.failed_during_disconnect = 0
        self.response_times = []
        self.errors = []
        self.operations = []  # 记录所有操作
        self.collect_operations = []
        self.comment_operations = []

    def add_result(self, result: OperationResult):
        self.total_operations += 1
        self.operations.append(result)
        if result.operation_type == "collect":
            self.collect_operations.append(result)
        else:
            self.comment_operations.append(result)
        
        if result.success:
            self.successful_operations += 1
            self.response_times.append(result.response_time)
        else:
            self.failed_operations += 1
        if not result.db_connected:
            self.operations_during_disconnect += 1
            if result.success:
                self.successful_during_disconnect += 1
            else:
                self.failed_during_disconnect += 1
        if result.error_message:
            self.errors.append(f"User {result.user_id} ({result.username}): {result.operation_type} - {result.error_message}")

    def print_summary(self):
        print("\n" + "="*70)
        print("数据库断连测试结果统计")
        print("="*70)
        print(f"总操作数: {self.total_operations}")
        print(f"成功操作: {self.successful_operations} ({self.successful_operations/self.total_operations*100:.2f}%)")
        print(f"失败操作: {self.failed_operations} ({self.failed_operations/self.total_operations*100:.2f}%)")
        print(f"\n断连期间操作:")
        print(f"  总操作数: {self.operations_during_disconnect}")
        print(f"  成功: {self.successful_during_disconnect}")
        print(f"  失败: {self.failed_during_disconnect}")
        if self.operations_during_disconnect > 0:
            success_rate = self.successful_during_disconnect / self.operations_during_disconnect * 100
            print(f"  成功率: {success_rate:.2f}%")
        
        print(f"\n操作类型统计:")
        print(f"  收藏操作: {len(self.collect_operations)}")
        print(f"  评论操作: {len(self.comment_operations)}")
        
        if self.response_times:
            print(f"\n响应性能:")
            print(f"  平均时间: {sum(self.response_times)/len(self.response_times):.2f}秒")
            print(f"  最快: {min(self.response_times):.2f}秒")
            print(f"  最慢: {max(self.response_times):.2f}秒")
        
        if self.errors:
            print(f"\n错误信息 (前10条):")
            for error in self.errors[:10]:
                print(f"  {error}")


# ==================== 全局统计 ====================
stats = Statistics()
db_disconnected = False
disconnect_start_time = 0
reconnect_time = 0


# ==================== 数据库操作 ====================
def check_db_connection():
    """检查数据库连接状态"""
    try:
        import pymysql
        conn = pymysql.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database="vibe_music",
            connect_timeout=2
        )
        conn.close()
        return True
    except:
        return False


# ==================== API 函数 ====================
async def login(session: aiohttp.ClientSession, username: str, password: str, email: str):
    """用户登录"""
    try:
        url = f"{BASE_URL}/user/login"
        data = {"email": email, "password": password}
        async with session.post(url, json=data) as response:
            if response.status == 200:
                result = await response.json()
                if isinstance(result, dict) and result.get("code") == 0:
                    token = result.get("data", "")
                    if isinstance(token, dict):
                        token = token.get("token", "")
                    return True, token
            return False, ""
    except Exception as e:
        return False, ""


async def collect_song(session: aiohttp.ClientSession, token: str, song_id: int) -> tuple[bool, float, str, bool, int]:
    """收藏歌曲"""
    start_time = time.time()
    is_disconnected = db_disconnected
    http_status = 200
    try:
        url = f"{BASE_URL}/favorite/collectSong"
        headers = {"Authorization": f"Bearer {token}"}
        params = {"songId": song_id}
        async with session.post(url, headers=headers, params=params, timeout=aiohttp.ClientTimeout(total=30)) as response:
            elapsed = time.time() - start_time
            http_status = response.status
            if response.status == 200:
                result = await response.json()
                if isinstance(result, dict) and result.get("code") == 0:
                    return True, elapsed, "", is_disconnected, http_status
                else:
                    error_msg = result.get("message", "未知错误") if isinstance(result, dict) else "响应格式错误"
                    return False, elapsed, error_msg, is_disconnected, http_status
            else:
                text = await response.text()
                return False, elapsed, f"HTTP {response.status}: {text[:100]}", is_disconnected, http_status
    except asyncio.TimeoutError:
        elapsed = time.time() - start_time
        return False, elapsed, "请求超时", is_disconnected, 0
    except Exception as e:
        elapsed = time.time() - start_time
        return False, elapsed, f"异常: {str(e)}", is_disconnected, 0


async def add_comment(session: aiohttp.ClientSession, token: str, song_id: int, content: str) -> tuple[bool, float, str, bool, int]:
    """添加评论"""
    start_time = time.time()
    is_disconnected = db_disconnected
    http_status = 200
    try:
        url = f"{BASE_URL}/comment/addSongComment"
        headers = {"Authorization": f"Bearer {token}"}
        data = {
            "songId": song_id,
            "content": content
        }
        async with session.post(url, headers=headers, json=data, timeout=aiohttp.ClientTimeout(total=30)) as response:
            elapsed = time.time() - start_time
            http_status = response.status
            if response.status == 200:
                result = await response.json()
                if isinstance(result, dict) and result.get("code") == 0:
                    return True, elapsed, "", is_disconnected, http_status
                else:
                    error_msg = result.get("message", "未知错误") if isinstance(result, dict) else "响应格式错误"
                    return False, elapsed, error_msg, is_disconnected, http_status
            else:
                text = await response.text()
                return False, elapsed, f"HTTP {response.status}: {text[:100]}", is_disconnected, http_status
    except asyncio.TimeoutError:
        elapsed = time.time() - start_time
        return False, elapsed, "请求超时", is_disconnected, 0
    except Exception as e:
        elapsed = time.time() - start_time
        return False, elapsed, f"异常: {str(e)}", is_disconnected, 0


# ==================== 用户模拟 ====================
async def simulate_user_operations(user_id: int, username: str, password: str, email: str, 
                                  song_ids: List[int], operation_count: int = 10):
    """模拟单个用户的操作（收藏和评论）"""
    results = []
    
    try:
        async with aiohttp.ClientSession() as session:
            # 登录
            login_success, token = await login(session, username, password, email)
            if not login_success:
                return results
            
            # 执行多次操作（收藏和评论交替）
            for i in range(operation_count):
                song_id = random.choice(song_ids)
                operation_type = "collect" if i % 2 == 0 else "comment"
                
                if operation_type == "collect":
                    success, response_time, error_msg, was_disconnected, http_status = await collect_song(session, token, song_id)
                else:
                    content = f"测试评论_{username}_{i}_{int(time.time())}"
                    success, response_time, error_msg, was_disconnected, http_status = await add_comment(session, token, song_id, content)
                
                result = OperationResult(
                    user_id=user_id,
                    username=username,
                    operation_type=operation_type,
                    operation_id=song_id,
                    success=success,
                    response_time=response_time,
                    error_message=error_msg,
                    timestamp=time.time(),
                    db_connected=not was_disconnected,
                    http_status=http_status
                )
                results.append(result)
                
                # 操作间隔
                await asyncio.sleep(0.2)
    
    except Exception as e:
        print(f"用户 {user_id} 操作异常: {e}")
    
    return results


# ==================== 数据完整性检查 ====================
def check_data_integrity():
    """检查数据完整性"""
    print("\n" + "="*70)
    print("数据完整性检查")
    print("="*70)
    
    # 统计断连期间的操作
    disconnect_ops = [op for op in stats.operations if not op.db_connected]
    successful_during_disconnect = [op for op in disconnect_ops if op.success]
    failed_during_disconnect = [op for op in disconnect_ops if not op.success]
    
    print(f"断连期间总操作数: {len(disconnect_ops)}")
    print(f"断连期间成功操作: {len(successful_during_disconnect)}")
    print(f"断连期间失败操作: {len(failed_during_disconnect)}")
    
    # 等待数据库完全恢复
    print("\n等待数据库连接稳定...")
    for i in range(5):
        if check_db_connection():
            break
        time.sleep(1)
    
    # 检查数据库中的实际数据
    try:
        import pymysql
        conn = pymysql.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database="vibe_music"
        )
        cursor = conn.cursor()
        
        # 检查收藏数据
        cursor.execute("SELECT COUNT(*) FROM tb_user_favorite WHERE create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)")
        favorite_count = cursor.fetchone()[0]
        print(f"\n最近10分钟新增收藏数: {favorite_count}")
        
        # 检查评论数据
        cursor.execute("SELECT COUNT(*) FROM tb_comment WHERE create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)")
        comment_count = cursor.fetchone()[0]
        print(f"最近10分钟新增评论数: {comment_count}")
        
        # 检查是否有重复数据（收藏）
        cursor.execute("""
            SELECT user_id, song_id, COUNT(*) as cnt 
            FROM tb_user_favorite 
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
            GROUP BY user_id, song_id 
            HAVING cnt > 1
        """)
        duplicate_favorites = cursor.fetchall()
        if duplicate_favorites:
            print(f"\n[WARNING] 发现 {len(duplicate_favorites)} 条重复收藏记录")
            for dup in duplicate_favorites[:5]:
                print(f"  用户 {dup[0]} 对歌曲 {dup[1]} 有 {dup[2]} 条记录")
        else:
            print(f"\n[OK] 未发现重复收藏记录")
        
        # 检查是否有重复数据（评论）
        cursor.execute("""
            SELECT user_id, song_id, content, COUNT(*) as cnt 
            FROM tb_comment 
            WHERE create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
            GROUP BY user_id, song_id, content 
            HAVING cnt > 1
        """)
        duplicate_comments = cursor.fetchall()
        if duplicate_comments:
            print(f"\n[WARNING] 发现 {len(duplicate_comments)} 条重复评论记录")
        else:
            print(f"[OK] 未发现重复评论记录")
        
        # 统计操作成功率
        expected_collects = len([op for op in stats.collect_operations if op.success])
        expected_comments = len([op for op in stats.comment_operations if op.success])
        
        print(f"\n操作统计:")
        print(f"  预期收藏数（成功操作）: {expected_collects}")
        print(f"  实际收藏数（数据库）: {favorite_count}")
        print(f"  预期评论数（成功操作）: {expected_comments}")
        print(f"  实际评论数（数据库）: {comment_count}")
        
        if abs(favorite_count - expected_collects) <= 10:  # 允许10个误差
            print(f"\n[OK] 收藏数据完整性检查通过（差异: {abs(favorite_count - expected_collects)}）")
        else:
            print(f"\n[WARNING] 收藏数据可能不完整，差异: {abs(favorite_count - expected_collects)}")
        
        if abs(comment_count - expected_comments) <= 10:
            print(f"[OK] 评论数据完整性检查通过（差异: {abs(comment_count - expected_comments)}）")
        else:
            print(f"[WARNING] 评论数据可能不完整，差异: {abs(comment_count - expected_comments)}")
        
        # 检查断连期间成功操作的数据是否保存
        disconnect_successful = [op for op in disconnect_ops if op.success]
        if disconnect_successful:
            print(f"\n断连期间成功操作分析:")
            print(f"  成功操作数: {len(disconnect_successful)}")
            print(f"  这些操作可能通过以下方式保存:")
            print(f"    1. 连接池重试机制")
            print(f"    2. 事务回滚后重试")
            print(f"    3. 缓存机制（如果使用）")
        
        cursor.close()
        conn.close()
        
    except ImportError:
        print("\n注意: 需要安装 pymysql 库才能进行数据完整性检查")
        print("安装命令: pip install pymysql")
    except Exception as e:
        print(f"\n数据完整性检查失败: {e}")
        import traceback
        traceback.print_exc()


# ==================== 主函数 ====================
async def main():
    """主测试函数"""
    global TEST_SONG_IDS, db_disconnected, disconnect_start_time
    
    print("="*70)
    print("数据库断连测试")
    print("="*70)
    print(f"后端地址: {BASE_URL}")
    print(f"并发用户数: {CONCURRENT_USERS}")
    print(f"每个用户操作数: 10 (5次收藏 + 5次评论)")
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70)
    print("\n重要提示:")
    print("  1. 测试开始10秒后，请在另一个终端执行: net stop MySQL80")
    print("  2. 等待60秒后，执行: net start MySQL80")
    print("  3. 或者脚本会自动检测数据库状态\n")
    
    # 获取测试歌曲ID
    print("正在获取测试歌曲ID...")
    try:
        async with aiohttp.ClientSession() as session:
            url = f"{BASE_URL}/song/getRecommendedSongs"
            async with session.get(url) as response:
                if response.status == 200:
                    result = await response.json()
                    if isinstance(result, dict) and result.get("code") == 0:
                        songs = result.get("data", [])
                        TEST_SONG_IDS = [song.get("songId") or song.get("song_id") for song in songs[:20] if song.get("songId") or song.get("song_id")]
    except Exception as e:
        print(f"获取歌曲ID失败: {e}")
        TEST_SONG_IDS = list(range(2, 22))
    
    if not TEST_SONG_IDS:
        TEST_SONG_IDS = list(range(2, 22))
    
    print(f"使用 {len(TEST_SONG_IDS)} 首歌曲进行测试\n")
    
    # 创建任务列表
    tasks = []
    for i in range(CONCURRENT_USERS):
        user = TEST_USERS[i % len(TEST_USERS)]
        task = simulate_user_operations(i + 1, user["username"], user["password"], user["email"], TEST_SONG_IDS, 10)
        tasks.append(task)
    
    # 启动测试任务
    print(f"开始执行 {CONCURRENT_USERS} 个用户的操作测试...")
    print(f"[{datetime.now().strftime('%H:%M:%S')}] 测试开始，10秒后请断开数据库连接\n")
    
    # 监控数据库连接状态
    async def monitor_db_status():
        global db_disconnected, disconnect_start_time
        last_status = True
        while True:
            await asyncio.sleep(2)
            current_status = check_db_connection()
            if current_status != last_status:
                if not current_status and last_status:
                    db_disconnected = True
                    disconnect_start_time = time.time()
                    print(f"\n[{datetime.now().strftime('%H:%M:%S')}] [检测] 数据库连接已断开")
                elif current_status and not last_status:
                    global reconnect_time
                    reconnect_time = time.time()
                    db_disconnected = False
                    duration = reconnect_time - disconnect_start_time if disconnect_start_time > 0 else 0
                    print(f"\n[{datetime.now().strftime('%H:%M:%S')}] [检测] 数据库连接已恢复 (断连时长: {duration:.2f}秒)")
            last_status = current_status
    
    # 启动监控任务
    monitor_task = asyncio.create_task(monitor_db_status())
    
    # 等待所有操作完成
    print(f"[{datetime.now().strftime('%H:%M:%S')}] 等待所有操作完成...")
    all_results = await asyncio.gather(*tasks)
    monitor_task.cancel()
    
    # 收集所有结果
    for user_results in all_results:
        for result in user_results:
            stats.add_result(result)
    
    # 打印统计信息
    stats.print_summary()
    
    # 数据完整性检查
    check_data_integrity()
    
    print(f"\n测试结束时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*70)


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

