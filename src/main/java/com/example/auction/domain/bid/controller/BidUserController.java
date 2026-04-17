package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.service.BidQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/me/bids")
@RequiredArgsConstructor
public class BidUserController {

    private final BidQueryService queryService;

    // 내 입찰 조회
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<BidListResponse>>> getMyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<BidListResponse> data = queryService.getMyBids(userDetails, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(String.valueOf(HttpStatus.OK.value()), "내 입찰 조회가 완료되었습니다.", data));
    }
}
