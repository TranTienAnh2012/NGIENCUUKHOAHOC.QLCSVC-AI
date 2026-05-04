package com.Tta.QLCSVC.DHNT.areas.admin.service;


import com.Tta.QLCSVC.DHNT.entity.LoaiThietBi;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.LoaiThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.PhongHocRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminThietBiService {

    private final ThietBiRepository thietBiRepository;
    private final LoaiThietBiRepository loaiThietBiRepository;
    private final PhongHocRepository phongHocRepository;


    @Transactional(readOnly = true)
    public Page<ThietBi> getAllThietBi(Pageable pageable) {
        Page<ThietBi> page = thietBiRepository.findAll(pageable);
        // Force initialization of lazy relationships + populate hinhAnhChinh
        page.getContent().forEach(tb -> {
            if (tb.getLoaiThietBi() != null) {
                tb.getLoaiThietBi().getTenLoai();
            }
            if (tb.getPhong() != null) {
                tb.getPhong().getTenPhong();
            }

        });
        return page;
    }

    @Transactional(readOnly = true)
    public ThietBi getThietBiById(Long id) {
        ThietBi thietBi = thietBiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));
        if (thietBi.getLoaiThietBi() != null) {
            thietBi.getLoaiThietBi().getTenLoai();
        }
        if (thietBi.getPhong() != null) {
            thietBi.getPhong().getTenPhong();
        }

        return thietBi;
    }

    public List<ThietBi> getThietBiByTrangThai(ThietBi.TrangThaiThietBi trangThai) {
        return thietBiRepository.findByTrangThai(trangThai);
    }

    @Transactional
    public ThietBi createThietBi(ThietBi thietBi) {
        if (thietBi.getLoaiThietBi() != null && thietBi.getLoaiThietBi().getId() != null) {
            LoaiThietBi loaiThietBi = loaiThietBiRepository.findById(thietBi.getLoaiThietBi().getId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("LoaiThietBi", "id", thietBi.getLoaiThietBi().getId()));
            thietBi.setLoaiThietBi(loaiThietBi);
        }

        if (thietBi.getPhong() != null && thietBi.getPhong().getId() != null) {
            PhongHoc phong = phongHocRepository.findById(thietBi.getPhong().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("PhongHoc", "id", thietBi.getPhong().getId()));
            thietBi.setPhong(phong);
        }

        thietBi.setMaThietBi("TB" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.TOT);

        return thietBiRepository.save(thietBi);
    }

    @Transactional
    public ThietBi updateThietBi(Long id, ThietBi thietBiDetails) {
        ThietBi thietBi = getThietBiById(id);

        thietBi.setTenThietBi(thietBiDetails.getTenThietBi());
        thietBi.setHangSanXuat(thietBiDetails.getHangSanXuat());
        thietBi.setModel(thietBiDetails.getModel());
        thietBi.setNamSanXuat(thietBiDetails.getNamSanXuat());
        thietBi.setNgayMua(thietBiDetails.getNgayMua());
        thietBi.setGiaMua(thietBiDetails.getGiaMua());
        thietBi.setGhiChu(thietBiDetails.getGhiChu());
        thietBi.setTrangThai(thietBiDetails.getTrangThai());

        // Update LoaiThietBi relationship
        if (thietBiDetails.getLoaiThietBi() != null && thietBiDetails.getLoaiThietBi().getId() != null) {
            LoaiThietBi loaiThietBi = loaiThietBiRepository.findById(thietBiDetails.getLoaiThietBi().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("LoaiThietBi", "id",
                            thietBiDetails.getLoaiThietBi().getId()));
            thietBi.setLoaiThietBi(loaiThietBi);
        }

        // Update Phong relationship
        if (thietBiDetails.getPhong() != null && thietBiDetails.getPhong().getId() != null) {
            PhongHoc phong = phongHocRepository.findById(thietBiDetails.getPhong().getId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("PhongHoc", "id", thietBiDetails.getPhong().getId()));
            thietBi.setPhong(phong);
        } else {
            // Allow setting phong to null if not provided
            thietBi.setPhong(null);
        }

        return thietBiRepository.save(thietBi);
    }

    @Transactional
    public void deleteThietBi(Long id) {
        if (!thietBiRepository.existsById(id)) {
            throw new ResourceNotFoundException("ThietBi", "id", id);
        }
        thietBiRepository.deleteById(id);
    }

}
