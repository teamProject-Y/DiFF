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

    private final Rq rq;

    private final AuthService authService;

    private final OAuthAccountService oAuthAccountService;

    private final MemberService memberService;

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    /** 토큰갱신 API */
    @GetMapping("/refresh")
    public ResponseEntity<Object> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        System.out.println("===== 🍪 [Get] /api/DiFF/auth/refresh =====");
        String newAccessToken = this.authService.refreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    /** 로컬, 소셜 통합 **/
    @GetMapping("/link/{provider}")
    public void socialLogin(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        System.out.println("===== 🔗 [Get] /api/DiFF/auth/link/{provider} =====");
        String redirectUrl = switch (provider.toLowerCase()) {
            case "github" -> "/oauth2/authorization/github";
            case "google" -> "/oauth2/authorization/google";
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/linked")
    public ResponseEntity<?> getLinked() {
        System.out.println("===== 🔗🐈‍⬛ [Get] /api/DiFF/auth/linked =====");

        Long memberId = rq.getLoginedMemberId();
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인 필요"));
        }
        Map<String, Boolean> linked = oAuthAccountService.getLinkedProviders(memberId);
        return ResponseEntity.ok(linked);
    }

    @PostMapping("/login")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {

        System.out.println("===== 🔐👤 [Post] /api/DiFF/auth/login =====");
        String email = member.getEmail();
        String loginPw = member.getLoginPw();

        System.out.println("🔐👤 로그인 시도: email=" + email + ", pw=" + loginPw);

        // 유효성 체크
        if (Ut.isEmpty(email) || !email.contains("@")) {
            System.out.println("🔐👤❌ 잘못된 이메일 입력: " + email);
            return ResponseEntity.badRequest().body(ResultData.from("F-1", "이메일을 바르게 입력해주세요"));
        }
        if (Ut.isEmpty(loginPw)) {
            System.out.println("🔐👤❌ 비밀번호 없음 (email=" + email + ")");
            return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 입력해주세요"));
        }

        // 회원 조회
        Member found = memberService.getMemberByEmail(email);
        if (found == null) {
            System.out.println("🔐👤❌ 존재하지 않는 계정: " + email);
            return ResponseEntity.status(401).body(ResultData.from("F-3", "존재하지 않는 계정입니다."));
        }
        System.out.println("🔐👤✅ 회원 조회 성공: id=" + found.getId()
                + ", nick=" + found.getNickName()
                + ", isVerified=" + found.getIsVerified());

        // 이메일 인증 여부 확인
        if (!Boolean.TRUE.equals(found.getIsVerified())) {
            System.out.println("🔐👤❌ 이메일 미인증 계정: id=" + found.getId() + ", email=" + found.getEmail());
            return ResponseEntity.status(403).body(ResultData.from("F-5", "이메일 인증이 필요합니다."));
        }

        // 비밀번호 매칭
        if (!passwordEncoder.matches(loginPw, found.getLoginPw())) {
            System.out.println("🔐👤❌ 비밀번호 불일치: email=" + email);
            return ResponseEntity.status(401).body(ResultData.from("F-4", "비밀번호가 일치하지 않습니다."));
        }

        // 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(found.getId(), found.getNickName(), found.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(found.getId(), found.getNickName(), found.getEmail());
        System.out.println("🔐👤🎟️ 토큰 발급 성공: id=" + found.getId()
                + ", email=" + email
                + ", accessToken=" + accessToken);

        // 응답
        return ResponseEntity.ok(
                ResultData.from("S-1", found.getNickName() + "님 환영합니다.",
                        "accessToken", accessToken,
                        "refreshToken", refreshToken)
        );
    }

    @PostMapping("/join")
    public ResponseEntity<ResultData> doJoin(@RequestBody Member member) {

        System.out.println("===== 👤🆕 [Post] /api/DiFF/auth/join =====");
        if (Ut.isEmpty(member.getLoginPw())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 작성하세요."));
        }

        String password = member.getLoginPw();
        String passwordPattern =
                "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>~+\\-])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>~+\\-]{8,}$";

        if (!password.matches(passwordPattern)) {
            return ResponseEntity.badRequest().body(ResultData.from(
                    "F-3",
                    "Password must be at least 8 characters long and contain at least one English letter, one number, and one special character."
            ));
        }

        if (Ut.isEmpty(member.getNickName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-4", "Please enter your nickname"));
        }
        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-6", "Please check your email"));
        }

        long id = memberService.join(
                member.getLoginPw(),
                member.getCheckLoginPw(),
                member.getNickName(),
                member.getEmail()
        );

        if (id == -409L) {
            return ResponseEntity.badRequest().body(ResultData.from("F-409", "This email address is already registered."));
        }
        if (id == -410L) {
            return ResponseEntity.badRequest().body(ResultData.from("F-410", "This email nickname is already registered."));
        }
        if (id == -400L) {
            return ResponseEntity.badRequest().body(ResultData.from("F-400", "Password does not match."));
        }

        return ResponseEntity.ok(ResultData.from("S-1",
                member.getNickName() + " \n" +
                        "Your registration has been completed. Please verify your email address."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {

        System.out.println("===== 🍪🆕 [Post] /api/DiFF/auth/refresh =====");

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
