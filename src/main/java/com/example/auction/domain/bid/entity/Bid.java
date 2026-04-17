package com.example.auction.domain.bid.entity;


import com.example.auction.common.entity.CreatableEntity;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "bids")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 1024)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false, name = "auction_id")
    private Long auctionId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidAuctionStatus status;

    public static Bid of(String description, BigDecimal price, Long auctionId, Long userId, BidAuctionStatus status) {
        Bid bid = new Bid();
        bid.description = description;
        bid.userId = userId;
        bid.auctionId = auctionId;
        bid.price = price;
        bid.status = status;

        return bid;
    }

    public void updateStatus(BidAuctionStatus status) {
        this.status = status;
    }
}
