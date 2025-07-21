<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:set var="pageTitle" value="LOGIN"></c:set>
<%@ include file="../common/head.jspf"%>

<script type="text/javascript">
    let MemberFindLoginPw__submitFormDone = false;
    function MemberFindLoginPw__submit(form) {
        if (MemberFindLoginPw__submitFormDone) {
            return;
        }
        form.loginId.value = form.loginId.value.trim();
        form.email.value = form.email.value.trim();
        if (form.loginId.value.length == 0) {
            alert('아이디 써라');
            form.loginId.focus();
            return;
        }
        if (form.email.value.length == 0) {
            alert('email 써라');
            form.email.focus();
            return;
        }
        MemberFindLoginPw__submitFormDone = true;
        alert('메일로 임시 비밀번호를 발송했습니다');
        form.submit();
    }
</script>

<section class="mt-8 text-xl px-4">
    <div class="mx-auto">
        <form action="../member/doFindLoginPw" method="POST" onsubmit="MemberFindLoginPw__submit(this);">
            <input type="hidden" name="afterFindLoginPwUri" value="${param.afterFindLoginPwUri  }" />
            <table class="login-box table-box-1" border="1">
                <tbody>
                <tr>
                    <th>아이디</th>
                    <td>
                        <input class="input input-bordered w-full max-w-xs" autocomplete="off" type="text" placeholder="아이디를 입력해주세요"
                               name="loginId" />
                    </td>
                </tr>
                <tr>
                    <th>이메일</th>
                    <td>
                        <input class="input input-bordered w-full max-w-xs" autocomplete="off" type="text" placeholder="이메일을 입력해주세요"
                               name="email" />
                    </td>
                </tr>
                <tr>
                    <th></th>
                    <td>
                        <button type="submit">비밀번호 찾기</button>
                    </td>
                </tr>
                <tr>
                    <th></th>
                    <td>
                        <a class="btn btn-active btn-ghost" href="../member/login">로그인</a>
                    </td>
                </tr>
                </tbody>
            </table>
        </form>
        <div class="btns">
            <button class="btn btn-outline" type="button" onclick="history.back();">뒤로가기</button>
        </div>
    </div>
</section>



<%@ include file="../common/foot.jspf"%>