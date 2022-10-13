package com.mashang.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mashang.reggie.common.R;
import com.mashang.reggie.entity.Employee;
import com.mashang.reggie.mapper.EmployeeMapper;
import com.mashang.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
    @Autowired
    private EmployeeService employeeService;

    @Override
    public R<Employee> login(HttpServletRequest request, Employee employee) {
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
            return R.error("账号不存在,登录失败");
        }
        // 4.查到有这个用户,进行密码的比对,如果不成功则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("密码错误,请输入正确的密码");
        }
        // 5.查看员工状态,如果为已禁用状态,则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("该账号已被禁用,登录失败");
        }
        // 6.登录成功,将员工id存入Session并返回登录成功结果
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
    @Override
    public R<String> logout(HttpServletRequest request) {
        // 要操作session 就要request对象
        // 1.清理Session中保存的当前登录员工的id
        request.getSession().removeAttribute("employee");
        // 2.返回退出成功的信息
        return R.success("退出成功");
    }

    /**
     * 新增员工
     *
     * @param request
     * @param employee
     * @return
     */
    @Override
    public R<String> add(HttpServletRequest request, Employee employee) {
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

    /**
     * 员工分页
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @Override
    public R<Page> employeePage(int page, int pageSize, String name) {
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
     * 修改员工信息
     *
     * @param request
     * @param employee
     * @return
     */
    @Override
    public R<String> employeeUpdate(HttpServletRequest request, Employee employee) {
        log.info(employee.toString());
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(empId);
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");
    }

    /**
     * 根据id查询员工信息
     *
     * @param id
     * @return
     */
    @Override
    public R<Employee> employeeGetById(Long id) {
        log.info("根据id查询员工信息............");
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }
}
