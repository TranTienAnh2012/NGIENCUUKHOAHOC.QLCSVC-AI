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
        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi updateMuonTra(Long id, MuonTraThietBi muonTraDetails) {
        MuonTraThietBi muonTra = getMuonTraById(id);
        muonTra.setThietBi(muonTraDetails.getThietBi());
        muonTra.setNguoiMuon(muonTraDetails.getNguoiMuon());
        muonTra.setNgayMuon(muonTraDetails.getNgayMuon());
        muonTra.setNgayTraDuKien(muonTraDetails.getNgayTraDuKien());
        muonTra.setNgayTraThucTe(muonTraDetails.getNgayTraThucTe());
        muonTra.setTrangThai(muonTraDetails.getTrangThai());
        muonTra.setGhiChu(muonTraDetails.getGhiChu());
        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public MuonTraThietBi updateTrangThai(Long id, MuonTraThietBi.TrangThaiMuonTra trangThai) {
        MuonTraThietBi muonTra = getMuonTraById(id);
        muonTra.setTrangThai(trangThai);
        if (trangThai == MuonTraThietBi.TrangThaiMuonTra.DA_TRA) {
            muonTra.setNgayTraThucTe(LocalDateTime.now());
        }
        return muonTraRepository.save(muonTra);
    }

    @Transactional
    public void deleteMuonTra(Long id) {
        if (!muonTraRepository.existsById(id)) {
            throw new ResourceNotFoundException("MuonTraThietBi", "id", id);
        }
        muonTraRepository.deleteById(id);
    }
}
