import asyncio
import aiohttp

async def test_login():
    async with aiohttp.ClientSession() as session:
        url = "http://localhost:8080/user/login"
        data = {
            "email": "testuser1@test.com",
            "password": "123456ab"
        }
        async with session.post(url, json=data) as response:
            print(f"Status: {response.status}")
            result = await response.json()
            print(f"Response: {result}")
            if result.get("code") == 0:
                print("✓ 登录成功")
            else:
                print(f"✗ 登录失败: {result.get('message')}")

asyncio.run(test_login())

