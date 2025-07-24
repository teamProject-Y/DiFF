package com.example.demo.vo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 여기는 JWT 토큰을 저장, 관리함
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auth {
    private Long id; // PK

    // 클라이언트측에서 서버측으로 API 요청을 보낼 때 사용됨
    // 로그인 요청 시 사용
    private String loginId;
    private String loginPw;

    // 토큰 정보
    private String tokenType;      // Bearer 등
    private String accessToken;    // 짧은 수명 토큰
    private String refreshToken;   // 긴 수명 토큰

    // 연관된 멤버 ID
    private Long memberId;         // FK (member.id)
    private LocalDateTime regDate;      // 생성일시
    private LocalDateTime updateDate; // 수정일시 (선택)



    /**
     * 로그인 요청 정보로부터 Auth VO 생성
     */
    public static Auth fromLoginRequest(String loginId, String loginPw) {
        return Auth.builder()
                .loginId(loginId)
                .loginPw(loginPw)
                .build();
    }

    /**
     * DB 조회 결과(Auth VO) -> 응답용 모델로 그대로 사용
     * (MyBatis 매퍼가 직접 Auth 객체를 반환하도록 설정)
     */
}
