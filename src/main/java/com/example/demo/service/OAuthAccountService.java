package com.example.demo.service;

import com.example.demo.repository.OAuthAccountRepository;
import com.example.demo.vo.OAuthAccount;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OAuthAccountService {

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    public OAuthAccountService(OAuthAccountRepository oAuthAccountRepository) {
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    public OAuthAccount findByProviderAndOauthId(String provider, String oauthId) {
        return oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
    }

    /**
     * 존재하면 그대로 반환, 없으면 생성 후 반환
     */
    @Transactional
    public OAuthAccount create(Long memberId, String provider, String oauthId) {
        OAuthAccount existing = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (existing != null) return existing;

        oAuthAccountRepository.insert(memberId, provider, oauthId);
        return oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
    }

    /**
     * 기존 oauthAccount 레코드를 특정 회원에 연결
     */
    @Transactional
    public void attachToMember(Long oauthAccountId, Long memberId) {
        oAuthAccountRepository.attachToMember(oauthAccountId, memberId);
    }

    public Map<String, Boolean> getLinkedProviders(Long memberId) {
        List<String> providers = oAuthAccountRepository.findProvidersByMemberId(memberId);
        boolean google = providers.stream().anyMatch("google"::equalsIgnoreCase);
        boolean github = providers.stream().anyMatch("github"::equalsIgnoreCase);
        return Map.of("google", google, "github", github);
    }

    public void upsertAccessToken(Long memberId, String provider, String oauthId,
                                  String accessToken, String tokenType) {
        // provider+oauthId로 찾고 없으면 insert, 있으면 update
        var row = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (row == null) {
            oAuthAccountRepository.insert(memberId, provider, oauthId); //, accessToken);
        } else {
            oAuthAccountRepository.updateAccessToken(row.getId(), accessToken);
            if (row.getMemberId() == null) oAuthAccountRepository.attachToMember(row.getId(), memberId);
        }
    }

    public String findGithubAccessTokenByMemberId(Long memberId) {
        return oAuthAccountRepository.findAccessTokenByMemberIdAndProvider(memberId, "github");
    }
}

