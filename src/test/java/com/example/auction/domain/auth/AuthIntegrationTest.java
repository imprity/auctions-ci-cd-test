package com.example.auction.domain.auth;

import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() throws Exception {
        AuthSignupRequest signupRequest = new AuthSignupRequest("test@test.com", "password123", UserRole.USER);
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));
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
    // 회원가입
    // ========================

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("signup@test.com", "password123", UserRole.USER);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("signup@test.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_duplicatedEmail() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest("signup@test.com", "password123", UserRole.USER);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용중인 이메일입니다"));
    }


    // ========================
    // 로그인
    // ========================

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalidPassword() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "wrongPassword");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다"));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 유저")
    void login_fail_userNotFound() throws Exception {
        // given
        AuthLoginRequest request = new AuthLoginRequest("notexist@test.com", "password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다"));
    }


    // ========================
    // 토큰 재발급
    // ========================

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_success() throws Exception {
        // given
        String refreshToken = getTokens()[1];

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(refreshToken));
    }


    // ========================
    // 로그아웃
    // ========================

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        String accessToken = getTokens()[0];

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 요청 성공"));
    }


    // 로그인해서 실제 토큰 추출하는 헬퍼 메서드
    private String[] getTokens() throws Exception {
        AuthLoginRequest loginRequest = new AuthLoginRequest("test@test.com", "password123");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(response).path("data").path("accessToken").asString();
        String refreshToken = objectMapper.readTree(response).path("data").path("refreshToken").asString();
        return new String[]{accessToken, refreshToken};
    }
}