package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminLoaiPhongService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.LoaiPhong;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/loai-phong")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Loại phòng", description = "API quản lý loại phòng học (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminLoaiPhongController {

    private final AdminLoaiPhongService loaiPhongService;

    @GetMapping
    @Operation(summary = "Lấy danh sách loại phòng", description = "Lấy danh sách tất cả loại phòng học (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<LoaiPhong>>> getAllLoaiPhong(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LoaiPhong> pageResult = loaiPhongService.getAllLoaiPhong(pageable);

        PageResponse<LoaiPhong> response = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast(),
                pageResult.isFirst());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả loại phòng", description = "Lấy danh sách tất cả loại phòng không phân trang")
    public ResponseEntity<ApiResponse<List<LoaiPhong>>> getAllLoaiPhongList() {
        return ResponseEntity.ok(ApiResponse.success(loaiPhongService.getAllLoaiPhong()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin loại phòng", description = "Lấy thông tin chi tiết một loại phòng")
    public ResponseEntity<ApiResponse<LoaiPhong>> getLoaiPhongById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loaiPhongService.getLoaiPhongById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo loại phòng mới", description = "Thêm loại phòng học mới vào hệ thống")
    public ResponseEntity<ApiResponse<LoaiPhong>> createLoaiPhong(@RequestBody LoaiPhong loaiPhong) {
        return ResponseEntity
                .ok(ApiResponse.success("Tạo loại phòng thành công", loaiPhongService.createLoaiPhong(loaiPhong)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật loại phòng", description = "Cập nhật thông tin loại phòng học")
    public ResponseEntity<ApiResponse<LoaiPhong>> updateLoaiPhong(
            @PathVariable Long id,
            @RequestBody LoaiPhong loaiPhong) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật loại phòng thành công", loaiPhongService.updateLoaiPhong(id, loaiPhong)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa loại phòng", description = "Xóa loại phòng học khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteLoaiPhong(@PathVariable Long id) {
        loaiPhongService.deleteLoaiPhong(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại phòng thành công", null));
    }
}
