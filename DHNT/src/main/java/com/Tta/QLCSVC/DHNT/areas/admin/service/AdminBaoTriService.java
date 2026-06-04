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
    private final com.Tta.QLCSVC.DHNT.repository.LinhKienRepository linhKienRepository;

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
        BaoTri saved = baoTriRepository.save(baoTri);
        checkAndResetLinhKien(saved);
        return saved;
    }

    @Transactional
    public BaoTri updateBaoTri(Long id, BaoTri baoTriDetails) {
        BaoTri baoTri = getBaoTriById(id);

        baoTri.setThietBi(baoTriDetails.getThietBi());
        baoTri.setBaoHong(baoTriDetails.getBaoHong());
        baoTri.setLinhKien(baoTriDetails.getLinhKien());
        baoTri.setNgayBaoTri(baoTriDetails.getNgayBaoTri());
        baoTri.setNoiDung(baoTriDetails.getNoiDung());
        baoTri.setChiPhi(baoTriDetails.getChiPhi());
        baoTri.setKetQua(baoTriDetails.getKetQua());
        baoTri.setNguoiThucHien(baoTriDetails.getNguoiThucHien());
        baoTri.setLoaiBaoTri(baoTriDetails.getLoaiBaoTri());

        BaoTri saved = baoTriRepository.save(baoTri);
        checkAndResetLinhKien(saved);
        return saved;
    }

    private void checkAndResetLinhKien(BaoTri baoTri) {
        if (baoTri.getKetQua() == BaoTri.KetQuaBaoTri.THANH_CONG && baoTri.getLinhKien() != null) {
            com.Tta.QLCSVC.DHNT.entity.LinhKien lk = baoTri.getLinhKien();
            // Đặt lại tuổi thọ về 0 và trạng thái HOAT_DONG vì đã bảo trì/thay mới thành công
            lk.setThoiGianDaSuDung(0);
            lk.setTrangThai(com.Tta.QLCSVC.DHNT.entity.LinhKien.TrangThaiLinhKien.HOAT_DONG);
            
            // Nếu có nhập ngày bảo hành mới (có thể mở rộng UI để nhập), nhưng hiện tại reset tạm
            lk.setNgayMua(java.time.LocalDate.now()); // Reset ngày mua thành ngày hôm nay
            
            linhKienRepository.save(lk);
        }
    }

    @Transactional
    public void deleteBaoTri(Long id) {
        if (!baoTriRepository.existsById(id)) {
            throw new ResourceNotFoundException("BaoTri", "id", id);
        }
        baoTriRepository.deleteById(id);
    }
}
