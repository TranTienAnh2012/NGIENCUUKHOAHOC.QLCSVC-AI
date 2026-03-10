package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.entity.ChatbotHoiThoai;
import com.Tta.QLCSVC.DHNT.repository.ChatbotHoiThoaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminChatbotService {

    private final ChatbotHoiThoaiRepository chatbotRepository;

    public Page<ChatbotHoiThoai> getAllConversations(Pageable pageable) {
        return chatbotRepository.findAll(pageable);
    }

    public void deleteConversation(Long id) {
        chatbotRepository.deleteById(id);
    }
}
