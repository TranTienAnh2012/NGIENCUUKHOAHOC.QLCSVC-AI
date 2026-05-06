package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import com.Tta.QLCSVC.DHNT.repository.PhongHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVCPhongHocService {

    private final PhongHocRepository phongHocRepository;

    @Transactional(readOnly = true)
    public List<PhongHoc> getAllPhongHocWithThietBiStats() {
        List<PhongHoc> phongHocs = phongHocRepository.findAll();
        // Initialize relationships for the view
        phongHocs.forEach(ph -> {
            if (ph.getLoaiPhong() != null) {
                ph.getLoaiPhong().getTenLoai();
            }
            // Fetch devices to calculate stats in the view later
            ph.getThietBis().size();
        });
        return phongHocs;
    }
}
