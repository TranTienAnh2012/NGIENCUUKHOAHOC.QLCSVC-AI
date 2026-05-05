package com.Tta.QLCSVC.DHNT.service;

import com.Tta.QLCSVC.DHNT.areas.admin.service.AdminThietBiService;
import com.Tta.QLCSVC.DHNT.entity.LoaiThietBi;
import com.Tta.QLCSVC.DHNT.entity.PhongHoc;
import com.Tta.QLCSVC.DHNT.entity.ThietBi;
import com.Tta.QLCSVC.DHNT.exception.ResourceNotFoundException;
import com.Tta.QLCSVC.DHNT.repository.LoaiThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.PhongHocRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests cho AdminThietBiService
 * Kiểm tra các chức năng quản lý thiết bị
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminThietBiService Tests")
class AdminThietBiServiceTest {

    @Mock
    private ThietBiRepository thietBiRepository;

    @Mock
    private LoaiThietBiRepository loaiThietBiRepository;

    @Mock
    private PhongHocRepository phongHocRepository;

    @InjectMocks
    private AdminThietBiService adminThietBiService;

    private ThietBi thietBi1;
    private ThietBi thietBi2;
    private LoaiThietBi loaiThietBi;
    private PhongHoc phongHoc;

    @BeforeEach
    void setUp() {
        loaiThietBi = new LoaiThietBi();
        loaiThietBi.setId(1L);
        loaiThietBi.setTenLoai("Thiết bị điện tử");

        phongHoc = new PhongHoc();
        phongHoc.setId(1L);
        phongHoc.setTenPhong("A101");

        thietBi1 = new ThietBi();
        thietBi1.setId(1L);
        thietBi1.setMaThietBi("TB001");
        thietBi1.setTenThietBi("Máy chiếu Epson EB-X49");
        thietBi1.setLoaiThietBi(loaiThietBi);
        thietBi1.setPhong(phongHoc);
        thietBi1.setHangSanXuat("Epson");
        thietBi1.setModel("EB-X49");
        thietBi1.setNamSanXuat(2022);
        thietBi1.setNgayMua(LocalDate.of(2022, 6, 15));
        thietBi1.setGiaMua(new BigDecimal("8500000"));
        thietBi1.setTrangThai(ThietBi.TrangThaiThietBi.TOT);

        thietBi2 = new ThietBi();
        thietBi2.setId(2L);
        thietBi2.setMaThietBi("TB002");
        thietBi2.setTenThietBi("Máy tính Dell Optiplex 7090");
        thietBi2.setLoaiThietBi(loaiThietBi);
        thietBi2.setPhong(phongHoc);
        thietBi2.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);
    }

    // ===========================================================
    // TEST: getAllThietBi(Pageable)
    // ===========================================================

    @Test
    @DisplayName("Lấy danh sách thiết bị theo trang - thành công")
    void getAllThietBi_WithPageable_ReturnPage() {
        // Given
        List<ThietBi> thietBiList = Arrays.asList(thietBi1, thietBi2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ThietBi> expectedPage = new PageImpl<>(thietBiList, pageable, 2);
        when(thietBiRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<ThietBi> result = adminThietBiService.getAllThietBi(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTenThietBi()).isEqualTo("Máy chiếu Epson EB-X49");
        verify(thietBiRepository, times(1)).findAll(pageable);
    }

    // ===========================================================
    // TEST: getThietBiById
    // ===========================================================

    @Test
    @DisplayName("Lấy thiết bị theo ID - tìm thấy")
    void getThietBiById_WhenExists_ReturnThietBi() {
        // Given
        when(thietBiRepository.findById(1L)).thenReturn(Optional.of(thietBi1));

        // When
        ThietBi result = adminThietBiService.getThietBiById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMaThietBi()).isEqualTo("TB001");
        assertThat(result.getTenThietBi()).isEqualTo("Máy chiếu Epson EB-X49");
    }

    @Test
    @DisplayName("Lấy thiết bị theo ID - không tìm thấy → ném ResourceNotFoundException")
    void getThietBiById_WhenNotExists_ThrowException() {
        // Given
        when(thietBiRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminThietBiService.getThietBiById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ThietBi")
                .hasMessageContaining("999");
    }

    // ===========================================================
    // TEST: getThietBiByTrangThai
    // ===========================================================

    @Test
    @DisplayName("Lọc thiết bị theo trạng thái TOT")
    void getThietBiByTrangThai_TOT_ReturnAvailableDevices() {
        // Given
        when(thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.TOT))
                .thenReturn(List.of(thietBi1));

        // When
        List<ThietBi> result = adminThietBiService.getThietBiByTrangThai(ThietBi.TrangThaiThietBi.TOT);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrangThai()).isEqualTo(ThietBi.TrangThaiThietBi.TOT);
    }

    @Test
    @DisplayName("Lọc thiết bị đang bảo trì")
    void getThietBiByTrangThai_BAO_TRI_ReturnMaintenanceDevices() {
        // Given
        when(thietBiRepository.findByTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI))
                .thenReturn(List.of(thietBi2));

        // When
        List<ThietBi> result = adminThietBiService.getThietBiByTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaThietBi()).isEqualTo("TB002");
    }

    // ===========================================================
    // TEST: createThietBi
    // ===========================================================

    @Test
    @DisplayName("Tạo thiết bị mới với loại và phòng hợp lệ - thành công")
    void createThietBi_WithValidData_ReturnCreatedThietBi() {
        // Given
        ThietBi newThietBi = new ThietBi();
        newThietBi.setTenThietBi("Màn hình Samsung 24 inch");
        newThietBi.setLoaiThietBi(loaiThietBi);
        newThietBi.setPhong(phongHoc);

        when(loaiThietBiRepository.findById(1L)).thenReturn(Optional.of(loaiThietBi));
        when(phongHocRepository.findById(1L)).thenReturn(Optional.of(phongHoc));

        ThietBi savedThietBi = new ThietBi();
        savedThietBi.setId(3L);
        savedThietBi.setTenThietBi("Màn hình Samsung 24 inch");
        savedThietBi.setTrangThai(ThietBi.TrangThaiThietBi.TOT);
        when(thietBiRepository.save(any(ThietBi.class))).thenReturn(savedThietBi);

        // When
        ThietBi result = adminThietBiService.createThietBi(newThietBi);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        // Verify mã thiết bị được tự động tạo (bắt đầu bằng "TB")
        verify(thietBiRepository).save(argThat(tb -> tb.getMaThietBi() != null && tb.getMaThietBi().startsWith("TB") &&
                tb.getTrangThai() == ThietBi.TrangThaiThietBi.TOT));
    }

    @Test
    @DisplayName("Tạo thiết bị với loại không tồn tại → ném exception")
    void createThietBi_WithInvalidLoaiThietBi_ThrowException() {
        // Given
        ThietBi newThietBi = new ThietBi();
        newThietBi.setTenThietBi("Thiết bị test");
        LoaiThietBi invalidLoai = new LoaiThietBi();
        invalidLoai.setId(999L);
        newThietBi.setLoaiThietBi(invalidLoai);

        when(loaiThietBiRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminThietBiService.createThietBi(newThietBi))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LoaiThietBi");
    }

    // ===========================================================
    // TEST: deleteThietBi
    // ===========================================================

    @Test
    @DisplayName("Xóa thiết bị - thành công")
    void deleteThietBi_WhenExists_DeletesSuccessfully() {
        // Given
        when(thietBiRepository.existsById(1L)).thenReturn(true);

        // When
        adminThietBiService.deleteThietBi(1L);

        // Then
        verify(thietBiRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Xóa thiết bị không tồn tại → ném ResourceNotFoundException")
    void deleteThietBi_WhenNotExists_ThrowException() {
        // Given
        when(thietBiRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> adminThietBiService.deleteThietBi(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ThietBi");

        verify(thietBiRepository, never()).deleteById(any());
    }

    // ===========================================================
    // TEST: updateThietBi
    // ===========================================================

    @Test
    @DisplayName("Cập nhật thiết bị - thành công")
    void updateThietBi_WhenExists_UpdatesFields() {
        // Given
        ThietBi updateDetails = new ThietBi();
        updateDetails.setTenThietBi("Máy chiếu Epson EB-X50 (Cập nhật)");
        updateDetails.setHangSanXuat("Epson");
        updateDetails.setModel("EB-X50");
        updateDetails.setTrangThai(ThietBi.TrangThaiThietBi.BAO_TRI);

        when(thietBiRepository.findById(1L)).thenReturn(Optional.of(thietBi1));
        when(thietBiRepository.save(any(ThietBi.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ThietBi result = adminThietBiService.updateThietBi(1L, updateDetails);

        // Then
        assertThat(result.getTenThietBi()).isEqualTo("Máy chiếu Epson EB-X50 (Cập nhật)");
        assertThat(result.getModel()).isEqualTo("EB-X50");
        assertThat(result.getTrangThai()).isEqualTo(ThietBi.TrangThaiThietBi.BAO_TRI);
    }
}
