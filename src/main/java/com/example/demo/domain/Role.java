package com.example.demo.domain;

// 사용자 권한을 표시함
public enum Role {

    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN");

    // "USER", "ADMIN"
    private String value; // 실제로 DB나 토큰에 기록할 값

    // Constructor
    Role(String value) { // 생성자
        this.value = value;
    }

    // GetValue
    public String getValue() { // 필요할 때 꺼내 쓰는 메소드
        return this.value;
    }
}
