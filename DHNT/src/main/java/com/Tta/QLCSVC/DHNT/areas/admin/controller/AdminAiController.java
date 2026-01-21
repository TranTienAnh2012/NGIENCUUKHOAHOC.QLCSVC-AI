package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminAiService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.AiDuLieuHuanLuyen;
import com.Tta.QLCSVC.DHNT.entity.AiModelMetrics;
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
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - AI Management", description = "API quản lý dữ liệu huấn luyện và chỉ số AI (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminAiController {

    private final AdminAiService adminAiService;

    @GetMapping("/training-data")
    @Operation(summary = "Lấy dữ liệu huấn luyện", description = "Lấy danh sách dữ liệu dùng để huấn luyện AI")
    public ResponseEntity<ApiResponse<PageResponse<AiDuLieuHuanLuyen>>> getAllTrainingData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AiDuLieuHuanLuyen> pageResult = adminAiService.getAllTrainingData(pageable);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<AiDuLieuHuanLuyen>(pageResult)));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Lấy chỉ số model", description = "Lấy danh sách chỉ số đánh giá model AI")
    public ResponseEntity<ApiResponse<PageResponse<AiModelMetrics>>> getAllMetrics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AiModelMetrics> pageResult = adminAiService.getAllMetrics(pageable);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<AiModelMetrics>(pageResult)));
    }

    @DeleteMapping("/training-data/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrainingData(@PathVariable Long id) {
        adminAiService.deleteTrainingData(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa dữ liệu huấn luyện thành công", null));
    }

    @DeleteMapping("/metrics/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMetric(@PathVariable Long id) {
        adminAiService.deleteMetric(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chỉ số thành công", null));
    }
}
