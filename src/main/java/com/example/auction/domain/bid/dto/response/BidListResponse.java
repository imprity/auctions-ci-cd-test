package com.example.auction.domain.bid.dto.response;

import com.example.auction.domain.bid.entity.Bid;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 경매별 입찰 조회
@Getter
@AllArgsConstructor
public class BidListResponse {

    private final Long bidId;
    private final Long auctionId;
    private final BigDecimal price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    public static BidListResponse from(Bid bid) {
        return new BidListResponse(
          bid.getId(),
          bid.getAuctionId(),
          bid.getPrice(),
          bid.getCreatedAt()
        );
    }

}
