package com.example.auction.domain.user.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import com.example.auction.domain.user.dto.UserChangePasswordRequest;
import com.example.auction.domain.user.dto.UserGetResponse;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    // ========================
    // 마이페이지 조회
    // ========================

    @Test
    @DisplayName("마이페이지 조회 성공")
    void myPage_success() {
        // given
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));

        // when
        UserGetResponse response = userService.myPage(1L);

        // then
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 유저 없음")
    void myPage_fail_userNotFound() {
        // given
        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.myPage(1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }


    // ========================
    // 비밀번호 변경
    // ========================

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        // given
        User user = User.of("test@test.com", "encodedOldPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword");

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPassword", "encodedOldPassword")).willReturn(true);
        given(passwordEncoder.encode("newPassword")).willReturn("encodedNewPassword");

        // when & then
        assertThatNoException().isThrownBy(() -> userService.changePassword(1L, request));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 유저 없음")
    void changePassword_fail_userNotFound() {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword");

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 기존과 동일")
    void changePassword_fail_sameAsOldPassword() {
        // given
        User user = User.of("test@test.com", "encodedOldPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserChangePasswordRequest request = new UserChangePasswordRequest("samePassword", "samePassword");

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.SAME_AS_OLD_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호 불일치")
    void changePassword_fail_invalidOldPassword() {
        // given
        User user = User.of("test@test.com", "encodedOldPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongOldPassword", "newPassword");

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_PASSWORD.getMessage());
    }


    // ========================
    // 회원 탈퇴
    // ========================

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() {
        // given
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));
        given(auctionRepository.existsByUserIdAndStatusIn(any(), any())).willReturn(false);
        given(bidRepository.existsByUserIdAndStatus(any(), any())).willReturn(false);

        // when
        UserWithdrawResponse response = userService.withdraw(1L);

        // then
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 유저 없음")
    void withdraw_fail_userNotFound() {
        // given
        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 진행 중인 경매 있음")
    void withdraw_fail_hasActiveAuction() {
        // given
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));
        given(auctionRepository.existsByUserIdAndStatusIn(any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.HAS_ACTIVE_AUCTION.getMessage());
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 진행 중인 입찰 있음")
    void withdraw_fail_hasActiveBid() {
        // given
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(user));
        given(auctionRepository.existsByUserIdAndStatusIn(any(), any())).willReturn(false);
        given(bidRepository.existsByUserIdAndStatus(any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.HAS_ACTIVE_BID.getMessage());
    }
}