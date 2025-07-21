<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="ARTICLE WRITE"></c:set>
<%@ include file="../common/head.jspf"%>

<button onclick="history.back()" class="block text-4xl pl-10 cursor-pointer">
    <i class="fa-solid fa-angle-left"></i>
</button>

<div class="container mx-auto mt-8 p-6 w-4/5 border border-solid border-neutral-300 rounded-xl overflow-hidden">


    <div class="font-semibold text-4xl text-neutral-800 px-1 py-6">Article Write</div>


    <form action="doWrite" method="POST" class="w-full">
        <div class="flex justify-center flex-col">
            <div class="flex px-3">
                <label for="boardId" class="flex mr-4">
                    <select name="boardId" id="boardId" required
                            class="block flex justify-center items-center px-8 border border-solid border-neutral-500 rounded-lg overflow-hidden">
                        <option value="0" disabled selected hidden class="text-neutral-400">게시판 선택</option>
                        <option value="1">공지사항</option>  <%-- ${memberId != 1 ? 'disabled' : ''} --%>
                        <option value="2">자유 게시판</option>
                        <option value="3">질문과 답변</option>
                    </select>
                </label>
                <div class="flex-grow">
                    <input class="w-full px-4 py-2 border border-neutral-500 border-solid rounded-lg " type="text" name="title" placeholder="제목을 입력하세요" />
                </div>
            </div>
            <br>
            <section>
					<textarea class="resize-none w-full h-96 p-4 border border-neutral-500 border-solid rounded-lg" name="body"
                              placeholder="내용을 입력하세요"></textarea>
            </section>
            <br>
        </div>

        <div class="text-right px-2 pb-2">
            <button type="submit" class="py-2.5 px-5 w-32 text-base font-large bg-neutral-800 text-neutral-200 rounded-lg hover:bg-neutral-700">Write</button>
        </div>

    </form>
</div>

<%@ include file="../common/foot.jspf"%>