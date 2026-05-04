package com.Tta.QLCSVC.DHNT.areas.giaovien.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
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
public class GiaoVienBaoHongService {

    private final BaoHongRepository baoHongRepository;
    private final ThietBiRepository thietBiRepository;
    private final NguoiDungRepository nguoiDungRepository;

    @Transactional(readOnly = true)
    public List<BaoHong> getMyReports() {
        NguoiDung currentUser = getCurrentUser();
        return baoHongRepository.findByNguoiBaoId(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public long getMyReportCount() {
        NguoiDung currentUser = getCurrentUser();
        return baoHongRepository.findByNguoiBaoId(currentUser.getId()).size();
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getMyRecentReports(int limit) {
        NguoiDung currentUser = getCurrentUser();
        List<BaoHong> all = baoHongRepository.findByNguoiBaoId(currentUser.getId());
        all.forEach(bh -> {
            if (bh.getThietBi() != null)
                bh.getThietBi().getTenThietBi();
        });
        return all.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : a.getNgayBao();
                    LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : b.getNgayBao();
                    return bTime.compareTo(aTime);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional
    public BaoHong reportDamage(Long thietBiId, String moTa, BaoHong.MucDoNghiemTrong mucDo) {
        NguoiDung currentUser = getCurrentUser();

        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));

        BaoHong baoHong = new BaoHong();
        baoHong.setThietBi(thietBi);
        baoHong.setNguoiBao(currentUser);
        baoHong.setNgayBao(LocalDateTime.now());
        baoHong.setMoTaLoi(moTa);
        baoHong.setMucDoNghiemTrong(mucDo != null ? mucDo : BaoHong.MucDoNghiemTrong.TRUNG_BINH);
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);

        // Update equipment status
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.HONG);
        thietBiRepository.save(thietBi);

        return baoHongRepository.save(baoHong);
    }

    private NguoiDung getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return nguoiDungRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", auth.getName()));
    }
}
