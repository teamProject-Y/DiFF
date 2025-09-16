package com.example.demo.repository;

import com.example.demo.vo.Auth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthRepository {

    void insert(Auth newAuth);

    Auth findByRefreshToken(String refreshToken);

    void updateAccessToken(Long id, String newAccessToken);

    String getTokenByMemberIdAndProvider(Long memberId, String provider);
}
