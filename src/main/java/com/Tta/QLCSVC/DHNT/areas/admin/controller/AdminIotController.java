package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminIotService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.IotDuLieuCamBien;
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
@RequestMapping("/api/admin/iot")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - IoT", description = "API quản lý dữ liệu IoT (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminIotController {

    private final AdminIotService adminIotService;

    @GetMapping
    @Operation(summary = "Lấy dữ liệu IoT", description = "Lấy danh sách dữ liệu cảm biến (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<IotDuLieuCamBien>>> getAllIotData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IotDuLieuCamBien> pageResult = adminIotService.getAllIotData(pageable);

        PageResponse<IotDuLieuCamBien> response = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast(),
                pageResult.isFirst());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa dữ liệu IoT", description = "Xóa một bản ghi dữ liệu cảm biến")
    public ResponseEntity<ApiResponse<Void>> deleteIotData(@PathVariable Long id) {
        adminIotService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa dữ liệu thành công", null));
    }
}
