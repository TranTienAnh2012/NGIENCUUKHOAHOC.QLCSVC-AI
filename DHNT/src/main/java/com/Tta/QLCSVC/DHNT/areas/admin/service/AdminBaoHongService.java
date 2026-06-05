package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.dto.PhanCongRequest;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBaoHongService {

    private final BaoHongRepository baoHongRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final com.Tta.QLCSVC.DHNT.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<BaoHong> getAllBaoHong(Pageable pageable) {
        Page<BaoHong> page = baoHongRepository.findAll(pageable);
        page.getContent().forEach(bh -> {
            if (bh.getThietBi() != null) bh.getThietBi().getTenThietBi();
            if (bh.getNguoiBao() != null) bh.getNguoiBao().getHoTen();
            if (bh.getNguoiPhuTrach() != null) bh.getNguoiPhuTrach().getHoTen();
        });
        return page;
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getAllBaoHong() {
        List<BaoHong> list = baoHongRepository.findAll();
        list.forEach(bh -> {
            if (bh.getThietBi() != null) bh.getThietBi().getTenThietBi();
            if (bh.getNguoiBao() != null) bh.getNguoiBao().getHoTen();
            if (bh.getNguoiPhuTrach() != null) bh.getNguoiPhuTrach().getHoTen();
        });
        return list;
    }

    @Transactional(readOnly = true)
    public BaoHong getBaoHongById(Long id) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        if (baoHong.getThietBi() != null) baoHong.getThietBi().getTenThietBi();
        if (baoHong.getNguoiBao() != null) baoHong.getNguoiBao().getHoTen();
        if (baoHong.getNguoiPhuTrach() != null) baoHong.getNguoiPhuTrach().getHoTen();
        return baoHong;
    }

    public List<BaoHong> getBaoHongByThietBi(Long thietBiId) {
        return baoHongRepository.findByThietBiId(thietBiId);
    }

    public List<BaoHong> getBaoHongByTrangThai(BaoHong.TrangThaiBaoHong trangThai) {
        return baoHongRepository.findByTrangThai(trangThai);
    }

    public List<BaoHong> getBaoHongByMucDo(BaoHong.MucDoNghiemTrong mucDo) {
        return baoHongRepository.findByMucDoNghiemTrong(mucDo);
    }

    @Transactional
    public BaoHong createBaoHong(BaoHong baoHong) {
        if (baoHong.getTrangThai() == null) {
            baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);
        }
        BaoHong saved = baoHongRepository.save(baoHong);
        if (saved.getThietBi() != null) {
            notificationService.sendToRole(NguoiDung.VaiTro.NHAN_VIEN_CSVC,
                "🚨 Báo hỏng mới: " + saved.getThietBi().getTenThietBi(),
                "Admin vừa tạo phiếu báo hỏng cho thiết bị: " + saved.getThietBi().getTenThietBi(),
                "BAO_HONG", "/nhanvien-csvc/bao-hong");
        }
        return saved;
    }

    @Transactional
    public BaoHong updateBaoHong(Long id, BaoHong baoHongDetails) {
        BaoHong baoHong = getBaoHongById(id);
        baoHong.setThietBi(baoHongDetails.getThietBi());
        baoHong.setNguoiBao(baoHongDetails.getNguoiBao());
        baoHong.setNgayBao(baoHongDetails.getNgayBao());
        baoHong.setMoTaLoi(baoHongDetails.getMoTaLoi());
        baoHong.setMucDoNghiemTrong(baoHongDetails.getMucDoNghiemTrong());
        baoHong.setTrangThai(baoHongDetails.getTrangThai());
        BaoHong saved = baoHongRepository.save(baoHong);
        if (saved.getThietBi() != null) {
            notificationService.sendToRole(NguoiDung.VaiTro.ADMIN, "🔄 Cập nhật báo hỏng",
                "Phiếu báo hỏng thiết bị " + saved.getThietBi().getTenThietBi() + " vừa được cập nhật.",
                "BAO_HONG", "/admin/bao-hong/view/" + id);
        }
        return saved;
    }

    @Transactional
    public BaoHong updateTrangThai(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = getBaoHongById(id);
        baoHong.setTrangThai(trangThai);
        BaoHong saved = baoHongRepository.save(baoHong);
        if (saved.getThietBi() != null && (trangThai == BaoHong.TrangThaiBaoHong.HOAN_THANH
                || trangThai == BaoHong.TrangThaiBaoHong.HUY)) {
            saved.getThietBi().setTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.TOT);
        }
        if (saved.getNguoiBao() != null && saved.getThietBi() != null) {
            notificationService.sendToUser(saved.getNguoiBao().getId(),
                "🛠️ Cập nhật báo hỏng: " + saved.getThietBi().getTenThietBi(),
                "Trạng thái chuyển sang: " + trangThai.name(),
                "BAO_HONG", "/giao-vien/bao-hong");
        }
        return saved;
    }

    /**
     * Admin phân công nhân viên CSVC xử lý phiếu báo hỏng.
     * Strict validation: nhanVien phải có VaiTro = NHAN_VIEN_CSVC, không chấp nhận role khác.
     */
    @Transactional
    public BaoHong phanCongNhanVien(Long baoHongId, PhanCongRequest req) {
        BaoHong baoHong = getBaoHongById(baoHongId);

        NguoiDung nhanVien = nguoiDungRepository.findById(req.getNhanVienId())
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "id", req.getNhanVienId()));

        if (nhanVien.getVaiTro() != NguoiDung.VaiTro.NHAN_VIEN_CSVC) {
            throw new IllegalArgumentException(
                "Chỉ có thể phân công cho nhân viên CSVC, không phải: " + nhanVien.getVaiTro().getTen());
        }

        baoHong.setNguoiPhuTrach(nhanVien);
        baoHong.setTrangThaiPhanCong(BaoHong.TrangThaiPhanCong.CHO_XAC_NHAN);
        if (req.getGhiChuAdmin() != null && !req.getGhiChuAdmin().isBlank()) {
            String existing = baoHong.getGhiChu() != null ? baoHong.getGhiChu() : "";
            baoHong.setGhiChu(existing + "\n[Admin] " + req.getGhiChuAdmin());
        }

        BaoHong saved = baoHongRepository.save(baoHong);

        String tenThietBi = saved.getThietBi() != null ? saved.getThietBi().getTenThietBi() : "Thiết bị";
        notificationService.sendToUser(nhanVien.getId(),
            "📋 Bạn được phân công xử lý báo hỏng",
            "Admin giao: " + tenThietBi + ". Vui lòng xác nhận hoặc từ chối.",
            "PHAN_CONG", "/nhanvien-csvc/bao-hong/assignment/" + baoHongId);

        return saved;
    }

    /** Admin hủy phân công - đặt lại CHUA_PHAN_CONG */
    @Transactional
    public BaoHong huyCongViec(Long baoHongId) {
        BaoHong baoHong = getBaoHongById(baoHongId);
        NguoiDung nvCu = baoHong.getNguoiPhuTrach();
        baoHong.setNguoiPhuTrach(null);
        baoHong.setTrangThaiPhanCong(BaoHong.TrangThaiPhanCong.CHUA_PHAN_CONG);
        BaoHong saved = baoHongRepository.save(baoHong);
        if (nvCu != null && saved.getThietBi() != null) {
            notificationService.sendToUser(nvCu.getId(),
                "❌ Phân công đã bị thu hồi",
                "Admin đã thu hồi phân công thiết bị: " + saved.getThietBi().getTenThietBi(),
                "PHAN_CONG", "/nhanvien-csvc/bao-hong");
        }
        return saved;
    }

    @Transactional
    public void deleteBaoHong(Long id) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        if (baoHong.getThietBi() != null) {
            baoHong.getThietBi().setTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.TOT);
            notificationService.sendToRole(NguoiDung.VaiTro.ADMIN, "🗑️ Xóa báo hỏng",
                "Phiếu báo hỏng thiết bị " + baoHong.getThietBi().getTenThietBi() + " vừa bị xóa.",
                "BAO_HONG", "/admin/bao-hong");
        }
        baoHongRepository.delete(baoHong);
    }
}
