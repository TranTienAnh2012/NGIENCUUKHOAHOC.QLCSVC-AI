package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.controller;

import com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThietBiService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nhanvien-csvc/thiet-bi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NHAN_VIEN_CSVC', 'ADMIN')")
@Tag(name = "CSVC - Thiết bị", description = "API quản lý thiết bị (CSVC)")
@SecurityRequirement(name = "bearer-jwt")
public class CSVCThietBiController {

    private final CSVCThietBiService csvcThietBiService;

    @GetMapping("/damaged")
    @Operation(summary = "Thiết bị hỏng", description = "Lấy danh sách thiết bị bị hỏng")
    public ResponseEntity<ApiResponse<List<ThietBi>>> getDamagedEquipment() {
        List<ThietBi> thietBis = csvcThietBiService.getDamagedThietBi();
        return ResponseEntity.ok(ApiResponse.success(thietBis));
    }

    @GetMapping("/maintenance")
    @Operation(summary = "Thiết bị đang bảo trì", description = "Lấy danh sách thiết bị đang bảo trì")
    public ResponseEntity<ApiResponse<List<ThietBi>>> getMaintenanceEquipment() {
        List<ThietBi> thietBis = csvcThietBiService.getMaintenanceThietBi();
        return ResponseEntity.ok(ApiResponse.success(thietBis));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái", description = "Cập nhật trạng thái thiết bị")
    public ResponseEntity<ApiResponse<ThietBi>> updateStatus(
            @PathVariable Long id,
            @RequestParam String trangThai) {
        ThietBi.TrangThaiThietBi status = ThietBi.TrangThaiThietBi.valueOf(trangThai);
        ThietBi updated = csvcThietBiService.updateTrangThai(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }
}
