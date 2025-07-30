package com.example.demo.config;

import com.example.demo.service.AuthService;
import com.example.demo.service.MemberService;
import com.example.demo.service.OAuthAccountsService;
import com.example.demo.vo.Member;
import com.example.demo.vo.OAuthAccount;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private @Lazy MemberService memberService;

    @Autowired
    private OAuthAccountsService oAuthAccountsService;

    @Autowired
    private AuthService authService;

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Rq rq;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        System.out.println("==== [OAuth2SuccessHandler] 소셜 로그인 성공! ====");
        System.out.println("auth class: " + authentication.getClass());
        System.out.println("authorities: " + authentication.getAuthorities());

        // ✅ 소셜 로그인한 사용자 정보 추출
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String oauthId;
        if ("google".equals(registrationId)) {
            oauthId = oauthUser.getAttribute("sub");
        } else {
            oauthId = oauthUser.getName();
        }

        if (oauthId == null) {
            throw new RuntimeException("❌ OAuth ID 를 가져올 수 없습니다.");
        }
        System.out.println("🌐 oauthId: " + oauthId);

        // ✅ provider 포함된 OAuthAccount 조회
        OAuthAccount oAuthAccount = oAuthAccountsService.findByOauthId(oauthId);
        if (oAuthAccount == null) {
            throw new RuntimeException("❌ 해당 OAuth 계정을 찾을 수 없습니다.");
        }

        String provider = oAuthAccount.getProvider();
        System.out.println("✅ provider: " + provider);

        // ✅ Member 조회
        Member member = memberService.getByOauthIdAndProvider(oauthId, provider);
        if (member == null) {
            throw new RuntimeException("❌ 해당 OAuth ID와 Provider에 해당하는 회원이 없습니다.");
        }

        // ✅ 닉네임, 이메일 추출
        String nickName = oauthUser.getAttribute("login");
        if (nickName == null) {
            nickName = oauthUser.getAttribute("name"); // Google fallback
        }
        String email = oauthUser.getAttribute("email");
        request.getSession().setAttribute("loginedMember", member);
        request.getSession().setAttribute("loginedMemberNickName", member.getNickName());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        rq.login(member);
        rq.setLoginedMember(member);
        System.out.println("session loginedMemberId: " + rq.getLoginedMemberId());

        // ✅ JWT 발급
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getNickName(), member.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId(), member.getNickName(), member.getEmail());
        authService.saveToken(member.getId(), accessToken, refreshToken);

        System.out.println("🎯 Access Token: " + accessToken);
        System.out.println("🎯 Refresh Token: " + refreshToken);

        // ✅ 프론트엔드로 리다이렉트
        String redirectUrl = "http://localhost:3000/DiFF/home/main"
                + "?access_token=" + accessToken
                + "&refresh_token=" + refreshToken;

        response.sendRedirect(redirectUrl);
    }
}

