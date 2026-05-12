package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.HinhAnhThietBi;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.HinhAnhThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GiaoVienThietBiService {

    private final ThietBiRepository thietBiRepository;
    private final HinhAnhThietBiRepository hinhAnhRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ThietBi> getAllAvailableThietBi() {
        List<ThietBi> thietBis = thietBiRepository.findByTrangThaiWithDetails(ThietBi.TrangThaiThietBi.TOT);

        // Nếu thiết bị chưa có hinhAnhChinh, lấy từ bảng hinh_anh_thiet_bi
        for (ThietBi tb : thietBis) {
            if (tb.getHinhAnhChinh() == null || tb.getHinhAnhChinh().isEmpty()) {
                List<HinhAnhThietBi> images = hinhAnhRepository.findByThietBiId(tb.getId());
                if (!images.isEmpty()) {
                    // Ưu tiên ảnh loại HINH_ANH_CHINH, fallback ảnh đầu tiên
                    String url = images.stream()
                            .filter(img -> img.getLoaiHinhAnh() == HinhAnhThietBi.LoaiHinhAnh.HINH_ANH_CHINH)
                            .map(HinhAnhThietBi::getUrlHinhAnh)
                            .findFirst()
                            .orElse(images.get(0).getUrlHinhAnh());
                    tb.setHinhAnhChinh(url);
                }
            }
        }
        return thietBis;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ThietBi getThietBiById(Long id) {
        ThietBi thietBi = thietBiRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", id));

        // Populate hinhAnhChinh from hinhAnhs list if not set
        if (thietBi.getHinhAnhChinh() == null || thietBi.getHinhAnhChinh().isEmpty()) {
            List<HinhAnhThietBi> images = thietBi.getHinhAnhs();
            if (images != null && !images.isEmpty()) {
                String url = images.stream()
                        .filter(img -> img.getLoaiHinhAnh() == HinhAnhThietBi.LoaiHinhAnh.HINH_ANH_CHINH)
                        .map(HinhAnhThietBi::getUrlHinhAnh)
                        .findFirst()
                        .orElse(images.get(0).getUrlHinhAnh());
                thietBi.setHinhAnhChinh(url);
            }
        }

        return thietBi;
    }
}
