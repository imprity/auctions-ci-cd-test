package com.example.auction.domain.user;

import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.user.dto.UserChangePasswordRequest;
import com.example.auction.domain.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 회원가입
        AuthSignupRequest signupRequest = new AuthSignupRequest("test@test.com", "password123", UserRole.USER);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)));

        // 로그인 후 토큰 추출
        AuthLoginRequest loginRequest = new AuthLoginRequest("test@test.com", "password123");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        accessToken = objectMapper.readTree(response).path("data").path("accessToken").asString();
    }

    @AfterEach
    void cleanUpRedis() {
        var factory = redisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }

    // ========================
    // 마이페이지 조회
    // ========================

    @Test
    @DisplayName("마이페이지 조회 성공")
    void myPage_success() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }


    // ========================
    // 비밀번호 변경
    // ========================

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호 불일치")
    void changePassword_fail_invalidOldPassword() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongPassword", "newPassword123");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 기존과 동일")
    void changePassword_fail_sameAsOldPassword() throws Exception {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("password123", "password123");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다"));
    }


    // ========================
    // 회원 탈퇴
    // ========================

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() throws Exception {
        mockMvc.perform(delete("/api/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("탈퇴 요청 성공"))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    @DisplayName("회원 탈퇴 후 로그인 불가")
    void withdraw_then_login_fail() throws Exception {
        // given - 탈퇴
        mockMvc.perform(delete("/api/users")
                .header("Authorization", "Bearer " + accessToken));

        // when & then - 탈퇴한 유저로 로그인 시도
        AuthLoginRequest loginRequest = new AuthLoginRequest("test@test.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다"));
    }
}
