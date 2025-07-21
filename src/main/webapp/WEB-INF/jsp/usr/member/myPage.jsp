<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:set var="pageTitle" value="MYPAGE"></c:set>
<%@ include file="../common/head.jspf"%>
<hr />

<section class="mt-24 text-xl px-4">
    <div class="mx-auto">
        <table class="table" border="1" cellspacing="0" cellpadding="5" style="width: 100%; border-collapse: collapse;">
            <tbody>
            <tr>
                <th>가입일</th>
                <td style="text-align: center;">${rq.loginedMember.regDate }</td>

            </tr>
            <tr>
                <th>아이디</th>
                <td style="text-align: center;">${rq.loginedMember.loginId }</td>

            </tr>
            <tr>
                <th>이름</th>
                <td style="text-align: center;">${rq.loginedMember.name }</td>
            </tr>
            <tr>
                <th>닉네임</th>
                <td style="text-align: center;">${rq.loginedMember.nickname }</td>

            </tr>
            <tr>
                <th>이메일</th>
                <td style="text-align: center;">${rq.loginedMember.email }</td>

            </tr>
            <tr>
                <th>전화번호</th>
                <td style="text-align: center;">${rq.loginedMember.cellphoneNum }</td>
            </tr>
            <tr>
                <th>회원정보 수정</th>
                <td style="text-align: center;">
                    <a href="../member/checkPw" class="btn btn-primary">수정 </a>
                </td>
            </tr>
            </tbody>
        </table>
        <div class="btns">
            <button class="btn" type="button" onclick="history.back()">뒤로가기</button>
        </div>
    </div>
</section>

<%@ include file="../common/foot.jspf"%>