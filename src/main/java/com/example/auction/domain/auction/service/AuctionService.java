package com.example.auction.domain.auction.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.util.AuctionUtil;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.user.exception.UserErrorEnum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames =  {"getAuction"},
        key = "#auctionId"
    )
    public GetAuctionResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        return GetAuctionResponse.from(auction);
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = {"getManyAuctionsPublic"},
        key = "@auctionCacheService.getManyAuctionsPublicCacheKey(#condition)",
        condition = "@auctionCacheService.shouldCacheGetManyAuctionsPublic(#condition)"
    )
    public PageResponse<GetManyAuctionsResponse> getManyAuctionsPublic (
            AuctionSearchCondition condition
    ) {
        // 조회하고 싶은 status가 없는 경우 READY, ACTIVE한 경매만 조회하기
        condition.setDefaultStatusesIfEmpty(
                AuctionStatus.READY, AuctionStatus.ACTIVE
        );

        // 취소된 경매는 보여주지 말기
        if (
                condition.getStatus() != null &&
                condition.getStatus().contains(AuctionStatus.CANCELLED)
        ) {
            throw new ServiceErrorException(
                    AuctionErrorEnum.AUCTION_SEARCH_FORBIDDEN_STATUS_FILTER
            );
        }

        AuctionUtil.throwIfSearchConditionNotValid(condition);

        Page<@NonNull Auction> auctions = auctionRepository.findByCondition(
                condition
        );

        Page<@NonNull GetManyAuctionsResponse> auctionsDto = auctions.map(GetManyAuctionsResponse::from);
        
        return PageResponse.create(auctionsDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<GetManyAuctionsResponse> getManyAuctionsMe (
            CustomUserDetails userDetails,
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        Page<@NonNull Auction> auctions = auctionRepository.findByUserIdAndCondition(
                userDetails.getUserId(), condition
        );

        Page<@NonNull GetManyAuctionsResponse> auctionsDto = auctions.map(GetManyAuctionsResponse::from);
        
        return PageResponse.create(auctionsDto);
    }

    @Transactional()
    @CachePut(
        cacheNames = {"getAuction"},
        key = "#result.getId()"
    )
    @CacheEvict(
        cacheNames = {"getManyAuctionsPublic"},
        allEntries = true
    )
    public GetAuctionResponse createAuction(
            CustomUserDetails userDetails,
            CreateAuctionRequest req
    ) {
        // 경매 생성은 중요한 작업이기 때문에 JWT만을 믿지 않고 DB에 유저가 있는지 확인
        userRepository.findById(userDetails.getUserId()).orElseThrow(()->
            new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND)
        );

        AuctionUtil.throwIfCreateAuctionRequestNotValid(req);

        Auction auction = Auction.of(
                userDetails.getUserId(), 
                req.getDescription(),
                req.getMaxPrice(),
                req.getItemName(),
                req.getStartedAt(),
                req.getEndedAt(),
                req.getCategory()
        );

        auction = auctionRepository.saveAndFlush(auction);

        return GetAuctionResponse.from(auction);
    }

    @Transactional()
    @Caching(
        evict = {
            @CacheEvict(
                cacheNames = {"getManyAuctionsPublic"},
                allEntries = true
            ),
            @CacheEvict(
                cacheNames = {"getAuction"},
                key = "#auctionId"
            ),
        }
    )
    public void cancelAuction(
            Long auctionId,
            CustomUserDetails details
    ) {
        // 경매를 취소 할 때 비관적 락을 걸지 않고 select를 합니다.
        //
        // 일단 저희가 경매 시작 직전에 취소를 막기 때문에 동시성 문제가 발생할 일이 없고
        // 또 여러 사람이 같은 경매를 취소 할 일이 없기 때문입니다. 
        //
        // (현재 경매는 본인만 취소가 가능합니다.
        // 그리고 설령 저희가 나중에 관리자가 남의 경매를 취소하게 변경하더라도 
        // 경매자와 고객이 동시에 취소 할려고
        // 해서 동시성 문제가 발생 할 거라 생각 하지는 않습니다)

        LocalDateTime now = LocalDateTime.now();

        // 경매 취소는 중요한 작업이기 때문에 JWT만을 믿지 않고 DB에 유저가 있는지 확인
        userRepository.findById(details.getUserId()).orElseThrow(()->
            new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND)
        );

        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        // 경매 주인이 아니라면 에러를 던지기
        if (!auction.getUserId().equals(details.getUserId())) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_FORBIDDEN_FROM_CANCEL);
        }

        // 경매가 이미 취소되어 있다면 noop
        if (auction.getStatus().equals(AuctionStatus.CANCELLED)) {
            return;
        }

        // 경매가 준비 상태가 아니라면 에러를 던지기
        if (!auction.getStatus().equals(AuctionStatus.READY)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_STATUS_NOT_CANCELLABLE);
        }

        // 경매를 취소하기 너무 늦었다면 에러를 던지기
        LocalDateTime canCancelAfter = auction.getStartedAt().minus(Duration.ofMinutes(10));

        if (!now.isBefore(canCancelAfter)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_TOO_LATE_TO_CANCEL);
        }

        auction.cancel();

        auctionRepository.saveAndFlush(auction);
    }
}
