<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="ARTICLE MODIFY"></c:set>
<%@ include file="../common/head.jspf"%>

<button onclick="history.back()" class="block text-4xl pl-10 cursor-pointer">
    <i class="fa-solid fa-angle-left"></i>
</button>

<div class="container mx-auto">
    <div class="title text-neutral-800 text-4xl font-bold mx-2 my-6">
	<span>
	Article ${article.id} Modify
	</span>
    </div>
    <div class="border bg-neutral-100 border-neutral-400 rounded-3xl px-8 py-5">
        <form action="doModify" method="POST">
            <div style="display: flex; flex-direction: column; justify-content: center;">
                <input type="hidden" name="id" value="${article.id}">
                제목
                <div class="border border-neutral-500 border-solid rounded-md overflow-hidden">
                    <input class="w-full p-2" required="required" type="text" name="title" value="${article.title}">
                </div>
                <br>
                내용
                <div class="border border-neutral-500 border-solid rounded-md overflow-hidden">
                    <textarea class="w-full p-2"  required="required" name="body">${article.body}</textarea>
                </div>
                <br>
                <div class="flex justify-end">
                    <button class="border border-neutral-400 rounded-lg px-3 py-2 hover:bg-neutral-300"
                            type="submit">수정하기</button>
                </div>
            </div>
        </form>
    </div>
</div>
<%@ include file="../common/foot.jspf"%>