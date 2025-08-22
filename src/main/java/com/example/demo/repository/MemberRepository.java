package com.example.demo.repository;

import com.example.demo.vo.Follow;
import org.apache.ibatis.annotations.Mapper;

import com.example.demo.vo.Member;

import java.util.List;

@Mapper
public interface MemberRepository {

    // 지울 거
    public int doJoin(String loginId, String loginPw, String name, String nickName, String email);

    // 남길 거
    public int join(String loginPw, String nickName, String email);

    // 지울 거
    public int isExistsNameNEmail(String name, String email);

    // 남길 거
    public int isExistsEmail(String email);

    public void doLogin(int id);

    public Long getLastInsertId();

    public Member getMemberById(Long id);

    // 지울 거
    public Member getMemberByLoginId(String loginId);

    public Member getMemberByEmail(String email);

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email);

    public Member getByOauthId(String oauthId);

    public void save(Member member);

    public void saveMember(Member member);

    public Member getById(Long memberId);

    public Member getByOauthIdAndProvider(String oauthId, String provider);

    public List<Follow> getFollowsByMemberId(Long memberId);

    public List<Member> getFollowingList(Long memberId);

    public Member getMemberByNickName(String nickName);

    public void uploadProfileImg(Long memberId, String profileUrl);
}