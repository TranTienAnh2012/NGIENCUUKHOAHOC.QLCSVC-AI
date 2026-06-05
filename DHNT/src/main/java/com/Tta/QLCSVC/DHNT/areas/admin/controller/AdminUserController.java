package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminNguoiDungService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Người dùng", description = "API quản lý người dùng (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminUserController {

    private final AdminNguoiDungService adminNguoiDungService;

    @GetMapping
    @Operation(summary = "Lấy danh sách người dùng", description = "Lấy danh sách tất cả người dùng (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<NguoiDung>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NguoiDung> pageResult = adminNguoiDungService.getAllUsers(pageable);

        PageResponse<NguoiDung> response = new PageResponse<>(
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
    @Operation(summary = "Lấy toàn bộ người dùng", description = "Lấy toàn bộ danh sách người dùng không phân trang (dùng cho dropdown)")
    public ResponseEntity<ApiResponse<java.util.List<NguoiDung>>> getAllUsersNoPagination() {
        return ResponseEntity.ok(ApiResponse.success(adminNguoiDungService.getAllUsersList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin người dùng", description = "Lấy thông tin chi tiết một người dùng")
    public ResponseEntity<ApiResponse<NguoiDung>> getUserById(@PathVariable Long id) {
        NguoiDung nguoiDung = adminNguoiDungService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(nguoiDung));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa người dùng", description = "Xóa người dùng khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminNguoiDungService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
    }

    @PostMapping
    @Operation(summary = "Tạo người dùng mới", description = "Thêm người dùng mới vào hệ thống")
    public ResponseEntity<ApiResponse<NguoiDung>> createUser(@Valid @RequestBody NguoiDung nguoiDung) {
        NguoiDung created = adminNguoiDungService.createUser(nguoiDung);
        return ResponseEntity.ok(ApiResponse.success("Tạo người dùng thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật người dùng", description = "Cập nhật thông tin người dùng")
    public ResponseEntity<ApiResponse<NguoiDung>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody NguoiDung nguoiDung) {
        NguoiDung updated = adminNguoiDungService.updateUser(id, nguoiDung);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", updated));
    }
}
