package com.example.auction.common.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorStoreConfig {

    @Bean
    public PgVectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(768)              // nomic-embed-text 출력 벡터 차원수
                .distanceType(COSINE_DISTANCE) // 벡터 간 유사도 측정 방식 (코사인 거리)
                .indexType(HNSW)              // 근사 최근접 이웃 검색 인덱스 (속도/정확도 균형)
                .initializeSchema(true)       // 최초 실행 시 vector_store 테이블 자동 생성
                .build();
    }
}