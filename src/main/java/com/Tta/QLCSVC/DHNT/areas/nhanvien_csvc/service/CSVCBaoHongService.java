package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVCBaoHongService {

    private final BaoHongRepository baoHongRepository;

    @Transactional(readOnly = true)
    public List<BaoHong> getAllBaoHong() {
        List<BaoHong> list = baoHongRepository.findAll();
        // Force initialization of lazy relationships
        list.forEach(bh -> {
            if (bh.getThietBi() != null) {
                bh.getThietBi().getTenThietBi();
            }
            if (bh.getNguoiBao() != null) {
                bh.getNguoiBao().getHoTen();
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public BaoHong getBaoHongById(Long id) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        // Force initialization
        if (baoHong.getThietBi() != null) {
            baoHong.getThietBi().getTenThietBi();
        }
        if (baoHong.getNguoiBao() != null) {
            baoHong.getNguoiBao().getHoTen();
        }
        return baoHong;
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getPendingReports() {
        List<BaoHong> list = baoHongRepository.findPendingReports();
        // Force initialization of lazy relationships
        list.forEach(bh -> {
            if (bh.getThietBi() != null) {
                bh.getThietBi().getTenThietBi();
            }
            if (bh.getNguoiBao() != null) {
                bh.getNguoiBao().getHoTen();
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getUrgentReports() {
        List<BaoHong> list = baoHongRepository.findUrgentReports();
        // Force initialization of lazy relationships
        list.forEach(bh -> {
            if (bh.getThietBi() != null) {
                bh.getThietBi().getTenThietBi();
            }
            if (bh.getNguoiBao() != null) {
                bh.getNguoiBao().getHoTen();
            }
        });
        return list;
    }

    @Transactional
    public BaoHong updateStatus(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        baoHong.setTrangThai(trangThai);
        return baoHongRepository.save(baoHong);
    }
}
