package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.vo.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UsrMemberController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @PostMapping("/doJoin")
    @ResponseBody
    public ResponseEntity<ResultData> doJoin(@RequestBody Member member) {
        System.out.println("✅ doJoin 진입");
        System.out.println("입력 받은 아이디: " + member.getLoginId());
        System.out.println("입력 받은 비밀번호: " + member.getLoginPw());
        System.out.println("입력 받은 비밀번호 확인: " + member.getCheckLoginPw());
        System.out.println("입력 받은 이름: " + member.getName());
        System.out.println("입력 받은 닉네임: " + member.getNickName());
        System.out.println("입력 받은 이메일: " + member.getEmail());

        try {
            // 1. 유효성 검사
            if (Ut.isEmpty(member.getLoginId()))
                return ResponseEntity.badRequest().body(ResultData.from("F-1", "아이디를 쓰시오"));
            if (Ut.isEmpty(member.getLoginPw()))
                return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 쓰시오"));
            if (Ut.isEmpty(member.getName()))
                return ResponseEntity.badRequest().body(ResultData.from("F-3", "이름을 쓰시오"));
            if (Ut.isEmpty(member.getNickName()))
                return ResponseEntity.badRequest().body(ResultData.from("F-4", "닉네임을 쓰시오"));
            if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@"))
                return ResponseEntity.badRequest().body(ResultData.from("F-6", "이메일 정확히 쓰시오"));

            // 2. 회원가입 처리
            long id = memberService.doJoin(
                    member.getLoginId(),
                    member.getLoginPw(),
                    member.getCheckLoginPw(),
                    member.getName(),
                    member.getNickName(),
                    member.getEmail()
            );

            if (id == -1)
                return ResponseEntity.badRequest().body(ResultData.from("F-8", String.format("%s는 이미 사용 중인 아이디입니다.", member.getLoginId())));
            if (id == -2)
                return ResponseEntity.badRequest().body(ResultData.from("F-9", String.format("이름 %s과 이메일 %s은(는) 이미 사용 중입니다.", member.getName(), member.getEmail())));

            // 3. 자동 로그인 처리
            Auth authRq = new Auth();
            authRq.setLoginId(member.getLoginId());
            authRq.setLoginPw(member.getLoginPw());

            Auth auth = authService.login(authRq);

            if (auth == null) {
                return ResponseEntity.status(401).body(ResultData.from("F-10", "자동 로그인에 실패했습니다. 로그인 페이지로 이동합니다."));
            }

            rq.setAccessToken(auth.getAccessToken());
            rq.setLoginedMember(memberService.getMemberByLoginId(member.getLoginId()));

            return ResponseEntity.ok(ResultData.from("S-1", member.getNickName() + " 님 회원가입을 축하합니다."));
        } catch (Exception e) {
            e.printStackTrace(); // 로그로 서버에서 어디서 죽었는지 추적
            return ResponseEntity.status(500).body(ResultData.from("F-ERR", "서버 오류: " + e.getMessage()));
        }
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

    @GetMapping("/myInfo")
    public Member myInfo(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById(rq.getLoginedMemberId());

        return member;
    }

//    @RequestMapping("/modify")
//    public String modify(Model model, HttpServletRequest req) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());
//
//        model.addAttribute("member", member);
//
//        return "/modify";
//    }

    @PutMapping("/checkPw")
    public ResponseEntity<ResultData> checkPw(
            HttpServletRequest req,
            @RequestBody Map<String, String> requestBody) {

        String pw = requestBody.get("pw");

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((Long) rq.getLoginedMemberId());

        // 암호화 비교
        if (!passwordEncoder.matches(pw, member.getLoginPw())) {
            return ResponseEntity.ok(ResultData.from("F-1", "비밀번호 불일치"));
        }

        return ResponseEntity.ok(ResultData.from("S-1", "비밀번호 일치 성공"));
    }

    // 로그인 체크 -> 유무 체크 -> 권한 체크
    @PutMapping("/modify")
    public ResponseEntity<ResultData> doModify(@RequestHeader("Authorization") String authorization, @RequestBody Member member) {

        // 토큰에서 memberId 추출
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 입력 검증

        if (Ut.isEmpty(member.getName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-1", "이름을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getNickName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2", "닉네임을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-3", "유효한 이메일을 입력해주세요"));
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