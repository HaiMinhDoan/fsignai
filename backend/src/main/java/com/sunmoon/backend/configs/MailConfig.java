package com.sunmoon.backend.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    // Đọc từ biến môi trường (MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD) để đổi được
    // App Password khi nó hết hạn mà không phải build lại ảnh, và để mật khẩu không nằm trong image.
    // Giá trị mặc định giữ nguyên như cũ nên chạy ở máy cá nhân không cần khai báo gì thêm.
    @Value("${mail.host:smtp.gmail.com}")
    private String host;

    @Value("${mail.port:587}")
    private int port;

    @Value("${mail.username:sale@slmsolar.com}")
    private String username;

    @Value("${mail.password:vvdl swax vcny type}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS bắt buộc với port 587
        props.put("mail.debug", "false");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com"); // Rất quan trọng
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return mailSender;
    }

}