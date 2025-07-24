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

    private Rq rq;

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("해당 유저 존재안함. 유저 이름 : " + username));
        return new CustomMemberDetails(member); // 위에서 생성한 CustomerUserDetails Class
    }

    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        Member member = memberService.getMemberById(rq.getLoginedMemberId())
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저 존재안함. 유저 아이디 : " + userId));
        return new CustomMemberDetails(member); // 위에서 생성한 CustomerUserDetails Class
    }
}

// UserDetailsService를 User 기반으로 implements
// MemberRepository(원문은 User) 를 통해 User Entity 에 접근,
// Authority 가 추가된 CustomUserDetails 객체를 반환하는 과정을 거침.
// 우선 이름만 찾는 거 넣음.
