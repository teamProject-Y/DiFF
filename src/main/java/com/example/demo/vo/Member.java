package com.example.demo.vo;

import java.time.LocalDateTime;
import com.example.demo.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private String loginId;
    private String loginPw;
    private String checkLoginPw;
    private String nickName;
    private String email;
    private boolean delStatus;
    private LocalDateTime delDate;
    private String profileUrl;
    private Role role;
    private String introduce;
    private String fcmToken;
    private String githubUrl;

    private Boolean isVerified;
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiry;
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    private Long extra__likeCounts;
    private Long extra__repoCounts;
    private Long extra__postCounts;
}