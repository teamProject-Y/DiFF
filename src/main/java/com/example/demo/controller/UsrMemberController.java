package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import util.Ut;

import java.util.Map;

@RestController
@RequestMapping("/DiFF/member")
public class UsrMemberController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private MemberService memberService;

    public UsrMemberController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

//    @PostMapping("/join")
//    public ResponseEntity<ResultData> doJoin(@RequestBody Member dto) {
//        if (Ut.isEmpty(dto.getLoginId()))
//            return ResponseEntity.badRequest().body(ResultData.from("F-1","아이디를 쓰시오"));
//        // ... 기타 유효성 검사 ...
//
//        long newId = memberService.doJoin(
//                dto.getLoginId(), dto.getLoginPw(),
//                dto.getName(), dto.getNickName(),
//                dto.getEmail()
//        );
//
//        if (newId < 1)
//            return ResponseEntity.badRequest().body(ResultData.from("F-8","이미 사용 중인 정보가 있습니다"));
//
//        Member m = memberService.getMemberById(newId);
//        return ResponseEntity.ok(ResultData.from("S-1", m.getNickName()+"님 가입 성공"));
//    }

    // 액션메서드
    @RequestMapping("/doJoin")
    @ResponseBody
    public String doJoin(String loginId, String loginPw, String checkLoginPw, String name, String nickName, String email) {

        if(Ut.isEmpty(loginId)) return Ut.jsHistoryBack("F-1", "아이디를 쓰시오");
        if(Ut.isEmpty(loginPw)) return Ut.jsHistoryBack("F-2", "비밀번호를 쓰시오");
        if(Ut.isEmpty(name)) return Ut.jsHistoryBack("F-3", "이름을 쓰시오");
        if(Ut.isEmpty(nickName)) return Ut.jsHistoryBack("F-4", "닉네임을 쓰시오");
        if(Ut.isEmpty(email) || !email.contains("@")) return Ut.jsHistoryBack("F-6", "이메일 정확히 쓰시오");
        if(!loginPw.equals(checkLoginPw)) return Ut.jsHistoryBack("F-7", "비밀번호가 일치하지 않소");

        long id = memberService.doJoin(loginId, loginPw, name, nickName, email);

        if(id == -1) return Ut.jsHistoryBack("F-8", Ut.f("%s는 이미 사용 중인 아이디입니다.", loginId));
        if(id == -2) return Ut.jsHistoryBack("F-9", Ut.f("이름 %s과 이메일 %s은(는) 이미 사용 중입니다.", loginId, email));

        return Ut.jsReplace("S-1", Ut.f("%s 님 회원가입을 축하합니다.", nickName), "/");
    }

    @RequestMapping("/login")
    public String login() {

        System.out.println("login 메서드 진입");

        return "/login";
    }

    @RequestMapping("/doLogin")
    @ResponseBody
    public String doLogin(@RequestBody Member member) {

        System.out.println("제발 여기로 와라");

        if (Ut.isEmpty(member.getLoginId()))
            return Ut.jsHistoryBack("F-1","아이디를 입력해주세요");
        if (Ut.isEmpty(member.getLoginPw()))
            return Ut.jsHistoryBack("F-2","비밀번호를 입력해주세요");

        Member m = memberService.getMemberByLoginId(member.getLoginId());
        if (m == null)
            return Ut.jsHistoryBack("F-3","존재하지 않는 아이디");
        if (!m.getLoginPw().equals(member.getLoginPw()))
            return Ut.jsHistoryBack("F-A","비밀번호 불일치");

        rq.login(m);

        return Ut.jsHistoryBack("S-1", m.getNickName()+"님 환영");
    }

    @RequestMapping("/doLogout")
    @ResponseBody
    public String doLogout(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");

        rq.logout();

        return Ut.jsReplace("S-1", "로그아웃 되었습니다", "/DiFF/home/main");

    }

    @RequestMapping("/myInfo")
    public String myInfo(Model model, HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        model.addAttribute("member", member);

        return "/myInfo";
    }

    @RequestMapping("/modify")
    public String modify(Model model, HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        model.addAttribute("member", member);

        return "/modify";
    }

    @RequestMapping("/checkPw")
    @ResponseBody
    public ResultData checkPw(HttpServletRequest req, String pw) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        if(!member.getLoginPw().equals(pw)) {
            return ResultData.from("F-1", "비밀번호 불일치");
        }

        return ResultData.from("S-1", "비밀번호 일치 성공");
    }

    // 로그인 체크 -> 유무 체크 -> 권한 체크
    @RequestMapping("/doModify")
    @ResponseBody
    public String doModify(HttpServletRequest req, String loginId, String loginPw, String name, String nickName, String email) {

        Rq rq = (Rq) req.getAttribute("rq");
        long loginedMemberId = rq.getLoginedMemberId();

//		if(Ut.isEmpty(loginId)) return Ut.jsHistoryBack("F-1", "아이디를 쓰시오");
//		if(memberService.isUsableLoginId(loginId)) return Ut.jsHistoryBack("F-7", "사용 중인 아이디입니다.");
        if(Ut.isEmpty(loginPw)) return Ut.jsHistoryBack("F-2", "비밀번호를 쓰시오");
        if(Ut.isEmpty(name)) return Ut.jsHistoryBack("F-3", "이름을 쓰시오");
        if(Ut.isEmpty(nickName)) return Ut.jsHistoryBack("F-4", "닉네임을 쓰시오");
        if(Ut.isEmpty(email) || !email.contains("@")) return Ut.jsHistoryBack("F-6", "이메일 정확히 쓰시오");

        int memberUpdate = memberService.modifyMember(loginedMemberId, loginId, loginPw, name, nickName, email);

        return Ut.jsReplace("S-1", Ut.f("%s 회원님 정보 수정 완료", nickName), "../member/myInfo");
    }

    ////////// CLI
    @PostMapping("/verifyGitUser")
    @ResponseBody
    public ResultData verifyGitUser(@RequestBody Map<String, String> requestMap) {

        System.err.println("git config user.name = " + requestMap.get("email"));
        String email = requestMap.get("email");

        Integer verifiedMemberId = memberService.isVerifiedUser(email);

        if(verifiedMemberId != null) {
            System.out.println("succes memberID: " + verifiedMemberId);
            return ResultData.from("S-1", "사용자 인증 완료", "인증된 사용자 id", verifiedMemberId);
        }else {
            System.out.println("failed");
            return ResultData.from("F-1", "사용자 인증 실패");
        }
    }

    @PostMapping("/getdiFf")
    public ResultData getDiFF(@RequestBody Map<String, String> requestMap) {

        return null;
    }

}