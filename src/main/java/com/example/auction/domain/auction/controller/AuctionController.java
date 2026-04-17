package com.example.auction.domain.auction.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.service.AuctionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor()
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping("/api/auctions/{auctionId}")
    public ResponseEntity<@NonNull BaseResponse<GetAuctionResponse>> getAuction(@PathVariable Long auctionId) {
        GetAuctionResponse res = auctionService.getAuction(auctionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "경매 단건조회를 하였습니다",
                        res
                ));
    }

    @GetMapping("/api/auctions")
    public ResponseEntity<@NonNull BaseResponse<PageResponse<GetManyAuctionsResponse>>> getManyAuctionsPublic(
            @ModelAttribute @Valid AuctionSearchCondition conditionDto
    ) {
        PageResponse<GetManyAuctionsResponse> res = auctionService.getManyAuctionsPublic(conditionDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "경매 전체 조회를 하였습니다",
                        res
                ));
    }

    @GetMapping("/api/me/auctions")
    public ResponseEntity<@NonNull BaseResponse<PageResponse<GetManyAuctionsResponse>>> getManyAuctionsMe(
            @ModelAttribute @Valid AuctionSearchCondition conditionDto,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        PageResponse<GetManyAuctionsResponse> res = auctionService.getManyAuctionsMe(details, conditionDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "경매 전체 조회를 하였습니다",
                        res
                ));
    }

    @PostMapping("/api/auctions")
    public ResponseEntity<@NonNull BaseResponse<GetAuctionResponse>> createAuction(
            @RequestBody @Valid CreateAuctionRequest req,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        GetAuctionResponse res = auctionService.createAuction(details, req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        HttpStatus.CREATED.name(),
                        "경매를 생성 하였습니다",
                        res
                ));
    }

    @DeleteMapping("/api/auctions/{auctionId}")
    public ResponseEntity<Void> cancelAuction(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        auctionService.cancelAuction(auctionId, details);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
