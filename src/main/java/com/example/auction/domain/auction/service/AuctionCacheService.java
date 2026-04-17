package com.example.auction.domain.auction.service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCacheService {

    public boolean shouldCacheGetManyAuctionsPublic(
            AuctionSearchCondition condition
    ) {
        // 검색 키워드가 있을 경우 cache를 하는 것이 의미 없으므로 skip
        if (condition.getKeyword() != null) {
            return false;
        }

        // 만약에 가격의 범위를 포함해 검색한다면 skip
        if (!(condition.getMaxPriceMin() == null && condition.getMaxPriceMax() == null)) {
            return false;
        }

        // 만약에 페이지가 10 페이지를 넘어갔다면 skip
        if (condition.getPage() >= 9) {
            return false;
        }

        return true;
    }

    public String getManyAuctionsPublicCacheKey(
            AuctionSearchCondition condition
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("status=[");
        if (condition.getStatus() != null) {
            String statusKey = condition.getStatus()
                .stream().sorted().map(Enum::name).collect(Collectors.joining(","));
            builder.append(statusKey);
        }
        builder.append("]");

        builder.append("category=[");
        if (condition.getCategory() != null) {
            builder.append(condition.getCategory());
        }
        builder.append("]");

        builder.append("page=[");
        builder.append(condition.getPage());
        builder.append("]");

        builder.append("pageSize=[");
        builder.append(condition.getPageSize());
        builder.append("]");

        return builder.toString();
    }
}
