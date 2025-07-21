<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="MEMBER JOIN"></c:set>
<%@ include file="../common/head.jspf"%>


<div class="container mx-auto mt-12 max-w-min p-4 bg-neutral-200 border border-solid border-neutral-300 rounded-lg">
    <div class="title my-3 text-center text-2xl font-semibold">
        Join
    </div>
    <form onsubmit="return validate();" name="join" action="doJoin" method="POST">

        <div calss="flex flex-col justify-center">

            <input type="text" name="loginId" id="loginId" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96 p-2.5" placeholder="ID">
            <!-- 			<div class="text-neutral-400 mb-6 px-2">Enter at least 4 letters and numbers</div> -->
            <input type="password" name="loginPw" id="loginPw" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="Password">
            <!-- 			<div class="text-neutral-400 mb-6 px-2">Enter at least 4 letters and numbers</div> -->
            <input type="password" name="checkLoginPw" id="checkLoginPw" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="Password Check">
            <!-- 			<div class="text-neutral-400 mb-6 px-2">Enter your password again to confirm it</div> -->
            <input type="text" name="name" id="name" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="Name">
            <input type="text" name="nickName" id="nickName" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="NickName">
            <input type="text" name="cellPhone" id="cellPhone" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="cell-phone">
            <input type="email" name="email" id="email" class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="e-mail">
        </div>
        <button type="submit" class="py-2.5 px-5 me-2 mb-2 w-96 text-sm font-large bg-neutral-800 text-neutral-200 rounded-lg hover:bg-neutral-700">Join</button>
    </form>
    <div class="sub-menu text-center my-4 flex justify-center">
        <a class="hover:text-underline" href="login">Login</a>
    </div>
</div>

<%@ include file="../common/foot.jspf"%>