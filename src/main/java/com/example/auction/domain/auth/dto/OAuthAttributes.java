package com.example.auction.domain.auth.dto;

import com.example.auction.domain.user.enums.AuthProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuthAttributes {

    private String email;
    private AuthProvider provider;
    private String providerId;
    private String nameAttributeKey;

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .email((String) attributes.get("email"))
                .provider(AuthProvider.GOOGLE)
                .providerId((String) attributes.get("sub"))
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));

        return OAuthAttributes.builder()
                .email(providerId + "@kakao.social")
                .provider(AuthProvider.KAKAO)
                .providerId(providerId)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        String providerId = (String) response.get("id");

        return OAuthAttributes.builder()
                .email(providerId + "@naver.social")
                .provider(AuthProvider.NAVER)
                .providerId(providerId)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}
