package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaoHongRepository extends JpaRepository<BaoHong, Long> {

    List<BaoHong> findByThietBiId(Long thietBiId);

    @Query("SELECT b FROM BaoHong b LEFT JOIN FETCH b.thietBi WHERE b.nguoiBao.id = :nguoiBaoId")
    List<BaoHong> findByNguoiBaoId(@org.springframework.data.repository.query.Param("nguoiBaoId") Long nguoiBaoId);

    List<BaoHong> findByTrangThai(BaoHong.TrangThaiBaoHong trangThai);

    List<BaoHong> findByMucDoNghiemTrong(BaoHong.MucDoNghiemTrong mucDo);

    @Query("SELECT b FROM BaoHong b WHERE b.trangThai = 'CHO_XU_LY' OR b.trangThai = 'DANG_XU_LY' ORDER BY b.mucDoNghiemTrong DESC, b.ngayBao ASC")
    List<BaoHong> findPendingReports();

    @Query("SELECT b FROM BaoHong b WHERE b.trangThai = 'CHO_XU_LY' AND (b.mucDoNghiemTrong = 'CAO' OR b.mucDoNghiemTrong = 'KHAN_CAP')")
    List<BaoHong> findUrgentReports();

    long countByTrangThai(BaoHong.TrangThaiBaoHong trangThai);

    List<BaoHong> findTop5ByOrderByNgayBaoDesc();
}
