package com.mashang.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j  //输出日志,方便调试,由lombook提供
@SpringBootApplication // springboot的启动类
@ServletComponentScan // 想要让过滤器生效,要在启动类上面加个注解
@EnableTransactionManagement // 开启事务注解的支持
@EnableCaching // 在启动类上开启缓存注解功能
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class, args);
        log.info("项目启动成功");
    }
}
