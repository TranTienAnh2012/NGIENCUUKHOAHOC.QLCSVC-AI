package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.PhongHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPhongHocService {

    private final PhongHocRepository phongHocRepository;

    public Page<PhongHoc> getAllPhongHoc(Pageable pageable) {
        return phongHocRepository.findAll(pageable);
    }

    public List<PhongHoc> getAllPhongHoc() {
        return phongHocRepository.findAll();
    }

    public PhongHoc getPhongHocById(Long id) {
        return phongHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhongHoc", "id", id));
    }

    @Transactional
    public PhongHoc createPhongHoc(PhongHoc phongHoc) {
        if (phongHocRepository.findByMaPhong(phongHoc.getMaPhong()).isPresent()) {
            throw new RuntimeException("Mã phòng " + phongHoc.getMaPhong() + " đã tồn tại trong hệ thống.");
        }
        return phongHocRepository.save(phongHoc);
    }

    @Transactional
    public PhongHoc updatePhongHoc(Long id, PhongHoc phongHocDetails) {
        PhongHoc phongHoc = getPhongHocById(id);

        // Kiểm tra mã phòng mới nếu có thay đổi
        if (!phongHoc.getMaPhong().equals(phongHocDetails.getMaPhong())) {
            if (phongHocRepository.findByMaPhong(phongHocDetails.getMaPhong()).isPresent()) {
                throw new RuntimeException("Mã phòng " + phongHocDetails.getMaPhong() + " đã tồn tại.");
            }
            phongHoc.setMaPhong(phongHocDetails.getMaPhong());
        }

        phongHoc.setTenPhong(phongHocDetails.getTenPhong());
        phongHoc.setToaNha(phongHocDetails.getToaNha());
        phongHoc.setTang(phongHocDetails.getTang());
        phongHoc.setSucChua(phongHocDetails.getSucChua());
        phongHoc.setLoaiPhong(phongHocDetails.getLoaiPhong());
        return phongHocRepository.save(phongHoc);
    }

    @Transactional
    public void deletePhongHoc(Long id) {
        PhongHoc phongHoc = getPhongHocById(id);
        phongHocRepository.delete(phongHoc);
    }
}
