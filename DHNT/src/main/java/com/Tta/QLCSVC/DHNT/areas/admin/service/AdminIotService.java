package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.IotDuLieuCamBien;
import com.Tta.QLCSVC.DHNT.repository.IotDuLieuCamBienRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminIotService {

    private final IotDuLieuCamBienRepository iotRepository;

    public Page<IotDuLieuCamBien> getAllIotData(Pageable pageable) {
        return iotRepository.findAll(pageable);
    }

    public void deleteById(Long id) {
        iotRepository.deleteById(id);
    }
}
