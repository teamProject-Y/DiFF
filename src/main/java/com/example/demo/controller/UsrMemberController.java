package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
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
@RequiredArgsConstructor
public class UsrMemberController {

    @Autowired
    private BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private MemberService memberService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    @PostMapping("/doLogin")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {

        System.out.println("제발 여기로 와라");

        if (Ut.isEmpty(member.getLoginId()))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","아이디를 입력해주세요"));
        if (Ut.isEmpty(member.getLoginPw()))
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));

        Member m = memberService.getMemberByLoginId(member.getLoginId());
        if (m == null)
            return ResponseEntity.status(404).body(ResultData.from("F-3","존재하지 않는 아이디"));
        if (!m.getLoginPw().equals(member.getLoginPw()))
            return ResponseEntity.status(401).body(ResultData.from("F-A","비밀번호 불일치"));

        rq.login(m);
        rq.setLoginedMember(m);

        return ResponseEntity.ok(ResultData.from("S-1", m.getNickName()+"님 환영"));
    }

    @PostMapping("/doLogout")
    public ResponseEntity<ResultData> doLogout(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");

        rq.logout();

        return ResponseEntity.ok(ResultData.from("S-1", "로그아웃 되었습니다"));

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
    @PutMapping("/doModify")
    public ResponseEntity<ResultData> doModify(@RequestHeader("Authorization") String authorization, @RequestBody Member member) {

        // 토큰에서 memberId 추출
        String token = authorization.substring(7);
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 입력 검증
        if (Ut.isEmpty(member.getLoginId())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-1", "아이디를 입력해주세요"));
        }
        if (Ut.isEmpty(member.getLoginPw())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 입력해주세요"));
        }
        if (Ut.isEmpty(member.getName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-3", "이름을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getNickName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-4", "닉네임을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-6", "유효한 이메일을 입력해주세요"));
        }

        // 서비스에 수정 요청
        int updated = memberService.modifyMember(memberId, member.getLoginId(), member.getLoginPw(), member.getName(), member.getNickName(), member.getEmail()
        );

        //  결과 검사
        if (updated == 0) {
            return ResponseEntity.badRequest().body(ResultData.from("F-7", "회원정보 수정에 실패했습니다"));
        }

        // 성공 응답
        return ResponseEntity.ok(ResultData.from("S-1", "회원정보가 성공적으로 수정되었습니다")
        );
    }


    ////////////////////////////////////////////// CLI ///////////////////////////////////////////////////
    @PostMapping("/verifyGitUser")
    @ResponseBody
    public ResultData verifyGitUser(@RequestBody Map<String, String> requestMap) {

        String email = requestMap.get("email");
        Integer verifiedMemberId = memberService.isVerifiedUser(email);

        if(verifiedMemberId != null) {
            System.out.println("git email로 찾은 memberID: " + verifiedMemberId);
            return ResultData.from("S-1", "사용자 인증 완료", "인증된 사용자 id", verifiedMemberId);
        }else {
            System.err.println("git email로 찾은 member 없음");
            return ResultData.from("F-1", "사용자 인증 실패");
        }
    }

}