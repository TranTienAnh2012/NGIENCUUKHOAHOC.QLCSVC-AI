package com.Tta.QLCSVC.DHNT.repository;

import com.Tta.QLCSVC.DHNT.entity.IotDuLieuCamBien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IotDuLieuCamBienRepository extends JpaRepository<IotDuLieuCamBien, Long> {

    List<IotDuLieuCamBien> findByPhongId(Long phongId);

    List<IotDuLieuCamBien> findByPhongIdAndTimestampBetween(Long phongId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT i FROM IotDuLieuCamBien i WHERE i.phong.id = :phongId ORDER BY i.timestamp DESC LIMIT 1")
    IotDuLieuCamBien findLatestByPhong(Long phongId);
}
