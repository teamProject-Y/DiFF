<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="ARTICLE LIST"></c:set>
<%@ include file="../common/head.jspf"%>

<button onclick="history.back()" class="block text-4xl pl-10 cursor-pointer">
    <i class="fa-solid fa-angle-left"></i>
</button>

<div class="container mx-auto my-10 w-5/6">
	<span class="text-4xl font-bold m-4">
		Article list
	</span>
    <div class="flex justify-between items-end text-neutral-800 mx-2 my-3">
        <div class="articleCnt font-normal mx-4">
            총 게시 글 : ${totalCnt }
        </div>

        <div class="search-bar flex justify-between items-center h-8 px-2 text-sm mt-auto mb-0">
            <form class="search-box flex" action="list" method="POST">

                <label for="boardId" class="flex">
                    <select name="boardId" id="boardId"
                            class="block flex justify-center items-center px-4 border border-solid border-neutral-500 rounded-lg overflow-hidden">
                        <option value="0" disabled ${boardId eq 0 ? 'selected' : ''} hidden class="text-neutral-400">게시판 선택</option>
                        <option value="0">전체 게시판</option>
                        <option value="1" ${boardId eq 1 ? 'selected' : ''}>공지사항</option>
                        <option value="2" ${boardId eq 2 ? 'selected' : ''}>자유 게시판</option>
                        <option value="3" ${boardId eq 3 ? 'selected' : ''}>QnA</option>
                    </select>
                </label>

                <label class="input border border-solid border-neutral-500 mx-2 overflow-hidden rounded-md pl-0 pr-3">
                    <select name="searchItem" id="searchItem"
                            class="block h-full pl-3 pr-1 flex justify-center items-center overflow-hidden ">
                        <!-- 					    <option value="0" disabled hidden class="text-neutral-400">검색 기준</option> -->
                        <option value="1" ${searchItem eq 1 ? 'selected' : ''}>제목</option>
                        <option value="2" ${searchItem eq 2 ? 'selected' : ''}>내용</option>
                        <option value="3" ${searchItem eq 3 ? 'selected' : ''}>작성자</option>
                    </select>
                    <input type="text" name="keyword" placeholder="Search"  value="${not empty keyword ? keyword : ''}"/>
                </label>

                <button type="submit" class="btn block flex items-center justify-center p-4 bg-neutral-800 text-neutral-200 text-base font-large rounded-md hover:bg-neutral-700">
                    <svg class="h-[1em]" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
                        <g stroke-linejoin="round" stroke-linecap="round" stroke-width="2.5" fill="none" stroke="currentColor">
                            <circle cx="11" cy="11" r="8"></circle>
                            <path d="m21 21-4.3-4.3"></path>
                        </g>
                    </svg>
                </button>

                <a href="write"
                   class="block ml-6 px-5 whitespace-nowrap text-base flex items-center justify-center font-large rounded-md hover:bg-neutral-300">
                    글 작성</a>
            </form>

        </div>
    </div>


    <div class="container text-center">
        <!-- 		article table -->
        <div class="mx-auto my-4 border-solid border rounded-xl overflow-hidden">
            <table class="w-full text-sm text-center rtl:text-right text-neutral-800">
                <thead class="h-5 text-base text-neutral-200 bg-neutral-800">
                <tr>
                    <th scope="col" class="px-6 py-4">NO</th>
                    <th scope="col" class="px-6 py-4">BOARD</th>
                    <th scope="col" class="px-6 py-4 w-1/2">TITLE</th>
                    <th scope="col" class="px-6 py-4">WRITER</th>
                    <th scope="col" class="px-6 py-4">sumReaction</th>
                    <th scope="col" class="px-6 py-4">goodReaction</th>
                    <th scope="col" class="px-6 py-4">badReaction</th>
                    <th scope="col" class="px-6 py-4">HITS</th>
                    <th scope="col" class="px-6 py-4 whitespace-nowrap">REGISTATION DATE</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="article" items="${articles }">
                    <tr class="border-b bg-neutral-200 border-neutral-300 hover:bg-neutral-300">
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.id }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.extra_boardCode }</td>
                        <td onclick="location.href='detail?id=${article.id}'"><a class="block text-left pl-6" href="detail?id=${article.id }">${article.title }</a></td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.extra_writer }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.extra_sumReactionPoint }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.extra_goodReactionPoint }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.extra_badReactionPoint }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.hits }</td>
                        <td class="px-5 py-3" onclick="location.href='detail?id=${article.id}'">${article.regDate.toString().substring(0, 10) }</td>
                    </tr>
                </c:forEach>
                <c:if test="${empty articles }">
                    <tr class="text-center">
                        <td colspan="5" class="text-center text-lg py-8">게시글이 없습니다</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <!-- 		pagenation -->
        <div class="inline-flex mx-auto justify-between text-xl">


            <c:forEach var="i" begin="1" end="${totalPage}">
                <c:choose>
                    <c:when test="${page eq i}">
                        <a href="?keyword=${keyword}&boardId=${boardId}&searchItem=${searchItem}&page=${i}"
                           class="block mx-1 w-8 px-2 py-1 hover:underline bg-neutral-200 rounded-full">${i}</a>
                    </c:when>
                    <c:otherwise>
                        <a href="?keyword=${keyword}&boardId=${boardId}&searchItem=${searchItem}&page=${i}"
                           class="block mx-1 w-8 py-1 hover:underline">${i}</a>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </div>
    </div>
</div>

<%@ include file="../common/foot.jspf"%>