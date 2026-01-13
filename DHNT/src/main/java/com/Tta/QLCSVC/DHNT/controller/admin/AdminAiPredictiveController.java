package com.Tta.QLCSVC.DHNT.controller.admin;

import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.AiDuDoanBaoTri;
import com.Tta.QLCSVC.DHNT.service.AiPredictiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/predictive")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - AI Predictive", description = "API dự đoán bảo trì thông minh (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminAiPredictiveController {

    private final AiPredictiveService aiPredictiveService;

    @GetMapping("/high-risk")
    @Operation(summary = "Thiết bị có rủi ro cao", description = "Lấy danh sách thiết bị được AI cảnh báo có nguy cơ hỏng cao")
    public ResponseEntity<ApiResponse<List<AiDuDoanBaoTri>>> getHighRiskEquipments() {
        return ResponseEntity.ok(ApiResponse.success(aiPredictiveService.getHighRiskEquipments()));
    }

    @PostMapping("/run/{thietBiId}")
    @Operation(summary = "Chạy dự đoán", description = "Yêu cầu AI phân tích rủi ro hỏng hóc cho một thiết bị cụ thể")
    public ResponseEntity<ApiResponse<AiDuDoanBaoTri>> runPrediction(@PathVariable Long thietBiId) {
        AiDuDoanBaoTri result = aiPredictiveService.runPrediction(thietBiId);
        return ResponseEntity.ok(ApiResponse.success("Phân tích AI hoàn tất", result));
    }
}
