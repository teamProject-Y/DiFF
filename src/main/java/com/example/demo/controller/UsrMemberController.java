package com.example.demo.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.repository.RepositoryRepository;
import com.example.demo.service.RepositoryService;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import com.example.util.Ut;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/member")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class UsrMemberController {

    @Autowired
    private BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RepositoryService repositoryService;
    public UsrMemberController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

//    @PostMapping("/doJoin")
//    @ResponseBody
//    public ResponseEntity<ResultData> doJoin(@RequestBody Member member) {
//        System.out.println("✅ doJoin 진입");
//        System.out.println("입력 받은 비밀번호: " + member.getLoginPw());
//        System.out.println("입력 받은 비밀번호 확인: " + member.getCheckLoginPw());
//        System.out.println("입력 받은 닉네임: " + member.getNickName());
//        System.out.println("입력 받은 이메일: " + member.getEmail());
//
//        try {
//            // 1. 유효성 검사
//            if (Ut.isEmpty(member.getLoginPw()))
//                return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 작성하세요."));
//            if (Ut.isEmpty(member.getNickName()))
//                return ResponseEntity.badRequest().body(ResultData.from("F-4", "닉네임을 쓰시오"));
//            if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@"))
//                return ResponseEntity.badRequest().body(ResultData.from("F-6", "이메일 정확히 쓰시오"));
//
//            // 2. 회원가입 처리
//            long id = memberService.join(
//                    member.getLoginPw(),
//                    member.getCheckLoginPw(),
//                    member.getNickName(),
//                    member.getEmail()
//            );
//
//            if (id == -409)
//                return ResponseEntity.badRequest().body(ResultData.from("F-409", "이미 가입된 이메일입니다."));
//            if (id == -400)
//                return ResponseEntity.badRequest().body(ResultData.from("F-400", "비밀번호가 일치하지 않습니다."));
//
//            // 3. 가입된 회원 정보 다시 불러오기
//            Member newMember = memberService.getMemberByEmail(member.getEmail());
//
//            // 4. 곧바로 JWT 발급 (DB 저장 X)
//            String accessToken = jwtTokenProvider.generateAccessToken(
//                    newMember.getId(),
//                    newMember.getNickName(),
//                    newMember.getEmail()
//            );
//
//            String refreshToken = jwtTokenProvider.generateRefreshToken(
//                    newMember.getId(),
//                    newMember.getNickName(),
//                    newMember.getEmail()
//            );
//
//            // 5. 응답 반환 (자동 로그인 효과)
//            return ResponseEntity.ok(
//                    ResultData.from("S-1",
//                            newMember.getNickName() + " 님 회원가입을 축하합니다.",
//                            "accessToken", accessToken,
//                            "refreshToken", refreshToken
//                    )
//            );
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body(ResultData.from("F-ERR", "서버 오류: " + e.getMessage()));
//        }
//    }
//
//    @PostMapping("/doLogout")
//    public ResponseEntity<ResultData> doLogout(HttpServletRequest req) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//
//        rq.logout();
//
//        return ResponseEntity.ok(ResultData.from("S-1", "로그아웃 되었습니다"));
//
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {
//
//        System.out.println("doLogin 진입");
//
//        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
//            return ResponseEntity.badRequest().body(ResultData.from("F-1","이메일을 바르게 입력해주세요"));
//        }
//        if (Ut.isEmpty(member.getLoginPw())) {
//            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));
//        }
//
//        // ✅ 실제 회원 조회 (비밀번호 검증 포함)
//        Member foundMember = memberService.getMemberByEmail(member.getEmail());
//        if (foundMember == null) {
//            return ResponseEntity.status(401).body(ResultData.from("F-3","가입되지 않은 이메일입니다"));
//        }
//
//        if (!new BCryptPasswordEncoder().matches(member.getLoginPw(), foundMember.getLoginPw())) {
//            return ResponseEntity.status(401).body(ResultData.from("F-4","비밀번호가 일치하지 않습니다"));
//        }
//
//        // ✅ 토큰 직접 생성 (DB 저장 안 함)
//        String accessToken = jwtTokenProvider.generateAccessToken(
//                foundMember.getId(),
//                foundMember.getNickName(),
//                foundMember.getEmail()
//        );
//
//        String refreshToken = jwtTokenProvider.generateRefreshToken(
//                foundMember.getId(),
//                foundMember.getNickName(),
//                foundMember.getEmail()
//        );
//
//        // rq 같은 세션 유틸에 굳이 저장할 필요 없음 (Stateless 구조)
//        // 다만 원하면 request scope 등에 accessToken만 저장 가능
//
//        // ✅ 응답으로 accessToken + refreshToken 둘 다 내려줌
//        return ResponseEntity.ok(
//                ResultData.from("S-1",
//                        foundMember.getNickName() + "님 환영",
//                        "accessToken", accessToken,
//                        "refreshToken", refreshToken
//                )
//        );
//    }
//
//
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
//        String refreshToken = body.get("refreshToken");
//
//        if (!jwtTokenProvider.validateToken(refreshToken)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token invalid");
//        }
//
//        Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);
//        String nickName = jwtTokenProvider.getNickNameFromToken(refreshToken);
//        String email = jwtTokenProvider.getMemberEmailFromToken(refreshToken);
//
//        String newAccessToken = jwtTokenProvider.generateAccessToken(memberId, nickName, email);
//
//        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
//    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(
            HttpServletRequest req,
            @RequestParam(required = false) String nickName) {
        System.out.println("\n===== [GET] /api/DiFF/member/profile =====");

        Member member;
        if (nickName != null) {
            member = memberService.getMemberByNickName(nickName);
            if (member == null) {
                System.out.println("해당 닉네임을 가진 회원이 없습니다: " + nickName);
            }
        } else {
            Rq rq = (Rq) req.getAttribute("rq");
            Long memberId = ((Number) rq.getLoginedMemberId()).longValue();
            member = memberService.getMemberById(memberId);
        }

        System.out.println("member 닉네임.  "+ member.getNickName());
        List<Repository> repositories = repositoryService.getRepositoriesByMemberId(member.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("member", member);
        result.put("repositories", repositories);

        return ResponseEntity.ok(result);
    }

//    @RequestMapping("/modify")
//    public String modify(Model model, HttpServletRequest req) {
//
//        Rq rq = (Rq) req.getAttribute("rq");
//        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());
//
//        model.addAttribute("member", member);
//
//        return "/modify";
//    }

    @RequestMapping("/checkPw")
    @ResponseBody
    public ResultData checkPw(HttpServletRequest req, String pw) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        if(!member.getLoginPw().equals(pw)) {
            return ResultData.from("F-1", "비밀번호 불일치");
        }

        return ResultData.from("S-1", "비밀번호 일치 성공");
    }

    // 로그인 체크 -> 유무 체크 -> 권한 체크
    @PutMapping("/doModify")
    public ResponseEntity<ResultData> doModify(@RequestHeader("Authorization") String authorization, @RequestBody Member member) {

        // 토큰에서 memberId 추출
        String token = authorization.substring(7);
        Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

        // 입력 검증
        if (Ut.isEmpty(member.getLoginId())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-1", "아이디를 입력해주세요"));
        }
        if (Ut.isEmpty(member.getLoginPw())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 입력해주세요"));
        }
        if (Ut.isEmpty(member.getName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-3", "이름을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getNickName())) {
            return ResponseEntity.badRequest().body(ResultData.from("F-4", "닉네임을 입력해주세요"));
        }
        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(ResultData.from("F-6", "유효한 이메일을 입력해주세요"));
        }

        // 서비스에 수정 요청
        int updated = memberService.modifyMember(memberId, member.getLoginId(), member.getLoginPw(), member.getName(), member.getNickName(), member.getEmail()
        );

        //  결과 검사
        if (updated == 0) {
            return ResponseEntity.badRequest().body(ResultData.from("F-7", "회원정보 수정에 실패했습니다"));
        }

        // 성공 응답
        return ResponseEntity.ok(ResultData.from("S-1", "회원정보가 성공적으로 수정되었습니다")
        );
    }

    @GetMapping("/followingList")
    public ResponseEntity<ResultData> showFollowingList(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [GET] /api/DiFF/member/followingList =====");
        System.out.println("memberId = " + memberId);

        List<Member> followingList = memberService.getFollowingList(memberId);

        return ResponseEntity.ok(ResultData.from("S-1", "팔로잉 목록 조회 성공", "followingList", followingList));
    }


    @GetMapping("/followerList")
    public ResponseEntity<ResultData> showFollowerList(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [GET] /api/DiFF/member/followingList =====");
        System.out.println("memberId = " + memberId);

        List<Member> followerList = memberService.getFollowerList(memberId);

        return ResponseEntity.ok(ResultData.from("S-1", "팔로잉 목록 조회 성공", "followingList", followerList));
    }

    @PostMapping("/follow")
    public ResponseEntity<ResultData> follow(HttpServletRequest req,
                                             @RequestParam Long fromMemberId) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        List<Member> followingList = memberService.getFollowingList(memberId);

        boolean alreadyFollowing = false;
        for (Member m : followingList) {
            if (m.getId().equals(fromMemberId)) {
                alreadyFollowing = true;
                break;
            }
        }

        if (alreadyFollowing) {
            return ResponseEntity.ok(ResultData.from("F-1", "이미 팔로우 중입니다."));
        }

        memberService.follow(memberId, fromMemberId);

        return ResponseEntity.ok(ResultData.from("S-1", "팔로우 성공"));
    }

    @DeleteMapping("/unfollow")
    public ResponseEntity<ResultData> unfollow(HttpServletRequest req,
                                               @RequestParam Long fromMemberId) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        List<Member> followingList = memberService.getFollowingList(memberId);

        boolean alreadyFollowing = false;
        for (Member m : followingList) {
            if (m.getId().equals(fromMemberId)) {
                alreadyFollowing = true;
                break;
            }
        }

        if (!alreadyFollowing) {
            return ResponseEntity.ok(ResultData.from("F-1", "팔로우 중이 아닙니다."));
        }

        memberService.unfollow(memberId, fromMemberId);

        return ResponseEntity.ok(ResultData.from("S-1", "언팔로우 성공"));
    }


    @PostMapping("/uploadProfileImg")
    @ResponseBody
    public String uploadProfileImg(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        System.out.println("uploadProfileImg 메서드 진입");
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String profileUrl = (String) uploadResult.get("secure_url");

            Rq rq = (Rq) req.getAttribute("rq");
            Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

            System.out.println("프로필 이미지 업로드 성공: " + profileUrl);

            memberService.uploadProfileImg(memberId, profileUrl);

            return profileUrl;

        } catch (IOException e) {
            e.printStackTrace();
            return "업로드 실패: " + e.getMessage();
        }
    }

}