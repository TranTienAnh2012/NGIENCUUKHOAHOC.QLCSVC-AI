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
import com.Tta.QLCSVC.DHNT.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVCBaoHongService {

    private final BaoHongRepository baoHongRepository;
    private final BaoTriRepository baoTriRepository;
    private final ThietBiRepository thietBiRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<BaoHong> getAllBaoHong() {
        List<BaoHong> list = baoHongRepository.findAll();
        list.forEach(this::initLazy);
        return list;
    }

    @Transactional(readOnly = true)
    public BaoHong getBaoHongById(Long id) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        initLazy(baoHong);
        return baoHong;
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getPendingReports() {
        List<BaoHong> list = baoHongRepository.findPendingReports();
        list.forEach(this::initLazy);
        return list;
    }

    @Transactional(readOnly = true)
    public List<BaoHong> getUrgentReports() {
        List<BaoHong> list = baoHongRepository.findUrgentReports();
        list.forEach(this::initLazy);
        return list;
    }

    /**
     * Lấy danh sách phiếu được phân công cho nhân viên đang đăng nhập.
     * Query sử dụng composite index nên O(log N).
     */
    @Transactional(readOnly = true)
    public List<BaoHong> getAssignedToCurrentUser() {
        NguoiDung currentUser = getCurrentUser();
        List<BaoHong> list = baoHongRepository.findAssignedToNhanVien(currentUser.getId());
        list.forEach(this::initLazy);
        return list;
    }

    /**
     * Lấy danh sách phiếu đang CHO_XAC_NHAN của nhân viên hiện tại — dùng cho notification badge.
     */
    @Transactional(readOnly = true)
    public List<BaoHong> getPendingAcceptanceForCurrentUser() {
        NguoiDung currentUser = getCurrentUser();
        return baoHongRepository.findPendingAcceptanceByNhanVien(currentUser.getId());
    }

    /** Count phiếu chờ xác nhận — sidebar badge */
    @Transactional(readOnly = true)
    public long countPendingAcceptanceForCurrentUser() {
        NguoiDung currentUser = getCurrentUser();
        return baoHongRepository.countPendingAcceptanceByNhanVien(currentUser.getId());
    }

    /**
     * Nhân viên XÁC NHẬN nhận việc.
     * IDOR Guard: chỉ nhân viên được gán mới được chấp nhận.
     */
    @Transactional
    public BaoHong acceptAssignment(Long baoHongId) {
        NguoiDung currentUser = getCurrentUser();
        BaoHong baoHong = getBaoHongById(baoHongId);

        validateAssignee(baoHong, currentUser.getId());

        baoHong.setTrangThaiPhanCong(BaoHong.TrangThaiPhanCong.DA_NHAN);
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        BaoHong saved = baoHongRepository.save(baoHong);

        // Notify Admin
        String tenThietBi = saved.getThietBi() != null ? saved.getThietBi().getTenThietBi() : "Thiết bị";
        notificationService.sendToRole(NguoiDung.VaiTro.ADMIN,
            "✅ Nhân viên đã nhận phân công",
            currentUser.getHoTen() + " đã xác nhận xử lý: " + tenThietBi,
            "PHAN_CONG", "/admin/bao-hong/view/" + baoHongId);

        return saved;
    }

    /**
     * Nhân viên TỪ CHỐI nhận việc.
     * IDOR Guard: chỉ nhân viên được gán mới được từ chối.
     */
    @Transactional
    public BaoHong rejectAssignment(Long baoHongId, String lyDoTuChoi) {
        NguoiDung currentUser = getCurrentUser();
        BaoHong baoHong = getBaoHongById(baoHongId);

        validateAssignee(baoHong, currentUser.getId());

        baoHong.setTrangThaiPhanCong(BaoHong.TrangThaiPhanCong.TU_CHOI);
        baoHong.setLyDoTuChoi(lyDoTuChoi);
        // Giữ nguyên TrangThaiBaoHong = CHO_XU_LY để Admin có thể gán lại
        BaoHong saved = baoHongRepository.save(baoHong);

        String tenThietBi = saved.getThietBi() != null ? saved.getThietBi().getTenThietBi() : "Thiết bị";
        notificationService.sendToRole(NguoiDung.VaiTro.ADMIN,
            "❌ Nhân viên từ chối phân công",
            currentUser.getHoTen() + " từ chối: " + tenThietBi + ". Lý do: " + lyDoTuChoi,
            "PHAN_CONG", "/admin/bao-hong/view/" + baoHongId);

        return saved;
    }

    /**
     * AI Auto-Create: Khi nhân viên đồng ý qua chatbot với 1 phiếu báo hỏng chưa phân công,
     * AI sẽ tự động: accept assignment + tạo BaoTri liên kết.
     * Transaction ACID: nếu bất kỳ bước nào fail, toàn bộ rollback.
     */
    @Transactional
    public BaoTri aiAutoAcceptAndCreateBaoTri(Long baoHongId, Long nhanVienId) {
        BaoHong baoHong = getBaoHongById(baoHongId);
        NguoiDung nhanVien = nguoiDungRepository.findById(nhanVienId)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "id", nhanVienId));

        if (nhanVien.getVaiTro() != NguoiDung.VaiTro.NHAN_VIEN_CSVC) {
            throw new IllegalArgumentException("Chỉ nhân viên CSVC mới có thể tự nhận phiếu qua AI");
        }

        // Cập nhật BaoHong: DA_NHAN + DANG_XU_LY
        baoHong.setNguoiPhuTrach(nhanVien);
        baoHong.setTrangThaiPhanCong(BaoHong.TrangThaiPhanCong.DA_NHAN);
        baoHong.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        baoHongRepository.save(baoHong);

    // Tạo lịch BaoTri tự động
        ThietBi thietBi = baoHong.getThietBi();
        BaoTri baoTri = new BaoTri();
        baoTri.setThietBi(thietBi);
        baoTri.setBaoHong(baoHong);
        baoTri.setNguoiThucHien(nhanVien);
        baoTri.setLoaiBaoTri(BaoTri.LoaiBaoTri.SUA_CHUA);
        baoTri.setNgayBaoTri(LocalDate.now());
        String autoNoiDung = "[AI Auto] Lịch bảo trì được tạo tự động. Thiết bị: " + 
            (thietBi != null ? thietBi.getTenThietBi() : "N/A") + ". " +
            "Mô tả lỗi: " + baoHong.getMoTaLoi() + ". Mức độ: " + baoHong.getMucDoNghiemTrong();
        baoTri.setNoiDung(autoNoiDung);
        baoTri.setChiPhi(BigDecimal.ZERO);

        // Cập nhật trạng thái thiết bị
        if (thietBi != null) {
            thietBi.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
            thietBiRepository.save(thietBi);
        }

        BaoTri saved = baoTriRepository.save(baoTri);

        // Notify Admin
        String tenThietBi = thietBi != null ? thietBi.getTenThietBi() : "Thiết bị";
        notificationService.sendToRole(NguoiDung.VaiTro.ADMIN,
            "🤖 AI tự tạo lịch bảo trì",
            nhanVien.getHoTen() + " đã nhận và AI tự tạo lịch cho: " + tenThietBi,
            "BAO_TRI", "/admin/bao-tri/view/" + saved.getId());

        return saved;
    }

    /**
     * Tương tự aiAutoAcceptAndCreateBaoTri nhưng dành cho nút bấm trên UI (tự lấy current user).
     * Cho phép nhận cả phiếu CHUA_PHAN_CONG.
     */
    @Transactional
    public BaoTri aiAutoScheduleFromUI(Long baoHongId) {
        NguoiDung currentUser = getCurrentUser();
        return aiAutoAcceptAndCreateBaoTri(baoHongId, currentUser.getId());
    }

    @Transactional
    public BaoHong updateStatus(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
        baoHong.setTrangThai(trangThai);
        BaoHong saved = baoHongRepository.save(baoHong);
        if (saved.getNguoiBao() != null && saved.getThietBi() != null) {
            notificationService.sendToUser(saved.getNguoiBao().getId(),
                "🛠️ Cập nhật báo hỏng: " + saved.getThietBi().getTenThietBi(),
                "Trạng thái báo hỏng chuyển sang: " + trangThai.name(),
                "BAO_HONG", "/giao-vien/bao-hong");
        }
        return saved;
    }

    // ========== HELPERS ==========

    private void initLazy(BaoHong bh) {
        if (bh.getThietBi() != null) bh.getThietBi().getTenThietBi();
        if (bh.getNguoiBao() != null) bh.getNguoiBao().getHoTen();
        if (bh.getNguoiPhuTrach() != null) bh.getNguoiPhuTrach().getHoTen();
    }

    /** Lấy NguoiDung từ SecurityContext (email = username) */
    private NguoiDung getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("NguoiDung", "email", email));
    }

    /** IDOR Guard: kiểm tra nhân viên hiện tại có phải người được gán không */
    private void validateAssignee(BaoHong baoHong, Long currentUserId) {
        if (baoHong.getNguoiPhuTrach() == null ||
                !baoHong.getNguoiPhuTrach().getId().equals(currentUserId)) {
            throw new SecurityException("Bạn không được phép thực hiện hành động này trên phiếu báo hỏng #" + baoHong.getId());
        }
        if (baoHong.getTrangThaiPhanCong() != BaoHong.TrangThaiPhanCong.CHO_XAC_NHAN) {
            throw new IllegalStateException("Phiếu này không ở trạng thái CHO_XAC_NHAN, không thể chấp nhận/từ chối");
        }
    }
}
