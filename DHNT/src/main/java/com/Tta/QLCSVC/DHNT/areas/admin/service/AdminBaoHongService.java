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

    public Page<BaoHong> getAllBaoHong(Pageable pageable) {
        return baoHongRepository.findAll(pageable);
    }

    public List<BaoHong> getAllBaoHong() {
        return baoHongRepository.findAll();
    }

    public BaoHong getBaoHongById(Long id) {
        return baoHongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BaoHong", "id", id));
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
        return baoHongRepository.save(baoHong);
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
        return baoHongRepository.save(baoHong);
    }

    @Transactional
    public BaoHong updateTrangThai(Long id, BaoHong.TrangThaiBaoHong trangThai) {
        BaoHong baoHong = getBaoHongById(id);
        baoHong.setTrangThai(trangThai);
        return baoHongRepository.save(baoHong);
    }

    @Transactional
    public void deleteBaoHong(Long id) {
        if (!baoHongRepository.existsById(id)) {
            throw new ResourceNotFoundException("BaoHong", "id", id);
        }
        baoHongRepository.deleteById(id);
    }
}
