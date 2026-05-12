package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThongBao;
import com.Tta.QLCSVC.DHNT.repository.ThongBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThongBaoService {

    private final ThongBaoRepository thongBaoRepository;

    public List<ThongBao> getUnreadNotifications(Long userId) {
        return thongBaoRepository.findByNguoiDungIdAndDaDocFalse(userId);
    }

    public long getUnreadCount(Long userId) {
        return thongBaoRepository.countByNguoiDungIdAndDaDocFalse(userId);
    }

    @Transactional
    public void markAsRead(Long thongBaoId) {
        thongBaoRepository.findById(thongBaoId).ifPresent(thongBao -> {
            thongBao.setDaDoc(true);
            thongBaoRepository.save(thongBao);
        });
    }

    @Transactional
    public ThongBao createNotification(NguoiDung user, String title, String content, String type) {
        ThongBao thongBao = new ThongBao();
        thongBao.setNguoiDung(user);
        thongBao.setTieuDe(title);
        thongBao.setNoiDung(content);
        thongBao.setLoaiThongBao(type);
        thongBao.setDaDoc(false);
        return thongBaoRepository.save(thongBao);
    }
}
