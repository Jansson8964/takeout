package com.mashang.reggie.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {
    /***
     * 下单
     * @param orders
     */
    void submit(Orders orders);
}
