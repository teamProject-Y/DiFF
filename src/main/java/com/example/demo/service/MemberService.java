package com.example.demo.service;

import com.example.demo.repository.OAuthAccountRepository;
import com.example.demo.vo.Follow;
import com.example.demo.vo.OAuthAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuthAccountRepository oAuthAccountRepository;

    // 회원 조회
    public Member getMemberById(Long id) {

        return memberRepository.getMemberById(id);
    }

    // 회원가입
    public Long join(String loginPw, String checkLoginPw, String nickName, String email) {

        if(memberRepository.isExistsEmail(email) == 1) return -409L; // 중복 이메일
        if(!loginPw.equals(checkLoginPw)) return -400L; // 비밀번호

        // 비밀번호 암호화 후 저장
        String encPw = passwordEncoder.encode(loginPw);
        memberRepository.join(encPw, nickName, email);

        return (long) memberRepository.getLastInsertId(); // 방금 가입된 멤버의 id 반환
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.getMemberByEmail(email);
    }

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email) {
        Member member = memberRepository.getMemberById(loginedMemberId);

        if (loginPw != null && !loginPw.trim().isEmpty()) {
            member.setLoginPw(passwordEncoder.encode(loginPw));
        }
        if (loginId != null) member.setLoginId(loginId);
        if (name != null) member.setName(name);
        if (nickName != null) member.setNickName(nickName);
        if (email != null) member.setEmail(email);

        return memberRepository.modifyMember(loginedMemberId, loginId, loginPw, name, nickName, email);
    }

    // OAuth 로그인/연동 처리
    public Member processOAuthLogin(String provider, String oauthId, String email, String nickName) {
        System.out.println("procOAuthlogin 진입");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("OAuth 로그인 실패: 이메일이 존재하지 않음");
        }

        email = email.trim();
        System.out.println("1. processOAuthLogin email: " + email + ", nickName: " + nickName);
        // 이미 연결된 계정인지 확인
        OAuthAccount account = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (account != null) {
            return memberRepository.getById(account.getMemberId());
        }

        // 이메일로 기존 회원 확인
        Member member = memberRepository.getMemberByEmail(email);
        if (member == null) {
            // 없다면 새로 등록
            member = new Member();
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

        System.out.println("2. processOAuthLogin email: " + email + ", nickName: " + nickName);

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

    public Member getByOauthIdAndProvider(String oauthId, String provider) {
        return memberRepository.getByOauthIdAndProvider(oauthId, provider);
    }

    public List<Member> getFollowingList(Long memberId) {
        return memberRepository.getFollowingList(memberId);
    }

    public Member getMemberByNickName(String nickName) {
        return memberRepository.getMemberByNickName(nickName);
    }

    public void uploadProfileImg(Long memberId, String profileUrl) {
        memberRepository.uploadProfileImg(memberId, profileUrl);
    }
}