package com.mashang.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.common.BaseContext;
import com.mashang.reggie.common.CustomException;
import com.mashang.reggie.entity.*;
import com.mashang.reggie.mapper.OrderMapper;
import com.mashang.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 下单功能,一共操作了三张表,1.向订单表插入数据 2.向订单明细表插入数据 3.清空购物车表的数据
     *
     * @param orders
     */
    // 涉及到多张表,事务控制
    @Transactional
    @Override
    public void submit(Orders orders) {
        // 1.获得当前用户id
        Long userId = BaseContext.getCurrentId();

        // 2.查询当前用户的购物车数据,要想查购物车数据,要注入购物车的service
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // ShoppingCart::getUserId 这是数据库中字段对应的实体类, 这个eq方法相当于把userId赋值给前面这个实体类
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, userId);
        // .list方法是返回一个满足.list(条件)的集合
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(lambdaQueryWrapper);
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空,不能下单");
        }
        // 查询用户数据
        User user = userService.getById(userId);
        // 查询地址数据
        // 页面发送请求到服务器的时候,已经把addressId放在数据里了
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误,不能下单");
        }
        // 3.向订单表插入数据,一条数据(需要根据userId将用户信息传进来,等下需要user的电话等信息
        // 前端传回来的属性太少,这里要一个个设置这些属性
        long orderId = IdWorker.getId();// MP提供的一个类,直接拿来作为id,订单号

        // 原子操作,保证在多线程的情况下计算也没问题
        AtomicInteger amount = new AtomicInteger(0); // amount,具备原子性

        //解决amount 把购物车数据遍历一遍,金额一个个累加
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber()); // 当前菜品或套餐的份数
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount()); // 单份的金额
            // addAndGet是当前进行累加的方法
            // item.getAmount单份的金额 multiply 乘法的方法
            // item.getNumber份数
            // 最后计算出来当前菜品或套餐最后总的金额
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        // 这里number是订单号 不是数量
        orders.setNumber(String.valueOf(orderId)); // 需要的是string类型,不过提供的是long型,需要string.valueof
        orders.setUserId(userId);
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2); // 订单状态 1待付款，2待派送，3已派送，4已完成，5已取消
        orders.setUserName(user.getName());
        // consigner寄件人 consignee收件人
        orders.setConsignee(addressBook.getConsignee());
        orders.setAmount(new BigDecimal(amount.get()));  // 计算总金额
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        // 向订单表中插入一条数据
        this.save(orders);
        // 4.向订单明细表插入数据,可能是多条数据
        orderDetailService.saveBatch(orderDetails);
        // 5.清空购物车数据
        shoppingCartService.remove(lambdaQueryWrapper);
    }
}
