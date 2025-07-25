<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="MEMBER LOGIN"></c:set>
<%@ include file="../common/head.jspf"%>


<div class="container mx-auto mt-32 max-w-min p-4 bg-neutral-200 border border-solid border-neutral-300 rounded-lg">
    <div class="title mt-4 mb-8 text-center text-2xl font-semibold">
        Login
    </div>
    <form name="login" action="/usr/member/doLogin" method="POST">

        <!-- 로그인 실패 메시지 -->
        <c:if test="${not empty param.error}">
            <div class="text-red-500 text-center mb-4">❌ 로그인 실패: 아이디 또는 비밀번호를 확인하세요.</div>
        </c:if>

        <div style="display:flex; flex-direction:column; justify-content: center;">

            <input type="text" name="loginId" class="mb-6 bg-neutral-50 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96 p-2.5" placeholder="ID">
            <input type="text" name="loginPw" class="mb-6 bg-neutral-50 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="Password">

        </div>
        <button type="submit" class="py-2.5 px-5 me-2 mb-2 w-96 text-sm font-large bg-neutral-800 text-neutral-200 rounded-lg hover:bg-neutral-700">Login</button>
    </form>
    <div class="sub-menu text-center my-4 flex justify-center">
        <a class="hover:text-underline" href="join">Join</a>
        <!-- 			<a class="hover:text-underline"  href="#">Find ID</a> -->
        <!-- 			<a class="hover:text-underline"  href="#">Find Password</a>		 -->
    </div>



    <div class="mt-6">
        <a href="/oauth2/authorization/github"
           class="flex items-center justify-center gap-3 bg-black text-white py-2 px-4 rounded hover:bg-gray-800 transition">
            <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
                 alt="GitHub Logo" class="w-6 h-6">
            <span>GitHub로 로그인</span>
        </a>

        <div class="flex justify-center mt-4">
            <a href="/oauth2/authorization/google" class="gsi-material-button" >
                <div class="gsi-material-button-state"></div>
                <div class="gsi-material-button-content-wrapper">
                    <div class="gsi-material-button-icon">
                        <!-- 구글 로고 SVG -->
                        <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" xmlns:xlink="http://www.w3.org/1999/xlink" style="display: block;" >
                            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
                            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                            <path fill="none" d="M0 0h48v48H0z"></path>
                        </svg>
                    </div>
                    <span class="gsi-material-button-contents">Sign in with Google</span>
                    <span style="display: none;">Sign in with Google</span>
                </div>
            </a>
        </div>

    </div>

<%@ include file="../common/foot.jspf"%>