package com.example.demo.config;


import com.example.demo.vo.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

// Security에서 인증에 사용하기 위한 User 정보를 담은 객체.
// interface UserDetails를 implements 하여 CustomUserDetails로 커스텀
// 웹 시큐리티 JWT의 데이터 흐름에서 얘가 1번째다.
// User.java를 기반으로 implements 함
// 쉽게 말하면 User에 Authority(권한)가 추가된 커스텀유저 클래스인 느낌이라고 함
public class CustomMemberDetails implements UserDetails {
    private final Member member;

    // Constructor
    public CustomMemberDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_USER, ROLE_ADMIN 같은 enum.name() 을 권한으로 사용
        return Collections.singleton(new SimpleGrantedAuthority(member.getRole().name()));
    }

    /// ////////////////////////////////////
    
    public Long getId() {
        return member.getId();
    }
    
    public String getEmail() {
        return member.getEmail();
    }
    
    public String getContact() {
        return member.getContact();
    }
    
    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override
    public String getPassword() {
        return member.getLoginPw();
    }

    /// /////////////////////////////////
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
