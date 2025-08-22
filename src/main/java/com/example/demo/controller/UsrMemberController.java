package com.example.demo.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.repository.RepositoryRepository;
import com.example.demo.service.AuthService;
import com.example.demo.service.RepositoryService;
import com.example.demo.vo.*;
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

    @PostMapping("/doJoin")
    @ResponseBody
    public ResponseEntity<ResultData> doJoin(@RequestBody Member member) {
        System.out.println("✅ doJoin 진입");
        //System.out.println("입력 받은 아이디: " + member.getLoginId());
        System.out.println("입력 받은 비밀번호: " + member.getLoginPw());
        System.out.println("입력 받은 비밀번호 확인: " + member.getCheckLoginPw());
        //System.out.println("입력 받은 이름: " + member.getName());
        System.out.println("입력 받은 닉네임: " + member.getNickName());
        System.out.println("입력 받은 이메일: " + member.getEmail());

        try {
            // 1. 유효성 검사
//            if (Ut.isEmpty(member.getLoginId()))
//                return ResponseEntity.badRequest().body(ResultData.from("F-1", "아이디를 쓰시오"));
            if (Ut.isEmpty(member.getLoginPw()))
                return ResponseEntity.badRequest().body(ResultData.from("F-2", "비밀번호를 작성하세요."));
//            if (Ut.isEmpty(member.getName()))
//                return ResponseEntity.badRequest().body(ResultData.from("F-3", "이름을 쓰시오"));
            if (Ut.isEmpty(member.getNickName()))
                return ResponseEntity.badRequest().body(ResultData.from("F-4", "닉네임을 쓰시오"));
            if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@"))
                return ResponseEntity.badRequest().body(ResultData.from("F-6", "이메일 정확히 쓰시오"));

            // 2. 회원가입 처리
            long id = memberService.join(
                    // member.getLoginId(),
                    member.getLoginPw(),
                    member.getCheckLoginPw(),
                    // member.getName(),
                    member.getNickName(),
                    member.getEmail()
            );

            if (id == -409)
                return ResponseEntity.badRequest().body(ResultData.from("F-409", "이미 가입된 이메일입니다."));
            if (id == -400)
                return ResponseEntity.badRequest().body(ResultData.from("F-400", "비밀번호가 일치하지 않습니다."));

            // 3. 자동 로그인 처리
            Auth authRq = new Auth();
            authRq.setEmail(member.getEmail());
            authRq.setLoginPw(member.getLoginPw());

            Auth auth = authService.login(authRq);

            if (auth == null) {
                System.out.println("자동 로그인 실패");
                return ResponseEntity.status(401).body(ResultData.from("F-10", "자동 로그인에 실패했습니다. 로그인 페이지로 이동합니다."));
            }

            System.out.println("🎸 자동 로그인 됐음 auth: " +  auth);

            rq.setAccessToken(auth.getAccessToken());
            rq.setLoginedMember(memberService.getMemberByEmail(member.getEmail()));

            System.out.println("email: " + member.getEmail());
            System.out.println("logined member nickname: " + rq.getLoginedMember().getNickName());

            System.out.println("🎸 rq 저장된 토큰: " + rq.getAccessToken());

            return ResponseEntity.ok(
                    // .header(HttpHeaders.SET_COOKIE, cookie.toString()) // 쿠키 전략이면 활성화
                    ResultData.from("S-1",
                            member.getNickName() + " 님 회원가입을 축하합니다.",
                            "accessToken", auth.getAccessToken()
                    )
            );

        } catch (Exception e) {

            e.printStackTrace(); // 로그로 서버에서 어디서 죽었는지 추적
            return ResponseEntity.status(500).body(ResultData.from("F-ERR", "서버 오류: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ResultData> doLogin(@RequestBody Member member) {

        System.out.println("doLogin 진입" + " 제발 여기로 와라");

        System.out.println(member.getEmail());
        System.out.println(member.getLoginPw());

        if (Ut.isEmpty(member.getEmail()) || !member.getEmail().contains("@"))
            return ResponseEntity.badRequest().body(ResultData.from("F-1","이메일을 바르게 입력해주세요"));
        if (Ut.isEmpty(member.getLoginPw()))
            return ResponseEntity.badRequest().body(ResultData.from("F-2","비밀번호를 입력해주세요"));

        Auth authRq = new Auth();
        authRq.setEmail(member.getEmail());
        authRq.setLoginPw(member.getLoginPw());
        Auth auth = authService.login(authRq);
        System.out.println(new BCryptPasswordEncoder().encode("diff"));
        if (auth == null)

            return ResponseEntity.status(401).body(ResultData.from("F-3","로그인 실패"));

        rq.setAccessToken(auth.getAccessToken());
        rq.setLoginedMember(memberService.getMemberByLoginId(member.getEmail()));

        return ResponseEntity.ok(ResultData.from("S-1", member.getNickName()+"님 환영", "accessToken", auth.getAccessToken()));
    }

    @PostMapping("/doLogout")
    public ResponseEntity<ResultData> doLogout(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");

        rq.logout();

        return ResponseEntity.ok(ResultData.from("S-1", "로그아웃 되었습니다"));

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

    @RequestMapping("/modify")
    public String modify(Model model, HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        model.addAttribute("member", member);

        return "/modify";
    }

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
        System.out.println("팔로잉 수: " + followingList.size());

        return ResponseEntity.ok(ResultData.from("S-1", "팔로잉 목록 조회 성공", "followingList", followingList));
    }


    ////////////////////////////////////////////// CLI ///////////////////////////////////////////////////
//    @PostMapping("/verifyGitUser")
//    @ResponseBody
//    public ResultData verifyGitUser(@RequestBody Map<String, String> requestMap) {
//
//        String email = requestMap.get("email");
//        Integer verifiedMemberId = memberService.isVerifiedUser(email);
//
//        if(verifiedMemberId != null) {
//            System.out.println("git email로 찾은 memberID: " + verifiedMemberId);
//            return ResultData.from("S-1", "사용자 인증 완료", "인증된 사용자 id", verifiedMemberId);
//        }else {
//            System.err.println("git email로 찾은 member 없음");
//            return ResultData.from("F-1", "사용자 인증 실패");
//        }
//    }

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
            // DB에 프로필 이미지 URL 저장
            memberService.uploadProfileImg(memberId, profileUrl);

            return profileUrl;

        } catch (IOException e) {
            e.printStackTrace();
            return "업로드 실패: " + e.getMessage();
        }
    }

}