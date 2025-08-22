package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.vo.Member;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberRepository {

    public int join(@Param("loginPw") String loginPw,
                    @Param("nickName") String nickName,
                    @Param("email") String email);

    public Member getByOauthIdAndProvider(@Param("oauthId") String oauthId,
                                          @Param("provider") String provider);

    public void uploadProfileImg(@Param("memberId") Long memberId,
                                 @Param("profileUrl") String profileUrl);

    public int modifyMember(@Param("loginedMemberId") long loginedMemberId,
                            @Param("loginId") String loginId,
                            @Param("loginPw") String loginPw,
                            @Param("name") String name,
                            @Param("nickName") String nickName,
                            @Param("email") String email);

    public int isExistsEmail(String email);

    public Long getLastInsertId();

    public Member getMemberById(Long id);

    public Member getMemberByEmail(String email);

    public void saveMember(Member member);

    public Member getById(Long memberId);

    public List<Member> getFollowingList(Long memberId);

    public Member getMemberByNickName(String nickName);
}