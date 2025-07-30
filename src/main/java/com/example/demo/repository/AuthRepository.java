package com.example.demo.repository;

import com.example.demo.vo.Auth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthRepository {
    public Auth findByMemberId(Long id);

    public void updateTokens(Auth auth);

    public void insert(Auth newAuth);

    public Auth findByRefreshToken(String refreshToken);

    public void updateAccessToken(Long id, String newAccessToken);

    void saveAuth(Auth auth);

    void updateAuthByMemberId(Auth auth);
}
