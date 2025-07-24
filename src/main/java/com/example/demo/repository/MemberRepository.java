package com.example.demo.repository;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import com.example.demo.vo.Member;

import java.util.Optional;

@Mapper
public interface MemberRepository {

    //	@Insert("INSERT INTO `member` SET regDate = NOW(), loginId = #{loginId}, loginPw = #{loginPw}, `name` = #{name}")
    public int doJoin(String loginId, String loginPw, String name, String nickName, String email);

    //	@Select("SELECT * FROM `member` WHERE id = #{id}")
    public void doLogin(int id);

    public int getLastInsertId();

    //  @Select("SELECT * FROM `member` WHERE loginId = #{loginId}")
    public Member getMemberById(Long id);

    //  @Select("SELECT COUNT(*) FROM `member` WHERE loginId = #{loginId}")
    public int isJoinableLogInId(String loginId);

    //  @Select("SELECT COUNT(*) FROM `member` WHERE `name` = #{name} AND email = #{email}")
    public int isExistsNameNEmail(String name, String email);

    //  @Select("SELECT * FROM `member` WHERE loginId = #{loginId}")
    public Member getMemberByLoginId(String loginId);

    public int modifyMember(long loginedMemberId, String loginId, String loginPw, String name, String nickName, String email);

    public Member getByOauthId(String oauthId);

    public void save(Member member);

    public Member getMemberByEmail(String email);

    public void saveMember(Member member);

    public Member getById(Long memberId);
}