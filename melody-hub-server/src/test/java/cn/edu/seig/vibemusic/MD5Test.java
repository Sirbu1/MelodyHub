package cn.edu.seig.vibemusic;

import org.springframework.util.DigestUtils;

public class MD5Test {
    public static void main(String[] args) {
        String password = "Test@1234";
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes());
        System.out.println("Password: " + password);
        System.out.println("MD5: " + md5);
        
        // 测试其他密码
        String[] passwords = {"123456", "password", "admin123", "test1234", "User@123"};
        for (String p : passwords) {
            System.out.println(p + " -> " + DigestUtils.md5DigestAsHex(p.getBytes()));
        }
    }
}

