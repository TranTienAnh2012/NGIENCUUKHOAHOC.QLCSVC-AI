package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.LoaiPhong;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.LoaiPhongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLoaiPhongService {

    private final LoaiPhongRepository loaiPhongRepository;

    public Page<LoaiPhong> getAllLoaiPhong(Pageable pageable) {
        return loaiPhongRepository.findAll(pageable);
    }

    public List<LoaiPhong> getAllLoaiPhong() {
        return loaiPhongRepository.findAll();
    }

    public LoaiPhong getLoaiPhongById(Long id) {
        return loaiPhongRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoaiPhong", "id", id));
    }

    @Transactional
    public LoaiPhong createLoaiPhong(LoaiPhong loaiPhong) {
        return loaiPhongRepository.save(loaiPhong);
    }

    @Transactional
    public LoaiPhong updateLoaiPhong(Long id, LoaiPhong loaiPhongDetails) {
        LoaiPhong loaiPhong = getLoaiPhongById(id);
        loaiPhong.setTenLoai(loaiPhongDetails.getTenLoai());
        loaiPhong.setMoTa(loaiPhongDetails.getMoTa());
        loaiPhong.setSoPhong(loaiPhongDetails.getSoPhong());
        return loaiPhongRepository.save(loaiPhong);
    }

    @Transactional
    public void deleteLoaiPhong(Long id) {
        LoaiPhong loaiPhong = getLoaiPhongById(id);

        // Gỡ loại phòng khỏi các phòng học liên quan (SET NULL)
        if (loaiPhong.getPhongHocs() != null) {
            loaiPhong.getPhongHocs().forEach(ph -> ph.setLoaiPhong(null));
        }

        loaiPhongRepository.delete(loaiPhong);
    }
}
