package com.example.demo.service;

import com.example.demo.repository.*;
import com.example.demo.vo.Member;
import com.example.demo.vo.OAuthAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final RepositoryRepository repositoryRepository;

    private final ArticleRepository articleRepository;

    private final ReactionRepository reactionRepository;

    private final MailService mailService;

    private final PasswordEncoder passwordEncoder;

    private final OAuthAccountRepository oAuthAccountRepository;

    public Member getMemberById(Long id) {
        return memberRepository.getMemberById(id);
    }

    public Long join(String loginPw, String checkLoginPw, String nickName, String email) {
        // 이메일 중복 체크
        if (memberRepository.isExistsEmail(email) == 1) return -409L;

        // 닉네임 중복 체크
        if (memberRepository.isExistsNickName(nickName) == 1) return -410L;

        // 비밀번호 불일치 체크
        if (!loginPw.equals(checkLoginPw)) return -400L;

        // 비밀번호 암호화
        String encPw = passwordEncoder.encode(loginPw);

        // 회원 저장 (isVerified = false)
        memberRepository.join(encPw, nickName, email);
        Long memberId = (long) memberRepository.getLastInsertId();

        // 이메일 인증 토큰 생성 & DB 업데이트
        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusDays(1); // 24시간 유효
        memberRepository.updateEmailVerificationToken(memberId, token, String.valueOf(expiry));

        // 인증 메일 발송
        String link = "http://diff.io.kr/api/DiFF/member/verify?token=" + token;
        mailService.sendMail(email, "이메일 인증",
                nickName + "님, 아래 링크를 클릭하여 이메일 인증을 완료하세요:\n" + link);

        return memberId;
    }


    public void verifyEmail(String token) {
        System.out.println("📌 verifyEmail() 실행됨, token=" + token);

        Member member = memberRepository.findByEmailVerificationToken(token);
        if (member == null) {
            throw new RuntimeException("잘못된 토큰입니다.");
        }

        if (member.getEmailVerificationExpiry() != null &&
                member.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("토큰이 만료되었습니다.");
        }

        memberRepository.verifyEmail(member.getId());
        System.out.println("✅ 이메일 인증 완료 → memberId=" + member.getId());
    }

    public Member getMemberByEmail(String email) {
        return memberRepository.getMemberByEmail(email);
    }

    public Member processOAuthLogin(String provider, String oauthId, String email, String nickName) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("OAuth 로그인 실패: 이메일이 존재하지 않음");
        }

        email = email.trim();
        // 이미 연결된 계정 확인
        OAuthAccount account = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (account != null) {
            return memberRepository.getMemberById(account.getMemberId());
        }

        // 이메일로 기존 회원 확인
        Member member = memberRepository.getMemberByEmail(email);
        if (member == null) {
            // 없다면 새로 등록
            member = new Member();
            member.setEmail(email);
            member.setNickName(nickName);
            memberRepository.saveMember(member);
        }

        // oauth_account 등록
        OAuthAccount newAccount = OAuthAccount.builder()
                .memberId(member.getId())
                .provider(provider)
                .oauthId(oauthId)
                .build();
        oAuthAccountRepository.saveOAuthAccount(newAccount);

        return member;
    }

    public Member getByProviderAndOauthId(String provider, String oauthId) {
        OAuthAccount acc = oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
        if (acc == null) return null;
        return memberRepository.getMemberById(acc.getMemberId());
    }

    public Integer isVerifiedUser(String email) {
        Member member = memberRepository.getMemberByEmail(email);
        if (member == null) return null;
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

    public void follow(Long toMemberId, Long fromMemberId) {
        memberRepository.follow(toMemberId, fromMemberId);
    }

    public void unfollow(Long toMemberId, Long fromMemberId) {
        memberRepository.unfollow(toMemberId, fromMemberId);
    }

    public List<Member> getFollowerList(Long memberId) {
        return memberRepository.getFollowerList(memberId);
    }

    public int modifyNickName(Long memberId, String nickName) {
        if (memberRepository.countByNickName(nickName) > 0) {
            return -1; // 중복
        }
        return memberRepository.modifyNickName(memberId, nickName);
    }

    public int modifyIntroduce(Long memberId, String introduce) {
        return memberRepository.modifyIntroduce(memberId, introduce);
    }

    public void updateFcmToken(Long memberId, String token) {
        memberRepository.updateFcmToken(memberId, token);
    }

    public void saveFcmToken(Long memberId, String fcmToken) {
        memberRepository.saveFcmToken(memberId, fcmToken);
    }

    public void requestPasswordReset(String email) {
        Member member = memberRepository.getMemberByEmail(email);
        if (member == null) throw new RuntimeException("없는 회원");

        if (member.getResetToken() != null &&
                member.getResetTokenExpiry() != null &&
                member.getResetTokenExpiry().isAfter(LocalDateTime.now())) {

            System.out.println("♻ 기존 resetToken 재사용: " + member.getResetToken());

        } else {
            // 새 토큰 발급
            String token = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusHours(1);

            memberRepository.updateResetToken(member.getId(), token, String.valueOf(expiry));
            member.setResetToken(token);
            member.setResetTokenExpiry(expiry);

            System.out.println("🆕 새 resetToken 발급: " + token);
        }

        String link = "http://diff.io.kr:3000/DiFF/member/resetPw?token=" + member.getResetToken();
        mailService.sendMail(email, "비밀번호 재설정",
                "비밀번호를 바꾸려면 클릭: " + link + "\n\n만료 시간: " + member.getResetTokenExpiry());
    }

    public void updatePassword(String token, String newPw) {
        Member member = memberRepository.findByResetToken(token);
        if (member == null) throw new RuntimeException("잘못된 토큰");

        if (member.getResetTokenExpiry() != null &&
                member.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("토큰 만료");
        }

        memberRepository.updatePassword(
                member.getId(),
                passwordEncoder.encode(newPw)
        );

        memberRepository.clearResetToken(member.getId());
    }


    public List<Member> searchMembers(String keyword) {
        return memberRepository.searchMembers("%" + keyword + "%");
    }

    public Member updateMemberForPrint(Member member) {

        member.setExtra__likeCounts(reactionRepository.getLikeCountsByMemberId(member.getId()));
        member.setExtra__repoCounts(repositoryRepository.getRepoCountsByMemberId(member.getId()));
        member.setExtra__postCounts(articleRepository.getArticleCountsByMemberId(member.getId()));

        return member;
    }

    public void updateNotificationSetting(Long memberId, String type, boolean enabled) {
        memberRepository.updateNotificationSetting(memberId, type, enabled);
    }

    public int deleteAccount(Long id) {
        return memberRepository.deleteAccount(id);
    }
}
