package com.example.auction.domain.auth.service;

import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthLoginResponse;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    @Transactional
    public AuthSignupResponse signup(AuthSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceErrorException(AuthErrorEnum.DUPLICATED_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.of(request.email(), encodedPassword, request.role());
        userRepository.save(user);

        return new AuthSignupResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    @Transactional
    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.email()).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceErrorException(AuthErrorEnum.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                refreshTokenExpireTime,
                TimeUnit.MILLISECONDS
        );

        return new AuthLoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthLoginResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new ServiceErrorException(AuthErrorEnum.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        String savedToken = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new ServiceErrorException(AuthErrorEnum.INVALID_TOKEN);
        }

        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());

        return new AuthLoginResponse(newAccessToken, refreshToken);
    }

    @Transactional
    public void logout(Long userId, String accessToken) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        long remainingTtl = jwtProvider.getRemainingTtl(accessToken);
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                remainingTtl,
                TimeUnit.MILLISECONDS
        );
    }
}
