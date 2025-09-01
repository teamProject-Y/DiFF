package com.example.demo.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.service.RepositoryService;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private AuthService authService;
    @Autowired
    private RepositoryService repositoryService;
    public UsrMemberController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

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

    @PutMapping("/doModifyNickName")
    public ResponseEntity<ResultData> doModifyNickName(
            HttpServletRequest req,
            @RequestBody Member member
    ) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int updated = memberService.modifyNickName(memberId, member.getNickName());

        if (updated == -1) {
            return ResponseEntity.badRequest()
                    .body(ResultData.from("F-8", "이미 사용 중인 닉네임입니다"));
        }

        if (updated == 0) {
            return ResponseEntity.badRequest()
                    .body(ResultData.from("F-7", "회원정보 수정에 실패했습니다"));
        }

        return ResponseEntity.ok(
                ResultData.from("S-1", "회원정보가 성공적으로 수정되었습니다")
        );
    }

    @PutMapping("/doModifyIntroduce")
    public ResponseEntity<ResultData> doModifyIntroduce(
            HttpServletRequest req,
            @RequestBody Member member
    ) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int updated = memberService.modifyIntroduce(memberId, member.getIntroduce());

        if (updated == 0) {
            return ResponseEntity.badRequest()
                    .body(ResultData.from("F-7", "자기소개 수정에 실패했습니다"));
        }

        return ResponseEntity.ok(
                ResultData.from("S-1", "자기소개가 성공적으로 수정되었습니다")
        );
    }


    @GetMapping("/followingList")
    public ResponseEntity<ResultData> showFollowingList(
            @RequestParam(required = false) String nickName,
            HttpServletRequest req) {

        System.out.println("\n===== [GET] /api/DiFF/member/followingList =====");
        System.out.println("👉 프론트에서 전달된 nickName = " + nickName);

        Long targetId;
        if (nickName != null) {
            Member target = memberService.getMemberByNickName(nickName);
            if (target == null) {
                System.out.println("⚠️ 해당 닉네임 없음: " + nickName);
                return ResponseEntity.ok(ResultData.from("F-1", "해당 닉네임 회원 없음", "followingList", List.of()));
            }
            targetId = target.getId();
            System.out.println("✅ targetId(조회 대상 회원) = " + targetId);
        } else {
            Rq rq = (Rq) req.getAttribute("rq");
            targetId = ((Number) rq.getLoginedMemberId()).longValue();
            System.out.println("👉 닉네임 없음 → 로그인 사용자 기준 targetId = " + targetId);
        }

        List<Member> followingList = memberService.getFollowingList(targetId);
        System.out.println("📌 팔로잉 수 = " + followingList.size());

        return ResponseEntity.ok(ResultData.from("S-1", "팔로잉 목록 조회 성공", "followingList", followingList));
    }

    @GetMapping("/followerList")
    public ResponseEntity<ResultData> showFollowerList(
            @RequestParam(required = false) String nickName,
            HttpServletRequest req) {

        System.out.println("\n===== [GET] /api/DiFF/member/followerList =====");
        System.out.println("👉 프론트에서 전달된 nickName = " + nickName);

        Long targetId;
        if (nickName != null) {
            Member target = memberService.getMemberByNickName(nickName);
            if (target == null) {
                System.out.println("⚠️ 해당 닉네임 없음: " + nickName);
                return ResponseEntity.ok(ResultData.from("F-1", "해당 닉네임 회원 없음", "followerList", List.of()));
            }
            targetId = target.getId();
            System.out.println("✅ targetId(조회 대상 회원) = " + targetId);
        } else {
            Rq rq = (Rq) req.getAttribute("rq");
            targetId = ((Number) rq.getLoginedMemberId()).longValue();
            System.out.println("👉 닉네임 없음 → 로그인 사용자 기준 targetId = " + targetId);
        }

        List<Member> followerList = memberService.getFollowerList(targetId);
        System.out.println("📌 팔로워 수 = " + followerList.size());

        return ResponseEntity.ok(ResultData.from("S-1", "팔로워 목록 조회 성공", "followerList", followerList));
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

    @PostMapping("/updateToken")
    public ResponseEntity<String> updateToken(@RequestBody Map<String, String> body) {
        Long memberId = rq.getLoginedMemberId();
        String token = body.get("token");
        memberService.updateFcmToken(memberId, token);
        return ResponseEntity.ok("토큰 저장 완료");
    }

    @PostMapping("/saveFcmToken")
    public ResponseEntity<String> saveFcmToken(@RequestBody Map<String, String> request,
                                               HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();
        System.out.println("📥 /api/DiFF/member/saveFcmToken 요청 도착 → memberId: " + loginedMemberId);
        String token = request.get("fcmToken");
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ fcmToken 값이 비어있습니다.");
        }
        System.out.println("🎯 추출된 fcmToken: " + token);
        memberService.saveFcmToken(loginedMemberId, token);
        return ResponseEntity.ok("✅ fcmToken 저장 완료");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        System.out.println("📩 [이메일 인증 요청] token=" + token);

        try {
            memberService.verifyEmail(token);
            System.out.println("✅ [이메일 인증 성공] token=" + token);
            return ResponseEntity.ok("이메일 인증 완료!");
        } catch (Exception e) {
            System.out.println("❌ [이메일 인증 실패] token=" + token + ", 이유: " + e.getMessage());
            return ResponseEntity.status(400).body("이메일 인증 실패: " + e.getMessage());
        }
    }

    @PostMapping("/findPw")
    public ResponseEntity<String> requestReset(@RequestParam String email) {
        memberService.requestPasswordReset(email);
        return ResponseEntity.ok("비밀번호 재설정 이메일을 발송했습니다.");
    }


    @PostMapping("/updatePassword")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPw) {
        System.out.println("📩 [resetPassword 요청]");
        System.out.println("👉 token=" + token);
        System.out.println("👉 newPw=" + newPw);
        memberService.updatePassword(token, newPw);
        return ResponseEntity.ok("비밀번호 재설정 완료!");
    }

}