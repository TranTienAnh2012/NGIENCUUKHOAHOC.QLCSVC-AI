package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienThietBiService;
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
@RequestMapping("/api/giao-vien/thiet-bi")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GIAO_VIEN', 'ADMIN')")
@Tag(name = "Giáo viên - Thiết bị", description = "API xem thiết bị (Giáo viên)")
@SecurityRequirement(name = "bearer-jwt")
public class GiaoVienThietBiController {

    private final GiaoVienThietBiService giaoVienThietBiService;

    @GetMapping("/available")
    @Operation(summary = "Thiết bị khả dụng", description = "Xem danh sách thiết bị có thể mượn")
    public ResponseEntity<ApiResponse<List<ThietBi>>> getAvailableThietBi() {
        List<ThietBi> thietBis = giaoVienThietBiService.getAllAvailableThietBi();
        return ResponseEntity.ok(ApiResponse.success(thietBis));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thiết bị", description = "Xem thông tin chi tiết thiết bị")
    public ResponseEntity<ApiResponse<ThietBi>> getThietBiById(@PathVariable Long id) {
        ThietBi thietBi = giaoVienThietBiService.getThietBiById(id);
        return ResponseEntity.ok(ApiResponse.success(thietBi));
    }
}
