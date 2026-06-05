package com.Tta.QLCSVC.DHNT.areas.admin.service;

import com.Tta.QLCSVC.DHNT.repository.BaoHongRepository;
import com.Tta.QLCSVC.DHNT.repository.BaoTriRepository;
import com.Tta.QLCSVC.DHNT.repository.MuonTraThietBiRepository;
import com.Tta.QLCSVC.DHNT.repository.ThietBiRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminThongKeService {

    private final EntityManager entityManager;
    private final BaoHongRepository baoHongRepository;
    private final BaoTriRepository baoTriRepository;
    private final ThietBiRepository thietBiRepository;
    private final MuonTraThietBiRepository muonTraThietBiRepository;

    public Map<String, Object> getThongKeTongQuan() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Thống kê Báo hỏng (Day/Week/Month) - Pushed to DB via JPQL
        // We use native queries for simple date filtering due to DB dialect differences, or JPQL if using standard dates.
        // Assuming MySQL/H2, standard JPQL works with dates in Spring Data but native is sometimes easier for exact intervals.
        String qBaoHongDay = "SELECT COUNT(b) FROM BaoHong b WHERE DATE(b.ngayBao) = CURRENT_DATE";
        String qBaoHongWeek = "SELECT COUNT(b) FROM BaoHong b WHERE YEARWEEK(b.ngayBao, 1) = YEARWEEK(CURRENT_DATE, 1)";
        String qBaoHongMonth = "SELECT COUNT(b) FROM BaoHong b WHERE MONTH(b.ngayBao) = MONTH(CURRENT_DATE) AND YEAR(b.ngayBao) = YEAR(CURRENT_DATE)";

        Long baoHongDay = safeCountQuery(qBaoHongDay);
        Long baoHongWeek = safeCountQuery(qBaoHongWeek);
        Long baoHongMonth = safeCountQuery(qBaoHongMonth);
        stats.put("baoHongDay", baoHongDay);
        stats.put("baoHongWeek", baoHongWeek);
        stats.put("baoHongMonth", baoHongMonth);

        // Chi tiết Báo hỏng (Hôm nay)
        String qChiTietBaoHongDay = "SELECT b.id, tb.tenThietBi, nd.hoTen, b.ngayBao, b.moTaLoi " +
                                    "FROM BaoHong b JOIN b.thietBi tb LEFT JOIN b.nguoiBao nd " +
                                    "WHERE DATE(b.ngayBao) = CURRENT_DATE ORDER BY b.ngayBao DESC";
        stats.put("chiTietBaoHongDay", entityManager.createQuery(qChiTietBaoHongDay).getResultList());

        // Chi tiết Báo hỏng (Tuần này)
        String qChiTietBaoHongWeek = "SELECT b.id, tb.tenThietBi, nd.hoTen, b.ngayBao, b.moTaLoi " +
                                     "FROM BaoHong b JOIN b.thietBi tb LEFT JOIN b.nguoiBao nd " +
                                     "WHERE YEARWEEK(b.ngayBao, 1) = YEARWEEK(CURRENT_DATE, 1) ORDER BY b.ngayBao DESC";
        stats.put("chiTietBaoHongWeek", entityManager.createQuery(qChiTietBaoHongWeek).getResultList());

        // 2. Thống kê thiết bị đang được mượn
        String qMuonTraActive = "SELECT COUNT(m) FROM MuonTraThietBi m WHERE m.trangThai = 'DANG_MUON'";
        stats.put("dangMuon", safeCountQuery(qMuonTraActive));
        
        // Chi tiết thiết bị đang mượn
        String qChiTietDangMuon = "SELECT m.id, tb.tenThietBi, m.ngayMuon, m.ngayTraDuKien, m.nguoiMuon.hoTen " +
                                  "FROM MuonTraThietBi m JOIN m.thietBi tb LEFT JOIN m.nguoiMuon nd " +
                                  "WHERE m.trangThai = 'DANG_MUON' ORDER BY m.ngayMuon DESC";
        Query tDangMuonQ = entityManager.createQuery(qChiTietDangMuon);
        stats.put("chiTietDangMuon", tDangMuonQ.getResultList());

        // 3. Tổng chi phí bảo trì trong tháng
        String qChiPhiMonth = "SELECT SUM(bt.chiPhi) FROM BaoTri bt WHERE MONTH(bt.ngayBaoTri) = MONTH(CURRENT_DATE) AND YEAR(bt.ngayBaoTri) = YEAR(CURRENT_DATE)";
        stats.put("chiPhiMonth", safeSumQuery(qChiPhiMonth));
        
        // Chi tiết chi phí tháng
        String qChiTietMonth = "SELECT bt.id, tb.tenThietBi, bt.ngayBaoTri, bt.chiPhi, nd.hoTen " +
                               "FROM BaoTri bt JOIN bt.thietBi tb LEFT JOIN bt.nguoiThucHien nd " +
                               "WHERE MONTH(bt.ngayBaoTri) = MONTH(CURRENT_DATE) AND YEAR(bt.ngayBaoTri) = YEAR(CURRENT_DATE) AND bt.chiPhi > 0 " +
                               "ORDER BY bt.ngayBaoTri DESC";
        Query tMonthQ = entityManager.createQuery(qChiTietMonth);
        stats.put("chiTietMonth", tMonthQ.getResultList());

        // 4. Tổng chi phí bảo trì trong năm
        String qChiPhiYear = "SELECT SUM(bt.chiPhi) FROM BaoTri bt WHERE YEAR(bt.ngayBaoTri) = YEAR(CURRENT_DATE)";
        stats.put("chiPhiYear", safeSumQuery(qChiPhiYear));
        
        // Chi tiết chi phí năm
        String qChiTietYear = "SELECT bt.id, tb.tenThietBi, bt.ngayBaoTri, bt.chiPhi, nd.hoTen " +
                              "FROM BaoTri bt JOIN bt.thietBi tb LEFT JOIN bt.nguoiThucHien nd " +
                              "WHERE YEAR(bt.ngayBaoTri) = YEAR(CURRENT_DATE) AND bt.chiPhi > 0 " +
                              "ORDER BY bt.ngayBaoTri DESC";
        Query tYearQ = entityManager.createQuery(qChiTietYear);
        stats.put("chiTietYear", tYearQ.getResultList());

        // 4. Top 5 thiết bị hay hỏng nhất (TRONG THÁNG NÀY)
        String qTopHieuSuat = "SELECT b.thietBi.tenThietBi, COUNT(b.id) FROM BaoHong b " +
                              "WHERE MONTH(b.ngayBao) = MONTH(CURRENT_DATE) AND YEAR(b.ngayBao) = YEAR(CURRENT_DATE) " +
                              "GROUP BY b.thietBi.id, b.thietBi.tenThietBi ORDER BY COUNT(b.id) DESC";
        Query topQuery = entityManager.createQuery(qTopHieuSuat);
        topQuery.setMaxResults(5);
        List<Object[]> topHieuSuat = topQuery.getResultList();
        
        List<Map<String, Object>> topThietBi = new ArrayList<>();
        for (Object[] row : topHieuSuat) {
            Map<String, Object> map = new HashMap<>();
            map.put("tenThietBi", row[0]);
            map.put("soLanHong", row[1]);
            topThietBi.add(map);
        }
        stats.put("topThietBiHayHong", topThietBi);

        // 5. Hiệu suất nhân viên (Ai xử lý nhiều phiếu bảo trì nhất TRONG THÁNG NÀY)
        String qStaffKpi = "SELECT b.nguoiThucHien.hoTen, COUNT(b.id) FROM BaoTri b " +
                "WHERE b.nguoiThucHien IS NOT NULL AND b.ketQua = 'THANH_CONG' " +
                "AND MONTH(b.ngayBaoTri) = MONTH(CURRENT_DATE) AND YEAR(b.ngayBaoTri) = YEAR(CURRENT_DATE) " +
                "GROUP BY b.nguoiThucHien.id, b.nguoiThucHien.hoTen " +
                "ORDER BY COUNT(b.id) DESC";
        Query kpiQuery = entityManager.createQuery(qStaffKpi);
        kpiQuery.setMaxResults(5);
        List<Object[]> kpiList = kpiQuery.getResultList();
        
        List<Map<String, Object>> topNhanVien = new ArrayList<>();
        for (Object[] row : kpiList) {
            Map<String, Object> map = new HashMap<>();
            map.put("hoTen", row[0]);
            map.put("soPhieuHoanThanh", row[1]);
            topNhanVien.add(map);
        }
        stats.put("topNhanVien", topNhanVien);

        // 6. Dữ liệu biểu đồ (12 tháng gần nhất)
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartDataBaoHong = new ArrayList<>();
        List<Long> chartDataBaoTri = new ArrayList<>();
        List<Long> chartDataMuon = new ArrayList<>();
        List<Long> chartDataTra = new ArrayList<>();
        List<Long> chartDataTreHan = new ArrayList<>();
        
        for (int i = 11; i >= 0; i--) {
            // Lùi i tháng
            java.time.LocalDate targetDate = java.time.LocalDate.now().minusMonths(i);
            int targetMonth = targetDate.getMonthValue();
            int targetYear = targetDate.getYear();
            
            chartLabels.add("Tháng " + targetMonth + "/" + targetYear);
            
            String queryBaoHong = "SELECT COUNT(b) FROM BaoHong b WHERE MONTH(b.ngayBao) = " + targetMonth + " AND YEAR(b.ngayBao) = " + targetYear;
            chartDataBaoHong.add(safeCountQuery(queryBaoHong));

            String queryBaoTri = "SELECT COUNT(bt) FROM BaoTri bt WHERE MONTH(bt.ngayBaoTri) = " + targetMonth + " AND YEAR(bt.ngayBaoTri) = " + targetYear + " AND bt.ketQua = 'THANH_CONG'";
            chartDataBaoTri.add(safeCountQuery(queryBaoTri));
            
            String queryMuon = "SELECT COUNT(m) FROM MuonTraThietBi m WHERE MONTH(m.ngayMuon) = " + targetMonth + " AND YEAR(m.ngayMuon) = " + targetYear;
            chartDataMuon.add(safeCountQuery(queryMuon));
            
            String queryTra = "SELECT COUNT(m) FROM MuonTraThietBi m WHERE m.trangThai = 'DA_TRA' AND MONTH(m.ngayTraThucTe) = " + targetMonth + " AND YEAR(m.ngayTraThucTe) = " + targetYear;
            chartDataTra.add(safeCountQuery(queryTra));
            
            String queryTreHan = "SELECT COUNT(m) FROM MuonTraThietBi m WHERE m.trangThai = 'QUA_HAN' AND MONTH(m.ngayTraDuKien) = " + targetMonth + " AND YEAR(m.ngayTraDuKien) = " + targetYear;
            chartDataTreHan.add(safeCountQuery(queryTreHan));
        }
        stats.put("chartLabels", chartLabels);
        stats.put("chartData", chartDataBaoHong);
        stats.put("chartDataBaoTri", chartDataBaoTri);
        stats.put("chartDataMuon", chartDataMuon);
        stats.put("chartDataTra", chartDataTra);
        stats.put("chartDataTreHan", chartDataTreHan);

        return stats;
    }

    private Long safeCountQuery(String queryStr) {
        try {
            Object result = entityManager.createQuery(queryStr).getSingleResult();
            return result != null ? ((Number) result).longValue() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private BigDecimal safeSumQuery(String queryStr) {
        try {
            Object result = entityManager.createQuery(queryStr).getSingleResult();
            return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
