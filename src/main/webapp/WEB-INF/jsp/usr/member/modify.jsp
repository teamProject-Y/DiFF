<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:set var="pageTitle" value="MY INFO MODIFY"></c:set>
<%@ include file="../common/head.jspf"%>

<script>
    // 	비밀번호 확인 함수
    function memberInfo_modify() {
        const value = $(".checkPw-inputArea").val();
        $.get('../member/checkPw',{
            pw : value,
            ajaxMode : 'Y'
        }, function(data){
            console.log(data);
            if(data?.resultCode?.startsWith("S-1")) {
                console.log("success");
                window.alert("비밀번호가 확인되었습니다.");
                $(".checkPw").addClass("hidden");
                $(".modifyInfo").removeClass("hidden");
            }else {
                window.alert("비밀번호가 일치하지 않습니다.");
                console.log("fail");
            }
        }, 'json');
    }
</script>

<button onclick="history.back()" class="block text-4xl pl-10 cursor-pointer">
    <i class="fa-solid fa-angle-left"></i>
</button>

<div class="container">

    <div class="checkPw">
        <div class="mx-auto mt-10 max-w-min p-4 bg-neutral-200 border border-solid border-neutral-300 rounded-lg">
            <div class="title mt-4 mb-8 text-center text-2xl font-semibold">
                Check Your Password
            </div>
            <div style="display:flex; flex-direction:column; justify-content: center;">
                <input type="password" name="loginPw" class="checkPw-inputArea mb-6 bg-neutral-50 border border-neutral-300
					text-neutral-800 text-sm rounded-lg block w-96  p-2.5" placeholder="Password">
            </div>
            <button onClick="memberInfo_modify()" class="py-2.5 px-5 me-2 mb-2 w-96 text-sm font-large bg-neutral-800
		 		text-neutral-200 rounded-lg hover:bg-neutral-700">VERIFY</button>
            <div class="sub-menu text-center my-4 flex justify-center text-sm">
                <a class="hover:text-underline" href="#">FIND PASSWORD</a>
            </div>
        </div>
    </div>
    <div class="modifyInfo hidden">
        <div class="container mx-auto mt-12 max-w-min px-8 py-4 bg-neutral-200 border border-solid border-neutral-300 rounded-lg">
            <div class="title my-3 text-center text-2xl font-semibold">
                회원 정보 수정
            </div>
            <form onsubmit="return validate();" name="doModify" action="doModify" method="POST">

                <div calss="flex flex-col justify-center">

                    아이디 <span class="text-red-400 text-sm">*수정이 불가합니다.*</span>
                    <input type="text" name="loginId" id="loginId" placeholder="ID" value="${member.loginId }" disabled
                           class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96 p-2.5">
                    비밀번호 <input type="text" name="loginPw" id="loginPw" placeholder="Password"  value="${member.loginPw }"
                                class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96 p-2.5 bg-neutral-50">
                    이름 <input type="text" name="name" id="name" placeholder="Name"  value="${member.name }"
                              class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5 bg-neutral-50">
                    닉네임 <input type="text" name="nickName" id="nickName" placeholder="NickName"  value="${member.nickName }"
                               class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5 bg-neutral-50">
                    전화번호 <input type="text" name="cellPhone" id="cellPhone" placeholder="cell-phone"  value="${member.cellPhone }"
                                class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5 bg-neutral-50">
                    e-mail <input type="email" name="email" id="email" placeholder="e-mail"  value="${member.email }"
                                  class="mb-6 border border-neutral-300 text-neutral-800 text-sm rounded-lg block w-96  p-2.5 bg-neutral-50">
                </div>
                <button type="submit" class="py-2.5 px-5 me-2 mb-2 w-96 text-sm font-large bg-neutral-800 text-neutral-200 rounded-lg hover:bg-neutral-700">UPDATE</button>
            </form>
            <!-- 			<div class="sub-menu text-center my-4 flex justify-center"> -->
            <!-- 				<a class="hover:text-underline" href="login">UPDATE</a>	 -->
            <!-- 			</div> -->
        </div>
    </div>

</div>



<%@ include file="../common/foot.jspf"%>