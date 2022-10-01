package com.mashang.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.dto.DishDto;
import com.mashang.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {
    // 新增菜品,同时插入菜品对应的口味数据,需要操作两张表:dish dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    // 根据id查询菜品信息以及对应的口味信息
    public DishDto getByIdWithFlavors(Long id);

    // 更新菜品信息以及对应的口味信息
    void updateWithFlavor(DishDto dishDto);

    // 逻辑删除 批量删除菜品
    void deleteByIds(List<Long> ids);
}
