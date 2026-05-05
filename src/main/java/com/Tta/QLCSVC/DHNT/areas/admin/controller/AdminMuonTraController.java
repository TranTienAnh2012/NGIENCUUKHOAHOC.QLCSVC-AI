package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminMuonTraService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.dto.PageResponse;
import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
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
@RequestMapping("/api/admin/muon-tra")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Mượn trả", description = "API quản lý mượn trả thiết bị (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminMuonTraController {

    private final AdminMuonTraService adminMuonTraService;

    @GetMapping
    @Operation(summary = "Lấy danh sách mượn trả", description = "Lấy danh sách tất cả phiếu mượn trả (phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<MuonTraThietBi>>> getAllMuonTra(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MuonTraThietBi> pageResult = adminMuonTraService.getAllMuonTra(pageable);

        PageResponse<MuonTraThietBi> response = new PageResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast(),
                pageResult.isFirst());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phiếu mượn trả", description = "Lấy thông tin chi tiết một phiếu mượn trả")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> getMuonTraById(@PathVariable Long id) {
        MuonTraThietBi muonTra = adminMuonTraService.getMuonTraById(id);
        return ResponseEntity.ok(ApiResponse.success(muonTra));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy phiếu theo người mượn", description = "Lấy danh sách phiếu mượn trả của một người dùng")
    public ResponseEntity<ApiResponse<List<MuonTraThietBi>>> getMuonTraByUser(@PathVariable Long userId) {
        List<MuonTraThietBi> muonTraList = adminMuonTraService.getMuonTraByNguoiMuon(userId);
        return ResponseEntity.ok(ApiResponse.success(muonTraList));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Lấy phiếu theo thiết bị", description = "Lấy danh sách phiếu mượn trả của một thiết bị")
    public ResponseEntity<ApiResponse<List<MuonTraThietBi>>> getMuonTraByThietBi(@PathVariable Long thietBiId) {
        List<MuonTraThietBi> muonTraList = adminMuonTraService.getMuonTraByThietBi(thietBiId);
        return ResponseEntity.ok(ApiResponse.success(muonTraList));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Lấy phiếu theo trạng thái", description = "Lấy danh sách phiếu mượn trả theo trạng thái (DANG_MUON, DA_TRA, QUA_HAN)")
    public ResponseEntity<ApiResponse<List<MuonTraThietBi>>> getMuonTraByStatus(@PathVariable String status) {
        MuonTraThietBi.TrangThaiMuonTra trangThai = MuonTraThietBi.TrangThaiMuonTra.valueOf(status);
        List<MuonTraThietBi> muonTraList = adminMuonTraService.getMuonTraByTrangThai(trangThai);
        return ResponseEntity.ok(ApiResponse.success(muonTraList));
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu mượn trả mới", description = "Thêm phiếu mượn trả mới vào hệ thống")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> createMuonTra(@RequestBody MuonTraThietBi muonTra) {
        MuonTraThietBi created = adminMuonTraService.createMuonTra(muonTra);
        return ResponseEntity.ok(ApiResponse.success("Tạo phiếu mượn trả thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phiếu mượn trả", description = "Cập nhật toàn bộ thông tin phiếu mượn trả")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> updateMuonTra(
            @PathVariable Long id,
            @RequestBody MuonTraThietBi muonTra) {
        MuonTraThietBi updated = adminMuonTraService.updateMuonTra(id, muonTra);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phiếu mượn trả thành công", updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái", description = "Cập nhật trạng thái phiếu mượn trả")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        MuonTraThietBi.TrangThaiMuonTra trangThai = MuonTraThietBi.TrangThaiMuonTra.valueOf(status);
        MuonTraThietBi updated = adminMuonTraService.updateTrangThai(id, trangThai);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phiếu mượn trả", description = "Xóa phiếu mượn trả khỏi hệ thống")
    public ResponseEntity<ApiResponse<Void>> deleteMuonTra(@PathVariable Long id) {
        adminMuonTraService.deleteMuonTra(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phiếu mượn trả thành công", null));
    }
}
