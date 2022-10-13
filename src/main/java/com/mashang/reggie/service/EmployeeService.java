package com.mashang.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mashang.reggie.common.R;
import com.mashang.reggie.entity.Employee;

import javax.servlet.http.HttpServletRequest;

public interface EmployeeService extends IService<Employee> {

    R<Employee> login(HttpServletRequest request, Employee employee);

    R<String> logout(HttpServletRequest request);

    R<String> add(HttpServletRequest request, Employee employee);

    R<Page> employeePage(int page, int pageSize, String name);

    R<String> employeeUpdate(HttpServletRequest request, Employee employee);

    R<Employee> employeeGetById(Long id);
}
