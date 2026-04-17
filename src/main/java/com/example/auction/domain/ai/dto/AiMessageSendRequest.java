package com.example.auction.domain.ai.dto;

public record AiMessageSendRequest(
        // 코드래빗은 2000자를 제안했지만, 현재 서비스는 짧은 질문에 대답하는 채팅봇이고 긴 문서를 분석하는 용도가 아니기에 500자 정도면 충분할것같음
        String content // 검증은 AiService.streamMessage() 내부 Flux.defer()에서 수행 (SSE MediaType 충돌 방지)
        // 때문에 `@Valid` 제거했음. 자체 검증 예정.
)
{
}