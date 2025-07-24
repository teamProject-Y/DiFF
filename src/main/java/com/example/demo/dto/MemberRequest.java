package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequest {

    @NotBlank(groups = { ValidationGroups.Join.class, ValidationGroups.Modify.class })
    private String loginId;

    @NotBlank(groups = ValidationGroups.Join.class)
    private String loginPw;

    // 가입 시에만 확인용 필드로 사용
    @NotBlank(groups = ValidationGroups.Join.class)
    private String checkLoginPw;

    @NotBlank(groups = { ValidationGroups.Join.class, ValidationGroups.Modify.class })
    private String name;

    @NotBlank(groups = { ValidationGroups.Join.class, ValidationGroups.Modify.class })
    private String nickName;

    @NotBlank(groups = { ValidationGroups.Join.class, ValidationGroups.Modify.class })
    @Email(groups = { ValidationGroups.Join.class, ValidationGroups.Modify.class })
    private String email;

}
