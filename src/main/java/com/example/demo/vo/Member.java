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

    public Member(String loginId, String loginPw, String name, String nickName, String email) {
        this.loginId = loginId;
        this.loginPw = loginPw;
        this.name = name;
        this.nickName = nickName;
        this.email = email;
    }
    private Long id;
    private String oauthId;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;
    private String loginId;
    private String loginPw;
    private String checkLoginPw;
    private String name;
    private String nickName;
    private String email;
    private boolean delStatus;
    private LocalDateTime delDate;

    private boolean verified;
    private Role role;
    private String contact;

}