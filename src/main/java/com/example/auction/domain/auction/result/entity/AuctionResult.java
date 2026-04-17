package com.example.auction.domain.auction.result.entity;

import com.example.auction.common.entity.ModifiableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "auction_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionResult extends ModifiableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "buyer_id", nullable = false) // 입찰 주최자
    private Long buyerId;

    @Column(name = "seller_id", nullable = false) // 입찰 참여자
    private Long sellerId;

    @Column(name = "bid_id", nullable = false)
    private Long bidId;

    public static AuctionResult of(
            BigDecimal price, Long auctionId, Long buyerId, Long sellerId, Long bidId
    ) {
        AuctionResult result = new AuctionResult();

        result.price = price;
        result.auctionId = auctionId;
        result.buyerId = buyerId;
        result.sellerId = sellerId;
        result.bidId = bidId;

        return result;
    }
}
