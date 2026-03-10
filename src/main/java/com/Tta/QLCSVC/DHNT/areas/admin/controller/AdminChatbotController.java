package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminChatbotService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chatbot")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Chatbot Audit", description = "API kiểm toán lịch sử chatbot (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminChatbotController {

    private final AdminChatbotService adminChatbotService;

    @GetMapping("/logs")
    @Operation(summary = "Lấy nhật ký chatbot", description = "Lấy danh sách các cuộc hội thoại với chatbot")
    public ResponseEntity<ApiResponse<PageResponse<ChatbotHoiThoai>>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatbotHoiThoai> pageResult = adminChatbotService.getAllConversations(pageable);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<ChatbotHoiThoai>(pageResult)));
    }

    @DeleteMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLog(@PathVariable Long id) {
        adminChatbotService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhật ký hội thoại thành công", null));
    }
}
