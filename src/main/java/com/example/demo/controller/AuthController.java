package com.example.demo.controller;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /** ✅ 로컬 로그인 */
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

    /** ✅ 회원가입 + 자동 로그인 */
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

        // ✅ 자동 로그인 효과: 토큰 바로 발급
        String accessToken = jwtTokenProvider.generateAccessToken(newMember.getId(), newMember.getNickName(), newMember.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(newMember.getId(), newMember.getNickName(), newMember.getEmail());

        return ResponseEntity.ok(ResultData.from("S-1",
                newMember.getNickName() + " 님 회원가입을 축하합니다.",
                "accessToken", accessToken,
                "refreshToken", refreshToken));
    }

    /** ✅ 토큰 재발급 */
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
