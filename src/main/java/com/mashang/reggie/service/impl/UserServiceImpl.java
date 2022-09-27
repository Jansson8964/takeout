package com.mashang.reggie.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.entity.User;
import com.mashang.reggie.mapper.UserMapper;
import com.mashang.reggie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Value("${spring.mail.username}")
    private String from;   // 邮件发送人

    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public void sendMsg(String to, String subject, String context) {
        //发送简单邮件，简单邮件不包括附件等别的
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(from);
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(context);

        // 真正的发送邮件操作，从 from到 to
        javaMailSender.send(mailMessage);

    }
}
