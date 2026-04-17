package com.example.auction.common.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    // async dispatch(SSE 스트리밍) 시에도 JWT 필터를 재실행해 SecurityContext 유지
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtProvider.resolveToken(request.getHeader("Authorization"));

        boolean isValid = token != null && jwtProvider.validateAccessToken(token);
        boolean blacklisted = true;

        if (isValid) {
            try {
                blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
            } catch (Exception e) {
                log.error("Redis blacklist 확인 실패: {}", e.getMessage());
            }
        }

        if (isValid && !blacklisted) {
            request.setAttribute("accessToken", token);

            Long userId = jwtProvider.getUserId(token);
            String role = jwtProvider.getRole(token);

            CustomUserDetails userDetails = new CustomUserDetails(userId, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
