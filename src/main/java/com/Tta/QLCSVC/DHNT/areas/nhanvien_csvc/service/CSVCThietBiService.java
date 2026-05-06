package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVCThietBiService {

    private final ThietBiRepository thietBiRepository;

    public List<ThietBi> getThietBiByTrangThai(ThietBi.TrangThaiThietBi trangThai) {
        return thietBiRepository.findByTrangThai(trangThai);
    }

    public List<ThietBi> getDamagedThietBi() {
        return thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.HONG);
    }

    public List<ThietBi> getMaintenanceThietBi() {
        return thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
    }

    @Transactional
    public ThietBi updateTrangThai(Long id, ThietBi.TrangThaiThietBi trangThai) {
        ThietBi thietBi = thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));
        thietBi.setTrangThai(trangThai);
        return thietBiRepository.save(thietBi);
    }

    @Transactional(readOnly = true)
    public List<ThietBi> getAllThietBiWithDetails() {
        List<ThietBi> list = thietBiRepository.findAll();
        // Force initialization of lazy relationships
        list.forEach(tb -> {
            if (tb.getLoaiThietBi() != null) {
                tb.getLoaiThietBi().getTenLoai();
            }
            if (tb.getPhong() != null) {
                tb.getPhong().getTenPhong();
            }
            tb.getBaoHongs().size();
        });
        return list;
    }

    @Transactional(readOnly = true)
    public ThietBi getThietBiById(Long id) {
        ThietBi tb = thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));
        if (tb.getLoaiThietBi() != null) {
            tb.getLoaiThietBi().getTenLoai();
        }
        if (tb.getPhong() != null) {
            tb.getPhong().getTenPhong();
        }
        tb.getBaoHongs().size();
        tb.getBaoTris().size();
        return tb;
    }
}
