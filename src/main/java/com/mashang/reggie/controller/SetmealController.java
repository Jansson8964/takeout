package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.dto.SetmealDto;
import com.mashang.reggie.entity.Category;
import com.mashang.reggie.entity.Setmeal;
import com.mashang.reggie.service.CategoryService;
import com.mashang.reggie.service.SetmealDishService;
import com.mashang.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Select;
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
        log.info("ids:{}",ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }
}
