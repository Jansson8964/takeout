package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.dto.DishDto;
import com.mashang.reggie.dto.SetmealDto;
import com.mashang.reggie.entity.Category;
import com.mashang.reggie.entity.Dish;
import com.mashang.reggie.entity.Setmeal;
import com.mashang.reggie.entity.SetmealDish;
import com.mashang.reggie.service.CategoryService;
import com.mashang.reggie.service.DishService;
import com.mashang.reggie.service.SetmealDishService;
import com.mashang.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;

    /**
     * 保存套餐
     *
     * @return
     */
    @PostMapping
    // SetmealDto 在setmeal的基础上又额外扩充了一下集合
    // 用@RequestBody来接收前端提交的json数据
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        // 新增和修改的返回值一般都是R<String>
        log.info("接收的参数为:{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        // 分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();
        // 构造条件查询器
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        // 阿帕奇的stringUtils才有isNotEmpty方法
        // 根据name进行like模糊查询
        lambdaQueryWrapper.eq(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, lambdaQueryWrapper);
        // 对象拷贝

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map((item) -> {
            // item是遍历出来的每一个套餐(setmeal)实体
            // 根据分类id查询分类对象
            Category category = categoryService.getById(item.getCategoryId());
            SetmealDto setmealDto = new SetmealDto();
            if (category != null) {
                // 分类名称
                String categoryName = category.getName();
                // 上面那个拷贝是处理records以外的信息，现在这个拷贝是将records里面的范型setmeal中的信息拷贝到新的范型setmealdto中来
                // 把records里遍历出来的item拷贝到setmealDto里
                BeanUtils.copyProperties(item, setmealDto);
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto; //不要忘记把dto return回去
        }).collect(Collectors.toList()); //上面整块都是map方法的参数,这里需要用collect收集起来
        dtoPage.setRecords(list);
        // 返回dtoPage对象
        return R.success(dtoPage);
    }

    /**
     * 修改套餐
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info(setmealDto.toString());
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");

    }


    /**
     * 根据id查询套餐
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDishes(id);
        return R.success(setmealDto);

    }

    /**
     * 删除套餐
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    // 注解@RequestParam接收的参数是来自requestHeader中，即请求头
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids:{}", ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 启售停售功能
     * 批量启售批量停售
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        setmealService.updateSetmealStatusById(status, ids);
        return R.success("修改售卖状态成功!");
    }

    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        // 构造查询条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        lambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(lambdaQueryWrapper);
        return R.success(list);
    }

    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 这里返回的是dto 对象，因为前端需要copies这个属性
     * 前端主要要展示的信息是:套餐中菜品的基本信息，图片，菜品描述，以及菜品的份数
     *
     * @param SetmealId
     * @return
     */
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long SetmealId) {
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId, SetmealId);

        List<SetmealDish> list = setmealDishService.list(lambdaQueryWrapper);

        List<DishDto> dishDtos = list.stream().map((setmealDish) -> {
            DishDto dishDto = new DishDto();
            //其实这个BeanUtils的拷贝是浅拷贝，这里要注意一下
            BeanUtils.copyProperties(setmealDish, dishDto);
            //这里是为了把套餐中的菜品的基本信息填充到dto中，比如菜品描述，菜品图片等菜品的基本信息
            Long dishId = setmealDish.getDishId();
            Dish dish = dishService.getById(dishId);
            BeanUtils.copyProperties(dish, dishDto);
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dishDtos);
    }
}
