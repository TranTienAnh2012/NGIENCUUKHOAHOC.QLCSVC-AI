package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.InvalidOperationException;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GiaoVienMuonTraService {

    private final MuonTraThietBiRepository muonTraRepository;
    private final ThietBiRepository thietBiRepository;
    private final NguoiDungRepository nguoiDungRepository;

    @Transactional(readOnly = true)
    public List<MuonTraThietBi> getMyBorrowings() {
        NguoiDung currentUser = getCurrentUser();
        List<MuonTraThietBi> list = muonTraRepository.findByNguoiMuonId(currentUser.getId());
        // Force initialization of lazy relationships
        list.forEach(mt -> {
            if (mt.getThietBi() != null) {
                mt.getThietBi().getTenThietBi();
            }
        });
        return list;
    }

    @Transactional
    public MuonTraThietBi borrowEquipment(Long thietBiId, LocalDateTime ngayTraDuKien) {
        NguoiDung currentUser = getCurrentUser();

        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));

        if (thietBi.getTrangThai() != ThietBi.TrangThaiThietBi.TOT) {
            throw new InvalidOperationException("Thiết bị không khả dụng để mượn");
        }

        List<MuonTraThietBi> activeBorrowings = muonTraRepository.findActiveBorrowingsByThietBi(thietBiId);
        if (!activeBorrowings.isEmpty()) {
            throw new InvalidOperationException("Thiết bị đang được mượn");
        }

        MuonTraThietBi muonTra = new MuonTraThietBi();
        muonTra.setThietBi(thietBi);
        muonTra.setNguoiMuon(currentUser);
        muonTra.setNgayMuon(LocalDateTime.now());
        muonTra.setNgayTraDuKien(ngayTraDuKien);
        muonTra.setTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);

        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi returnEquipment(Long muonTraId) {
        MuonTraThietBi muonTra = muonTraRepository.findById(muonTraId)
                .orElseThrow(() -> new ResourceNotFoundException("MuonTraThietBi", "id", muonTraId));

        if (muonTra.getTrangThai() != MuonTraThietBi.TrangThaiMuonTra.DANG_MUON) {
            throw new InvalidOperationException("Thiết bị đã được trả");
        }

        muonTra.setNgayTraThucTe(LocalDateTime.now());
        muonTra.setTrangThai(MuonTraThietBi.TrangThaiMuonTra.DA_TRA);

        return muonTraRepository.save(muonTra);
    }

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return nguoiDungRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", auth.getName()));
    }
}
