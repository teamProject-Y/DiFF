package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.interceptor.NeedLoginInterceptor;
import com.example.demo.interceptor.NeedLogoutInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebMvcConfigurer implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

    @Autowired
    BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    NeedLoginInterceptor needLoginInterceptor;

    @Autowired
    NeedLogoutInterceptor needLogoutInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration ir;

        // 로그인된 사용자 정보 세팅용
        ir = registry.addInterceptor(beforeActionInterceptor);
        ir.addPathPatterns("/**");
        ir.excludePathPatterns("/resource/**");
        ir.excludePathPatterns("/error");
        ir.excludePathPatterns("/favicon.ico");

        // 로그인 필요한 요청
        ir = registry.addInterceptor(needLoginInterceptor);
        ir.addPathPatterns("/usr/article/write");
        ir.addPathPatterns("/usr/article/doWrite");
        ir.addPathPatterns("/usr/article/modify");
        ir.addPathPatterns("/usr/article/doModify");
        ir.addPathPatterns("/usr/article/doDelete");
        ir.addPathPatterns("/usr/member/doLogout");
        ir.addPathPatterns("/usr/reply/doWrite");
        ir.addPathPatterns("/usr/reactionPoint/doGoodReaction");
        ir.addPathPatterns("/usr/reactionPoint/doBadReaction");

        // 로그인 상태에서는 접근 금지
        ir = registry.addInterceptor(needLogoutInterceptor);
        ir.addPathPatterns("/usr/member/login");
        ir.addPathPatterns("/usr/member/doLogin");
        ir.addPathPatterns("/usr/member/join");
        ir.addPathPatterns("/usr/member/doJoin");
    }



}