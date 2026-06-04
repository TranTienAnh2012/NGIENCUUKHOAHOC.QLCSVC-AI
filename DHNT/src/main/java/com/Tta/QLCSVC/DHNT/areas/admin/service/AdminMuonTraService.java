package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMuonTraService {

    private final MuonTraThietBiRepository muonTraRepository;

    private final AdminLinhKienService adminLinhKienService;

    @Transactional(readOnly = true)
    public Page<MuonTraThietBi> getAllMuonTra(Pageable pageable) {
        Page<MuonTraThietBi> page = muonTraRepository.findAll(pageable);
        // Force initialization of lazy relationships
        page.getContent().forEach(mt -> {
            if (mt.getThietBi() != null) {
                mt.getThietBi().getTenThietBi();
            }
            if (mt.getNguoiMuon() != null) {
                mt.getNguoiMuon().getHoTen();
            }
        });
        return page;
    }

    @Transactional(readOnly = true)
    public List<MuonTraThietBi> getAllMuonTra() {
        List<MuonTraThietBi> list = muonTraRepository.findAll();
        // Force initialization of lazy relationships
        list.forEach(mt -> {
            if (mt.getThietBi() != null) {
                mt.getThietBi().getTenThietBi();
            }
            if (mt.getNguoiMuon() != null) {
                mt.getNguoiMuon().getHoTen();
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public MuonTraThietBi getMuonTraById(Long id) {
        MuonTraThietBi muonTra = muonTraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MuonTraThietBi", "id", id));
        // Force initialization of lazy relationships
        if (muonTra.getThietBi() != null) {
            muonTra.getThietBi().getTenThietBi();
        }
        if (muonTra.getNguoiMuon() != null) {
            muonTra.getNguoiMuon().getHoTen();
        }
        return muonTra;
    }

    public List<MuonTraThietBi> getMuonTraByNguoiMuon(Long nguoiMuonId) {
        return muonTraRepository.findByNguoiMuonId(nguoiMuonId);
    }

    public List<MuonTraThietBi> getMuonTraByThietBi(Long thietBiId) {
        return muonTraRepository.findByThietBiId(thietBiId);
    }

    public List<MuonTraThietBi> getMuonTraByTrangThai(MuonTraThietBi.TrangThaiMuonTra trangThai) {
        return muonTraRepository.findByTrangThai(trangThai);
    }

    @Transactional
    public MuonTraThietBi createMuonTra(MuonTraThietBi muonTra) {
        if (muonTra.getTrangThai() == null) {
            muonTra.setTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);
        }
        
        // Validation: Không cho phép mượn thiết bị hỏng, bảo trì hoặc đang bảo hành
        if (muonTra.getThietBi() != null) {
            com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi trangThaiTB = muonTra.getThietBi().getTrangThai();
            if (trangThaiTB == com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.HONG ||
                trangThaiTB == com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.BAO_TRI ||
                trangThaiTB == com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.DANG_BAO_HANH) {
                throw new com.Tta.QLCSVC.DHNT.exception.InvalidOperationException(
                        "Không thể mượn thiết bị đang hỏng, bảo trì hoặc đang gửi bảo hành (Status: " + trangThaiTB + ")");
            }
        }

        // Kiểm tra nếu tạo phiếu DANG_MUON thì không được có conflict
        if (muonTra.getTrangThai() == MuonTraThietBi.TrangThaiMuonTra.DANG_MUON
                && muonTra.getThietBi() != null) {
            Long thietBiId = muonTra.getThietBi().getId();
            var existing = muonTraRepository.findByThietBiIdAndTrangThai(
                    thietBiId, MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);
            if (!existing.isEmpty()) {
                throw new com.Tta.QLCSVC.DHNT.exception.InvalidOperationException(
                        "Thiết bị đang được mượn bởi người khác, không thể tạo phiếu mới");
            }
        }
        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi updateMuonTra(Long id, MuonTraThietBi muonTraDetails) {
        MuonTraThietBi muonTra = getMuonTraById(id);
        boolean wasReturned = muonTra.getTrangThai() == MuonTraThietBi.TrangThaiMuonTra.DA_TRA;

        // Chỉ update field nào được gửi lên (non-null), giữ nguyên các field còn lại
        if (muonTraDetails.getThietBi() != null) {
            muonTra.setThietBi(muonTraDetails.getThietBi());
        }
        if (muonTraDetails.getNguoiMuon() != null) {
            muonTra.setNguoiMuon(muonTraDetails.getNguoiMuon());
        }
        if (muonTraDetails.getNgayMuon() != null) {
            muonTra.setNgayMuon(muonTraDetails.getNgayMuon());
        }
        if (muonTraDetails.getNgayTraDuKien() != null) {
            muonTra.setNgayTraDuKien(muonTraDetails.getNgayTraDuKien());
        }
        muonTra.setNgayTraThucTe(muonTraDetails.getNgayTraThucTe());
        
        if (muonTraDetails.getTrangThai() != null) {
            muonTra.setTrangThai(muonTraDetails.getTrangThai());
        }
        muonTra.setGhiChu(muonTraDetails.getGhiChu());

        // Kiểm tra logic tự động tính số giờ nếu trạng thái chuyển sang DA_TRA
        if (!wasReturned && muonTra.getTrangThai() == MuonTraThietBi.TrangThaiMuonTra.DA_TRA) {
            if (muonTra.getNgayTraThucTe() == null) {
                muonTra.setNgayTraThucTe(LocalDateTime.now());
            }
            autoCalculateUsageHours(muonTra);
        }

        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi updateTrangThai(Long id, MuonTraThietBi.TrangThaiMuonTra trangThai) {
        MuonTraThietBi muonTra = getMuonTraById(id);
        boolean wasReturned = muonTra.getTrangThai() == MuonTraThietBi.TrangThaiMuonTra.DA_TRA;
        
        muonTra.setTrangThai(trangThai);
        
        if (!wasReturned && trangThai == MuonTraThietBi.TrangThaiMuonTra.DA_TRA) {
            muonTra.setNgayTraThucTe(LocalDateTime.now());
            autoCalculateUsageHours(muonTra);
        }
        
        return muonTraRepository.save(muonTra);
    }

    /**
     * Tự động tính số giờ sử dụng thiết bị và cập nhật vào linh kiện có đơn vị "Giờ"
     */
    private void autoCalculateUsageHours(MuonTraThietBi muonTra) {
        if (muonTra.getNgayMuon() != null && muonTra.getNgayTraThucTe() != null && muonTra.getThietBi() != null) {
            long minutesUsed = java.time.Duration.between(muonTra.getNgayMuon(), muonTra.getNgayTraThucTe()).toMinutes();
            
            // Tính số giờ sử dụng: làm tròn lên. Nếu đã có phát sinh mượn-trả (dù 1 phút) thì tính bét nhất là 1 giờ (do hao mòn bật/tắt máy)
            int hoursUsed = (int) Math.ceil(minutesUsed / 60.0);
            if (hoursUsed == 0 && minutesUsed >= 0) { // Kể cả vừa mượn xong trả liền (0 phút)
                hoursUsed = 1;
            }
            
            if (hoursUsed > 0) {
                List<com.Tta.QLCSVC.DHNT.entity.LinhKien> linhKiens = adminLinhKienService.getLinhKienByThietBi(muonTra.getThietBi().getId());
                for (com.Tta.QLCSVC.DHNT.entity.LinhKien lk : linhKiens) {
                    if ("Giờ".equalsIgnoreCase(lk.getDonViTinh())) {
                        adminLinhKienService.capNhatSuDung(lk.getId(), hoursUsed);
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteMuonTra(Long id) {
        if (!muonTraRepository.existsById(id)) {
            throw new ResourceNotFoundException("MuonTraThietBi", "id", id);
        }
        muonTraRepository.deleteById(id);
    }
}
