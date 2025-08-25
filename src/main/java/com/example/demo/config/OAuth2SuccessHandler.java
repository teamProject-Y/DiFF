package com.example.demo.config;

import com.example.demo.service.MemberService;
import com.example.demo.service.OAuthAccountService;
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
    private OAuthAccountService oAuthAccountService;

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    private Rq rq;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        String oauthId = "google".equals(registrationId) ? oauthUser.getAttribute("sub") : oauthUser.getName();
        if (oauthId == null) throw new RuntimeException("❌ OAuth ID 를 가져올 수 없습니다.");

        String provider = registrationId; // "github" | "google"

        String mode = (String) request.getSession().getAttribute("OAUTH_MODE");
        Long linkTargetMemberId = (Long) request.getSession().getAttribute("LINK_TARGET_MEMBER_ID");

        // 🔗 링크 모드
        if ("link".equalsIgnoreCase(mode) && linkTargetMemberId != null) {
            OAuthAccount existing = oAuthAccountService.findByProviderAndOauthId(provider, oauthId);

            if (existing != null && existing.getMemberId() != null && !existing.getMemberId().equals(linkTargetMemberId)) {
                response.sendError(409, "이미 다른 계정과 연동된 소셜 계정입니다.");
                return;
            }

            if (existing == null) {
                oAuthAccountService.create(linkTargetMemberId, provider, oauthId);
            } else if (existing.getMemberId() == null) {
                oAuthAccountService.attachToMember(existing.getId(), linkTargetMemberId);
            }

            Member linked = memberService.getMemberById(linkTargetMemberId);
            if (linked == null) {
                response.sendError(404, "연동 대상 회원을 찾을 수 없습니다.");
                return;
            }

            // 세션 플래그 정리
            request.getSession().removeAttribute("OAUTH_MODE");
            request.getSession().removeAttribute("LINK_TARGET_MEMBER_ID");

            // ✅ JWT 발급 (DB 저장 X)
            String accessToken = jwtTokenProvider.generateAccessToken(linked.getId(), linked.getNickName(), linked.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(linked.getId(), linked.getNickName(), linked.getEmail());

            String redirectUrl = "http://localhost:3000/DiFF/home/main"
                    + "?access_token=" + accessToken
                    + "&refresh_token=" + refreshToken
                    + "&linked=" + provider;
            response.sendRedirect(redirectUrl);
            return;
        }

        // 🔐 일반 소셜 로그인
        OAuthAccount oAuthAccount = oAuthAccountService.findByProviderAndOauthId(provider, oauthId);
        if (oAuthAccount == null) {
            throw new RuntimeException("❌ 해당 OAuth 계정을 찾을 수 없습니다.");
        }

        Member member = memberService.getByOauthIdAndProvider(oauthId, provider);
        if (member == null) {
            throw new RuntimeException("❌ 해당 OAuth ID와 Provider에 해당하는 회원이 없습니다.");
        }

        rq.login(member);
        rq.setLoginedMember(member);

        // ✅ JWT 발급 (DB 저장 X)
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getNickName(), member.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId(), member.getNickName(), member.getEmail());

        String redirectUrl = "http://localhost:3000/DiFF/home/main"
                + "?access_token=" + accessToken
                + "&refresh_token=" + refreshToken;
        response.sendRedirect(redirectUrl);
    }
}
