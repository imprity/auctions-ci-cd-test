package com.example.auction.domain.ai.service;

import com.example.auction.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// 후기 텍스트 임베딩 저장 + 유사도 검색 — 판매자 신뢰도 RAG 분석에 활용
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEmbeddingService {

    private static final int TOP_K = 5;                   // 유사도 검색 결과 최대 개수
    private static final double SIMILARITY_THRESHOLD = 0.4; // 최소 유사도 점수 (0~1)

    private final VectorStore vectorStore;

    // 후기 1건을 벡터로 변환해 pgvector에 저장 — 리뷰 생성 시 호출
    public void embed(Review review) {
        if (review.getDescription() == null || review.getDescription().isBlank()) {
            return; // 텍스트 없는 별점 전용 후기는 임베딩 대상 아님
        }

        Document document = new Document(
                review.getDescription(),
                Map.of(
                        "sellerId", review.getRevieweeId(), // sellerId 필터링 키
                        "score", review.getScore(),
                        "reviewId", review.getId()
                )
        );
        vectorStore.add(List.of(document));
    }

    // sellerId 필터 + 의미 유사도 기반 후기 텍스트 검색 — LLM 컨텍스트 주입용
    public List<String> search(Long sellerId, String query) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .filterExpression("sellerId == " + sellerId) // 해당 판매자 후기만 필터링
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .toList();
    }
}
