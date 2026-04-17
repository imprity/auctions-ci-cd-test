package com.example.auction.domain.chat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 생성
    @PostMapping("/rooms")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> createRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        HttpStatus.CREATED.name(),
                        "채팅방을 생성했습니다",
                        chatService.createRoom(userDetails.getUserId())
                ));
    }

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<BaseResponse<List<ChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "채팅방 목록을 조회했습니다",
                        chatService.getRooms(userDetails.getUserId())
                ));
    }

    // 채팅방 삭제
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<BaseResponse<Void>> deleteRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        chatService.deleteRoom(roomId, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "채팅방을 삭제했습니다",
                        null
                ));
    }

    // 메시지 목록 조회 (커서 기반 페이징)
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<BaseResponse<ChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "메시지 목록을 조회했습니다",
                        chatService.getMessages(roomId, userDetails.getUserId(), cursor, size)
                ));
    }
}