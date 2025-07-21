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
        ir.addPathPatterns("/DiFF/article/write");
        ir.addPathPatterns("/DiFF/article/doWrite");
        ir.addPathPatterns("/DiFF/article/modify");
        ir.addPathPatterns("/DiFF/article/doModify");
        ir.addPathPatterns("/DiFF/article/doDelete");
        ir.addPathPatterns("/DiFF/member/doLogout");
        ir.addPathPatterns("/DiFF/reply/doWrite");
        ir.addPathPatterns("/DiFF/reactionPoint/doGoodReaction");
        ir.addPathPatterns("/DiFF/reactionPoint/doBadReaction");

        // 로그인 상태에서는 접근 금지
        ir = registry.addInterceptor(needLogoutInterceptor);
        ir.addPathPatterns("/DiFF/member/login");
        ir.addPathPatterns("/DiFF/member/doLogin");
        ir.addPathPatterns("/DiFF/member/join");
        ir.addPathPatterns("/DiFF/member/doJoin");
    }



}