package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.service.MemberService;
import com.example.demo.service.OAuthAccountService;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;
import com.example.util.Ut;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/DiFF/auth")
public class UsrAuthController {

    private final AuthService authService;
    private final OAuthAccountService oAuthAccountService;
    private final Rq rq;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /** 토큰갱신 API */
    @GetMapping("/refresh")
    public ResponseEntity<Object> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        String newAccessToken = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    /** 로컬, 소셜 통합 **/
    @GetMapping("/link/{provider}")
    public void socialLogin(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        String redirectUrl = switch (provider.toLowerCase()) {
            case "github" -> "/oauth2/authorization/github";
            case "google" -> "/oauth2/authorization/google";
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/linked")
    public ResponseEntity<?> getLinked() {

        Long memberId = rq.getLoginedMemberId();
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인 필요"));
        }
        Map<String, Boolean> linked = oAuthAccountService.getLinkedProviders(memberId);
        return ResponseEntity.ok(linked);
    }

    @PostMapping("/login")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {
        String email = member.getEmail();
        String loginPw = member.getLoginPw();

        // 1. 유효성 체크
        if (Ut.isEmpty(email) || !email.contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-1","이메일을 바르게 입력해주세요"));
        }
        if (Ut.isEmpty(loginPw)) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));
        }

        // 2. 회원 조회
        Member found = memberService.getMemberByEmail(email);
        if (found == null) {
            return ResponseEntity.status(401).body(ResultData.from("F-3","존재하지 않는 계정입니다."));
        }

        // 3. 비밀번호 매칭
        if (!passwordEncoder.matches(loginPw, found.getLoginPw())) {
            return ResponseEntity.status(401).body(ResultData.from("F-4","비밀번호가 일치하지 않습니다."));
        }

        // 4. 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(found.getId(), found.getNickName(), found.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(found.getId(), found.getNickName(), found.getEmail());

        // 5. 응답
        return ResponseEntity.ok(
                ResultData.from("S-1", found.getNickName()+"님 환영합니다.",
                        "accessToken", accessToken,
                        "refreshToken", refreshToken)
        );

    }


    @PostMapping("/join")
    public ResponseEntity<ResultData> doJoin(@RequestBody Member member) {
        if (Ut.isEmpty(member.getLoginPw())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 작성하세요."));
        }
        if (Ut.isEmpty(member.getNickName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-4","닉네임을 쓰시오"));
        }
        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-6","이메일 정확히 쓰시오"));
        }

        long id = memberService.join(
                member.getLoginPw(),
                member.getCheckLoginPw(),
                member.getNickName(),
                member.getEmail()
        );

        if (id == -409) {
            return ResponseEntity.badRequest().body(ResultData.from("F-409", "이미 가입된 이메일입니다."));
        }
        if (id == -400) {
            return ResponseEntity.badRequest().body(ResultData.from("F-400", "비밀번호가 일치하지 않습니다."));
        }

        Member newMember = memberService.getMemberByEmail(member.getEmail());

        String accessToken = jwtTokenProvider.generateAccessToken(newMember.getId(), newMember.getNickName(), newMember.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(newMember.getId(), newMember.getNickName(), newMember.getEmail());

        return ResponseEntity.ok(ResultData.from("S-1",
                newMember.getNickName() + " 님 회원가입을 축하합니다.",
                "accessToken", accessToken,
                "refreshToken", refreshToken));
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token invalid");
        }

        Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);
        String nickName = jwtTokenProvider.getNickNameFromToken(refreshToken);
        String email = jwtTokenProvider.getMemberEmailFromToken(refreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(memberId, nickName, email);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

}
