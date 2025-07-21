<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="MY PAGE"></c:set>
<%@ include file="../common/head.jspf"%>

<button onclick="history.back()" class="block text-4xl pl-10 cursor-pointer">
    <i class="fa-solid fa-angle-left"></i>
</button>

<div class="container w-11/12 mx-auto mt-16 p-4">
    <div class="flex items-end m-2">
        <div class="avatar m-2">
            <div class="w-24 rounded-lg">
                <img src="https://img.daisyui.com/images/profile/demo/batperson@192.webp" />
            </div>
        </div>
        <div class=" text-xl m-2">
            <strong>${member.nickName} 님</strong> 반갑습니다.
        </div>
        <div class="flex-grow"></div>
        <div class="updateProfile">
            <a href="modify" class="block p-3 text-base flex items-center
		 	justify-center font-large rounded-md hover:bg-neutral-300 border border-neutral-300">
                회원 정보 수정</a>
        </div>
    </div>

    <div class="mt-10 px-2">
        <div class="m-3 text-lg font-bold">회원 정보</div>
        <table class="bg-neutral-300 rounded-xl">
            <tr>
                <th class="px-6 py-4">이름</th>
                <td class="px-10 py-4">${member.name}</td>
            </tr>
            <tr>
                <th class="px-6 py-4">전화번호</th>
                <td class="px-10 py-4">${member.cellPhone}</td>
            </tr>
            <tr>
                <th class="px-6 py-4">이메일</th>
                <td class="px-10 py-4">${member.email}</td>
            </tr>
        </table>
    </div>
</div>

<%@ include file="../common/foot.jspf"%>