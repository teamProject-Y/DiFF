//package com.example.demo.domain;
//
//import jakarta.persistence.*;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@NoArgsConstructor
//@Getter
//@Entity
//public class Member extends BaseTime {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(length = 50, nullable = false, unique = true)
//    private String email;
//
//    @Column(length = 50, nullable = false, unique = true)
//    private String contact;
//
//    @Column(length = 50, nullable = false, unique = true)
//    private String username;
//
//    @Column(length = 50, nullable = false, unique = true)
//    private String password;
//
//    @Enumerated(EnumType.STRING)
//    private Role role;
//
//    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE)
//    private Auth auth;
//
//    @Builder
//    public Member(String email, String contact, String username, String password, Role role) {
//        this.role = role;
//        this.email = email;
//        this.contact = contact;
//        this.username = username;
//        this.password = password;
//    }
//
//}
