package com.example.auction.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int currentPage,
        int totalPages,
        long totalElements,
        int size,
        boolean isLast
) {
    public static <T> PageResponse<T> create(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isLast()
        );
    }
}
