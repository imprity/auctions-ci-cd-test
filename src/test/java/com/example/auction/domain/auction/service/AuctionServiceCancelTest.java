package com.example.auction.domain.auction.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuctionServiceCancelTest {
    @InjectMocks
    private AuctionService auctionService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("기본적인 경매 취소")
    void canCancelRegularAuction() {
        // GIVEN
        User user = createMockUser();

        Auction auction = Auction.of(
                user.getId(),
                "test auction description",
                BigDecimal.valueOf(1000),
                "test auction name",
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(40),
                AuctionProductCategory.BOOKS
        );
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(auction.getId())).willReturn(Optional.of(auction));

        // WHEN & THEN (취소를 한 후 에러가 발생 안하는지만 확인)
        auctionService.cancelAuction(
            auction.getId(),
            new CustomUserDetails(user.getId(), user.getRole().toString())
        );
    }

    @Test
    @DisplayName("취소된 경매를 다시 취소 해도 에러가 아님")
    void cancelIdempotency() {
        // GIVEN
        User user = createMockUser();

        Auction auction = Auction.of(
                user.getId(),
                "test auction description",
                BigDecimal.valueOf(1000),
                "test auction name",
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(40),
                AuctionProductCategory.BOOKS
        );
        ReflectionTestUtils.setField(auction, "id", 1L);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.CANCELLED);

        given(auctionRepository.findById(auction.getId())).willReturn(Optional.of(auction));

        // WHEN & THEN (취소를 한 후 에러가 발생 안하는지만 확인)
        auctionService.cancelAuction(
                auction.getId(),
                new CustomUserDetails(user.getId(), user.getRole().toString())
        );
    }

    @Test
    @DisplayName("경매 시작 직전에는 경매 취소를 할 수 없다")
    void forbidLateCancel() {
        // GIVEN
        User user = createMockUser();

        Auction auction = Auction.of(
                user.getId(),
                "test auction description",
                BigDecimal.valueOf(1000),
                "test auction name",
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(1),
                AuctionProductCategory.BOOKS
        );
        ReflectionTestUtils.setField(auction, "id", 1L);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(auctionRepository.findById(auction.getId())).willReturn(Optional.of(auction));

        // WHEN & THEN 
        assertThrows(ServiceErrorException.class, () -> {
            auctionService.cancelAuction(
                    auction.getId(),
                    new CustomUserDetails(user.getId(), user.getRole().toString())
            );
        });
    }

    @Test
    @DisplayName("자기 자신의 경매만 취소가 가능하다")
    void canOnlyCancelYourOwn() {
        // NOTE: 지금 현재로서는 자기 자신의 경매만 취소가 가능합니다.
        //  하지만 나중에 관리자도 남의 경매를 취소가 가능하게 한다면 이를 반영해야 할 듯 합니다.

        // GIVEN
        User user = createMockUser();

        Auction auction = Auction.of(
                69L,
                "test auction description",
                BigDecimal.valueOf(1000),
                "test auction name",
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(40),
                AuctionProductCategory.BOOKS
        );
        ReflectionTestUtils.setField(auction, "id", 1L);

        given(auctionRepository.findById(auction.getId())).willReturn(Optional.of(auction));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            auctionService.cancelAuction(
                    auction.getId(),
                    new CustomUserDetails(user.getId(), user.getRole().toString())
            );
        });
    }

    @ParameterizedTest()
    @MethodSource("getThrowOnNonCancellableStatusSources")
    @DisplayName("준비, 취소 된 경매 이외에 경매는 취소 할 수 없다")
    void throwOnNonCancellableStatus(AuctionStatus status) {
        // GIVEN
        User user = createMockUser();

        Auction auction = Auction.of(
                user.getId(),
                "test auction description",
                BigDecimal.valueOf(1000),
                "test auction name",
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(40),
                AuctionProductCategory.BOOKS
        );
        ReflectionTestUtils.setField(auction, "id", 1L);
        ReflectionTestUtils.setField(auction, "status", status);

        given(auctionRepository.findById(auction.getId())).willReturn(Optional.of(auction));

        // WHEN & THEN
        if (status == AuctionStatus.READY || status == AuctionStatus.CANCELLED) {
            auctionService.cancelAuction(
                    auction.getId(),
                    new CustomUserDetails(user.getId(), user.getRole().toString())
            );
        }else {
            assertThrows(ServiceErrorException.class, () -> {
                auctionService.cancelAuction(
                        auction.getId(),
                        new CustomUserDetails(user.getId(), user.getRole().toString())
                );
            });
        }
    }

    private static Stream<Arguments> getThrowOnNonCancellableStatusSources() {
        return Arrays.stream(AuctionStatus.values()).map(Arguments::of);
    }

    private User createMockUser() {
        User user = User.of("test@test.com", "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        return user;
    }
}
