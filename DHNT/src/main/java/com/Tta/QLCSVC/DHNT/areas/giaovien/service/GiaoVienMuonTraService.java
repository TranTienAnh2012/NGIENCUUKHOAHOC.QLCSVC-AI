package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.MuonTraThietBi;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.InvalidOperationException;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.HinhAnhThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import com.Tta.QLCSVC.DHNT.entity.HinhAnhThietBi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GiaoVienMuonTraService {

    private final MuonTraThietBiRepository muonTraRepository;
    private final ThietBiRepository thietBiRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final HinhAnhThietBiRepository hinhAnhRepository;

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
        populateThietBiImages(list);
        return list;
    }

    @Transactional(readOnly = true)
    public List<MuonTraThietBi> getMyActiveBorrowings() {
        NguoiDung currentUser = getCurrentUser();
        List<MuonTraThietBi> active = muonTraRepository.findMyCurrentBorrowings(currentUser.getId());
        // Force init lazy thietBi
        active.forEach(mt -> {
            if (mt.getThietBi() != null)
                mt.getThietBi().getTenThietBi();
        });
        populateThietBiImages(active);
        return active.stream().limit(3).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getMyActiveBorrowingsCount() {
        NguoiDung currentUser = getCurrentUser();
        return muonTraRepository.findMyCurrentBorrowings(currentUser.getId()).size();
    }

    @Transactional(readOnly = true)
    public long getMyOverdueBorrowingsCount() {
        NguoiDung currentUser = getCurrentUser();
        List<MuonTraThietBi> active = muonTraRepository.findMyCurrentBorrowings(currentUser.getId());
        LocalDateTime now = LocalDateTime.now();
        return active.stream()
                .filter(mt -> mt.getNgayTraDuKien() != null && mt.getNgayTraDuKien().isBefore(now))
                .count();
    }

    @Transactional(readOnly = true)
    public long getMyTotalBorrowingsCount() {
        NguoiDung currentUser = getCurrentUser();
        return muonTraRepository.findByNguoiMuonId(currentUser.getId()).size();
    }

    @Transactional(readOnly = true)
    public long getMyCompletedBorrowingsCount() {
        NguoiDung currentUser = getCurrentUser();
        List<MuonTraThietBi> all = muonTraRepository.findByNguoiMuonId(currentUser.getId());
        return all.stream()
                .filter(mt -> mt.getTrangThai() == MuonTraThietBi.TrangThaiMuonTra.DA_TRA)
                .count();
    }

    @Transactional(readOnly = true)
    public List<MuonTraThietBi> getMyRecentBorrowings(int limit) {
        NguoiDung currentUser = getCurrentUser();
        List<MuonTraThietBi> all = muonTraRepository.findByNguoiMuonId(currentUser.getId());
        all.forEach(mt -> {
            if (mt.getThietBi() != null)
                mt.getThietBi().getTenThietBi();
        });
        populateThietBiImages(all);
        return all.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : a.getNgayMuon();
                    LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : b.getNgayMuon();
                    return bTime.compareTo(aTime);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void populateThietBiImages(List<MuonTraThietBi> list) {
        for (MuonTraThietBi mt : list) {
            ThietBi tb = mt.getThietBi();
            if (tb != null && (tb.getHinhAnhChinh() == null || tb.getHinhAnhChinh().isEmpty())) {
                List<HinhAnhThietBi> images = hinhAnhRepository.findByThietBiId(tb.getId());
                if (!images.isEmpty()) {
                    String url = images.stream()
                            .filter(img -> img.getLoaiHinhAnh() == HinhAnhThietBi.LoaiHinhAnh.HINH_ANH_CHINH)
                            .map(HinhAnhThietBi::getUrlHinhAnh)
                            .findFirst()
                            .orElse(images.get(0).getUrlHinhAnh());
                    tb.setHinhAnhChinh(url);
                }
            }
        }
    }

    @Transactional
    public MuonTraThietBi borrowEquipment(Long thietBiId, LocalDateTime ngayTraDuKien, String ghiChu) {
        NguoiDung currentUser = getCurrentUser();

        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));

        if (thietBi.getTrangThai() != ThietBi.TrangThaiThietBi.TOT) {
            throw new InvalidOperationException("Thiết bị không khả dụng để mượn");
        }

        List<MuonTraThietBi> activeBorrowings = muonTraRepository.findByThietBiIdAndTrangThai(
                thietBiId, MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);
        if (!activeBorrowings.isEmpty()) {
            throw new InvalidOperationException("Thiết bị đang được mượn");
        }

        MuonTraThietBi muonTra = new MuonTraThietBi();
        muonTra.setThietBi(thietBi);
        muonTra.setNguoiMuon(currentUser);
        muonTra.setNgayMuon(LocalDateTime.now());
        muonTra.setNgayTraDuKien(ngayTraDuKien);
        muonTra.setGhiChu(ghiChu);
        muonTra.setTrangThai(MuonTraThietBi.TrangThaiMuonTra.DANG_MUON);

        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi returnEquipment(Long muonTraId) {
        MuonTraThietBi muonTra = muonTraRepository.findById(muonTraId)
                .orElseThrow(() -> new ResourceNotFoundException("MuonTraThietBi", "id", muonTraId));

        if (muonTra.getTrangThai() != MuonTraThietBi.TrangThaiMuonTra.DANG_MUON && 
            muonTra.getTrangThai() != MuonTraThietBi.TrangThaiMuonTra.QUA_HAN) {
            throw new InvalidOperationException("Thiết bị không ở trạng thái có thể trả");
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
