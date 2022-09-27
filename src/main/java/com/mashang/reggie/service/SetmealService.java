package com.mashang.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.dto.SetmealDto;
import com.mashang.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    // 将套餐基本信息和关联的菜品信息一块保存了

    /**
     * 新增套餐,同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 修改套餐,涉及到套餐和菜品的关联关系
     * @param setmealDto
     */
    void updateWithDish(SetmealDto setmealDto);

    /**
     * 根据id查询套餐以及对应的菜品信息
     */
    SetmealDto getByIdWithDishes(Long id);

    /**
     * 删除套餐,同时需要删除套餐和菜品的关联数据
     * @param ids
     */
    void removeWithDish(List<Long> ids);
}
