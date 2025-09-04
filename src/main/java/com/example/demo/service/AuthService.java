package com.example.demo.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.repository.AuthRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Auth;
import com.example.demo.vo.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 로그인 **/
    // curl로 로그인 시 이녀석 반응
    @Transactional
    public Map<String, String> login(Auth authRq) {

        String email = authRq.getEmail();
        String loginPw = authRq.getLoginPw();

        Member member = memberRepository.getMemberByEmail(email);

        if (member == null) {
            throw new UsernameNotFoundException("해당 유저를 찾을 수 없습니다. email = " + email);
        }

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginPw, member.getLoginPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다. email = " + email);
        }

        // 3. 토큰 발급 (DB 저장 ❌)
        String accessToken = jwtTokenProvider.generateAccessToken(
                member.getId(), member.getNickName(), member.getEmail()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                member.getId(), member.getNickName(), member.getEmail()
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }

    /** Token 갱신 */
    @Transactional
    public String refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return null;
        }
        Auth auth = authRepository.findByRefreshToken(refreshToken);
        if (auth == null) {
            throw new IllegalArgumentException("해당 토큰을 찾을 수 없습니다.\nREFRESH_TOKEN = " + refreshToken);
        }
        Member member = memberRepository.getMemberById(auth.getMemberId());
        String newAccessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getNickName(), member.getEmail());

        auth.setAccessToken(newAccessToken);
        authRepository.updateAccessToken(auth.getId(), newAccessToken);
        return newAccessToken;
    }

    public String getGithubToken(Long memberId) {
        return authRepository.getTokenByMemberIdAndProvider(memberId, "github");
    }
}
