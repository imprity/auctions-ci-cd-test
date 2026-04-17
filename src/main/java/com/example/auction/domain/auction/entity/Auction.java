package com.example.auction.domain.auction.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.common.entity.CreatableEntity;
import com.example.auction.domain.auction.enums.AuctionProductCategory;
import com.example.auction.domain.auction.enums.AuctionStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@Entity
@Table(name = "auctions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Size(max=1024)
    @Column(name="description", nullable = true)
    private String description;

    // TODO: 팀원들과 상의 이후 scale 과 precision 추가
    @Column(name="max_price", nullable = false)
    private BigDecimal maxPrice;

    @Size(max=256)
    @Column(name="item_name", nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name="auction_status", nullable = false)
    private AuctionStatus status;

    @Column(name="started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name="ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name="cancelled_at", nullable = true)
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name="category", nullable = false)
    private AuctionProductCategory category;

    public static Auction of(
        @NonNull Long userId,
        String description,
        @NonNull BigDecimal maxPrice,
        @NonNull String itemName,
        @NonNull LocalDateTime startedAt,
        @NonNull LocalDateTime endedAt,
        @NonNull AuctionProductCategory category
    ) {
        Auction auction = new Auction();

        auction.userId = userId;
        auction.description = description;
        auction.maxPrice = maxPrice;
        auction.itemName = itemName;

        auction.status = AuctionStatus.READY;

        auction.startedAt = startedAt;
        auction.endedAt = endedAt;

        auction.category = category;

        return auction;
    }

    // auction 상태 변경 메서드들
    // READY -> ACTIVE (경매 시작)
    public void activate() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.ACTIVE;
        }
    }

    // ACTIVE -> DONE (낙찰)
    public void close() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.DONE;
        }
    }

    // ACTIVE -> NO_BID (유찰)
    public void noBid() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.NO_BID;
        }
    }

    // READY -> CANCELLED (취소)
    public void cancel() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
        }
    }

}
