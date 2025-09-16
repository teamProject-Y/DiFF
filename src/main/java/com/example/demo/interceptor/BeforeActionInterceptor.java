package com.example.demo.interceptor;

import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.Rq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BeforeActionInterceptor implements HandlerInterceptor {

    @Autowired
    private Rq rq;

    @Autowired
    private MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {

        if (rq.isLogined()) {
            Member member = memberService.getMemberById((long) rq.getLoginedMemberId());
            rq.setLoginedMember(member);
        }

        return true;
    }
}