package com.example.demo.service;

import com.example.demo.vo.Member;
import com.example.demo.vo.Rq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service("googleOAuth2UserService")
public class GoogleOAuth2UserService extends DefaultOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private MemberService memberService;

    @Autowired
    private Rq rq;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String oauthId = oauthUser.getAttribute("sub");
        String username = oauthUser.getAttribute("name");
        String email = oauthUser.getAttribute("email");

        memberService.processOAuthLogin(provider, oauthId, email, username);
        Member member = memberService.getByProviderAndOauthId(oauthId,provider);
        System.out.println(oauthId+" "+username + " " + email);

        if (member != null) {
            rq.login(member);
        }

        return oauthUser;
    }
}