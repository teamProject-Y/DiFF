package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.vo.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;

import jakarta.servlet.http.HttpServletRequest;
import com.example.util.Ut;

import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/member")
@CrossOrigin(origins = "http://localhost:3000")
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
    @Autowired
    private AuthService authService;

    public UsrMemberController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

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

        Auth auth = null;
        try {
            Auth authRq = new Auth();
            authRq.setLoginId(loginId);
            authRq.setLoginPw(loginPw);

            auth = authService.login(authRq);

            rq.setAccessToken(auth.getAccessToken());
            rq.setLoginedMember(memberService.getMemberByLoginId(loginId));
        } catch (Exception e) {
            return Ut.jsReplace("F-10", "자동 로그인에 실패했습니다. 로그인 페이지로 이동합니다.", "/member/login");
        }

        return Ut.jsReplace("S-1", Ut.f("%s 님 회원가입을 축하합니다.", nickName), "/");
    }


//    @RequestMapping("/login")
//    public String login() {
//
//        System.out.println("login 메서드 진입");
//
//        return "/login";
//    }

    @PostMapping("/login")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {

        System.out.println("doLogin 진입"+"제발 여기로 와라");

        if (Ut.isEmpty(member.getLoginId()))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","아이디를 입력해주세요"));
        if (Ut.isEmpty(member.getLoginPw()))
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));

        Auth authRq = new Auth();
        authRq.setLoginId(member.getLoginId());
        authRq.setLoginPw(member.getLoginPw());
        Auth auth = authService.login(authRq);

        System.out.println(new BCryptPasswordEncoder().encode("diff"));
        if (auth == null)

            return ResponseEntity.status(401).body(ResultData.from("F-3","로그인 실패"));

        rq.setAccessToken(auth.getAccessToken());
        rq.setLoginedMember(memberService.getMemberByLoginId(member.getLoginId()));

        return ResponseEntity.ok(ResultData.from("S-1", member.getNickName()+"님 환영", "accessToken", auth.getAccessToken()));
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
        Member member = memberService.getMemberById(rq.getLoginedMemberId());

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

}