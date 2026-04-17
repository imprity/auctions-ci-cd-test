package com.example.auction.domain.user.controller;

import com.example.auction.common.config.security.CustomAccessDeniedHandler;
import com.example.auction.common.config.security.CustomAuthenticationEntryPoint;
import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.domain.user.dto.UserChangePasswordRequest;
import com.example.auction.domain.user.dto.UserGetResponse;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @BeforeEach
    void setUpSecurityContext() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========================
    // 마이페이지 조회
    // ========================

    @Test
    @DisplayName("마이페이지 조회 성공")
    void myPage_success() throws Exception {
        // given
        UserGetResponse response = new UserGetResponse(1L, "test@test.com", BigDecimal.valueOf(4.5), UserRole.USER);

        given(userService.myPage(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }


    // ========================
    // 비밀번호 변경
    // ========================

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword");

        doNothing().when(userService).changePassword(any(), any());

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호 변경 요청 성공"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호 blank")
    void changePassword_fail_blankOldPassword() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("", "newPassword123");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("기존 비밀번호를 입력해주세요"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 8자 미만")
    void changePassword_fail_shortNewPassword() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "1234567");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이어야 합니다"));
    }


    // ========================
    // 회원 탈퇴
    // ========================

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() throws Exception {
        // given
        UserWithdrawResponse response = new UserWithdrawResponse(
                1L, "test@test.com", UserRole.USER, LocalDateTime.now(), LocalDateTime.now()
        );

        given(userService.withdraw(1L)).willReturn(response);

        // when & then
        mockMvc.perform(delete("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("탈퇴 요청 성공"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }
}