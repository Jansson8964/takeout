package com.mashang.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.common.CustomException;
import com.mashang.reggie.dto.SetmealDto;
import com.mashang.reggie.entity.DishFlavor;
import com.mashang.reggie.entity.Setmeal;
import com.mashang.reggie.entity.SetmealDish;
import com.mashang.reggie.mapper.SetmealMapper;
import com.mashang.reggie.service.SetmealDishService;
import com.mashang.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    @Transactional // 加入事务注解保证一致性
    public void saveWithDish(SetmealDto setmealDto) {
        // 保存套餐的基本信息,操作setmeal,执行insert操作
        setmealService.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        })).collect(Collectors.toList());

        // 保存套餐和菜品的关联信息,操作setmeal_dish表
        // 关联关系可能有多条,需要批量保存
        setmealDishService.saveBatch(setmealDishes);

    }

    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        setmealService.updateById(setmealDto);
        // 1.清理当前套餐对应菜品数据--setmeal dish表的delete操作
        // 这里其实要发送的sql是delete from setmeal dish where setmeal_id = ???
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        // 删除setmeal里的dish信息  DELETE FROM setmeal_dish WHERE (setmeal_id = ?)
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        // 2.添加当前提交过来的套餐数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        // 批量保存
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 根据id查询套餐详细信息
     *
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDishes(Long id) {
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
        List<SetmealDish> dishes = setmealDishService.list(lambdaQueryWrapper);
        setmealDto.setSetmealDishes(dishes);
        return setmealDto;
    }

    /**
     * 删除套餐,同时删除关联的菜品数据
     *
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        // select count(*) from setmeal where id in (1,2,3) and status =1;
        // 查询套餐状态,确定是否可以删除
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Setmeal::getId, ids);
        lambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        int count = this.count(lambdaQueryWrapper);
        if (count > 0) {
            // 如果状态为售卖中,不能删除,抛出一个业务异常
            throw new CustomException("套餐正在售卖中,不能删除");
        }
        // 如果可以删除,先删除套餐表中的数据
        this.removeByIds(ids);
        // 再删除关系表中的数据
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.in(SetmealDish::getSetmealId, ids);
        setmealService.remove(lambdaQueryWrapper);

    }
}

