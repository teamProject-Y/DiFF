package com.example.demo.controller;

import com.example.demo.service.MemberService;
import com.example.demo.vo.Member;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import util.Ut;

@RequiredArgsConstructor
@RestController
@RequestMapping("/usr/member")
public class UsrMemberApiController {

    @Autowired
    private Rq rq;

    private final MemberService memberService;
    private final HttpSession session;

    @PostMapping("/join")
    public ResponseEntity<ResultData> doJoin(@RequestBody Member dto) {
        if (Ut.isEmpty(dto.getLoginId()))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","아이디를 쓰시오"));
        // ... 기타 유효성 검사 ...

        long newId = memberService.doJoin(
                dto.getLoginId(), dto.getLoginPw(),
                dto.getName(), dto.getNickName(),
                dto.getCellPhone(), dto.getEmail()
        );

        if (newId < 1)
            return ResponseEntity.badRequest().body(ResultData.from("F-8","이미 사용 중인 정보가 있습니다"));

        Member m = memberService.getMemberById(newId);
        return ResponseEntity.ok(ResultData.from("S-1", m.getNickName()+"님 가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member dto) {
        if (Ut.isEmpty(dto.getLoginId()))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","아이디를 입력해주세요"));
        if (Ut.isEmpty(dto.getLoginPw()))
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));

        Member m = memberService.getMemberByLoginId(dto.getLoginId());
        if (m == null)
            return ResponseEntity.badRequest().body(ResultData.from("F-3","존재하지 않는 아이디"));
        if (!m.getLoginPw().equals(dto.getLoginPw()))
            return ResponseEntity.badRequest().body(ResultData.from("F-A","비밀번호 불일치"));

        session.setAttribute("loginedMemberId", m.getId());
        return ResponseEntity.ok(ResultData.from("S-1", m.getNickName()+"님 환영"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResultData> doLogout() {
        session.invalidate();
        return ResponseEntity.ok(ResultData.from("S-1","로그아웃 되었습니다"));
    }

    @GetMapping("/myInfo")
    public ResponseEntity<?> myInfo() {
        Long id = (Long)session.getAttribute("loginedMemberId");
        if (id == null)
            return ResponseEntity.status(401).body(ResultData.from("F-1","로그인 후 이용해주세요"));
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @GetMapping("/checkPw")
    public ResponseEntity<ResultData> checkPw(@RequestParam String pw) {
        Long id = (Long)session.getAttribute("loginedMemberId");
        Member m = memberService.getMemberById(id);
        if (!m.getLoginPw().equals(pw))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","비밀번호 불일치"));
        return ResponseEntity.ok(ResultData.from("S-1","비밀번호 확인 완료"));
    }

    @PutMapping("/modify")
    public ResponseEntity<ResultData> doModify(@RequestBody Member dto) {
        Long id = (Long)session.getAttribute("loginedMemberId");
        // ... 유효성 검사 ...
        memberService.modifyMember(
                id, dto.getLoginId(), dto.getLoginPw(),
                dto.getName(), dto.getNickName(),
                dto.getCellPhone(), dto.getEmail()
        );
        return ResponseEntity.ok(ResultData.from("S-1","정보가 수정되었습니다"));
    }
}