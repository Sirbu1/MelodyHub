package cn.edu.seig.vibemusic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置类
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.protocol}")
    private String protocol;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private String starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable}")
    private String sslEnable;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.port}")
    private String socketFactoryPort;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.class}")
    private String socketFactoryClass;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
    private String connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout}")
    private String timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout}")
    private String writeTimeout;

    @Bean
    public JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", smtpAuth);
        properties.setProperty("mail.smtp.starttls.enable", starttlsEnable);
        properties.setProperty("mail.smtp.starttls.required", starttlsRequired);
        properties.setProperty("mail.smtp.ssl.enable", sslEnable);
        properties.setProperty("mail.smtp.socketFactory.port", socketFactoryPort);
        properties.setProperty("mail.smtp.socketFactory.class", socketFactoryClass);
        properties.setProperty("mail.smtp.connectiontimeout", connectionTimeout);
        properties.setProperty("mail.smtp.timeout", timeout);
        properties.setProperty("mail.smtp.writetimeout", writeTimeout);

        mailSender.setJavaMailProperties(properties);
        return mailSender;
    }
}
