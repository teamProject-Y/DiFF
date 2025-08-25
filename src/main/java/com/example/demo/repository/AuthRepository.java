package com.example.demo.repository;

import com.example.demo.vo.Auth;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface AuthRepository {

    Auth findByMemberId(Long id);

    void updateTokens(Auth auth);

    void insert(Auth newAuth);

    Auth findByRefreshToken(String refreshToken);

    void updateAccessToken(Long id, String newAccessToken);

    void saveAuth(Auth auth);

    void updateAuthByMemberId(Auth auth);

    String getTokenByMemberIdAndProvider(Long memberId, String provider);
}
