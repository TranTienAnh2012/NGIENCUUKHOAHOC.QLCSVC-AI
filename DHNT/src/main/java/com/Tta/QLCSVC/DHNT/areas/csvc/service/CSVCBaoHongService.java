package com.Tta.QLCSVC.DHNT.areas.csvc.service;

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

    public List<BaoHong> getPendingReports() {
        return baoHongRepository.findPendingReports();
    }

    public List<BaoHong> getUrgentReports() {
        return baoHongRepository.findUrgentReports();
    }

    @Transactional
    public BaoHong updateStatus(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        baoHong.setTrangThai(trangThai);
        return baoHongRepository.save(baoHong);
    }
}
