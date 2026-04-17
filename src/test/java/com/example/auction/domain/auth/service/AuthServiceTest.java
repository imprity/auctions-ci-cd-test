package com.example.auction.domain.auth.service;

import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthLoginResponse;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpireTime", 604800000L);
    }

    // ========================
    // 회원가입
    // ========================

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password123", UserRole.USER);
        User user = User.of(request.email(), "encodedPassword", request.role());
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        AuthSignupResponse response = authService.signup(request);

        // then
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_duplicatedEmail() {
        // given
        AuthSignupRequest request = new AuthSignupRequest("test@test.com", "password123", UserRole.USER);

        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.DUPLICATED_EMAIL.getMessage());
    }


    // ========================
    // 로그인
    // ========================

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        //given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "password123");
        User user = User.of(request.email(), "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByEmailAndDeletedFalse(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("accessToken");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        AuthLoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 유저")
    void login_fail_userNotFound() {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "password123");

        given(userRepository.findByEmailAndDeletedFalse(request.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalidPassword() {
        // given
        AuthLoginRequest request = new AuthLoginRequest("test@test.com", "wrongPassword");
        User user = User.of(request.email(), "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByEmailAndDeletedFalse(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_PASSWORD.getMessage());
    }


    // ========================
    // 토큰 재발급
    // ========================

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_success() {
        // given
        String refreshToken = "refreshToken";
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(user.getId());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:" + user.getId())).willReturn(refreshToken);
        given(userRepository.findByIdAndDeletedFalse(user.getId())).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("newAccessToken");

        // when
        AuthLoginResponse response = authService.refreshToken(refreshToken);

        // then
        assertThat(response.accessToken()).isEqualTo("newAccessToken");
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refreshToken_fail_invalidToken() {
        // given
        String refreshToken = "refreshToken";

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰 없음")
    void refreshToken_fail_savedTokenNotFound() {
        // given
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:" + userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 토큰과 불일치")
    void refreshToken_fail_tokenMismatch() {
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:" + userId)).willReturn("differentRefreshToken");

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유저 없음(탈퇴)")
    void refreshToken_fail_userNotFound() {
        // given
        String refreshToken = "validRefreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:" + userId)).willReturn(refreshToken);
        given(userRepository.findByIdAndDeletedFalse(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }


    // ========================
    // 로그아웃
    // ========================

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        String accessToken = "accessToken";
        Long userId = 1L;
        long remainingTtl = 300000L;

        given(jwtProvider.getRemainingTtl(accessToken)).willReturn(remainingTtl);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.logout(userId, accessToken);

        // then
        verify(redisTemplate).delete("refresh:" + userId);
        verify(valueOperations).set("blacklist:" + accessToken, "logout", remainingTtl, TimeUnit.MILLISECONDS);
    }
}