package com.example.auction.domain.user.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.user.dto.UserChangePasswordRequest;
import com.example.auction.domain.user.dto.UserGetResponse;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<BaseResponse<UserGetResponse>> myPage(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "마이페이지 조회 요청 성공", userService.myPage(userId)));
    }

    @PatchMapping("/password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserChangePasswordRequest request
    ) {
        Long userId = userDetails.getUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "비밀번호 변경 요청 성공", null));
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<UserWithdrawResponse>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "탈퇴 요청 성공", userService.withdraw(userId)));
    }
}
