package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GiaoVienThietBiService {

    private final ThietBiRepository thietBiRepository;

    public List<ThietBi> getAllAvailableThietBi() {
        return thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.TOT);
    }

    public ThietBi getThietBiById(Long id) {
        return thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));
    }
}
