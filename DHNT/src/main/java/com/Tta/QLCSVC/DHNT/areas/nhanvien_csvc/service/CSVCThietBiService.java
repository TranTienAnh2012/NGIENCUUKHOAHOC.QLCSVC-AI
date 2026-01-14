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
}
