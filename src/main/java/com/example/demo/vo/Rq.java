package com.example.demo.vo;

import java.io.IOException;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.MemberService;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import com.example.util.Ut;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class Rq {

    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final HttpSession session;

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;

    private Member loginedMember;
    private boolean isLogined = false;
    private long loginedMemberId = 0;
    private String loginedMemberNickName;
    private String accessToken;

    public Rq(HttpServletRequest req, HttpServletResponse resp, JwtTokenProvider jwtTokenProvider, @Lazy MemberService memberService) {
        this.req = req;
        this.resp = resp;
        this.session = req.getSession();
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberService = memberService;

        // 세션 기반 로그인 체크
        if (session.getAttribute("loginedMemberId") != null) {
            isLogined = true;
            loginedMemberId = (long) session.getAttribute("loginedMemberId");
            Member member = memberService.getMemberById(loginedMemberId);
            setLoginedMember(member);
        }

        // JWT 토큰 기반 로그인 체크
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
            try {
                if (jwtTokenProvider.validateToken(accessToken)) {
                    loginedMemberId = jwtTokenProvider.getMemberIdFromToken(accessToken);
                    Member member = memberService.getMemberById(loginedMemberId);
                    if (member != null) {
                        setLoginedMember(member);
                        isLogined = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("JWT 토큰 검증 실패: " + e.getMessage());
            }
        }

        this.req.setAttribute("rq", this);
    }
    public void setLoginedMember(Member member) {
        if (member == null) {
            return;
        }

        this.loginedMember = member;
        this.loginedMemberId = member.getId();
        this.loginedMemberNickName= member.getNickName();
    }


    public void printHistoryBack(String msg) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        println("<script>");
        if (!Ut.isEmpty(msg)) {
            println("alert('" + msg.replace("'", "\\'") + "');");
        }
        println("history.back();");
        println("</script>");
        resp.getWriter().flush();
        resp.getWriter().close();
    }

    private void println(String str) throws IOException {
        print(str + "\n");
    }

    private void print(String str) throws IOException {
        resp.getWriter().append(str);
    }

    public void login(Member member) {
        session.setAttribute("loginedMemberId", member.getId());
    }

}