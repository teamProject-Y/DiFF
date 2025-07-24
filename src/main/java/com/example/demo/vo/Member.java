package com.example.demo.vo;

import java.time.LocalDateTime;

import com.example.demo.domain.Role;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    private Long id;

    private String oauthId;

    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    private String loginId;
    private String loginPw;
    private String name;
    private String nickName;
    private String email;

    private boolean delStatus;
    private LocalDateTime delDate;

    private boolean verified;

    private Role role;
    private String contact;

    // OAuth 로그인 정보로부터 새 Member vo 생성 ? (기본 ROLE_USER 설정)
    public static Member fromOAuthLogin(String oauthId, String email, String nickName) {
        return Member.builder()
                .oauthId(oauthId)
                .email(email)
                .nickName(nickName)
                .role(Role.ROLE_USER)
                .build();
    }

}