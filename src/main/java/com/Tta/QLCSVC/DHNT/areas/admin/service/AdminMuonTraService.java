package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import com.Tta.QLCSVC.DHNT.service.NotificationService;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
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
    private final NotificationService notificationService;

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
        // ngayTraThucTe có thể set null (khi chưa trả) hoặc có giá trị
        muonTra.setNgayTraThucTe(muonTraDetails.getNgayTraThucTe());
        if (muonTraDetails.getTrangThai() != null) {
            muonTra.setTrangThai(muonTraDetails.getTrangThai());
        }
        // ghiChu: cho phép set rỗng
        muonTra.setGhiChu(muonTraDetails.getGhiChu());
        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi updateTrangThai(Long id, MuonTraThietBi.TrangThaiMuonTra trangThai) {
        MuonTraThietBi muonTra = getMuonTraById(id);
        MuonTraThietBi.TrangThaiMuonTra oldStatus = muonTra.getTrangThai();
        
        muonTra.setTrangThai(trangThai);
        if (trangThai == MuonTraThietBi.TrangThaiMuonTra.DA_TRA) {
            muonTra.setNgayTraThucTe(LocalDateTime.now());
        }
        
        MuonTraThietBi saved = muonTraRepository.save(muonTra);

        // Gửi thông báo nếu Admin duyệt hoặc từ chối
        if (oldStatus == MuonTraThietBi.TrangThaiMuonTra.CHO_DUYET && trangThai == MuonTraThietBi.TrangThaiMuonTra.DANG_MUON) {
            notificationService.sendToUser(
                    muonTra.getNguoiMuon().getId(),
                    "Đơn mượn thiết bị đã được duyệt",
                    "Yêu cầu mượn thiết bị " + muonTra.getThietBi().getTenThietBi() + " của bạn đã được duyệt. Bạn có thể đến nhận thiết bị.",
                    ThongBao.LoaiThongBao.MUON_TRA,
                    "/giaovien/muon-tra"
            );
        } else if (oldStatus == MuonTraThietBi.TrangThaiMuonTra.CHO_DUYET && trangThai == MuonTraThietBi.TrangThaiMuonTra.TU_CHOI) {
            notificationService.sendToUser(
                    muonTra.getNguoiMuon().getId(),
                    "Đơn mượn thiết bị bị từ chối",
                    "Yêu cầu mượn thiết bị " + muonTra.getThietBi().getTenThietBi() + " của bạn đã bị từ chối.",
                    ThongBao.LoaiThongBao.MUON_TRA,
                    "/giaovien/muon-tra"
            );
        }

        return saved;
    }

    @Transactional
    public void deleteMuonTra(Long id) {
        if (!muonTraRepository.existsById(id)) {
            throw new ResourceNotFoundException("MuonTraThietBi", "id", id);
        }
        muonTraRepository.deleteById(id);
    }
}
