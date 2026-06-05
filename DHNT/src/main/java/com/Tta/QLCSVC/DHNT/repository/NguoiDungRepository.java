package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<NguoiDung> findByVaiTroIn(java.util.List<NguoiDung.VaiTro> vaiTros);

    /** Lấy danh sách nhân viên CSVC — dùng cho dropdown phân công */
    java.util.List<NguoiDung> findByVaiTro(NguoiDung.VaiTro vaiTro);
}
