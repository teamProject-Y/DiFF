package com.example.demo.repository;

import com.example.demo.vo.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OAuthAccountRepository {


    OAuthAccount findByProviderAndOauthId(@Param("provider") String provider,
                                          @Param("oauthId") String oauthId);

    int insert(@Param("memberId") Long memberId,
               @Param("provider") String provider,
               @Param("oauthId") String oauthId);

    int attachToMember(@Param("id") Long id,
                       @Param("memberId") Long memberId);

    void saveOAuthAccount(OAuthAccount account);

    List<String> findProvidersByMemberId(@Param("memberId") Long memberId);

    // accessToken 업데이트
    int updateAccessToken(@Param("id") Long id,
                          @Param("accessToken") String accessToken);

    // memberId + provider 로 accessToken 조회
    String findAccessTokenByMemberIdAndProvider(@Param("memberId") Long memberId,
                                                @Param("provider") String provider);
}
