<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:set var="pageTitle" value="CHECKPW"></c:set>
<%@ include file="../common/head.jspf"%>
<hr />

<section class="mt-24 text-xl px-4">
    <div class="mx-auto">
        <form action="../member/doCheckPw" method="POST">
            <table class="table" border="1" cellspacing="0" cellpadding="5" style="width: 100%; border-collapse: collapse;">
                <tbody>
                <tr>
                    <th>아이디</th>
                    <td style="text-align: center;">${rq.loginedMember.loginId }</td>

                </tr>
                <tr>
                    <th>비밀번호</th>
                    <td style="text-align: center;">
                        <input class="input input-bordered input-primary input-sm w-full max-w-xs" name="loginPw" autocomplete="off"
                               type="text" placeholder="비밀번호를 입력해" />
                    </td>

                </tr>
                <tr>
                    <th></th>
                    <td style="text-align: center;">
                        <button type="submit" class="btn btn-primary">확인</button>
                    </td>

                </tr>
                </tbody>
            </table>
        </form>
        <div class="btns">
            <button class="btn" type="button" onclick="history.back()">뒤로가기</button>
        </div>
    </div>
</section>

<%@ include file="../common/foot.jspf"%>