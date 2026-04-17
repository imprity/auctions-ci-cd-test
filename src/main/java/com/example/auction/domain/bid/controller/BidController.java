package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.service.BidCommandFacade;
import com.example.auction.domain.bid.service.BidQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.auction.domain.bid.dto.response.BidResponse;

@RestController
@RequestMapping("/api/auctions/{auction_id}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidCommandFacade commandService;
    private final BidQueryService queryService;

    // 특정 경매에 입찰 생성 - 기본 버전, before/after 비교용
    @PostMapping("/v1")
    public ResponseEntity<BaseResponse<BidResponse>> placeBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auction_id") Long auctionId,
            @Valid @RequestBody BidRequest request
    ) {
        BidResponse data = commandService.placeBid(userDetails, auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(String.valueOf(HttpStatus.CREATED.value()), "입찰이 완료되었습니다", data));
    }

    // 특정 경매에 입찰 생성 - 분산락
    @PostMapping("/v2")
    public ResponseEntity<BaseResponse<BidResponse>> placeBidDis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auction_id") Long auctionId,
            @Valid @RequestBody BidRequest request
    ) {
        BidResponse data = commandService.placeBidDis(userDetails, auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(String.valueOf(HttpStatus.CREATED.value()), "입찰이 완료되었습니다", data));
    }


    // 특정 경매의 입찰 조회
    @GetMapping("/v1")
    public ResponseEntity<BaseResponse<PageResponse<BidListResponse>>> getBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auction_id") Long auctionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "price")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        PageResponse<BidListResponse> data = queryService.getBids(userDetails, auctionId, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(String.valueOf(HttpStatus.OK.value()), "입찰 내역 조회가 완료되었습니다", data));
    }

    // 입찰 결과 조회(1건)
    @GetMapping("/winner/v1")
    public ResponseEntity<BaseResponse<BidResponse>> getWinnerBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auction_id") Long auctionId
    ) {
        BidResponse data = queryService.getWinnerBid(userDetails, auctionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(String.valueOf(HttpStatus.OK.value()), "입찰 결과 조회가 완료되었습니다", data));
    }

    // (경매 진행중) 현재 최저가입찰 조회
    @GetMapping("/current/v1")
    public ResponseEntity<BaseResponse<BidResponse>> getCurrentMinBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auction_id") Long auctionId
    ) {
        BidResponse data = queryService.getCurrentMinBid(userDetails, auctionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(String.valueOf(HttpStatus.OK.value()), "현재 최저가 입찰 조회가 완료되었습니다", data));
    }
}
