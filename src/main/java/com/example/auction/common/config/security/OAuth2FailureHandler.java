package com.example.auction.common.config.security;

import com.example.auction.common.dto.BaseResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "소셜 로그인에 실패했습니다";

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String errorCode = oauth2Exception.getError() != null
                    ? oauth2Exception.getError().getErrorCode() : null;
            if (errorCode != null) {
                try {
                    httpStatus = HttpStatus.valueOf(errorCode);
                } catch (IllegalArgumentException ignored) {
                    httpStatus = HttpStatus.UNAUTHORIZED;
                }
            }
            message = exception.getMessage();
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(httpStatus.value());
        response.getWriter().write(objectMapper.writeValueAsString(
                BaseResponse.fail(httpStatus.name(), message)));
    }
}
