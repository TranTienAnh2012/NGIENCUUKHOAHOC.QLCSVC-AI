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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ThietBi> getAllAvailableThietBi() {
        // Use JOIN FETCH query to avoid N+1 problem and LazyInitializationException
        return thietBiRepository.findByTrangThaiWithDetails(ThietBi.TrangThaiThietBi.TOT);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ThietBi getThietBiById(Long id) {
        ThietBi thietBi = thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));
        // Force initialization of lazy relationships
        if (thietBi.getLoaiThietBi() != null) {
            thietBi.getLoaiThietBi().getTenLoai();
        }
        if (thietBi.getPhong() != null) {
            thietBi.getPhong().getTenPhong();
        }
        return thietBi;
    }
}
