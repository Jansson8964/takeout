package com.mashang.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.common.R;
import com.mashang.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    R<String> clean();
}
