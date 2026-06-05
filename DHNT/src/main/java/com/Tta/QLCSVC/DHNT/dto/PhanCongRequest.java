package com.Tta.QLCSVC.DHNT.dto;

import lombok.Data;

/**
 * DTO nhận request Admin phân công nhân viên cho 1 phiếu báo hỏng.
 * nhanVienId bắt buộc và phải trỏ tới user có VaiTro = NHAN_VIEN_CSVC.
 */
@Data
public class PhanCongRequest {
    private Long nhanVienId;   // ID nhân viên CSVC được phân công
    private String ghiChuAdmin; // Ghi chú hướng dẫn thêm (optional)
}
