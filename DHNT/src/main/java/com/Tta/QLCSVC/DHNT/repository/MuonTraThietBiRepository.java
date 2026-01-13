package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MuonTraThietBiRepository extends JpaRepository<MuonTraThietBi, Long> {

    List<MuonTraThietBi> findByNguoiMuonId(Long nguoiMuonId);

    List<MuonTraThietBi> findByThietBiId(Long thietBiId);

    List<MuonTraThietBi> findByTrangThai(MuonTraThietBi.TrangThaiMuonTra trangThai);

    @Query("SELECT m FROM MuonTraThietBi m WHERE m.trangThai = 'DANG_MUON' AND m.ngayTraDuKien < :now")
    List<MuonTraThietBi> findOverdueRecords(LocalDateTime now);

    @Query("SELECT m FROM MuonTraThietBi m WHERE m.thietBi.id = :thietBiId AND m.trangThai = 'DANG_MUON'")
    List<MuonTraThietBi> findActiveBorrowingsByThietBi(Long thietBiId);
}
