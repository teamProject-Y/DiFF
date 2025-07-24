package com.example.demo.service;

import com.example.demo.repository.OAuthAccountRepository;
import com.example.demo.vo.Member;
import com.example.demo.vo.OAuthAccount;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.repository.MemberRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {


    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuthAccountRepository oAuthAccountRepository;

    // 회원 조회
    public Member getMemberById(Long id) {
        return memberRepository.getMemberById(id);
    }
    
    
    // 회원 가입
    public Long doJoin(String loginId, String loginPw, String name, String nickName, String email) {

        if(memberRepository.isJoinableLogInId(loginId) == 1) return -1L; // 중복 아이디
        if(memberRepository.isExistsNameNEmail(name, email) == 1) return -2L; // 중복 이름, 이메일

        // 비밀번호 암호화 후 저장
        String encPw = passwordEncoder.encode(loginPw);
        memberRepository.doJoin(loginId, encPw, name, nickName, email);

        return (long) memberRepository.getLastInsertId(); // 방금 가입된 멤버의 id 반환
    }

    public Member getMemberByLoginId(String loginId) {
        return memberRepository.getMemberByLoginId(loginId);
    }

    // 회원 정보 수정
    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email) {
        String encPw = passwordEncoder.encode(loginPw);
        return memberRepository.modifyMember(loginedMemberId, loginId, encPw, name, nickName, email);
    }

//    // 회원 삭제
//    public void deleteMember(long loginId) {
//        Member member =
//    }

    // OAuth 로그인/연동 처리
    public Member processOAuthLogin(String provider, String oauthId, String email, String nickName) {
        System.out.println("procOAuthlogin 진입");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("OAuth 로그인 실패: 이메일이 존재하지 않음");
        }

        email = email.trim();

        // 이미 연결된 계정인지 확인
        OAuthAccount account = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (account != null) {
            return memberRepository.getById(account.getMemberId());
        }

        // 이메일로 기존 회원 확인
        Member member = memberRepository.getMemberByEmail(email);
        if (member == null) {
            // 없다면 새로 등록
            member = new com.example.demo.vo.Member();
            member.setEmail(email);
            member.setNickName(nickName);
            memberRepository.saveMember(member);
            System.out.println("memberService : " + member);
        }

        // oauth_account 등록
        OAuthAccount newAccount = OAuthAccount.builder()
                .memberId(member.getId())
                .provider(provider)
                .oauthId(oauthId)
                .build();
        oAuthAccountRepository.saveOAuthAccount(newAccount);

        System.out.println("processOAuthLogin email: " + email + ", nickName: " + nickName);

        return member;
    }


    public Member getByProviderAndOauthId(String provider, String oauthId) {
        OAuthAccount acc = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (acc == null) return null;
        return memberRepository.getById(acc.getMemberId());
    }

    public Integer isVerifiedUser(String email) {
        Member member = memberRepository.getMemberByEmail(email);
        if(member == null) return null;
        else return Math.toIntExact(member.getId());
    }
}