package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MuonTraThietBiRepository extends JpaRepository<MuonTraThietBi, Long> {

    // Lấy tất cả phiếu mượn của user, eager-load thietBi và nguoiMuon
    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi LEFT JOIN FETCH m.nguoiMuon WHERE m.nguoiMuon.id = :nguoiMuonId ORDER BY m.ngayMuon DESC")
    List<MuonTraThietBi> findByNguoiMuonId(@Param("nguoiMuonId") Long nguoiMuonId);

    // Lấy phiếu theo user + trangThai có eager-load thietBi và nguoiMuon (tránh
    // LazyInit)
    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi LEFT JOIN FETCH m.nguoiMuon WHERE m.nguoiMuon.id = :nguoiMuonId AND m.trangThai = :trangThai ORDER BY m.ngayMuon DESC")
    List<MuonTraThietBi> findByNguoiMuonIdAndTrangThai(@Param("nguoiMuonId") Long nguoiMuonId,
            @Param("trangThai") MuonTraThietBi.TrangThaiMuonTra trangThai);

    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi LEFT JOIN FETCH m.nguoiMuon WHERE m.nguoiMuon.id = :nguoiMuonId AND (m.trangThai = com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi$TrangThaiMuonTra.DANG_MUON OR m.trangThai = com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi$TrangThaiMuonTra.QUA_HAN) ORDER BY m.ngayMuon DESC")
    List<MuonTraThietBi> findMyCurrentBorrowings(@Param("nguoiMuonId") Long nguoiMuonId);

    List<MuonTraThietBi> findByThietBiId(Long thietBiId);

    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi tb LEFT JOIN FETCH tb.hinhAnhs LEFT JOIN FETCH m.nguoiMuon WHERE m.trangThai = :trangThai")
    List<MuonTraThietBi> findByTrangThai(@Param("trangThai") MuonTraThietBi.TrangThaiMuonTra trangThai);

    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi tb LEFT JOIN FETCH tb.hinhAnhs LEFT JOIN FETCH m.nguoiMuon WHERE m.trangThai = com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi$TrangThaiMuonTra.DANG_MUON AND m.ngayTraDuKien < :now")
    List<MuonTraThietBi> findOverdueRecords(@Param("now") LocalDateTime now);

    List<MuonTraThietBi> findByThietBiIdAndTrangThai(Long thietBiId, MuonTraThietBi.TrangThaiMuonTra trangThai);

    // Phiếu sắp đến hạn trả (trong vòng [now, deadline])
    @Query("SELECT m FROM MuonTraThietBi m LEFT JOIN FETCH m.thietBi LEFT JOIN FETCH m.nguoiMuon WHERE m.trangThai = com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi$TrangThaiMuonTra.DANG_MUON AND m.ngayTraDuKien BETWEEN :now AND :deadline")
    List<MuonTraThietBi> findDueSoonRecords(@Param("now") LocalDateTime now, @Param("deadline") LocalDateTime deadline);

    // Đếm tổng phiếu đang mượn
    long countByTrangThai(MuonTraThietBi.TrangThaiMuonTra trangThai);
}
