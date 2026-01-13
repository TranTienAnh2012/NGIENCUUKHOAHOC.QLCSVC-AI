package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.HinhAnhThietBi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhAnhThietBiRepository extends JpaRepository<HinhAnhThietBi, Long> {

    List<HinhAnhThietBi> findByThietBiId(Long thietBiId);

    List<HinhAnhThietBi> findByLoaiHinhAnh(HinhAnhThietBi.LoaiHinhAnh loaiHinhAnh);

    List<HinhAnhThietBi> findByNguoiChupId(Long nguoiChupId);
}
