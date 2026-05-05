package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminBaoHongService;
import com.Tta.QLCSVC.DHNT.entity.BaoHong;
import com.Tta.QLCSVC.DHNT.entity.NguoiDung;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests cho AdminBaoHongService
 * Kiểm tra các chức năng quản lý báo hỏng thiết bị
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBaoHongService Tests")
class AdminBaoHongServiceTest {

    @Mock
    private BaoHongRepository baoHongRepository;

    @InjectMocks
    private AdminBaoHongService adminBaoHongService;

    private BaoHong baoHong1;
    private BaoHong baoHong2;
    private ThietBi thietBi;
    private NguoiDung nguoiDung;

    @BeforeEach
    void setUp() {
        thietBi = new ThietBi();
        thietBi.setId(1L);
        thietBi.setTenThietBi("Máy chiếu Epson EB-X49");
        thietBi.setMaThietBi("TB001");
        thietBi.setTrangThai(ThietBi.TrangThaiThietBi.HONG);

        nguoiDung = new NguoiDung();
        nguoiDung.setId(1L);
        nguoiDung.setHoTen("Nguyễn Văn A");
        nguoiDung.setEmail("nguyenvana@dhnt.edu.vn");

        baoHong1 = new BaoHong();
        baoHong1.setId(1L);
        baoHong1.setThietBi(thietBi);
        baoHong1.setNguoiBao(nguoiDung);
        baoHong1.setMoTaLoi("Máy chiếu không hiển thị hình ảnh, đèn đỏ nhấp nháy");
        baoHong1.setMucDoNghiemTrong(BaoHong.MucDoNghiemTrong.CAO);
        baoHong1.setTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);
        baoHong1.setNgayBao(LocalDateTime.now());

