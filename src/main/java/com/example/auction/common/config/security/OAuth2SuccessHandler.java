package com.example.auction.common.config.security;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthLoginResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Number userId = oAuth2User.getAttribute("userId");
        User user = userRepository.findByIdAndDeletedFalse(userId.longValue()).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                refreshTokenExpireTime,
                TimeUnit.MILLISECONDS
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                BaseResponse.success(HttpStatus.OK.name(), "소셜 로그인 성공", new AuthLoginResponse(accessToken, refreshToken))));
    }
}
