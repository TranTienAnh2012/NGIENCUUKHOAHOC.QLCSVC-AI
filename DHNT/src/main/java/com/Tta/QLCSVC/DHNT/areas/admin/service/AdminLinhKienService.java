package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.LinhKien;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.LinhKienRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLinhKienService {

    private final LinhKienRepository linhKienRepository;
    private final ThietBiRepository thietBiRepository;

    @Transactional(readOnly = true)
    public List<LinhKien> getAllLinhKien() {
        List<LinhKien> list = linhKienRepository.findAll();
        list.forEach(lk -> {
            if (lk.getThietBi() != null) {
                lk.getThietBi().getTenThietBi();
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public LinhKien getLinhKienById(Long id) {
        LinhKien linhKien = linhKienRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LinhKien", "id", id));
        if (linhKien.getThietBi() != null) {
            linhKien.getThietBi().getTenThietBi();
        }
        return linhKien;
    }

    @Transactional(readOnly = true)
    public List<LinhKien> getLinhKienByThietBi(Long thietBiId) {
        List<LinhKien> list = linhKienRepository.findByThietBiId(thietBiId);
        list.forEach(lk -> {
            if (lk.getThietBi() != null) {
                lk.getThietBi().getTenThietBi();
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public List<LinhKien> getLinhKienByTrangThai(LinhKien.TrangThaiLinhKien trangThai) {
        return linhKienRepository.findByTrangThai(trangThai);
    }

    @Transactional
    public LinhKien createLinhKien(LinhKien linhKien, Long thietBiId) {
        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));
        linhKien.setThietBi(thietBi);

        if (linhKien.getTrangThai() == null) {
            linhKien.setTrangThai(LinhKien.TrangThaiLinhKien.HOAT_DONG);
        }
        if (linhKien.getThoiGianDaSuDung() == null) {
            linhKien.setThoiGianDaSuDung(0);
        }

        return linhKienRepository.save(linhKien);
    }

    @Transactional
    public LinhKien updateLinhKien(Long id, LinhKien details) {
        LinhKien linhKien = getLinhKienById(id);

        if (details.getTenLinhKien() != null) {
            linhKien.setTenLinhKien(details.getTenLinhKien());
        }
        if (details.getThongSoKyThuat() != null) {
            linhKien.setThongSoKyThuat(details.getThongSoKyThuat());
        }
        if (details.getNgayMua() != null) {
            linhKien.setNgayMua(details.getNgayMua());
        }
        if (details.getHanBaoHanh() != null) {
            linhKien.setHanBaoHanh(details.getHanBaoHanh());
        }
        if (details.getTuoiThoToiDa() != null) {
            linhKien.setTuoiThoToiDa(details.getTuoiThoToiDa());
        }
        if (details.getThoiGianDaSuDung() != null) {
            linhKien.setThoiGianDaSuDung(details.getThoiGianDaSuDung());
        }
        if (details.getDonViTinh() != null) {
            linhKien.setDonViTinh(details.getDonViTinh());
        }
        if (details.getTrangThai() != null) {
            linhKien.setTrangThai(details.getTrangThai());
        }

        // Tự động đánh dấu CAN_THAY_THE nếu sử dụng >= 90% tuổi thọ
        autoCheckTrangThai(linhKien);

        return linhKienRepository.save(linhKien);
    }

    @Transactional
    public LinhKien capNhatSuDung(Long id, Integer soGioThem) {
        LinhKien linhKien = getLinhKienById(id);
        int tongMoi = (linhKien.getThoiGianDaSuDung() != null ? linhKien.getThoiGianDaSuDung() : 0) + soGioThem;
        linhKien.setThoiGianDaSuDung(tongMoi);

        autoCheckTrangThai(linhKien);

        return linhKienRepository.save(linhKien);
    }

    @Transactional
    public void deleteLinhKien(Long id) {
        if (!linhKienRepository.existsById(id)) {
            throw new ResourceNotFoundException("LinhKien", "id", id);
        }
        linhKienRepository.deleteById(id);
    }

    /**
     * Tự động cập nhật trạng thái linh kiện dựa trên tuổi thọ.
     * Nếu đã sử dụng >= 90% tuổi thọ tối đa → CAN_THAY_THE.
     * Nếu đã sử dụng >= 100% → DA_HU_HONG (trừ khi đang bảo hành).
     * Complexity: O(1) per call.
     */
    private void autoCheckTrangThai(LinhKien linhKien) {
        if (linhKien.getTuoiThoToiDa() == null || linhKien.getTuoiThoToiDa() <= 0) {
            return;
        }
        if (linhKien.getTrangThai() == LinhKien.TrangThaiLinhKien.DANG_BAO_HANH) {
            return; // Không override trạng thái bảo hành
        }

        int used = linhKien.getThoiGianDaSuDung() != null ? linhKien.getThoiGianDaSuDung() : 0;
        int max = linhKien.getTuoiThoToiDa();
        double ratio = (double) used / max;

        if (ratio >= 1.0) {
            linhKien.setTrangThai(LinhKien.TrangThaiLinhKien.DA_HU_HONG);
        } else if (ratio >= 0.9) {
            linhKien.setTrangThai(LinhKien.TrangThaiLinhKien.CAN_THAY_THE);
        }
    }
}
