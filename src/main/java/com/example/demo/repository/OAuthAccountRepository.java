package com.example.demo.repository;

import com.example.demo.vo.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OAuthAccountRepository {

    OAuthAccount findByOauthId(@Param("oauthId") String oauthId);

    OAuthAccount findByProviderAndOauthId(@Param("provider") String provider,
                                          @Param("oauthId") String oauthId);

    OAuthAccount findById(@Param("id") Long id);

    int insert(@Param("memberId") Long memberId,
               @Param("provider") String provider,
               @Param("oauthId") String oauthId);

    int attachToMember(@Param("id") Long id,
                       @Param("memberId") Long memberId);

    void saveOAuthAccount(OAuthAccount account);

    List<String> findProvidersByMemberId(@Param("memberId") Long memberId);

    // ★ accessToken 업데이트 (id로)
    int updateAccessToken(@Param("id") Long id,
                          @Param("accessToken") String accessToken);

    // ★ accessToken 업데이트 (provider + oauthId로)
    int updateAccessTokenByProviderAndOauthId(@Param("provider") String provider,
                                              @Param("oauthId") String oauthId,
                                              @Param("accessToken") String accessToken);

    // ★ memberId + provider 로 accessToken 조회
    String findAccessTokenByMemberIdAndProvider(@Param("memberId") Long memberId,
                                                @Param("provider") String provider);
}
