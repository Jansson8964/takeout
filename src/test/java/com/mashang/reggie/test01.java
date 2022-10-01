package com.mashang.reggie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

@SpringBootTest
@RunWith(SpringRunner.class)

public class test01 {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testString() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("city123", "tokyo");
    }

    @Test
    public void testList() {
        ListOperations listOperations = redisTemplate.opsForList();

        // 存值
        listOperations.leftPush("mylist","a");
        listOperations.leftPushAll("mylist","b","c","d");

        // 取值
        List<String> mylist = listOperations.range("mylist",0,-1);
        for (String value : mylist) {
            System.out.println(value);
        }
        // 获得列表长度 llength
        Long size = listOperations.size("mylist");

        // 出队列
        Object element = listOperations.rightPop("mylist");
    }

//    @Test
//    public void testSet(){
//        SetOperations setOperations = redisTemplate.opsForSet();
//        // 存值
//        setOperations.add("myset","a","b","c","a");
//        // 取值
//        Set<String> myset = setOperations.members("myset");
//        for (String s : myset) {
//            System.out.println(s);
//        }
//        // 删除成员
//        setOperations.remove("myset","a","b");
//        myset = setOperations.members("myset");
//        for (String s : myset) {
//            System.out.println(s);
//        }
//    }

}
