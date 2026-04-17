package com.example.auction.domain.review;

import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.auth.dto.AuthLoginRequest;
import com.example.auction.domain.auth.dto.AuthSignupRequest;
import com.example.auction.domain.review.dto.ReviewCreateRequest;
import com.example.auction.domain.review.dto.ReviewModifyRequest;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionResultRepository auctionResultRepository;

    private String buyerToken;
    private String sellerToken;
    private Long auctionId;

    @BeforeEach
    void setUp() throws Exception {
        // 구매자, 판매자 회원가입
        signup("buyer@test.com", "password123");
        signup("seller@test.com", "password123");

        // 토큰 추출
        buyerToken = getAccessToken("buyer@test.com", "password123");
        sellerToken = getAccessToken("seller@test.com", "password123");

        // userId 조회
        Long buyerId = userRepository.findByEmailAndDeletedFalse("buyer@test.com").orElseThrow(
                () -> new IllegalStateException("buyer 유저 없음")).getId();
        Long sellerId = userRepository.findByEmailAndDeletedFalse("seller@test.com").orElseThrow(
                () -> new IllegalStateException("seller 유저 없음")).getId();

        // 경매 결과 저장
        auctionId = 1L;
        auctionResultRepository.save(
                AuctionResult.of(BigDecimal.valueOf(10000), auctionId, buyerId, sellerId, 1L)
        );
    }

    @AfterEach
    void cleanUpRedis() {
        var factory = redisTemplate.getConnectionFactory();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.serverCommands().flushDb();
            }
        }
    }


    // ========================
    // 리뷰 생성
    // ========================

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 생성 요청 성공"))
                .andExpect(jsonPath("$.data.score").value(5))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 이미 작성한 리뷰")
    void createReview_fail_alreadyReviewed() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, 5, "좋아요");
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 해당 경매에 리뷰를 작성했습니다"));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 경매 결과 없음")
    void createReview_fail_auctionResultNotFound() throws Exception {
        // given
        ReviewCreateRequest request = new ReviewCreateRequest(999L, 5, "좋아요");

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("경매 결과를 찾을 수 없습니다"));
    }


    // ========================
    // 작성한 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("작성한 리뷰 목록 조회 성공")
    void getWrittenReviewList_success() throws Exception {
        // given
        createReview(buyerToken, auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(get("/api/reviews/written")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("작성한 리뷰 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }


    // ========================
    // 받은 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("받은 리뷰 목록 조회 성공")
    void getReceivedReviewList_success() throws Exception {
        // given
        createReview(buyerToken, auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(get("/api/reviews/received")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("받은 리뷰 목록 조회 요청 성공"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }


    // ========================
    // 리뷰 상세 조회
    // ========================

    @Test
    @DisplayName("리뷰 상세 조회 성공")
    void getReview_success() throws Exception {
        // given
        Long reviewId = createReview(buyerToken, auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(get("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 상세 조회 요청 성공"))
                .andExpect(jsonPath("$.data.score").value(5));
    }


    // ========================
    // 리뷰 수정
    // ========================

    @Test
    @DisplayName("리뷰 수정 성공")
    void modifyReview_success() throws Exception {
        // given
        Long reviewId = createReview(buyerToken, auctionId, 5, "좋아요");
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요");

        // when & then
        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 수정 요청 성공"))
                .andExpect(jsonPath("$.data.score").value(1));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 작성자 아님")
    void modifyReview_fail_forbidden() throws Exception {
        // given
        Long reviewId = createReview(buyerToken, auctionId, 5, "좋아요");
        ReviewModifyRequest request = new ReviewModifyRequest(1, "별로에요");

        // when & then
        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리뷰 작성자가 아닙니다"));
    }


    // ========================
    // 리뷰 삭제
    // ========================

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_success() throws Exception {
        // given
        Long reviewId = createReview(buyerToken, auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("리뷰 삭제 요청 성공"));
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 작성자 아님")
    void deleteReview_fail_forbidden() throws Exception {
        // given
        Long reviewId = createReview(buyerToken, auctionId, 5, "좋아요");

        // when & then
        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리뷰 작성자가 아닙니다"));
    }


    // ========================
    // 헬퍼 메서드
    // ========================

    private void signup(String email, String password) throws Exception {
        AuthSignupRequest request = new AuthSignupRequest(email, password, UserRole.USER);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String getAccessToken(String email, String password) throws Exception {
        AuthLoginRequest request = new AuthLoginRequest(email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asString();
    }

    private Long createReview(String token, Long auctionId, int score, String description) throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(auctionId, score, description);
        String response = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("reviewId").asLong();
    }
}