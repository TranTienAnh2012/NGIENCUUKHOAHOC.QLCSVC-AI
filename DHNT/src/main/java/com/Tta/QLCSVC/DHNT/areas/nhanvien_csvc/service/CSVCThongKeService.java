package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CSVCThongKeService {

    private final ThietBiRepository thietBiRepository;
    private final BaoHongRepository baoHongRepository;
    private final BaoTriRepository baoTriRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getThongKeTongQuan() {
        Map<String, Object> stats = new HashMap<>();

        // Thống kê thiết bị
        long tongThietBi = thietBiRepository.count();
        long thietBiTot = thietBiRepository.countByTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.TOT);
        long thietBiHong = thietBiRepository.countByTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.HONG);
        long thietBiBaoTri = thietBiRepository.countByTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.BAO_TRI);

        stats.put("tongThietBi", tongThietBi);
        stats.put("thietBiTot", thietBiTot);
        stats.put("thietBiHong", thietBiHong);
        stats.put("thietBiBaoTri", thietBiBaoTri);

        // Thống kê báo hỏng
        long baoHongChoXuLy = baoHongRepository.countByTrangThai(com.Tta.QLCSVC.DHNT.entity.BaoHong.TrangThaiBaoHong.CHO_XU_LY);
        stats.put("baoHongChoXuLy", baoHongChoXuLy);

        // Lấy 5 báo hỏng gần nhất
        stats.put("recentBaoHongs", baoHongRepository.findTop5ByOrderByNgayBaoDesc());

        // Lấy 5 lịch bảo trì gần nhất
        stats.put("recentBaoTris", baoTriRepository.findTop5ByOrderByNgayBaoTriDesc());

        return stats;
    }
}
