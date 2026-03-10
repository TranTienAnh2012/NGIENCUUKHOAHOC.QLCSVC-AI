package com.Tta.QLCSVC.DHNT.controller;

import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import com.Tta.QLCSVC.DHNT.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot AI", description = "API tương tác với trợ lý ảo hỗ trợ quản lý CSVC")
@SecurityRequirement(name = "bearer-jwt")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/history")
    @Operation(summary = "Lịch sử hội thoại", description = "Xem lại các câu hỏi và trả lời trước đó của bạn")
    public ResponseEntity<ApiResponse<List<ChatbotHoiThoai>>> getChatHistory() {
        return ResponseEntity.ok(ApiResponse.success(chatbotService.getMyChatHistory()));
    }

    @GetMapping("/session")
    @Operation(summary = "Lấy session ID", description = "Lấy hoặc tạo session cho user hiện tại (tái sử dụng trong 24h)")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession() {
        String sessionId = chatbotService.getOrCreateSession();
        return ResponseEntity.ok(ApiResponse.success(new SessionResponse(sessionId)));
    }

    @PostMapping("/ask")
    @Operation(summary = "Hỏi Chatbot", description = "Gửi câu hỏi cho trợ lý ảo AI với session context")
    public ResponseEntity<ApiResponse<ChatbotHoiThoai>> askChatbot(@RequestBody ChatRequest request) {
        ChatbotHoiThoai result = chatbotService.askChatbot(
                request.getMessage(),
                request.getSessionId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // DTO classes
    @Data
    public static class ChatRequest {
        private String message;
        private String sessionId;
    }

    @Data
    @RequiredArgsConstructor
    public static class SessionResponse {
        private final String sessionId;
    }
}
