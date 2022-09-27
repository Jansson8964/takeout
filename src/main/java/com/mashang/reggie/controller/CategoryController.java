package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.entity.Category;
import com.mashang.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    // 添加requestbody注解才能拿到json数据
    public R<String> save(@RequestBody Category category) {
        log.info("category:{}", category);
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 分类信息的分页查询
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {
        log.info("page = {},pageSize={}", page, pageSize);
        // 分页构造器
        Page<Category> pageInfo = new Page(page, pageSize);
        // 条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper();
        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Category::getSort);
        // 进行分页查询
        categoryService.page(pageInfo, lambdaQueryWrapper);
        return R.success(pageInfo);
    }
    /**
     * 信息删除
     */
    /**
     * 根据id删除分类
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    // 参数里不用写responseBody,因为传过来的值是category?ids=1413384954989060097这种形式的
    public R<String> delete(Long ids) {
        log.info("删除分类,id为{}", ids);
        // categoryService.removeById(ids);
        categoryService.remove(ids);
        return R.success("删除成功");
    }

    /**
     * 根据id修改分类信息
     *
     * @param category
     * @return
     */
    @PutMapping
    // 前端传过来是json格式,这里要写requestbody的注解
    public R<String> update(@RequestBody Category category) {
        log.info("修改分类信息:{}", category);
        categoryService.updateById(category);
        return R.success("修改分类信息成功");

    }

    /**
     * 根据条件来查询分类数据
     *
     * @param category
     * @return
     */
    @GetMapping("/list")
    // 返回一个list集合 集合里的元素是category
    public R<List<Category>> list(Category category) {
        // 条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加条件
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        // 添加排序条件
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        List<Category> list = categoryService.list(queryWrapper);
        return R.success(list);

    }
}
