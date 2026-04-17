package com.example.auction.domain.auth.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthLoginResponse;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "회원가입 요청 성공", authService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthLoginResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "로그인 요청 성공", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<AuthLoginResponse>> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "토큰 재발급 요청 성공", authService.refreshToken(refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        String accessToken = (String) request.getAttribute("accessToken");
        authService.logout(userDetails.getUserId(), accessToken);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "로그아웃 요청 성공", null));
    }
}
