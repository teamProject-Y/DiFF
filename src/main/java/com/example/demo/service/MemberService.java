package com.example.demo.service;

import java.util.List;

import com.example.demo.repository.OAuthAccountRepository;
import com.example.demo.vo.OAuthAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Member;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member getMemberById(Long id) {

        return memberRepository.getMemberById(id);
    }

    public int doJoin(String loginId, String loginPw, String name, String nickName, String email) {

        if(memberRepository.isJoinableLogInId(loginId) == 1) return -1; // 중복 아이디
        if(memberRepository.isExistsNameNEmail(name, email) == 1) return -2; // 중복 이름, 이메일

        memberRepository.doJoin(loginId, loginPw, name, nickName, email);
        return memberRepository.getLastInsertId(); // 방금 가입된 멤버의 id 반환
    }

    public Member getMemberByLoginId(String loginId) {

        return memberRepository.getMemberByLoginId(loginId);
    }

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email) {
        return memberRepository.modifyMember(loginedMemberId, loginId, loginPw, name, nickName, email);
    }

    //    public boolean isUsableLoginId(String loginId) {
//        return memberRepository.isJoinableLogInId(loginId) != 1;
//    }
//
//    public void processOAuthPostLogin(String oauthId, String username, String email) {
//        System.out.println("processOAuthPostLogin() 진입");
//        System.out.println("oauthId: " + oauthId + ", username: " + username + ", email: " + email);
//
//        Member existing = memberRepository.getByOauthId(oauthId);
//        System.out.println("기존 회원 조회 결과: " + existing);
//
//        if (existing == null) {
//            Member newMember = Member.builder()
//                    .oauthId(oauthId)
//                    .nickName(username)
//                    .email(email)
//                    .build();
//
//            System.out.println("신규 회원 저장 시도: " + newMember);
//            memberRepository.save(newMember);
//            System.out.println("저장 완료");
//        } else {
//            System.out.println("이미 존재하는 회원 -> 저장 생략");
//        }
//    }
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
        else return (int) member.getId();
    }
}