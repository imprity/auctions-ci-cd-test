package com.example.auction.domain.auth.service;

import com.example.auction.domain.auth.dto.OAuthAttributes;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.entity.UserSocialAccount;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuthAttributes authAttributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrLoad(authAttributes);

        Map<String, Object> principalAttributes = new HashMap<>(oAuth2User.getAttributes());
        principalAttributes.put("userId", user.getId());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                principalAttributes,
                authAttributes.getNameAttributeKey()
        );
    }

    private User saveOrLoad(OAuthAttributes authAttributes) {
        Optional<UserSocialAccount> socialAccount = userSocialAccountRepository.findByProviderAndProviderId(
                authAttributes.getProvider(), authAttributes.getProviderId());

        if (socialAccount.isPresent()) {
            return userRepository.findByIdAndDeletedFalse(socialAccount.get().getUserId()).orElseThrow(
                    () -> new OAuth2AuthenticationException(new OAuth2Error(
                            UserErrorEnum.USER_NOT_FOUND.getStatus().name()),
                            UserErrorEnum.USER_NOT_FOUND.getMessage()));
        }

        if (userRepository.existsByEmail(authAttributes.getEmail())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    AuthErrorEnum.SOCIAL_LOGIN_EMAIL_CONFLICT.getStatus().name()),
                    AuthErrorEnum.SOCIAL_LOGIN_EMAIL_CONFLICT.getMessage());
        }

        try {
            User newUser = User.ofSocial(authAttributes.getEmail(), UserRole.USER);
            userRepository.save(newUser);

            UserSocialAccount newSocialAccount = UserSocialAccount.of(newUser.getId(), authAttributes.getProvider(), authAttributes.getProviderId());
            userSocialAccountRepository.save(newSocialAccount);

            return newUser;
        } catch (DataIntegrityViolationException e) {
            Optional<UserSocialAccount> existing = userSocialAccountRepository.findByProviderAndProviderId(
                    authAttributes.getProvider(), authAttributes.getProviderId());

            if (existing.isPresent()) {
                return userRepository.findByIdAndDeletedFalse(existing.get().getUserId()).orElseThrow(
                        () -> new OAuth2AuthenticationException(new OAuth2Error(
                                UserErrorEnum.USER_NOT_FOUND.getStatus().name()),
                                UserErrorEnum.USER_NOT_FOUND.getMessage()));
            }

            if (userRepository.existsByEmail(authAttributes.getEmail())) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        AuthErrorEnum.SOCIAL_LOGIN_EMAIL_CONFLICT.getStatus().name()),
                        AuthErrorEnum.SOCIAL_LOGIN_EMAIL_CONFLICT.getMessage());
            }

            throw e;
        }
    }
}
