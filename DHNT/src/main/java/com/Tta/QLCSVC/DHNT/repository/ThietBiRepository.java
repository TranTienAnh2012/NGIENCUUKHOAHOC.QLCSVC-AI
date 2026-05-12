package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThietBiRepository extends JpaRepository<ThietBi, Long> {
    Optional<ThietBi> findByMaThietBi(String maThietBi);

    List<ThietBi> findByTrangThai(ThietBi.TrangThaiThietBi trangThai);

    @Query("SELECT DISTINCT tb FROM ThietBi tb " +
            "LEFT JOIN FETCH tb.loaiThietBi " +
            "LEFT JOIN FETCH tb.phong " +
            "LEFT JOIN FETCH tb.hinhAnhs " +
            "WHERE tb.trangThai = :trangThai")
    List<ThietBi> findByTrangThaiWithDetails(@Param("trangThai") ThietBi.TrangThaiThietBi trangThai);

    @Query("SELECT tb FROM ThietBi tb " +
            "LEFT JOIN FETCH tb.loaiThietBi " +
            "LEFT JOIN FETCH tb.phong " +
            "LEFT JOIN FETCH tb.hinhAnhs " +
            "WHERE tb.id = :id")
    Optional<ThietBi> findByIdWithDetails(@Param("id") Long id);

    List<ThietBi> findByPhongId(Long phongId);

    List<ThietBi> findByLoaiThietBiId(Long loaiThietBiId);
}
