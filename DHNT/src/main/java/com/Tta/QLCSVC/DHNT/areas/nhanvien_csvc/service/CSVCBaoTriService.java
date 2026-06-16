package com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.BaoTri;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final NguoiDungRepository nguoiDungRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<BaoTri> getAllBaoTri() {
        List<BaoTri> list = baoTriRepository.findAll();
        list.forEach(this::initLazy);
        return list;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BaoTri getBaoTriById(Long id) {
        BaoTri bt = baoTriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", id));
        initLazy(bt);
        return bt;
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
        // Auto-set người thực hiện từ SecurityContext
        setNguoiThucHienFromContext(baoTri);

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

        // Auto-set người thực hiện từ SecurityContext
        setNguoiThucHienFromContext(baoTri);

        // Cập nhật trạng thái báo hỏng sang DANG_XU_LY
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        baoHongRepository.save(baoHong);

        // Cập nhật trạng thái thiết bị sang BAO_TRI
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
        thietBiRepository.save(thietBi);

        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public BaoTri hoanThanhBaoTri(Long baoTriId, BaoTri.KetQuaBaoTri ketQua, BigDecimal chiPhi, String ghiChu) {
        BaoTri baoTri = baoTriRepository.findById(baoTriId)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", baoTriId));

        baoTri.setKetQua(ketQua);
        baoTri.setChiPhi(chiPhi);

        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            String currentNoiDung = baoTri.getNoiDung() != null ? baoTri.getNoiDung() : "";
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String appendedNote = "[" + timestamp + "] " + ghiChu;
            baoTri.setNoiDung(currentNoiDung.isEmpty() ? appendedNote : currentNoiDung + "\n" + appendedNote);
        }

        // Cập nhật trạng thái thiết bị
        ThietBi thietBi = baoTri.getThietBi();
        if (ketQua == BaoTri.KetQuaBaoTri.THANH_CONG) {
            thietBi.setTrangThai(ThietBi.TrangThaiThietBi.TOT);
        } else {
            thietBi.setTrangThai(ThietBi.TrangThaiThietBi.HONG);
        }
        thietBiRepository.save(thietBi);

        // Cập nhật trạng thái Báo hỏng (Đơn hỏng) nếu có
        if (baoTri.getBaoHong() != null) {
            BaoHong baoHong = baoTri.getBaoHong();
            if (ketQua == BaoTri.KetQuaBaoTri.THANH_CONG) {
                baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.HOAN_THANH);
            }
            baoHongRepository.save(baoHong);
        }

        return baoTriRepository.save(baoTri);
    }

    @Transactional
    public BaoTri capNhatTienDo(Long baoTriId, BigDecimal chiPhiThem, String ghiChu) {
        BaoTri baoTri = baoTriRepository.findById(baoTriId)
                .orElseThrow(() -> new ResourceNotFoundException("BaoTri", "id", baoTriId));

        if (chiPhiThem != null && chiPhiThem.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentChiPhi = baoTri.getChiPhi() != null ? baoTri.getChiPhi() : BigDecimal.ZERO;
            baoTri.setChiPhi(currentChiPhi.add(chiPhiThem));
        }

        if (ghiChu != null && !ghiChu.trim().isEmpty()) {
            String currentNoiDung = baoTri.getNoiDung() != null ? baoTri.getNoiDung() : "";
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String appendedNote = "[" + timestamp + "] " + ghiChu;
            baoTri.setNoiDung(currentNoiDung.isEmpty() ? appendedNote : currentNoiDung + "\n" + appendedNote);
        }

        return baoTriRepository.save(baoTri);
    }

    /** Auto-set nguoiThucHien từ SecurityContext nếu chưa được set */
    private void setNguoiThucHienFromContext(BaoTri baoTri) {
        if (baoTri.getNguoiThucHien() == null) {
            try {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                nguoiDungRepository.findByEmail(email).ifPresent(baoTri::setNguoiThucHien);
            } catch (Exception ignored) {
                // Không bắt buộc — nếu không lấy được context thì để null
            }
        }
    }

    private NguoiDung getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", email));
    }

    private void initLazy(BaoTri bt) {
        if (bt.getThietBi() != null) bt.getThietBi().getTenThietBi();
        if (bt.getNguoiThucHien() != null) bt.getNguoiThucHien().getHoTen();
        if (bt.getBaoHong() != null) bt.getBaoHong().getNgayBao();
    }
}
