package com.mashang.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.mashang.reggie.common.BaseContext;
import com.mashang.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")  //过滤器需要加的注解
// urlPatterns : 具体拦截哪些路径 /* :拦截所有请求
@Slf4j
public class LoginCheckFilter implements Filter {
    // 路径匹配器,支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 实现doFilter 过滤的方法
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        /**
         * 1.获取本次请求的URI
         * 2.判断本次请求是否需要处理
         * 3.如果不需要处理,则直接放行
         * 4.判断登录状态,如果已登录,则直接放行
         * 5.如果未登录,则返回未登录结果
         */
        // 1.获取本次请求的URI
        String requestURI = request.getRequestURI();
        // uri 返回部分路径
        log.info("拦截到请求:{}", request.getRequestURI());
        // 2.判断本次请求是否需要处理
        //----------------------------------------------------------------------------
        // 定义一些请求路径,如果是这些路径就直接放行
        // 以下是放行的白名单
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",       //这些静态资源不用处理
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };

        boolean check = check(requestURI, urls);

        // 3.如果不需要处理,则直接放行
        if (check) {
            log.info("放行请求:{}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        // 4.判断登录状态,如果已登录,则直接放行
        if (request.getSession().getAttribute("employee") != null) {
            log.info("用户已登录,用户id为:{}", request.getSession().getAttribute("employee"));
            Long empId = (Long) request.getSession().getAttribute("employee");
            // 设置id
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request, response);
            return;
        }
        /**
         * 判断移动端登录状态
         */
        if (request.getSession().getAttribute("user") != null) {
            log.info("移动端用户已登录,用户id为:{}", request.getSession().getAttribute("user"));
            Long empId = (Long) request.getSession().getAttribute("user");
            // 设置id
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request, response);
            return;
        }

        // 5.如果未登录,则返回未登录结果
        // 通过输出流方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 进行路径匹配:检查本次匹配是否需要放行
     *
     * @param requestURI
     * @param urls
     * @return
     */
    public boolean check(String requestURI, String[] urls) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }// 整个for循环都结束了还没有返回就return false
        return false;
    }
}

