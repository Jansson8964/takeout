package com.mashang.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.common.CustomException;
import com.mashang.reggie.entity.Category;
import com.mashang.reggie.entity.Dish;
import com.mashang.reggie.entity.Setmeal;
import com.mashang.reggie.mapper.CategoryMapper;
import com.mashang.reggie.service.CategoryService;
import com.mashang.reggie.service.DishService;
import com.mashang.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
// 实现mapper接口和category这个实体类
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类
     *
     * @param ids
     */
    @Override
    public void remove(Long ids) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 查询当前分类是否关联了菜品,如果已经关联,抛出一个业务异常
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, ids);
        int count1 = dishService.count(dishLambdaQueryWrapper);
        if (count1 > 0) {
            // 已经关联菜品,抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品,不能删除");

        }
        // 查询当前分类是否关联了套餐,如果已经关联,抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件,根据分类id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, ids);
        int count2 = setmealService.count();
        if (count2 > 0) {
            // 已经关联套餐,抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐,不能删除");
        }

        // 正常删除分类
        // super继承ServiceImpl
        super.removeById(ids);

    }
}