        baoHong2 = new BaoHong();
        baoHong2.setId(2L);
        baoHong2.setThietBi(thietBi);
        baoHong2.setNguoiBao(nguoiDung);
        baoHong2.setMoTaLoi("Quạt máy chiếu kêu to, nhiều bụi");
        baoHong2.setMucDoNghiemTrong(BaoHong.MucDoNghiemTrong.TRUNG_BINH);
        baoHong2.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        baoHong2.setNgayBao(LocalDateTime.now().minusDays(3));
    }

    // ===========================================================
    // TEST: getAllBaoHong(Pageable)
    // ===========================================================

    @Test
    @DisplayName("Lấy danh sách báo hỏng theo trang - thành công")
    void getAllBaoHong_WithPageable_ReturnPageOfBaoHong() {
        // Given
        List<BaoHong> baoHongList = Arrays.asList(baoHong1, baoHong2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<BaoHong> expectedPage = new PageImpl<>(baoHongList, pageable, 2);

        when(baoHongRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<BaoHong> result = adminBaoHongService.getAllBaoHong(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(baoHongRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Lấy danh sách báo hỏng trang trống")
    void getAllBaoHong_EmptyPage_ReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<BaoHong> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(baoHongRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<BaoHong> result = adminBaoHongService.getAllBaoHong(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ===========================================================
    // TEST: getAllBaoHong() - không phân trang
    // ===========================================================

    @Test
    @DisplayName("Lấy tất cả báo hỏng không phân trang - thành công")
    void getAllBaoHong_NoPageable_ReturnListOfBaoHong() {
        // Given
        when(baoHongRepository.findAll()).thenReturn(Arrays.asList(baoHong1, baoHong2));

        // When
        List<BaoHong> result = adminBaoHongService.getAllBaoHong();

        // Then
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(0).getMoTaLoi()).contains("Máy chiếu không hiển thị");
        verify(baoHongRepository, times(1)).findAll();
    }

    // ===========================================================
    // TEST: getBaoHongById
    // ===========================================================

    @Test
    @DisplayName("Lấy báo hỏng theo ID - tìm thấy")
    void getBaoHongById_WhenExists_ReturnBaoHong() {
        // Given
        when(baoHongRepository.findById(1L)).thenReturn(Optional.of(baoHong1));

        // When
        BaoHong result = adminBaoHongService.getBaoHongById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMoTaLoi()).contains("Máy chiếu không hiển thị");
        assertThat(result.getMucDoNghiemTrong()).isEqualTo(BaoHong.MucDoNghiemTrong.CAO);
    }

    @Test
    @DisplayName("Lấy báo hỏng theo ID - không tìm thấy → ném ResourceNotFoundException")
    void getBaoHongById_WhenNotExists_ThrowResourceNotFoundException() {
        // Given
        when(baoHongRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminBaoHongService.getBaoHongById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BaoHong")
                .hasMessageContaining("99");
    }

    // ===========================================================
    // TEST: createBaoHong
    // ===========================================================

    @Test
    @DisplayName("Tạo báo hỏng mới - tự động đặt trạng thái CHO_XU_LY khi null")
    void createBaoHong_WithNullStatus_SetsDefaultStatus() {
        // Given
        BaoHong newBaoHong = new BaoHong();
        newBaoHong.setThietBi(thietBi);
        newBaoHong.setNguoiBao(nguoiDung);
        newBaoHong.setMoTaLoi("Bảng tương tác bị nứt màn hình");
        newBaoHong.setTrangThai(null); // chưa set trạng thái

        BaoHong savedBaoHong = new BaoHong();
        savedBaoHong.setId(3L);
        savedBaoHong.setTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);
        savedBaoHong.setMoTaLoi("Bảng tương tác bị nứt màn hình");

        when(baoHongRepository.save(any(BaoHong.class))).thenReturn(savedBaoHong);

        // When
        BaoHong result = adminBaoHongService.createBaoHong(newBaoHong);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        // Verify trạng thái được tự động đặt
        verify(baoHongRepository, times(1))
                .save(argThat(bh -> bh.getTrangThai() == BaoHong.TrangThaiBaoHong.CHO_XU_LY));
    }

    @Test
    @DisplayName("Tạo báo hỏng mới - trạng thái đã được đặt thì giữ nguyên")
    void createBaoHong_WithExistingStatus_KeepsStatus() {
        // Given
        BaoHong newBaoHong = new BaoHong();
        newBaoHong.setTrangThai(BaoHong.TrangThaiBaoHong.DANG_XU_LY);
        when(baoHongRepository.save(any(BaoHong.class))).thenReturn(newBaoHong);

        // When
        adminBaoHongService.createBaoHong(newBaoHong);

        // Then: trạng thái không bị ghi đè
        verify(baoHongRepository).save(argThat(bh -> bh.getTrangThai() == BaoHong.TrangThaiBaoHong.DANG_XU_LY));
    }

    // ===========================================================
    // TEST: updateTrangThai
    // ===========================================================

    @Test
    @DisplayName("Cập nhật trạng thái báo hỏng - thành công")
    void updateTrangThai_WhenBaoHongExists_UpdatesStatus() {
        // Given
        when(baoHongRepository.findById(1L)).thenReturn(Optional.of(baoHong1));
        BaoHong updated = new BaoHong();
        updated.setId(1L);
        updated.setTrangThai(BaoHong.TrangThaiBaoHong.HOAN_THANH);
        when(baoHongRepository.save(any(BaoHong.class))).thenReturn(updated);

        // When
        BaoHong result = adminBaoHongService.updateTrangThai(1L, BaoHong.TrangThaiBaoHong.HOAN_THANH);

        // Then
        assertThat(result.getTrangThai()).isEqualTo(BaoHong.TrangThaiBaoHong.HOAN_THANH);
        verify(baoHongRepository).save(argThat(bh -> bh.getTrangThai() == BaoHong.TrangThaiBaoHong.HOAN_THANH));
    }

    @Test
    @DisplayName("Cập nhật trạng thái báo hỏng - không tìm thấy → ném exception")
    void updateTrangThai_WhenNotExists_ThrowException() {
        // Given
        when(baoHongRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminBaoHongService.updateTrangThai(99L, BaoHong.TrangThaiBaoHong.HOAN_THANH))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===========================================================
    // TEST: deleteBaoHong
    // ===========================================================

    @Test
    @DisplayName("Xóa báo hỏng - thành công")
    void deleteBaoHong_WhenExists_DeletesSuccessfully() {
        // Given
        when(baoHongRepository.existsById(1L)).thenReturn(true);

        // When
        adminBaoHongService.deleteBaoHong(1L);

        // Then
        verify(baoHongRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Xóa báo hỏng - không tìm thấy → ném ResourceNotFoundException")
    void deleteBaoHong_WhenNotExists_ThrowResourceNotFoundException() {
        // Given
        when(baoHongRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> adminBaoHongService.deleteBaoHong(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BaoHong");
        verify(baoHongRepository, never()).deleteById(any());
    }

    // ===========================================================
    // TEST: getBaoHongByTrangThai
    // ===========================================================

    @Test
    @DisplayName("Lấy báo hỏng theo trạng thái CHO_XU_LY")
    void getBaoHongByTrangThai_CHO_XU_LY_ReturnFiltered() {
        // Given
        when(baoHongRepository.findByTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY))
                .thenReturn(List.of(baoHong1));

        // When
        List<BaoHong> result = adminBaoHongService.getBaoHongByTrangThai(BaoHong.TrangThaiBaoHong.CHO_XU_LY);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrangThai()).isEqualTo(BaoHong.TrangThaiBaoHong.CHO_XU_LY);
    }

    // ===========================================================
    // TEST: getBaoHongByMucDo
    // ===========================================================

    @Test
    @DisplayName("Lấy báo hỏng theo mức độ CAO")
    void getBaoHongByMucDo_CAO_ReturnHighSeverity() {
        // Given
        when(baoHongRepository.findByMucDoNghiemTrong(BaoHong.MucDoNghiemTrong.CAO))
                .thenReturn(List.of(baoHong1));

        // When
        List<BaoHong> result = adminBaoHongService.getBaoHongByMucDo(BaoHong.MucDoNghiemTrong.CAO);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMucDoNghiemTrong()).isEqualTo(BaoHong.MucDoNghiemTrong.CAO);
    }
}
