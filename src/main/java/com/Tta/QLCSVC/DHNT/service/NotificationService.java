package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.repository.NguoiDungRepository;
import com.Tta.QLCSVC.DHNT.repository.ThongBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ThongBaoRepository thongBaoRepository;
    private final NguoiDungRepository nguoiDungRepository;

    @Transactional
    public ThongBao sendToUser(Long userId, String tieuDe, String noiDung, ThongBao.LoaiThongBao loai, String url) {
        NguoiDung user = nguoiDungRepository.findById(userId).orElse(null);
        if (user == null) return null;

        ThongBao tb = new ThongBao();
        tb.setNguoiNhan(user);
        tb.setTieuDe(tieuDe);
        tb.setNoiDung(noiDung);
        tb.setLoaiThongBao(loai);
        tb.setUrlDetail(url);
        return thongBaoRepository.save(tb);
    }

    @Transactional
    public ThongBao sendToRole(NguoiDung.VaiTro role, String tieuDe, String noiDung, ThongBao.LoaiThongBao loai, String url) {
        ThongBao tb = new ThongBao();
        tb.setRoleNhan(role);
        tb.setTieuDe(tieuDe);
        tb.setNoiDung(noiDung);
        tb.setLoaiThongBao(loai);
        tb.setUrlDetail(url);
        return thongBaoRepository.save(tb);
    }

    @Transactional(readOnly = true)
    public List<ThongBao> getUserNotifications(Long userId, NguoiDung.VaiTro role) {
        return thongBaoRepository.findNotificationsForUser(userId, role);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, NguoiDung.VaiTro role) {
        return thongBaoRepository.countUnreadNotifications(userId, role);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        thongBaoRepository.findById(notificationId).ifPresent(tb -> {
            tb.setDaDoc(true);
            thongBaoRepository.save(tb);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId, NguoiDung.VaiTro role) {
        List<ThongBao> unread = thongBaoRepository.findNotificationsForUser(userId, role)
                .stream().filter(tb -> !tb.isDaDoc()).toList();
        unread.forEach(tb -> tb.setDaDoc(true));
        thongBaoRepository.saveAll(unread);
    }
}
