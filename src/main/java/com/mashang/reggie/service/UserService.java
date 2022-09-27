package com.mashang.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.entity.User;

public interface UserService extends IService<User> {
    /**
     * 发送邮箱
     * @param to
     * @param subject
     * @param context
     */
    // 发送邮件
    void sendMsg(String to,String subject,String context);
}
