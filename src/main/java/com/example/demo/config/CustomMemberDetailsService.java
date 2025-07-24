package com.example.demo.config;

import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// CustomUserDetails -> CustomUserDetailsService
@Service
@RequiredArgsConstructor
public class CustomMemberDetailsService implements UserDetailsService {

    private final MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // MyBatis 방식: 조회 결과가 null일 수 있음
        Member member = memberService.getMemberByLoginId(loginId);
        if (member == null) {
            throw new UsernameNotFoundException("해당 유저가 없습니다. username=" + loginId);
        }
        return new CustomMemberDetails(member);
    }

    // 필요하다면 ID 조회 버전도
    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        Member member = memberService.getMemberById(userId);
        if (member == null) {
            throw new IllegalArgumentException("해당 유저가 없습니다. userId=" + userId);
        }
        return new CustomMemberDetails(member);
    }


}

// UserDetailsService를 User 기반으로 implements
// MemberRepository(원문은 User) 를 통해 User Entity 에 접근,
// Authority 가 추가된 CustomUserDetails 객체를 반환하는 과정을 거침.
// 우선 이름만 찾는 거 넣음.
