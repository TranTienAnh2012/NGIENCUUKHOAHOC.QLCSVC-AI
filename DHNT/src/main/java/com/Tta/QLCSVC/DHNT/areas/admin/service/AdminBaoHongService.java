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
