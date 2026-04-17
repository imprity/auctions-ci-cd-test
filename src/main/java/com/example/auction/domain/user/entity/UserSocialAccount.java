package com.example.auction.domain.user.entity;

import com.example.auction.domain.user.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_social_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "provider"}),
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = false)
    private String providerId;

    public static UserSocialAccount of(Long userId, AuthProvider provider, String providerId) {
        UserSocialAccount socialAccount = new UserSocialAccount();
        socialAccount.userId = userId;
        socialAccount.provider = provider;
        socialAccount.providerId = providerId;

        return socialAccount;
    }
}
