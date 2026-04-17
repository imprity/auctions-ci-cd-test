package com.example.auction.domain.auth.controller;

import com.example.auction.common.config.security.CustomAccessDeniedHandler;
import com.example.auction.common.config.security.CustomAuthenticationEntryPoint;
import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthLoginResponse;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.auth.service.AuthService;
import com.example.auction.domain.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========================
    // 회원가입
    // ========================

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password123", UserRole.USER);
        AuthSignupResponse response = new AuthSignupResponse(1L, "test@test.com", UserRole.USER, LocalDateTime.now());

        given(authService.signup(any(AuthSignupRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 불일치")
    void signup_fail_invalidEmail() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("testEmail", "password123", UserRole.USER);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 8자 미만")
    void signup_fail_shortPassword() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "pw123", UserRole.USER);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이어야 합니다"));
    }

    @Test
    @DisplayName("회원가입 실패 - role 누락")
    void signup_fail_nullRole() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password123", null);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("역할을 선택해주세요"));
    }


    // ========================
    // 로그인
    // ========================

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "password123");
        AuthLoginResponse response = new AuthLoginResponse("accessToken", "refreshToken");

        given(authService.login(any(AuthLoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 형식 불일치")
    void login_fail_invalidEmail() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("testEmail", "password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 8자 미만")
    void login_fail_shortPassword() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "pw123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이어야 합니다"));
    }


    // ========================
    // 토큰 재발급
    // ========================

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_success() throws Exception {
        // given
        AuthLoginResponse response = new AuthLoginResponse("newAccessToken", "refreshToken");

        given(authService.refreshToken("refreshToken")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Refresh-Token", "refreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refreshToken_fail_invalidToken() throws Exception {
        // given
        given(authService.refreshToken("invalidToken"))
                .willThrow(new ServiceErrorException(AuthErrorEnum.INVALID_TOKEN));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Refresh-Token", "invalidToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(AuthErrorEnum.INVALID_TOKEN.getMessage()));
    }


    // ========================
    // 로그아웃
    // ========================

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        doNothing().when(authService).logout(any(), any());

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .requestAttr("accessToken", "accessToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 요청 성공"));

        verify(authService).logout(eq(1L), eq("accessToken"));
    }
}