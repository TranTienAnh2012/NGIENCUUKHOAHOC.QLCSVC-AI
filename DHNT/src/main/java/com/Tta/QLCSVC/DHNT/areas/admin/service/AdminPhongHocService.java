package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.PhongHocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPhongHocService {

    private final PhongHocRepository phongHocRepository;

    public List<PhongHoc> getAllPhongHoc() {
        return phongHocRepository.findAll();
    }

    public PhongHoc getPhongHocById(Long id) {
        return phongHocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PhongHoc", "id", id));
    }
}
