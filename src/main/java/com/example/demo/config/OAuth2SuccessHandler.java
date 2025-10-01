package com.example.demo.config;

import com.example.demo.service.GitHubOAuth2UserService;
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
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired private @Lazy MemberService memberService;
    @Autowired private OAuthAccountService oAuthAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired private Rq rq;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private GitHubOAuth2UserService gitHubOAuth2UserService;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        String oauthId = "google".equals(registrationId) ? oauthUser.getAttribute("sub") : oauthUser.getName();
        if (oauthId == null) throw new RuntimeException("❌ OAuth ID 를 가져올 수 없습니다.");

        String provider = registrationId;

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );
        String providerAccessToken = client.getAccessToken().getTokenValue();

        // 신규/기존 회원 처리
        Member member = memberService.getByProviderAndOauthId(oauthId, provider);
        if (member == null) {
            String email = oauthUser.getAttribute("email");

            // GitHub의 경우 이메일이 null일 수 있으므로 GitHubOAuth2UserService 호출
            if ((email == null || email.isBlank()) && "github".equalsIgnoreCase(provider)) {
                email = gitHubOAuth2UserService.fetchPrimaryEmail(providerAccessToken);
            }

            if (email == null || email.isBlank()) {
                response.sendError(400, "GitHub 계정에서 이메일을 가져올 수 없습니다. GitHub 설정에서 primary 이메일을 등록하거나 공개하세요.");
                return;
            }

            String name  = oauthUser.getAttribute("name");
            if (name == null || name.isBlank()) {
                int at = (email != null ? email.indexOf('@') : -1);
                name = (at > 0 ? email.substring(0, at) : "user_" + UUID.randomUUID().toString().substring(0, 8));
            }
            if (name.length() > 20) name = name.substring(0, 20);

            member = memberService.processOAuthLogin(provider, oauthId, email, name);
        }

        rq.login(member);
        rq.setLoginedMember(member);

        oAuthAccountService.upsertAccessToken(member.getId(), provider, oauthId, providerAccessToken, "Bearer");

        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getNickName(), member.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId(), member.getNickName(), member.getEmail());

        String redirectUrl = "https://diff.io.kr/DiFF/home/main"
                + "?access_token=" + accessToken
                + "&refresh_token=" + refreshToken;
        response.sendRedirect(redirectUrl);
    }
}

