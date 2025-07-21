package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.MemberRepository;
import com.example.demo.vo.Member;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member getMemberById(Long id) {

        return memberRepository.getMemberById(id);
    }

    public int doJoin(String loginId, String loginPw, String name, String nickName, String cellPhone, String email) {

        if(memberRepository.isJoinableLogInId(loginId) == 1) return -1; // 중복 아이디
        if(memberRepository.isExistsNameNEmail(name, email) == 1) return -2; // 중복 이름, 이메일

        memberRepository.doJoin(loginId, loginPw, name, nickName, cellPhone, email);
        return memberRepository.getLastInsertId(); // 방금 가입된 멤버의 id 반환
    }

    public Member getMemberByLoginId(String loginId) {

        return memberRepository.getMemberByLoginId(loginId);
    }

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String cellPhone, String email) {
        return memberRepository.modifyMember(loginedMemberId, loginId, loginPw, name, nickName, cellPhone, email);
    }

    public boolean isUsableLoginId(String loginId) {
        return memberRepository.isJoinableLogInId(loginId) != 1;
    }

    public void processOAuthPostLogin(String oauthId, String username, String email) {
        System.out.println("📥 processOAuthPostLogin() 진입");
        System.out.println("➡️ oauthId: " + oauthId + ", username: " + username + ", email: " + email);

        Member existing = memberRepository.getByOauthId(oauthId);
        System.out.println("🔎 기존 회원 조회 결과: " + existing);

        if (existing == null) {
            Member newMember = Member.builder()
                    .oauthId(oauthId)
                    .nickName(username)
                    .email(email)
                    .build();

            System.out.println("🆕 신규 회원 저장 시도: " + newMember);
            memberRepository.save(newMember);
            System.out.println("✅ 저장 완료");
        } else {
            System.out.println("ℹ️ 이미 존재하는 회원 - 저장 생략");
        }
    }


    public Member getByOauthId(String oauthId) {
        return memberRepository.getByOauthId(oauthId);
    }
}