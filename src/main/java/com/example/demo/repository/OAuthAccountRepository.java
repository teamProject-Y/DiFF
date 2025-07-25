package com.example.demo.repository;

import com.example.demo.vo.OAuthAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthAccountRepository {

    public OAuthAccount findByProviderAndOauthId(String provider,String oauthId);

    public void saveOAuthAccount(OAuthAccount account);
}