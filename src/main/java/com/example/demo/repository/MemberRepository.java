package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.vo.Member;

@Mapper
public interface MemberRepository {

    public int doJoin(String loginId, String loginPw, String name, String nickName, String email);

    public void doLogin(int id);

    public Long getLastInsertId();

    public Member getMemberById(Long id);

    public int isJoinableLogInId(String loginId);

    public int isExistsNameNEmail(String name, String email);

    public Member getMemberByLoginId(String loginId);

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email);

    public Member getByOauthId(String oauthId);

    public void save(Member member);

    public Member getMemberByEmail(String email);

    public void saveMember(Member member);

    public Member getById(Long memberId);

    Member getByOauthIdAndProvider(String oauthId, String provider);
}