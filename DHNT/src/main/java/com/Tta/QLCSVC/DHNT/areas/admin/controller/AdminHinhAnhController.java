package com.Tta.QLCSVC.DHNT.areas.admin.controller;

import com.Tta.QLCSVC.DHNT.dto.ApiResponse;
import com.Tta.QLCSVC.DHNT.entity.HinhAnhThietBi;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.HinhAnhThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import com.Tta.QLCSVC.DHNT.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/hinh-anh")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Hình ảnh", description = "API quản lý hình ảnh thiết bị (Admin)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminHinhAnhController {

    private final FileStorageService fileStorageService;
    private final HinhAnhThietBiRepository hinhAnhRepository;
    private final ThietBiRepository thietBiRepository;

    @GetMapping
    @Operation(summary = "Lấy tất cả hình ảnh", description = "Lấy danh sách tất cả hình ảnh thiết bị (phân trang)")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<HinhAnhThietBi>>> getAllImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<HinhAnhThietBi> result = hinhAnhRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/upload/{thietBiId}")
    @Operation(summary = "Upload hình ảnh", description = "Upload hình ảnh cho thiết bị")
    public ResponseEntity<ApiResponse<HinhAnhThietBi>> uploadImage(
            @PathVariable Long thietBiId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String loaiHinhAnh) throws IOException {

        // Lưu file
        String filename = fileStorageService.storeFile(file);

        // Tạo record trong database
        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));

        HinhAnhThietBi hinhAnh = new HinhAnhThietBi();
        hinhAnh.setThietBi(thietBi);
        hinhAnh.setUrlHinhAnh("/uploads/" + filename);

        if (loaiHinhAnh != null) {
            hinhAnh.setLoaiHinhAnh(HinhAnhThietBi.LoaiHinhAnh.valueOf(loaiHinhAnh));
        }

        HinhAnhThietBi saved = hinhAnhRepository.save(hinhAnh);

        return ResponseEntity.ok(ApiResponse.success("Upload hình ảnh thành công", saved));
    }

    @GetMapping("/thiet-bi/{thietBiId}")
    @Operation(summary = "Lấy hình ảnh thiết bị", description = "Lấy danh sách hình ảnh của thiết bị")
    public ResponseEntity<ApiResponse<List<HinhAnhThietBi>>> getImagesByThietBi(@PathVariable Long thietBiId) {
        List<HinhAnhThietBi> images = hinhAnhRepository.findByThietBiId(thietBiId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin hình ảnh", description = "Lấy thông tin chi tiết một hình ảnh")
    public ResponseEntity<ApiResponse<HinhAnhThietBi>> getImageById(@PathVariable Long id) {
        HinhAnhThietBi hinhAnh = hinhAnhRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HinhAnhThietBi", "id", id));
        return ResponseEntity.ok(ApiResponse.success(hinhAnh));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa hình ảnh", description = "Xóa hình ảnh thiết bị")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) throws IOException {
        HinhAnhThietBi hinhAnh = hinhAnhRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HinhAnhThietBi", "id", id));

        // Xóa file vật lý
        String url = hinhAnh.getUrlHinhAnh();
        if (url != null && url.startsWith("/uploads/")) {
            String filename = url.substring(url.lastIndexOf("/") + 1);
            fileStorageService.deleteFile(filename);
        }

        // Xóa record
        hinhAnhRepository.deleteById(id);

        return ResponseEntity.ok(ApiResponse.success("Xóa hình ảnh thành công", null));
    }

    @PutMapping("/{id}/loai")
    @Operation(summary = "Cập nhật loại hình ảnh", description = "Cập nhật loại hình ảnh (QR_CODE, KIEM_TRA_TINH_TRANG, BAO_HONG, MUON_TRA)")
    public ResponseEntity<ApiResponse<HinhAnhThietBi>> updateImageType(
            @PathVariable Long id,
            @RequestParam String loaiHinhAnh) {
        HinhAnhThietBi hinhAnh = hinhAnhRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HinhAnhThietBi", "id", id));

        hinhAnh.setLoaiHinhAnh(HinhAnhThietBi.LoaiHinhAnh.valueOf(loaiHinhAnh));
        HinhAnhThietBi saved = hinhAnhRepository.save(hinhAnh);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật loại hình ảnh thành công", saved));
    }
}
