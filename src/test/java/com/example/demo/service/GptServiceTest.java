//package com.example.demo.service;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//public class GptServiceTest {
//
//    @Autowired
//    private GptService gptService;
//
//    @Test
//    public void testSummarizeDiff() {
//        String dummyDiff = """
//--- a/src/main/resources/static/css/style.css
//+++ b/src/main/resources/static/css/style.css
//@@ -33,72 +33,75 @@
//+.page4{
//+    background-image: url("/img/joinForm.jpg");
//+}
//--- a/src/main/webapp/WEB-INF/jsp/usr/ftArticle/foot_detail.jsp
//+++ b/src/main/webapp/WEB-INF/jsp/usr/ftArticle/foot_detail.jsp
//@@ -1,245 +1,251 @@
//-<div class="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-6 mt-8">
//+<div class="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-6 my-8">
//-            <div class="flex gap-4 bg-gray-400 justify-center text-center text-sm mt-2 rounded text-white py-2">
//+            <div class="flex gap-4  justify-center text-center text-sm mt-2 rounded text-black py-2">
//-                        <div><img src="${weather.iconUrl}" style="width: 40px;"/></div>
//+
//+                        <!-- 👇 여기 div에 하늘색 배경 추가 -->
//+                        <div class="bg-blue-300 rounded-full flex justify-center items-center">
//+                            <img src="${weather.iconUrl}" style="width: 40px;" />
//+                        </div>
//+
//+
//--- a/src/main/webapp/WEB-INF/jsp/usr/home/join.jsp
//+++ b/src/main/webapp/WEB-INF/jsp/usr/home/join.jsp
//@@ -1,240 +1,240 @@
//-                <img src="/img/joinImg2.jpg" alt="가입 이미지"
//+                <img src="/img/left.jpg" alt="가입 이미지"
//-                <div>
//-                    <label class="block mb-1 font-semibold">생년월일</label>
//-                    <div class="flex space-x-2">
//-                        <select id="year" class="border border-gray-300 rounded px-2 py-1"><option>년도</option></select>
//-                        <select id="month" class="border border-gray-300 rounded px-2 py-1"><option>월</option></select>
//-                        <select id="day" class="border border-gray-300 rounded px-2 py-1"><option>일</option></select>
//-                    </div>
//-                    <input type="hidden" name="bornDate" id="bornDate" />
//-                </div>
//-
//-                <div class="flex space-x-4">
//-                        <label class="block mb-1 font-semibold">지역</label>
//-                        <select name="area" class="border border-gray-300 rounded px-4 py-1">
//-                            <option>서울</option><option>경기</option><option>강원</option><option>인천</option><option>대전</option>
//-                            <option>세종</option><option>충북</option><option>충남</option><option>대구</option><option>경북</option>
//-                            <option>경남</option><option>부산</option><option>울산</option><option>광주</option><option>전북</option>
//-                            <option>전남</option><option>제주</option>
//-                        </select>
//+                        <label class="block mb-1 font-semibold">생년월일</label>
//+                        <div class="flex space-x-2">
//+                            <select id="year" class="border border-gray-300 rounded px-2 py-1"><option>년도</option></select>
//+                            <select id="month" class="border border-gray-300 rounded px-2 py-1"><option>월</option></select>
//+                            <select id="day" class="border border-gray-300 rounded px-2 py-1"><option>일</option></select>
//+                        </div>
//+                        <input type="hidden" name="bornDate" id="bornDate" />
//-                    <div>
//-                        <label class="block mb-1 font-semibold">성별</label>
//-                        <select name="gender" class="border border-gray-300 rounded px-4 py-1">
//-                            <option>남자</option><option>여자</option>
//-                        </select>
//+
//+                    <div class="flex space-x-4">
//+                        <div>
//+                            <label class="block mb-1 font-semibold">지역</label>
//+                            <select name="area" class="border border-gray-300 rounded px-4 py-1">
//+                                <option>서울</option><option>경기</option><option>강원</option><option>인천</option><option>대전</option>
//+                                <option>세종</option><option>충북</option><option>충남</option><option>대구</option><option>경북</option>
//+                                <option>경남</option><option>부산</option><option>울산</option><option>광주</option><option>전북</option>
//+                                <option>전남</option><option>제주</option>
//+                            </select>
//+                        </div>
//+                        <div>
//+                            <label class="block mb-1 font-semibold">성별</label>
//+                            <select name="gender" class="border border-gray-300 rounded px-4 py-1">
//+                                <option>남자</option><option>여자</option>
//+                            </select>
//+                        </div>
//-                </div>
//-            <div class="flex justify-end">
//-                <button type="submit"
//-                        class="bg-green-600 text-white px-10 py-2 rounded-full hover:bg-green-700">
//-                    완료
//-                </button>
//-            </div>
//+                <div class="flex justify-end">
//+                    <button type="submit"
//+                            class="bg-green-600 text-white px-10 py-2 rounded-full hover:bg-green-700">
//+                        완료
//+                    </button>
//+                </div>
//--- a/src/main/webapp/WEB-INF/jsp/usr/home/main.jsp
//+++ b/src/main/webapp/WEB-INF/jsp/usr/home/main.jsp
//@@ -1,127 +1,123 @@
//-				  class="w-full max-w-md bg-white border border-gray-300 p-10 rounded-2xl shadow-2xl text-black">
//-				<div class="mb-6">
//+				  class="w-full max-w-md p-10 rounded-2xl shadow-2xl text-black" style="background-color: rgba(180, 180, 180, 0.5);">
//+				<div class="my-4">
//-				<div class="mb-8">
//+				<div class="mb-6">
//-				<div class="flex justify-between mb-6">
//-					<button type="button"
//-							class="px-6 py-3 border border-gray-500 text-green-900 rounded-lg hover:bg-green-50 transition">
//-						취소
//-					</button>
//+				<div class="my-2">
//-							class="px-6 py-3 bg-green-900 text-white rounded-lg hover:bg-green-800 transition font-semibold">
//+							class="w-full bg-white text-black hover:bg-gray-300 py-3 rounded-full transition font-semibold">
//-				<div class="flex justify-center">
//+				<div class="flex justify-center my-2">
//-							class="w-full bg-green-900 text-white hover:bg-green-800 py-3 rounded-full transition font-semibold">
//+							class="w-full bg-black text-white hover:bg-gray-800 py-3 rounded-full transition font-semibold">
//-				<h2 class="text-2xl mb-6 font-bold">${rq.loginedMember.nickName}님 환영합니다!</h2>
//+				<h2 class="text-2xl mb-6 font-bold">[${rq.loginedMember.nickName}] 님 환영합니다!</h2>
//        """;
//
//        String summary = gptService.summarizeDiff(dummyDiff);
//        System.out.println("✅✅✅✅✅✅GPT 요약 결과: " + summary);
//    }
//}
