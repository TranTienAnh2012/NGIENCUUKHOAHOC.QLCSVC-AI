package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThietBiRepository extends JpaRepository<ThietBi, Long> {
    Optional<ThietBi> findByMaThietBi(String maThietBi);

    List<ThietBi> findByTrangThai(ThietBi.TrangThaiThietBi trangThai);

    List<ThietBi> findByPhongId(Long phongId);

    List<ThietBi> findByLoaiThietBiId(Long loaiThietBiId);
}
