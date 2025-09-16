package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.vo.Member;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberRepository {

    int join(@Param("loginPw") String loginPw,
                    @Param("nickName") String nickName,
                    @Param("email") String email);

    Member getByOauthIdAndProvider(@Param("oauthId") String oauthId,
                                          @Param("provider") String provider);

    void uploadProfileImg(@Param("memberId") Long memberId,
                                 @Param("profileUrl") String profileUrl);

    int isExistsEmail(String email);

    Long getLastInsertId();

    Member getMemberById(Long id);

    Member getMemberByEmail(String email);

    void saveMember(Member member);

    List<Member> getFollowingList(Long memberId);

    Member getMemberByNickName(String nickName);

    void follow(@Param("toMemberId") Long toMemberId,
                @Param("fromMemberId") Long fromMemberId);

    void unfollow(@Param("toMemberId") Long toMemberId,
                  @Param("fromMemberId") Long fromMemberId);

    List<Member> getFollowerList(Long memberId);

    int modifyNickName(Long memberId,String nickName);

    int countByNickName(String nickName);

    int modifyIntroduce(Long memberId, String introduce);

    void updateFcmToken(Long memberId, String token);

    Member getFcmTokenById(Long memberId);

    void saveFcmToken(Long memberId, String fcmToken);

    void updateEmailVerificationToken(@Param("memberId") Long memberId,
                                      @Param("token") String token,
                                      @Param("expiry") String expiry);

    Member findByEmailVerificationToken(@Param("token") String token);

    void verifyEmail(@Param("memberId") Long memberId);

    void updateResetToken(@Param("memberId") Long memberId,
                          @Param("token") String token,
                          @Param("expiry") String expiry);

    Member findByResetToken(@Param("token") String token);

    void updatePassword(@Param("memberId") Long memberId,
                        @Param("loginPw") String loginPw);

    void clearResetToken(Long id);

    List<Member> searchMembers(String keyword);

    int isExistsNickName(String nickName);

    void updateNotificationSetting(Long memberId, String type, boolean enabled);

    int deleteAccount(Long id);
}