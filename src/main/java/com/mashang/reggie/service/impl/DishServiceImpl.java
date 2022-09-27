package com.mashang.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.dto.DishDto;
import com.mashang.reggie.entity.Dish;
import com.mashang.reggie.entity.DishFlavor;
import com.mashang.reggie.mapper.DishMapper;
import com.mashang.reggie.service.DishFlavorService;
import com.mashang.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    /**
     * 新增菜品,同时保存对应的口味数据
     *
     * @param dishDto
     */
    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    @Override
    @Transactional // 设计到多张表的操作,用事务控制,想要让这个注解生效,需要在启动类上开启事务支持
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品的基本信息到菜品表dish
        this.save(dishDto);
        // 菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        List<DishFlavor> flavors1 = new ArrayList<>();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishDto.getId());
            flavors1.add(flavor);
        }
        // 保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors1);
    }

    /**
     * 根据id查询菜品信息以及对应的口味信息
     *
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavors(Long id) {
        // 查询菜品基本信息,从dish表查询
        // this.getById(id) == dishService.getById(id);
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        // 通过copy,就把普通的属性,也就是菜品里的其他属性赋值给了dishdto
        BeanUtils.copyProperties(dish, dishDto);

        // 查询当前菜品对应的口味信息,从dish_flavor查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        // 将口味赋值给dishdto
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    @Override
    @Transactional  //操作多张表,加入事务注解,保证一致性
    public void updateWithFlavor(DishDto dishDto) {
        // 1.更新dish表基本信息
        dishService.updateById(dishDto);

        // 清理当前菜品对应口味数据--dish flavor表的delete操作
        // 这里其实要发送的sql是delete from dish_flavor where dish_id = ???
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        // 2.添加当前提交过来的口味数据--dish_flavor表的insert操作
        // 口味数据在dto里就已经封装了
        // 问题和新增的一模一样,没有dishId
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        // 批量保存
        dishFlavorService.saveBatch(flavors);
    }
}
