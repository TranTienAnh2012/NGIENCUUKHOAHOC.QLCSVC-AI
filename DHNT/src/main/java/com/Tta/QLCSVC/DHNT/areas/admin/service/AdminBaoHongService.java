package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
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
    private final com.Tta.QLCSVC.DHNT.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<BaoHong> getAllBaoHong(Pageable pageable) {
        Page<BaoHong> page = baoHongRepository.findAll(pageable);
        // Force initialization of lazy relationships
        page.getContent().forEach(bh -> {
            if (bh.getThietBi() != null) {
                bh.getThietBi().getTenThietBi();
            }
            if (bh.getNguoiBao() != null) {
                bh.getNguoiBao().getHoTen();
            }
        });
        return page;
    }

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
        // Force initialization of lazy relationships
        if (baoHong.getThietBi() != null) {
            baoHong.getThietBi().getTenThietBi();
        }
        if (baoHong.getNguoiBao() != null) {
            baoHong.getNguoiBao().getHoTen();
        }
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
        
        // Notify Nhân Viên CSVC
        if (saved.getThietBi() != null) {
            String title = "🚨 Báo hỏng mới từ Admin: " + saved.getThietBi().getTenThietBi();
            String msg = "Admin vừa tạo phiếu báo hỏng cho thiết bị: " + saved.getThietBi().getTenThietBi();
            notificationService.sendToRole(com.Tta.QLCSVC.DHNT.entity.NguoiDung.VaiTro.NHAN_VIEN_CSVC, title, msg, "BAO_HONG", "/nhanvien-csvc/bao-hong");
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
        
        // System Audit Log cho Admin
        if (saved.getThietBi() != null) {
            String title = "🔄 Hệ thống: Cập nhật báo hỏng";
            String msg = "Phiếu báo hỏng thiết bị " + saved.getThietBi().getTenThietBi() + " vừa được cập nhật.";
            notificationService.sendToRole(com.Tta.QLCSVC.DHNT.entity.NguoiDung.VaiTro.ADMIN, title, msg, "BAO_HONG", "/admin/bao-hong/view/" + id);
        }
        
        return saved;
    }

    @Transactional
    public BaoHong updateTrangThai(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = getBaoHongById(id);
        baoHong.setTrangThai(trangThai);
        BaoHong saved = baoHongRepository.save(baoHong);
        
        // Khôi phục thiết bị về hoạt động nếu phiếu báo hỏng được hoàn thành hoặc bị hủy
        if (saved.getThietBi() != null && (trangThai == BaoHong.TrangThaiBaoHong.HOAN_THANH || trangThai == BaoHong.TrangThaiBaoHong.HUY)) {
            saved.getThietBi().setTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.TOT);
        }
        
        // Notify the user who reported it
        if (saved.getNguoiBao() != null && saved.getThietBi() != null) {
            String title = "🛠️ Cập nhật báo hỏng: " + saved.getThietBi().getTenThietBi();
            String msg = "Trạng thái báo hỏng thiết bị " + saved.getThietBi().getTenThietBi() + " đã được chuyển sang: " + trangThai.name();
            notificationService.sendToUser(saved.getNguoiBao().getId(), title, msg, "BAO_HONG", "/giao-vien/bao-hong");
        }
        
        // System Audit Log cho Admin
        if (saved.getThietBi() != null) {
            String adminTitle = "🔄 Hệ thống: Đổi trạng thái báo hỏng";
            String adminMsg = "Thiết bị " + saved.getThietBi().getTenThietBi() + " -> Trạng thái: " + trangThai.name();
            notificationService.sendToRole(com.Tta.QLCSVC.DHNT.entity.NguoiDung.VaiTro.ADMIN, adminTitle, adminMsg, "BAO_HONG", "/admin/bao-hong/view/" + id);
        }
        
        return saved;
    }

    @Transactional
    public void deleteBaoHong(Long id) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
                
        // Khôi phục trạng thái thiết bị về TOT nếu xoá phiếu báo hỏng
        if (baoHong.getThietBi() != null) {
            baoHong.getThietBi().setTrangThai(com.Tta.QLCSVC.DHNT.entity.ThietBi.TrangThaiThietBi.TOT);
            // System Audit Log cho Admin
            String title = "🗑️ Hệ thống: Xóa báo hỏng";
            String msg = "Phiếu báo hỏng thiết bị " + baoHong.getThietBi().getTenThietBi() + " vừa bị xóa.";
            notificationService.sendToRole(com.Tta.QLCSVC.DHNT.entity.NguoiDung.VaiTro.ADMIN, title, msg, "BAO_HONG", "/admin/bao-hong");
        }
        
        baoHongRepository.delete(baoHong);
    }
}
