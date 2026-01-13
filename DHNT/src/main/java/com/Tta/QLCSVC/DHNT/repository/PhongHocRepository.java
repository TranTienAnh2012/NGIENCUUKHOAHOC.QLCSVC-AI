package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhongHocRepository extends JpaRepository<PhongHoc, Long> {
    Optional<PhongHoc> findByMaPhong(String maPhong);
}
