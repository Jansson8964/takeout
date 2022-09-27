package com.mashang.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mashang.reggie.common.R;
import com.mashang.reggie.entity.Employee;
import com.mashang.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     *
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    // 前端传过来是JSON格式的,后端接收的时候需要用 @RequestBody注解
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        // request的用处:登录成功之后需要把employee对象的id放到session域里,表示登录成功
        /**
         * 先分析自己处理的逻辑,具体的步骤梳理好了再进行编码
         * 1.将页面提交的密码password进行md5加密处理
         * 2.根据页面提交的用户名username查询数据库
         * 3.如果没有查询到结果则返回登录失败结果
         * 4.密码比对,如果不一致则返回登录失败结果
         * 5.查看员工状态,如果为已禁用状态,则返回员工已禁用结果
         * 6.登录成功,将员工id存入session并返回登录成功结果
         */

        //1.将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        // 进行md5加密
        password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        // 2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 比较二者 数据库里username有唯一约束unique,可以用getOne方法
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        // 3.如果没有查询到结果则返回登录失败结果
        if (emp == null) {
            return R.error("登录失败");
        }
        // 4.查到有这个用户,进行密码的比对,如果不成功则返回登陆失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("登录失败");
        }
        // 5.查看员工状态,如果为已禁用状态,则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("账号已禁用");
        }
        // 6.登录成功,将员工存入Session并返回登录成功结果
        // request.getSession() 获取session对象
        // void setAttribute(String name, Object o) 存储数据
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    /**
     * 员工退出
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        // 要操作session 就要request对象
        // 1.清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        // 2.返回退出成功的信息
        return R.success("退出成功");
    }

    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        log.info("新增员工,员工信息:{}", employee.toString());
        // 设置md5加密的初始密码123456
        // 需要用.getBytes方法变成byte数组
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));
        // 设置创建时间,用当前系统的创建时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        // 获得当前登录用户的id
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);
        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /***
     * 员工信息的分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        log.info("page = {},pageSize={},name={}", page, pageSize, name);
        // 构造分页构造器
        // page为1代表查第一页,pagesize为10代表查10条
        Page pageInfo = new Page(page, pageSize);
        // 构造条件构造器
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper();
        // 添加过滤条件
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo, lambdaQueryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     *
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        log.info(employee.toString());
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(empId);
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");
    }

    @GetMapping("{id}")
    public R<Employee> getById(@PathVariable Long id) {
        log.info("根据id查询员工信息............");
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }
}
