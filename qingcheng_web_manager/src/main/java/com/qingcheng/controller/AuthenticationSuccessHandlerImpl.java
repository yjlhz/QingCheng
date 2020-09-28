package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.system.LoginLog;
import com.qingcheng.service.system.LoginLogService;
import com.qingcheng.util.WebUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Reference
    private LoginLogService loginLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录后调用
        System.out.println("登录成功");

        String name = authentication.getName();//获取登录管理员名称
        String ip = request.getRemoteAddr();//获取ip
        String agent = request.getHeader("user-agent");

        LoginLog loginLog = new LoginLog();
        loginLog.setLoginName(name);//当前登录管理员
        loginLog.setLoginTime(new Date());//当前登录时间
        loginLog.setIp(ip);//远程客户端ip
        loginLog.setLocation(WebUtil.getCityByIP(ip));//根据ip得到地区
        loginLog.setBrowserName(WebUtil.getBrowserName(agent));//浏览器名称
        loginLogService.add(loginLog);//插入数据库

        //跳转页面,转发
        request.getRequestDispatcher("/main.html").forward(request,response);

    }
}
