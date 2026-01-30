package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVCBaoTriService {

    private final BaoTriRepository baoTriRepository;
    private final ThietBiRepository thietBiRepository;
    private final BaoHongRepository baoHongRepository;

    public List<BaoTri> getAllBaoTri() {
        return baoTriRepository.findAll();
    }

    public List<BaoTri> getBaoTriByThietBi(Long thietBiId) {
        return baoTriRepository.findByThietBiId(thietBiId);
    }

    @Transactional
    public BaoTri taoLichBaoTri(Long thietBiId, BaoTri baoTri) {
        ThietBi thietBi = thietBiRepository.findById(thietBiId)
                .orElseThrow(() -> new ResourceNotFoundException("ThietBi", "id", thietBiId));

        baoTri.setThietBi(thietBi);
        if (baoTri.getNgayBaoTri() == null) {
            baoTri.setNgayBaoTri(LocalDate.now());
        }

        // Cập nhật trạng thái thiết bị sang BAO_TRI
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
        thietBiRepository.save(thietBi);

        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public BaoTri taoLichBaoTriTuBaoHong(Long baoHongId, BaoTri baoTri) {
        BaoHong baoHong = baoHongRepository.findById(baoHongId)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", baoHongId));

        // Lấy thiết bị từ báo hỏng
        ThietBi thietBi = baoHong.getThietBi();

        // Thiết lập thông tin bảo trì
        baoTri.setThietBi(thietBi);
        baoTri.setBaoHong(baoHong);
        if (baoTri.getNgayBaoTri() == null) {
            baoTri.setNgayBaoTri(LocalDate.now());
        }
        if (baoTri.getLoaiBaoTri() == null) {
            baoTri.setLoaiBaoTri(BaoTri.LoaiBaoTri.SUA_CHUA);
        }

        // Cập nhật trạng thái báo hỏng sang DANG_XU_LY
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        baoHongRepository.save(baoHong);

        // Cập nhật trạng thái thiết bị sang BAO_TRI
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
        thietBiRepository.save(thietBi);

        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public BaoTri hoanThanhBaoTri(Long baoTriId, BaoTri.KetQuaBaoTri ketQua, BigDecimal chiPhi) {
        BaoTri baoTri = baoTriRepository.findById(baoTriId)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", baoTriId));

        baoTri.setKetQua(ketQua);
        baoTri.setChiPhi(chiPhi);

        // Trả thiết bị về trạng thái TOT
        ThietBi thietBi = baoTri.getThietBi();
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.TOT);
        thietBiRepository.save(thietBi);

        return baoTriRepository.save(baoTri);
    }
}
