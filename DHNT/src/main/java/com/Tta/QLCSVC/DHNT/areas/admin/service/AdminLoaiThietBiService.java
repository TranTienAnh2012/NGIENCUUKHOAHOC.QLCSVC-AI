package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.LoaiThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.LoaiThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLoaiThietBiService {

    private final LoaiThietBiRepository loaiThietBiRepository;

    public Page<LoaiThietBi> getAllLoaiThietBi(Pageable pageable) {
        return loaiThietBiRepository.findAll(pageable);
    }

    public List<LoaiThietBi> getAllLoaiThietBi() {
        return loaiThietBiRepository.findAll();
    }

    public LoaiThietBi getLoaiThietBiById(Long id) {
        return loaiThietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoaiThietBi", "id", id));
    }

    @Transactional
    public LoaiThietBi createLoaiThietBi(LoaiThietBi loaiThietBi) {
        return loaiThietBiRepository.save(loaiThietBi);
    }

    @Transactional
    public LoaiThietBi updateLoaiThietBi(Long id, LoaiThietBi loaiThietBiDetails) {
        LoaiThietBi loaiThietBi = getLoaiThietBiById(id);

        loaiThietBi.setTenLoai(loaiThietBiDetails.getTenLoai());
        loaiThietBi.setMoTa(loaiThietBiDetails.getMoTa());
        loaiThietBi.setThoiGianBaoHanhMacDinh(loaiThietBiDetails.getThoiGianBaoHanhMacDinh());

        return loaiThietBiRepository.save(loaiThietBi);
    }

    @Transactional
    public void deleteLoaiThietBi(Long id) {
        if (!loaiThietBiRepository.existsById(id)) {
            throw new ResourceNotFoundException("LoaiThietBi", "id", id);
        }
        loaiThietBiRepository.deleteById(id);
    }
}
