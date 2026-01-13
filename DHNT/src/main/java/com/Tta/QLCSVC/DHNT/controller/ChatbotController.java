package com.Tta.QLCSVC.DHNT.controller;

import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import com.Tta.QLCSVC.DHNT.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/ask")
    @Operation(summary = "Hỏi Chatbot", description = "Gửi câu hỏi cho trợ lý ảo AI")
    public ResponseEntity<ApiResponse<ChatbotHoiThoai>> askChatbot(@RequestBody String cauHoi) {
        // Handle raw string body if it contains quotes from JSON
        if (cauHoi.startsWith("\"") && cauHoi.endsWith("\"")) {
            cauHoi = cauHoi.substring(1, cauHoi.length() - 1);
        }
        ChatbotHoiThoai result = chatbotService.askChatbot(cauHoi);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
