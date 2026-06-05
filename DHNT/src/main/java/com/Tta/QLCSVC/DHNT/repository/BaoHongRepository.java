package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaoHongRepository extends JpaRepository<BaoHong, Long> {

    List<BaoHong> findByThietBiId(Long thietBiId);

    long countByTrangThai(BaoHong.TrangThaiBaoHong trangThai);

    List<BaoHong> findTop5ByOrderByNgayBaoDesc();

    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi WHERE b.nguoiBao.id = :nguoiBaoId")
    List<BaoHong> findByNguoiBaoId(@Param("nguoiBaoId") Long nguoiBaoId);

    List<BaoHong> findByTrangThai(BaoHong.TrangThaiBaoHong trangThai);

    List<BaoHong> findByMucDoNghiemTrong(BaoHong.MucDoNghiemTrong mucDo);

    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi LEFT JOIN FETCH b.nguoiBao " +
           "WHERE b.trangThai = 'CHO_XU_LY' OR b.trangThai = 'DANG_XU_LY' " +
           "ORDER BY b.mucDoNghiemTrong DESC, b.ngayBao ASC")
    List<BaoHong> findPendingReports();

    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi LEFT JOIN FETCH b.nguoiBao " +
           "WHERE b.trangThai = 'CHO_XU_LY' AND (b.mucDoNghiemTrong = 'CAO' OR b.mucDoNghiemTrong = 'KHAN_CAP')")
    List<BaoHong> findUrgentReports();

    // === ASSIGNMENT WORKFLOW QUERIES (Composite Index: nguoi_phu_trach_id + trang_thai_phan_cong) ===

    /** Lấy phiếu chưa được phân công — dành cho Admin hiển thị */
    List<BaoHong> findByTrangThaiPhanCong(BaoHong.TrangThaiPhanCong trangThaiPhanCong);

    /** Phiếu được gán cho nhân viên cụ thể đang chờ xác nhận hoặc đã nhận */
    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi LEFT JOIN FETCH b.nguoiBao " +
           "WHERE b.nguoiPhuTrach.id = :nhanVienId " +
           "AND b.trangThaiPhanCong IN ('CHO_XAC_NHAN', 'DA_NHAN') " +
           "ORDER BY b.mucDoNghiemTrong DESC, b.ngayBao ASC")
    List<BaoHong> findAssignedToNhanVien(@Param("nhanVienId") Long nhanVienId);

    /** Phiếu nhân viên đang CHO_XAC_NHAN - dùng cho notification badge */
    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi " +
           "WHERE b.nguoiPhuTrach.id = :nhanVienId AND b.trangThaiPhanCong = 'CHO_XAC_NHAN'")
    List<BaoHong> findPendingAcceptanceByNhanVien(@Param("nhanVienId") Long nhanVienId);

    /** Count phiếu chờ xác nhận — dùng cho badge sidebar */
    @Query("SELECT COUNT(b) FROM BaoHong b WHERE b.nguoiPhuTrach.id = :nhanVienId AND b.trangThaiPhanCong = 'CHO_XAC_NHAN'")
    long countPendingAcceptanceByNhanVien(@Param("nhanVienId") Long nhanVienId);

    /** Tất cả phiếu chưa được phân công — dùng cho AI chatbot suggest */
    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi " +
           "WHERE b.trangThaiPhanCong = 'CHUA_PHAN_CONG' AND b.trangThai = 'CHO_XU_LY' " +
           "ORDER BY b.mucDoNghiemTrong DESC, b.ngayBao ASC")
    List<BaoHong> findUnassignedPendingReports();
}
