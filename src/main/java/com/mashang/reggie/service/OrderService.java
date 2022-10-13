package com.mashang.reggie.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.dto.OrdersDto;
import com.mashang.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {
    /***
     * 下单
     * @param orders
     */
    void submit(Orders orders);

    /**
     * 分页
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    Page<OrdersDto> page(int page, int pageSize, String number, String beginTime, String endTime);
}
