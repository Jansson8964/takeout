package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.common.SystemConstants;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    // 注入redistemplate对象操作redis
    @Autowired
    private RedisTemplate redisTemplate;

    /***
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
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
        // 清理所有菜品的缓存数据
        // 把所有keys开头的缓存都清理掉
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        // 清理某个分类下面的菜品缓存数据
        // String key = "dish_" + dish.getCategoryId() + "_status_" + dish.getStatus();
//        String key = "dish_"+dishDto.getCategoryId()+"_status_"+ SystemConstants.DISH_STATUS_NORMAL;
//        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

//    /**
//     * 根据条件查询对应的菜品数据
//     *
//     * @param dish
//     * @return
//     */
//    //dish里面就有categoryId,参数直接传dish比较好,通用性比单单传一个categoryId好
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish) {
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
//        // 添加一个状态是1的,在售的
//        queryWrapper.eq(Dish::getStatus,1);
//        //添加一个排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }

    /**
     * 根据条件查询对应的菜品数据
     *
     * @param dish
     * @return
     */
    // 参考上面的分页
    //dish里面就有categoryId,参数直接传dish比较好,通用性比单单传一个categoryId好
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dishDtoList = null;
        // 动态的构造key
        String key = "dish_" + dish.getCategoryId() + "_status_" + dish.getStatus();
        //先 从redis中获取缓存数据,查询的时候根据分类来查询,对应的每一个分类下的菜品对应一份缓存数据,缓存数据根据key区分
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        // 如果时第一次查询某个分类或菜品,肯定查不到
        if (dishDtoList != null) {
            // 如果存在,直接返回,无需查询数据库
            return R.success(dishDtoList);
        } else {
            // 如果不存在,需要查询数据库,将查询到的菜品数据返回到redis
            //构造查询条件
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
            // 添加一个状态是1的,在售的
            queryWrapper.eq(Dish::getStatus, 1);
            //添加一个排序条件
            queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
            List<Dish> list = dishService.list(queryWrapper);
            dishDtoList = list.stream().map((item) -> {
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
                // 得到菜品的Id
                Long dishId = item.getId();
                LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
                List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
                dishDto.setFlavors(dishFlavorList);
                return dishDto;
            }).collect(Collectors.toList());
        }
        //如果不存在,需要查询数据库,将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
        return R.success(dishDtoList);

    }

    // http://localhost:8964/dish/status/0?ids=1413385247889891330
    // @PathVariable:接收请求路径中占位符的值
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        //  log.info("status:{}", status); // log.info("ids:{}", ids);
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(ids != null, Dish::getId, ids);
        List<Dish> list = dishService.list(lambdaQueryWrapper);
        for (Dish dish : list) {
            if (dish != null) {
                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }
        return R.success("售卖状态更改成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
//        dishService.removeByIds(ids);
//        return R.success("删除成功");
        //删除菜品  这里的删除是逻辑删除
        dishService.deleteByIds(ids);
        //删除菜品对应的口味  也是逻辑删除
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(queryWrapper);
        return R.success("菜品删除成功");
    }
}

