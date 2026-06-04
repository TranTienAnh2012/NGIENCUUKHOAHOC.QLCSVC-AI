package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BaoTriRepository extends JpaRepository<BaoTri, Long> {

    List<BaoTri> findByThietBiId(Long thietBiId);

    List<BaoTri> findTop5ByOrderByNgayBaoTriDesc();

    List<BaoTri> findByLoaiBaoTri(BaoTri.LoaiBaoTri loaiBaoTri);

    List<BaoTri> findByNgayBaoTriBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT b FROM BaoTri b WHERE b.thietBi.id = :thietBiId ORDER BY b.ngayBaoTri DESC")
    List<BaoTri> findMaintenanceHistoryByThietBi(Long thietBiId);

    @Query("SELECT SUM(b.chiPhi) FROM BaoTri b WHERE b.thietBi.id = :thietBiId")
    Double getTotalMaintenanceCostByThietBi(Long thietBiId);
}
