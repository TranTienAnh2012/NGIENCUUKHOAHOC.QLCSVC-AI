package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBaoTriService {

    private final BaoTriRepository baoTriRepository;

    public Page<BaoTri> getAllBaoTri(Pageable pageable) {
        return baoTriRepository.findAll(pageable);
    }

    public List<BaoTri> getAllBaoTri() {
        return baoTriRepository.findAll();
    }

    public BaoTri getBaoTriById(Long id) {
        return baoTriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", id));
    }

    public List<BaoTri> getBaoTriByThietBi(Long thietBiId) {
        return baoTriRepository.findByThietBiId(thietBiId);
    }

    public List<BaoTri> getBaoTriByDateRange(LocalDate start, LocalDate end) {
        return baoTriRepository.findByNgayBaoTriBetween(start, end);
    }

    @Transactional
    public BaoTri createBaoTri(BaoTri baoTri) {
        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public BaoTri updateBaoTri(Long id, BaoTri baoTriDetails) {
        BaoTri baoTri = getBaoTriById(id);

        baoTri.setThietBi(baoTriDetails.getThietBi());
        baoTri.setNgayBaoTri(baoTriDetails.getNgayBaoTri());
        baoTri.setNoiDung(baoTriDetails.getNoiDung());
        baoTri.setChiPhi(baoTriDetails.getChiPhi());
        baoTri.setKetQua(baoTriDetails.getKetQua());
        baoTri.setNguoiThucHien(baoTriDetails.getNguoiThucHien());
        baoTri.setLoaiBaoTri(baoTriDetails.getLoaiBaoTri());

        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public void deleteBaoTri(Long id) {
        if (!baoTriRepository.existsById(id)) {
            throw new ResourceNotFoundException("BaoTri", "id", id);
        }
        baoTriRepository.deleteById(id);
    }
}
