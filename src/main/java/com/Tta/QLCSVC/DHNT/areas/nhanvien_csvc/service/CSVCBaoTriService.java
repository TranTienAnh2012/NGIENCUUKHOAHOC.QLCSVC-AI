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

    @Transactional(readOnly = true)
    public List<BaoTri> getAllBaoTri() {
        List<BaoTri> list = baoTriRepository.findAll();
        list.forEach(bt -> {
            if (bt.getThietBi() != null) {
                bt.getThietBi().getTenThietBi();
            }
        });
        return list;
    }

    public List<BaoTri> getBaoTriByThietBi(Long thietBiId) {
        return baoTriRepository.findByThietBiId(thietBiId);
    }

    @Transactional(readOnly = true)
    public BaoTri getBaoTriById(Long id) {
        BaoTri bt = baoTriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", id));
        if (bt.getThietBi() != null) {
            bt.getThietBi().getTenThietBi();
        }
        if (bt.getBaoHong() != null) {
            bt.getBaoHong().getMoTaLoi();
        }
        return bt;
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
    @Transactional
    public BaoTri updateTienDoBaoTri(Long baoTriId, BaoTri.KetQuaBaoTri ketQua, String ghiChuThem, BigDecimal chiPhi) {
        BaoTri baoTri = baoTriRepository.findById(baoTriId)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", baoTriId));

        System.out.println("DEBUG UPDATE BAO TRI: ID=" + baoTriId + ", ghiChuThem=" + ghiChuThem + ", ketQua=" + ketQua);

        // FORCE HARDCODE GHI CHÚ ĐỂ KIỂM TRA
        String timeStamp = "[" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "] ";
        String oldGhiChu = baoTri.getNoiDung() != null && !baoTri.getNoiDung().isEmpty() ? baoTri.getNoiDung() + "\n" : "";
        String ghiChuMoi = (ghiChuThem != null && !ghiChuThem.trim().isEmpty()) ? ghiChuThem.trim() : "KHÔNG NHẬN ĐƯỢC GHI CHÚ TỪ FORM!";
        baoTri.setNoiDung(oldGhiChu + timeStamp + ghiChuMoi);

        if (chiPhi != null) {
            baoTri.setChiPhi(chiPhi);
        }

        if (ketQua != null) {
            baoTri.setKetQua(ketQua);
            // Cập nhật trạng thái thiết bị tương ứng
            ThietBi thietBi = baoTri.getThietBi();
            if (thietBi != null) {
                if (ketQua == BaoTri.KetQuaBaoTri.THANH_CONG) {
                    thietBi.setTrangThai(ThietBi.TrangThaiThietBi.TOT);
                } else if (ketQua == BaoTri.KetQuaBaoTri.THAT_BAI || ketQua == BaoTri.KetQuaBaoTri.CAN_THAY_THE) {
                    thietBi.setTrangThai(ThietBi.TrangThaiThietBi.HONG);
                }
                thietBiRepository.save(thietBi);
            }
        }

        BaoTri saved = baoTriRepository.saveAndFlush(baoTri);
        System.out.println("DEBUG UPDATE BAO TRI: DB noiDung=" + saved.getNoiDung());
        return saved;
    }
}
