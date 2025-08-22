package com.example.demo.repository;

import com.example.demo.vo.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OAuthAccountRepository {

    OAuthAccount findByOauthId(@Param("oauthId") String oauthId);

    OAuthAccount findByProviderAndOauthId(@Param("provider") String provider,
                                          @Param("oauthId") String oauthId);

    public void saveOAuthAccount(OAuthAccount account);


    OAuthAccount findById(@Param("id") Long id);

    int insert(@Param("memberId") Long memberId,
               @Param("provider") String provider,
               @Param("oauthId") String oauthId);

    int attachToMember(@Param("id") Long id,
                       @Param("memberId") Long memberId);
}