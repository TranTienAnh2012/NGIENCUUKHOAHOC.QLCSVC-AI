package com.Tta.QLCSVC.DHNT.areas.giaovien.controller;

import com.Tta.QLCSVC.DHNT.areas.giaovien.service.GiaoVienMuonTraService;
import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/giao-vien/muon-tra")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GIAO_VIEN', 'ADMIN')")
@Tag(name = "Giáo viên - Mượn trả", description = "API mượn/trả thiết bị (Giáo viên)")
@SecurityRequirement(name = "bearer-jwt")
public class GiaoVienMuonTraController {

    private final GiaoVienMuonTraService giaoVienMuonTraService;

    @GetMapping("/my-borrowings")
    @Operation(summary = "Lịch sử mượn của tôi", description = "Xem lịch sử mượn thiết bị")
    public ResponseEntity<ApiResponse<List<MuonTraThietBi>>> getMyBorrowings() {
        List<MuonTraThietBi> borrowings = giaoVienMuonTraService.getMyBorrowings();
        return ResponseEntity.ok(ApiResponse.success(borrowings));
    }

    @PostMapping("/borrow")
    @Operation(summary = "Mượn thiết bị", description = "Tạo phiếu mượn thiết bị")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> borrowEquipment(
            @RequestParam Long thietBiId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngayTraDuKien) {
        MuonTraThietBi muonTra = giaoVienMuonTraService.borrowEquipment(thietBiId, ngayTraDuKien);
        return ResponseEntity.ok(ApiResponse.success("Mượn thiết bị thành công", muonTra));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Trả thiết bị", description = "Trả thiết bị đã mượn")
    public ResponseEntity<ApiResponse<MuonTraThietBi>> returnEquipment(@PathVariable Long id) {
        MuonTraThietBi muonTra = giaoVienMuonTraService.returnEquipment(id);
        return ResponseEntity.ok(ApiResponse.success("Trả thiết bị thành công", muonTra));
    }
}
