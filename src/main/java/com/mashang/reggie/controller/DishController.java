package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.dto.DishDto;
import com.mashang.reggie.entity.Category;
import com.mashang.reggie.entity.Dish;
import com.mashang.reggie.entity.DishFlavor;
import com.mashang.reggie.service.CategoryService;
import com.mashang.reggie.service.DishFlavorService;
import com.mashang.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品和菜品口味的操作统一放到这个controller里
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    /***
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息的分页
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        // 1.构造分页构造器对象,泛型指定为实体
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        // 1.构造条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        lambdaQueryWrapper.like(name != null, Dish::getName, name);
        // 添加排序条件,根据更新时间降序排列
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo, lambdaQueryWrapper);
        // 对象拷贝
        // 前一个参数是源,后一个参数是目的,第三个参数是忽略的属性
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            // 1.先创建基本的dto对象,因为没有dto表,所以不能直接对Dishdto进行操作
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }
    // 返回的对象是什么,要看前端页面需要什么
    // 前端需要什么样的数据,我们返回的就是什么

    /**
     * 根据id查询菜品信息和对应的口味信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    // id是在请求的url里,所以要用@PathVariable变量
    // DishDto里有口味
    public R<DishDto> get(@PathVariable Long id) {
        // 不能直接用dishservice查,dishservice只能查到菜品表,我们这里还需要口味数据
        DishDto dishDto = dishService.getByIdWithFlavors(id);
        return R.success(dishDto);

    }

    /***
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        // 更新涉及到口味和菜品两张表
        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     *
     * @param dish
     * @return
     */
    //dish里面就有categoryId,参数直接传dish比较好,通用性比单单传一个categoryId好
    @GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 添加一个状态是1的,在售的
        queryWrapper.eq(Dish::getStatus,1);
        //添加一个排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        return R.success(list);
    }

}

