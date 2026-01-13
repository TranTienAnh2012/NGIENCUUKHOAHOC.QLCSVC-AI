package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminPhongHocService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/phong-hoc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Phòng học", description = "API quản lý phòng học (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminPhongHocController {

    private final AdminPhongHocService adminPhongHocService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng học", description = "Lấy danh sách tất cả phòng học")
    public ResponseEntity<ApiResponse<List<PhongHoc>>> getAllPhongHoc() {
        List<PhongHoc> phongHocs = adminPhongHocService.getAllPhongHoc();
        return ResponseEntity.ok(ApiResponse.success(phongHocs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phòng học", description = "Lấy thông tin chi tiết một phòng học")
    public ResponseEntity<ApiResponse<PhongHoc>> getPhongHocById(@PathVariable Long id) {
        PhongHoc phongHoc = adminPhongHocService.getPhongHocById(id);
        return ResponseEntity.ok(ApiResponse.success(phongHoc));
    }
}
