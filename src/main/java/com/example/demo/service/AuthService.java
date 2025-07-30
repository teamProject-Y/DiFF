package com.example.demo.service;

import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.JwtTokenProvider;
import com.example.demo.repository.AuthRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Auth;
import com.example.demo.vo.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 로그인 */
    @Transactional
    public Auth login(Auth authRq) {

        String loginId = authRq.getLoginId();
        String loginPw = authRq.getLoginPw();

        Member member = memberRepository.getMemberByLoginId(loginId);
        if (member == null) {
            throw new UsernameNotFoundException("해당 유저를 찾을 수 없습니다. username = " + loginId);
        }
        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginPw, member.getLoginPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다. username = " + loginId);
        }
        // 3. 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getNickName(), member.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId(), member.getNickName(), member.getEmail());



        // 4. 기존 Auth 존재 여부 확인
        Auth auths = authRepository.findByMemberId(member.getId());

        if (auths != null) {
            // 토큰만 갱신
            authRq.setAccessToken(accessToken);
            authRq.setRefreshToken(refreshToken);
            authRepository.updateTokens(authRq);
            return authRq;
        }
        // 5. Auth 없으면 신규 저장
        Auth newAuth = Auth.builder()
                .memberId(member.getId())
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        authRepository.insert(newAuth);
        return newAuth;
    }

//    /** 회원가입 */
//    @Transactional
//    public Long doJoin(Member member) {
//        member.setRole(Role.ROLE_USER);
//        member.setLoginPw(passwordEncoder.encode(member.getLoginPw()));
//        memberRepository.doJoin(member.getLoginId(),
//                member.getLoginPw(),
//                member.getName(),
//                member.getNickName(),
//                member.getEmail());
//
//        return memberRepository.getLastInsertId();
//    }

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
    public void saveToken(long memberId, String accessToken, String refreshToken) {
        Auth auth = Auth.builder()
                .memberId(memberId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();

        authRepository.saveAuth(auth);
    }

}
