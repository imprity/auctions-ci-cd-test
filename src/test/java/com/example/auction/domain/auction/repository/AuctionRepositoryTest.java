package com.example.auction.domain.auction.repository;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.common.config.JpaConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
// H2좀 그만 불러!!
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class})
class AuctionRepositoryTest {

    // TODO: 현재 DB를 초기화 하지 않고 @Transactional에만 의존하고 있습니다.
    //  나중에 DB를 초기화 하는 방법을 구현해야 합니다.

    @Autowired
    AuctionRepository auctionRepository;


    // 임시로 만든 가짜 유저 ID
    //
    // TODO: 나중에 저희가 FK체크를 DB 에서 구현하게 되면 이 테스트는 작동이 안될 것입니다.
    //  따로 유저를 만들거나 하는 방안을 마련해야 할 거 같습니다.
    public static final Long FAKE_USER_ID = 1L;

    /**
     * AuctionSearchCondition이 기본값일 때 모든 데이터를 가지고 오는지 확인합니다.
     */
    @Test
    @Transactional
    void findSimple() {
        Auction auction = Auction.of(
            FAKE_USER_ID,
            "test auction",
            BigDecimal.valueOf(1000),
            "test auction item name",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            AuctionProductCategory.ELECTRONICS
        );

        AuctionSearchCondition condition = new AuctionSearchCondition();

        auctionRepository.save(auction);

        List<@NonNull Auction> auctions = auctionRepository.findByCondition(
            condition
        ).stream().toList();

        assertThat(auctions).contains(auction);
    }

    /**
     * AuctionSearchCondition의 maxPriceMin과 maxPriceMax가 잘 작동하는 확인합니다.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFindMinMaxPriceRangeSources")
    @Transactional
    void findMinMaxPriceRange(String name, AuctionSearchCondition condition, int expectingAuction) {
        Function<BigDecimal, Auction> createAuction = (
            maxPrice
        ) -> {
            Auction auction = Auction.of(
                FAKE_USER_ID,
                "test auction",
                maxPrice,
                "test auction item name",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                AuctionProductCategory.ELECTRONICS
            );
            auctionRepository.save(auction);

            return auction;
        };

        // GIVEN
        Auction[] auctions = new Auction[] {
                createAuction.apply(BigDecimal.valueOf(1000)),
                createAuction.apply(BigDecimal.valueOf(2000)),
                createAuction.apply(BigDecimal.valueOf(3000))
        };

        // WHEN
        List<@NonNull Auction> foundAuctions = auctionRepository.findByCondition(
                condition
        ).stream().toList();

        // THEN
        assertThat(foundAuctions).containsExactly(auctions[expectingAuction]);
    }

    private static Stream<Arguments> getFindMinMaxPriceRangeSources() {
        AuctionSearchCondition cond1 = new AuctionSearchCondition();
        cond1.setMaxPriceMin(BigDecimal.valueOf(2500));

        AuctionSearchCondition cond2 = new AuctionSearchCondition();
        cond2.setMaxPriceMax(BigDecimal.valueOf(1500));

        AuctionSearchCondition cond3 = new AuctionSearchCondition();
        cond3.setMaxPriceMin(BigDecimal.valueOf(1500));
        cond3.setMaxPriceMax(BigDecimal.valueOf(2500));

        return Stream.of(
                Arguments.of("최소 가격 이상만 조회 가능", cond1, 2),
                Arguments.of("최대 가격 이하만 조회 가능", cond2, 0),
                Arguments.of("최소와 최대 가격 사이만 조회 가능", cond3, 1)
        );
    }

    /**
     * 경매의 상태로 필터링이 잘되는지 확인합니다
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFindByStatusSources")
    @Transactional
    void findByStatus(String name, AuctionSearchCondition condition, AuctionStatus[] statuses) {
        Function<AuctionStatus, Auction> createAuction = (
                status
        ) -> {
            Auction auction = Auction.of(
                    FAKE_USER_ID,
                    "test auction",
                    BigDecimal.valueOf(1000),
                    "test auction item name",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    AuctionProductCategory.ELECTRONICS
            );

            ReflectionTestUtils.setField(auction, "status", status);

            auctionRepository.save(auction);

            return auction;
        };

        // GIVEN
        createAuction.apply(AuctionStatus.READY);
        createAuction.apply(AuctionStatus.ACTIVE);
        createAuction.apply(AuctionStatus.DONE);
        createAuction.apply(AuctionStatus.NO_BID);
        createAuction.apply(AuctionStatus.CANCELLED);

        // WHEN
        List<AuctionStatus> foundAuctionStatuses = auctionRepository.findByCondition(
                condition
        ).stream()
            .map(a -> a.getStatus())
            .distinct()
            .toList();

        // THEN
        assertThat(foundAuctionStatuses).containsExactlyInAnyOrder(statuses);
    }

    private static Stream<Arguments> getFindByStatusSources() {
        AuctionSearchCondition cond1 = new AuctionSearchCondition();
        cond1.setDefaultStatusesIfEmpty(AuctionStatus.NO_BID);

        AuctionSearchCondition cond2 = new AuctionSearchCondition();
        cond2.setDefaultStatusesIfEmpty(AuctionStatus.ACTIVE, AuctionStatus.CANCELLED);

        return Stream.of(
                Arguments.of("경매 단일 조건 검색", cond1, new AuctionStatus[]{AuctionStatus.NO_BID}),
                Arguments.of("경매 다수 조건 검색", cond2, new AuctionStatus[]{AuctionStatus.ACTIVE, AuctionStatus.CANCELLED})
        );
    }
}
