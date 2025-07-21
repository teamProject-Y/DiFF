package com.example.demo.vo;

import java.io.IOException;
import java.security.Principal;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import util.Ut;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Getter
@Setter
public class Rq {

    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final HttpSession session;

    private Member loginedMember;
    private boolean isLogined = false;
    private long loginedMemberId = 0;
    private String loginedMemberNickName;


    public Rq(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
        this.session = req.getSession();

        if (session.getAttribute("loginedMemberId") != null) {
            isLogined = true;
            loginedMemberId = (long) session.getAttribute("loginedMemberId");
        }

        this.req.setAttribute("rq", this);
    }
    public void setLoginedMember(Member member) {
        if (member == null) {
            return;
        }

        this.loginedMember = member;
        System.out.println("rq");
        this.loginedMemberId = (int) member.getId();
        this.loginedMemberNickName= member.getNickName();
        System.out.println("Nickname = " + loginedMember.getNickName());
        System.out.println("Nickname = " + loginedMemberNickName);
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

    public void logout() {
        session.invalidate(); // ✅ 전체 세션 무효화
        this.loginedMember = null;
        this.isLogined = false;
        this.loginedMemberId = 0;
        this.loginedMemberNickName = null;
    }


    public void login(Member member) {
        session.setAttribute("loginedMemberId", member.getId());

    }

    public void initBeforeActionInterceptor() {
        System.err.println("initBeforeActionInterceptor 실행됨");
    }

    public String historyBackOnView(String msg) {
        req.setAttribute("msg", msg);
        req.setAttribute("historyBack", true);
        return "usr/common/js";
    }

    public String getCurrentUri() {
        String currentUri = req.getRequestURI();
        String queryString = req.getQueryString();

        System.out.println(currentUri);
        System.out.println(queryString);

        if (currentUri != null && queryString != null) {
            currentUri += "?" + queryString;
        }

        return currentUri;
    }


}