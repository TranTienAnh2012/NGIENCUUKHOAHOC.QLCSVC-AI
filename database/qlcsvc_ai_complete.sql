-- =============================================
-- HỆ THỐNG QUẢN LÝ CƠ SỞ VẬT CHẤT THÔNG MINH - QLCSVC-AI
-- Trường Đại học Nguyễn Trãi
-- Database: qlcsvc_ai
-- Version: 1.0.0
-- Created: 2026-01-11
-- =============================================

-- Tạo database
DROP DATABASE IF EXISTS qlcsvc_ai;
CREATE DATABASE qlcsvc_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qlcsvc_ai;

-- =============================================
-- PHẦN 1: BẢNG CORE (Hệ thống cơ bản)
-- =============================================

-- Bảng người dùng
CREATE TABLE nguoi_dung (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ho_ten VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mat_khau VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    so_dien_thoai VARCHAR(20),
    vai_tro ENUM('ADMIN', 'GIAO_VIEN', 'NHAN_VIEN_CSVC') NOT NULL DEFAULT 'GIAO_VIEN',
    trang_thai ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_vai_tro (vai_tro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý người dùng hệ thống';

-- Bảng phòng học
CREATE TABLE phong_hoc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ma_phong VARCHAR(20) UNIQUE NOT NULL COMMENT 'Mã phòng: P301, PTN101...',
    ten_phong VARCHAR(100) NOT NULL,
    toa_nha VARCHAR(50),
    tang INT,
    suc_chua INT COMMENT 'Số lượng sinh viên tối đa',
    loai_phong ENUM('LY_THUYET', 'THI_NGHIEM', 'MAY_TINH') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ma_phong (ma_phong),
    INDEX idx_loai_phong (loai_phong)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý phòng học/phòng thí nghiệm';

-- Bảng loại thiết bị
CREATE TABLE loai_thiet_bi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ten_loai VARCHAR(100) NOT NULL,
    mo_ta TEXT,
    thoi_gian_bao_hanh_mac_dinh INT COMMENT 'Thời gian bảo hành mặc định (tháng)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phân loại thiết bị';

-- Bảng thiết bị
CREATE TABLE thiet_bi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ma_thiet_bi VARCHAR(50) UNIQUE NOT NULL COMMENT 'Mã QR code',
    ten_thiet_bi VARCHAR(200) NOT NULL,
    loai_thiet_bi_id BIGINT NOT NULL,
    phong_id BIGINT,
    hang_san_xuat VARCHAR(100),
    model VARCHAR(100),
    nam_san_xuat INT,
    ngay_mua DATE,
    gia_mua DECIMAL(15,2),
    trang_thai ENUM('TOT', 'BAO_TRI', 'HONG', 'THANH_LY') DEFAULT 'TOT',
    ghi_chu TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loai_thiet_bi_id) REFERENCES loai_thiet_bi(id),
    FOREIGN KEY (phong_id) REFERENCES phong_hoc(id) ON DELETE SET NULL,
    
    INDEX idx_ma_thiet_bi (ma_thiet_bi),
    INDEX idx_loai_trang_thai (loai_thiet_bi_id, trang_thai),
    INDEX idx_phong (phong_id),
    INDEX idx_trang_thai (trang_thai),
    FULLTEXT INDEX ft_ten_thiet_bi (ten_thiet_bi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý thiết bị chi tiết';

-- =============================================
-- PHẦN 2: BẢNG GIAO DỊCH (Transaction Tables)
-- =============================================

-- Bảng mượn/trả thiết bị
CREATE TABLE muon_tra_thiet_bi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    nguoi_muon_id BIGINT NOT NULL,
    ngay_muon DATETIME NOT NULL,
    ngay_tra_du_kien DATETIME NULL,
    ngay_tra_thuc_te DATETIME NULL,
    trang_thai ENUM('DANG_MUON', 'DA_TRA', 'QUA_HAN') DEFAULT 'DANG_MUON',
    ghi_chu TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    FOREIGN KEY (nguoi_muon_id) REFERENCES nguoi_dung(id),
    
    INDEX idx_thiet_bi_trang_thai (thiet_bi_id, trang_thai),
    INDEX idx_nguoi_muon (nguoi_muon_id),
    INDEX idx_ngay_muon (ngay_muon DESC),
    INDEX idx_trang_thai (trang_thai)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử mượn/trả thiết bị';

-- Bảng báo hỏng
CREATE TABLE bao_hong (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    nguoi_bao_id BIGINT NOT NULL,
    ngay_bao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mo_ta_loi TEXT,
    muc_do_nghiem_trong ENUM('THAP', 'TRUNG_BINH', 'CAO', 'KHAN_CAP') DEFAULT 'TRUNG_BINH',
    trang_thai ENUM('CHO_XU_LY', 'DANG_XU_LY', 'HOAN_THANH', 'HUY') DEFAULT 'CHO_XU_LY',
    hinh_anh_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    FOREIGN KEY (nguoi_bao_id) REFERENCES nguoi_dung(id),
    
    INDEX idx_thiet_bi_trang_thai (thiet_bi_id, trang_thai),
    INDEX idx_muc_do (muc_do_nghiem_trong),
    INDEX idx_ngay_bao (ngay_bao DESC),
    INDEX idx_trang_thai (trang_thai),
    FULLTEXT INDEX ft_mo_ta_loi (mo_ta_loi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phiếu báo hỏng thiết bị';

-- Bảng bảo trì
CREATE TABLE bao_tri (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    bao_hong_id BIGINT,
    loai_bao_tri ENUM('DINH_KY', 'SUA_CHUA', 'PHONG_NGUA') NOT NULL,
    ngay_bao_tri DATE NOT NULL,
    noi_dung TEXT,
    chi_phi DECIMAL(15,2),
    nguoi_thuc_hien VARCHAR(100),
    ket_qua ENUM('THANH_CONG', 'THAT_BAI', 'CAN_THAY_THE'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    FOREIGN KEY (bao_hong_id) REFERENCES bao_hong(id) ON DELETE SET NULL,
    
    INDEX idx_thiet_bi (thiet_bi_id),
    INDEX idx_ngay_bao_tri (ngay_bao_tri DESC),
    INDEX idx_loai (loai_bao_tri)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử bảo trì/sửa chữa';

-- =============================================
-- PHẦN 3: BẢNG AI/ML
-- =============================================

-- Bảng dự đoán bảo trì (Predictive Maintenance)
CREATE TABLE ai_du_doan_bao_tri (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    ngay_du_doan DATE NOT NULL,
    xac_suat_hong DECIMAL(5,2) COMMENT 'Xác suất 0.00 - 1.00',
    muc_do_rui_ro ENUM('THAP', 'TRUNG_BINH', 'CAO', 'NGUY_HIEM'),
    ngay_du_kien_hong DATE,
    chi_phi_uoc_tinh DECIMAL(15,2),
    hanh_dong_de_xuat TEXT,
    do_tin_cay DECIMAL(5,2) COMMENT 'Độ tin cậy 0.00 - 1.00',
    phien_ban_model VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    
    INDEX idx_thiet_bi_ngay (thiet_bi_id, ngay_du_doan DESC),
    INDEX idx_rui_ro (muc_do_rui_ro),
    INDEX idx_ngay_du_kien (ngay_du_kien_hong)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kết quả dự đoán từ AI';

-- Bảng dữ liệu huấn luyện AI
CREATE TABLE ai_du_lieu_huan_luyen (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    features JSON NOT NULL COMMENT 'Features đã được tính toán',
    label VARCHAR(50) COMMENT 'Nhãn: 0/1 hoặc TOT/HONG',
    loai_du_lieu ENUM('TRAIN', 'TEST', 'VALIDATION') DEFAULT 'TRAIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    
    INDEX idx_loai_du_lieu (loai_du_lieu),
    INDEX idx_thiet_bi (thiet_bi_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dữ liệu training cho ML model';

-- Bảng metrics của AI model
CREATE TABLE ai_model_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ten_model VARCHAR(100) NOT NULL,
    phien_ban VARCHAR(50) NOT NULL,
    loai_metric VARCHAR(50) COMMENT 'accuracy, precision, recall, f1, etc.',
    gia_tri DECIMAL(10,4),
    ngay_danh_gia DATE,
    ghi_chu TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_model_version (ten_model, phien_ban),
    INDEX idx_ngay (ngay_danh_gia DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Theo dõi hiệu suất AI models';

-- Bảng hội thoại chatbot (NLP)
CREATE TABLE chatbot_hoi_thoai (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nguoi_dung_id BIGINT,
    session_id VARCHAR(100),
    tin_nhan TEXT NOT NULL,
    intent VARCHAR(100),
    entities JSON,
    phan_hoi TEXT,
    do_tin_cay DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL,
    
    INDEX idx_session (session_id),
    INDEX idx_intent (intent),
    INDEX idx_ngay_tao (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử hội thoại với chatbot';

-- Bảng hình ảnh thiết bị (Computer Vision)
CREATE TABLE hinh_anh_thiet_bi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    thiet_bi_id BIGINT NOT NULL,
    url_hinh_anh VARCHAR(500) NOT NULL,
    loai_hinh_anh ENUM('QR_CODE', 'KIEM_TRA_TINH_TRANG', 'BAO_HONG', 'MUON_TRA'),
    danh_gia_ai JSON COMMENT 'Kết quả phân tích từ CV model',
    nguoi_chup_id BIGINT,
    ngay_chup TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (thiet_bi_id) REFERENCES thiet_bi(id),
    FOREIGN KEY (nguoi_chup_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL,
    
    INDEX idx_thiet_bi (thiet_bi_id),
    INDEX idx_loai (loai_hinh_anh),
    INDEX idx_ngay_chup (ngay_chup DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lưu trữ hình ảnh và kết quả CV';

-- =============================================
-- PHẦN 4: BẢNG IOT (Optional)
-- =============================================

-- Bảng dữ liệu cảm biến IoT
CREATE TABLE iot_du_lieu_cam_bien (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phong_id BIGINT NOT NULL,
    nhiet_do DECIMAL(5,2) COMMENT 'Nhiệt độ (°C)',
    do_am DECIMAL(5,2) COMMENT 'Độ ẩm (%)',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (phong_id) REFERENCES phong_hoc(id),
    
    INDEX idx_phong_timestamp (phong_id, timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dữ liệu từ cảm biến IoT';

-- =============================================
-- PHẦN 5: SAMPLE DATA (Dữ liệu mẫu)
-- =============================================

-- Người dùng mẫu
INSERT INTO nguoi_dung (ho_ten, email, mat_khau, so_dien_thoai, vai_tro) VALUES
('Admin Hệ thống', 'admin@dhnt.edu.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi', '0123456789', 'ADMIN'),
('Nguyễn Văn An', 'nguyenvanan@dhnt.edu.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi', '0987654321', 'GIAO_VIEN'),
('Trần Thị Bình', 'tranthib@dhnt.edu.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi', '0912345678', 'NHAN_VIEN_CSVC'),
('Lê Văn Cường', 'levancuong@dhnt.edu.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi', '0909123456', 'GIAO_VIEN'),
('Phạm Thị Dung', 'phamthidung@dhnt.edu.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi', '0898765432', 'GIAO_VIEN');

-- Phòng học mẫu
INSERT INTO phong_hoc (ma_phong, ten_phong, toa_nha, tang, suc_chua, loai_phong) VALUES
('P301', 'Phòng 301', 'Tòa A', 3, 60, 'LY_THUYET'),
('P302', 'Phòng 302', 'Tòa A', 3, 60, 'LY_THUYET'),
('P205', 'Phòng máy tính 205', 'Tòa B', 2, 40, 'MAY_TINH'),
('P206', 'Phòng máy tính 206', 'Tòa B', 2, 40, 'MAY_TINH'),
('PTN101', 'Phòng thí nghiệm Vật lý', 'Tòa C', 1, 30, 'THI_NGHIEM'),
('PTN102', 'Phòng thí nghiệm Hóa học', 'Tòa C', 1, 30, 'THI_NGHIEM'),
('P401', 'Hội trường A', 'Tòa A', 4, 200, 'LY_THUYET');

-- Loại thiết bị mẫu
INSERT INTO loai_thiet_bi (ten_loai, mo_ta, thoi_gian_bao_hanh_mac_dinh) VALUES
('Máy chiếu', 'Máy chiếu đa năng phục vụ giảng dạy', 24),
('Laptop', 'Laptop phục vụ giảng dạy và học tập', 36),
('Loa', 'Loa hội trường và phòng học', 12),
('Micro', 'Micro không dây', 12),
('Bảng tương tác', 'Bảng tương tác thông minh', 24),
('Máy tính để bàn', 'PC phòng máy tính', 36),
('Máy in', 'Máy in văn phòng', 12);

-- Thiết bị mẫu
INSERT INTO thiet_bi (ma_thiet_bi, ten_thiet_bi, loai_thiet_bi_id, phong_id, hang_san_xuat, model, nam_san_xuat, ngay_mua, gia_mua, trang_thai) VALUES
('TB001', 'Máy chiếu Epson EB-X05', 1, 1, 'Epson', 'EB-X05', 2023, '2023-01-15', 8500000, 'TOT'),
('TB002', 'Máy chiếu BenQ MH535A', 1, 2, 'BenQ', 'MH535A', 2023, '2023-02-20', 9200000, 'TOT'),
('TB003', 'Laptop Dell Latitude 5420', 2, 3, 'Dell', 'Latitude 5420', 2022, '2022-06-20', 18000000, 'TOT'),
('TB004', 'Laptop HP ProBook 450 G8', 2, 3, 'HP', 'ProBook 450 G8', 2022, '2022-07-15', 17500000, 'TOT'),
('TB005', 'Loa JBL EON615', 3, 7, 'JBL', 'EON615', 2021, '2021-03-10', 12000000, 'BAO_TRI'),
('TB006', 'Micro Shure SM58', 4, 7, 'Shure', 'SM58', 2022, '2022-04-05', 3500000, 'TOT'),
('TB007', 'Bảng tương tác Samsung Flip 2', 5, 1, 'Samsung', 'Flip 2 WM55R', 2023, '2023-05-10', 45000000, 'TOT'),
('TB008', 'PC Dell OptiPlex 7090', 6, 3, 'Dell', 'OptiPlex 7090', 2023, '2023-08-01', 15000000, 'TOT'),
('TB009', 'PC HP EliteDesk 800 G8', 6, 4, 'HP', 'EliteDesk 800 G8', 2023, '2023-08-01', 14500000, 'TOT'),
('TB010', 'Máy in HP LaserJet Pro M404dn', 7, 3, 'HP', 'LaserJet Pro M404dn', 2022, '2022-09-15', 6500000, 'HONG');

-- Mượn/trả mẫu
INSERT INTO muon_tra_thiet_bi (thiet_bi_id, nguoi_muon_id, ngay_muon, ngay_tra_du_kien, ngay_tra_thuc_te, trang_thai) VALUES
(1, 2, '2026-01-10 08:00:00', '2026-01-10 12:00:00', '2026-01-10 11:45:00', 'DA_TRA'),
(3, 4, '2026-01-11 14:00:00', '2026-01-11 16:00:00', NULL, 'DANG_MUON'),
(2, 5, '2026-01-09 09:00:00', '2026-01-09 11:00:00', '2026-01-09 11:30:00', 'DA_TRA'),
(6, 2, '2026-01-08 13:00:00', '2026-01-08 15:00:00', '2026-01-08 15:20:00', 'DA_TRA');

-- Báo hỏng mẫu
INSERT INTO bao_hong (thiet_bi_id, nguoi_bao_id, mo_ta_loi, muc_do_nghiem_trong, trang_thai) VALUES
(5, 2, 'Loa bị rè, không phát ra âm thanh rõ ràng. Có tiếng kêu lạ khi bật nguồn.', 'CAO', 'DANG_XU_LY'),
(10, 4, 'Máy in không nhận lệnh in từ máy tính. Đèn báo lỗi nhấp nháy.', 'KHAN_CAP', 'CHO_XU_LY'),
(3, 5, 'Laptop bị nóng máy, quạt kêu to bất thường.', 'TRUNG_BINH', 'HOAN_THANH');

-- Bảo trì mẫu
INSERT INTO bao_tri (thiet_bi_id, bao_hong_id, loai_bao_tri, ngay_bao_tri, noi_dung, chi_phi, nguoi_thuc_hien, ket_qua) VALUES
(5, 1, 'SUA_CHUA', '2026-01-11', 'Thay loa bass mới, kiểm tra mạch khuếch đại', 1500000, 'Nguyễn Văn Tèo', 'THANH_CONG'),
(3, 3, 'SUA_CHUA', '2026-01-10', 'Vệ sinh quạt tản nhiệt, thay keo tản nhiệt', 500000, 'Trần Văn Tý', 'THANH_CONG'),
(1, NULL, 'DINH_KY', '2026-01-05', 'Vệ sinh ống kính, kiểm tra bóng đèn', 300000, 'Lê Văn Tèo', 'THANH_CONG');

-- AI Dự đoán mẫu
INSERT INTO ai_du_doan_bao_tri (thiet_bi_id, ngay_du_doan, xac_suat_hong, muc_do_rui_ro, ngay_du_kien_hong, chi_phi_uoc_tinh, hanh_dong_de_xuat, do_tin_cay, phien_ban_model) VALUES
(1, '2026-01-11', 0.78, 'CAO', '2026-03-15', 1500000, 'Nên bảo trì phòng ngừa trong tháng 2/2026. Kiểm tra bóng đèn và hệ thống làm mát.', 0.85, 'v1.0.0'),
(2, '2026-01-11', 0.45, 'TRUNG_BINH', '2026-05-20', 1200000, 'Theo dõi thêm 2 tháng, nếu có dấu hiệu bất thường thì bảo trì.', 0.72, 'v1.0.0'),
(8, '2026-01-11', 0.25, 'THAP', '2026-08-10', 800000, 'Thiết bị hoạt động tốt, bảo trì định kỳ theo lịch.', 0.90, 'v1.0.0');

-- AI Training data mẫu
INSERT INTO ai_du_lieu_huan_luyen (thiet_bi_id, features, label, loai_du_lieu) VALUES
(1, '{"device_age_months": 12, "failure_count_12m": 0, "repair_cost_avg": 300000, "usage_frequency_per_month": 15.5, "days_since_last_maintenance": 30}', '0', 'TRAIN'),
(5, '{"device_age_months": 34, "failure_count_12m": 2, "repair_cost_avg": 1200000, "usage_frequency_per_month": 8.2, "days_since_last_maintenance": 90}', '1', 'TRAIN'),
(10, '{"device_age_months": 16, "failure_count_12m": 1, "repair_cost_avg": 500000, "usage_frequency_per_month": 20.0, "days_since_last_maintenance": 120}', '1', 'TEST');

-- AI Model metrics mẫu
INSERT INTO ai_model_metrics (ten_model, phien_ban, loai_metric, gia_tri, ngay_danh_gia, ghi_chu) VALUES
('predictive_maintenance', 'v1.0.0', 'accuracy', 0.8542, '2026-01-11', 'Model đầu tiên, baseline'),
('predictive_maintenance', 'v1.0.0', 'precision', 0.8234, '2026-01-11', 'Precision cho class HONG'),
('predictive_maintenance', 'v1.0.0', 'recall', 0.7891, '2026-01-11', 'Recall cho class HONG'),
('predictive_maintenance', 'v1.0.0', 'f1_score', 0.8058, '2026-01-11', 'F1 score tổng thể');

-- Chatbot conversation mẫu
INSERT INTO chatbot_hoi_thoai (nguoi_dung_id, session_id, tin_nhan, intent, entities, phan_hoi, do_tin_cay) VALUES
(2, 'sess_20260111_001', 'Phòng 301 còn máy chiếu không?', 'check_availability', 
 '{"equipment_type": "máy chiếu", "room_number": "301"}',
 'Có 1 máy chiếu khả dụng tại phòng 301: Máy chiếu Epson EB-X05 (TB001)', 0.95),
(4, 'sess_20260111_002', 'Tôi muốn mượn laptop ngày mai', 'book_equipment',
 '{"equipment_type": "laptop", "date": "2026-01-12"}',
 'Hiện có 2 laptop khả dụng. Bạn muốn mượn vào khung giờ nào?', 0.88),
(5, 'sess_20260111_003', 'Máy in phòng 205 bị lỗi', 'report_damage',
 '{"equipment_type": "máy in", "room_number": "205"}',
 'Tôi đã ghi nhận báo hỏng. Nhân viên CSVC sẽ kiểm tra trong vòng 24h.', 0.92);

-- Hình ảnh thiết bị mẫu
INSERT INTO hinh_anh_thiet_bi (thiet_bi_id, url_hinh_anh, loai_hinh_anh, danh_gia_ai, nguoi_chup_id) VALUES
(1, '/images/equipment/TB001_qr.jpg', 'QR_CODE', NULL, 3),
(5, '/images/equipment/TB005_damage.jpg', 'BAO_HONG', 
 '{"condition": "BAO_TRI", "confidence": 0.92, "damages": [{"type": "SCRATCH", "severity": "MEDIUM", "location": "speaker_grill"}]}', 2),
(3, '/images/equipment/TB003_check.jpg', 'KIEM_TRA_TINH_TRANG',
 '{"condition": "TOT", "confidence": 0.95, "damages": []}', 4);

-- IoT sensor data mẫu
INSERT INTO iot_du_lieu_cam_bien (phong_id, nhiet_do, do_am, timestamp) VALUES
(3, 24.5, 65.2, '2026-01-11 08:00:00'),
(3, 25.1, 64.8, '2026-01-11 09:00:00'),
(3, 26.3, 63.5, '2026-01-11 10:00:00'),
(4, 23.8, 66.1, '2026-01-11 08:00:00'),
(5, 22.5, 58.3, '2026-01-11 08:00:00');

-- =============================================
-- PHẦN 6: VIEWS (Các view hữu ích)
-- =============================================

-- View: Thiết bị cần bảo trì gấp
CREATE VIEW v_thiet_bi_can_bao_tri AS
SELECT 
    tb.id,
    tb.ma_thiet_bi,
    tb.ten_thiet_bi,
    ph.ma_phong,
    ai.xac_suat_hong,
    ai.muc_do_rui_ro,
    ai.ngay_du_kien_hong,
    ai.hanh_dong_de_xuat
FROM thiet_bi tb
JOIN ai_du_doan_bao_tri ai ON tb.id = ai.thiet_bi_id
LEFT JOIN phong_hoc ph ON tb.phong_id = ph.id
WHERE ai.muc_do_rui_ro IN ('CAO', 'NGUY_HIEM')
  AND ai.ngay_du_doan = (
      SELECT MAX(ngay_du_doan) 
      FROM ai_du_doan_bao_tri 
      WHERE thiet_bi_id = tb.id
  )
ORDER BY ai.xac_suat_hong DESC;

-- View: Thống kê thiết bị theo trạng thái
CREATE VIEW v_thong_ke_thiet_bi AS
SELECT 
    ltb.ten_loai,
    COUNT(*) as tong_so,
    SUM(CASE WHEN tb.trang_thai = 'TOT' THEN 1 ELSE 0 END) as so_tot,
    SUM(CASE WHEN tb.trang_thai = 'BAO_TRI' THEN 1 ELSE 0 END) as so_bao_tri,
    SUM(CASE WHEN tb.trang_thai = 'HONG' THEN 1 ELSE 0 END) as so_hong,
    SUM(CASE WHEN tb.trang_thai = 'THANH_LY' THEN 1 ELSE 0 END) as so_thanh_ly
FROM thiet_bi tb
JOIN loai_thiet_bi ltb ON tb.loai_thiet_bi_id = ltb.id
GROUP BY ltb.id, ltb.ten_loai;

-- View: Lịch sử mượn trả chi tiết
CREATE VIEW v_lich_su_muon_tra AS
SELECT 
    mt.id,
    nd.ho_ten as nguoi_muon,
    tb.ma_thiet_bi,
    tb.ten_thiet_bi,
    ph.ma_phong,
    mt.ngay_muon,
    mt.ngay_tra_du_kien,
    mt.ngay_tra_thuc_te,
    mt.trang_thai,
    CASE 
        WHEN mt.trang_thai = 'DA_TRA' THEN TIMESTAMPDIFF(HOUR, mt.ngay_muon, mt.ngay_tra_thuc_te)
        ELSE NULL
    END as gio_muon
FROM muon_tra_thiet_bi mt
JOIN nguoi_dung nd ON mt.nguoi_muon_id = nd.id
JOIN thiet_bi tb ON mt.thiet_bi_id = tb.id
LEFT JOIN phong_hoc ph ON tb.phong_id = ph.id
ORDER BY mt.ngay_muon DESC;

-- =============================================
-- PHẦN 7: STORED PROCEDURES
-- =============================================

DELIMITER //

-- Procedure: Tính features cho một thiết bị
CREATE PROCEDURE sp_calculate_features(IN p_thiet_bi_id BIGINT)
BEGIN
    SELECT 
        tb.id as thiet_bi_id,
        TIMESTAMPDIFF(MONTH, tb.ngay_mua, NOW()) as device_age_months,
        
        (SELECT COUNT(*) 
         FROM bao_hong bh 
         WHERE bh.thiet_bi_id = tb.id 
           AND bh.ngay_bao >= DATE_SUB(NOW(), INTERVAL 12 MONTH)) as failure_count_12m,
        
        (SELECT COALESCE(AVG(bt.chi_phi), 0)
         FROM bao_tri bt 
         WHERE bt.thiet_bi_id = tb.id) as repair_cost_avg,
        
        (SELECT COUNT(*) / 12.0
         FROM muon_tra_thiet_bi mt
         WHERE mt.thiet_bi_id = tb.id
           AND mt.ngay_muon >= DATE_SUB(NOW(), INTERVAL 12 MONTH)) as usage_frequency_per_month,
        
        (SELECT COALESCE(DATEDIFF(NOW(), MAX(bt.ngay_bao_tri)), 999)
         FROM bao_tri bt
         WHERE bt.thiet_bi_id = tb.id) as days_since_last_maintenance,
        
        tb.loai_thiet_bi_id as device_category_encoded,
        COALESCE(tb.gia_mua / 1000000.0, 0) as price_in_millions
        
    FROM thiet_bi tb
    WHERE tb.id = p_thiet_bi_id;
END //

-- Procedure: Kiểm tra thiết bị khả dụng
CREATE PROCEDURE sp_check_availability(
    IN p_loai_thiet_bi VARCHAR(100),
    IN p_ma_phong VARCHAR(20),
    IN p_ngay_muon TIMESTAMP,
    IN p_ngay_tra TIMESTAMP
)
BEGIN
    SELECT 
        tb.id,
        tb.ma_thiet_bi,
        tb.ten_thiet_bi,
        tb.hang_san_xuat,
        tb.model,
        ph.ma_phong,
        ph.ten_phong
    FROM thiet_bi tb
    JOIN loai_thiet_bi ltb ON tb.loai_thiet_bi_id = ltb.id
    JOIN phong_hoc ph ON tb.phong_id = ph.id
    WHERE ltb.ten_loai LIKE CONCAT('%', p_loai_thiet_bi, '%')
      AND (p_ma_phong IS NULL OR ph.ma_phong = p_ma_phong)
      AND tb.trang_thai = 'TOT'
      AND tb.id NOT IN (
          SELECT thiet_bi_id 
          FROM muon_tra_thiet_bi
          WHERE trang_thai = 'DANG_MUON'
            AND ngay_muon <= p_ngay_tra
            AND ngay_tra_du_kien >= p_ngay_muon
      );
END //

DELIMITER ;

-- =============================================
-- PHẦN 8: TRIGGERS
-- =============================================

DELIMITER //

-- Trigger: Tự động cập nhật trạng thái thiết bị khi có báo hỏng
CREATE TRIGGER trg_bao_hong_after_insert
AFTER INSERT ON bao_hong
FOR EACH ROW
BEGIN
    IF NEW.muc_do_nghiem_trong IN ('CAO', 'KHAN_CAP') THEN
        UPDATE thiet_bi 
        SET trang_thai = 'HONG'
        WHERE id = NEW.thiet_bi_id;
    END IF;
END //

-- Trigger: Cập nhật trạng thái thiết bị sau khi bảo trì thành công
CREATE TRIGGER trg_bao_tri_after_insert
AFTER INSERT ON bao_tri
FOR EACH ROW
BEGIN
    IF NEW.ket_qua = 'THANH_CONG' THEN
        UPDATE thiet_bi 
        SET trang_thai = 'TOT'
        WHERE id = NEW.thiet_bi_id;
    ELSEIF NEW.ket_qua = 'CAN_THAY_THE' THEN
        UPDATE thiet_bi 
        SET trang_thai = 'THANH_LY'
        WHERE id = NEW.thiet_bi_id;
    END IF;
END //

DELIMITER ;

-- =============================================
-- PHẦN 9: INDEXES BỔ SUNG (Performance Optimization)
-- =============================================

-- Composite indexes cho queries phức tạp
CREATE INDEX idx_bao_hong_composite ON bao_hong(thiet_bi_id, trang_thai, ngay_bao);
CREATE INDEX idx_bao_tri_composite ON bao_tri(thiet_bi_id, loai_bao_tri, ngay_bao_tri);
CREATE INDEX idx_muon_tra_composite ON muon_tra_thiet_bi(nguoi_muon_id, trang_thai, ngay_muon);

-- =============================================
-- KẾT THÚC SCRIPT
-- =============================================

-- Hiển thị thông tin database
SELECT 'Database qlcsvc_ai đã được tạo thành công!' as message;
SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = 'qlcsvc_ai';
SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema = 'qlcsvc_ai' ORDER BY table_name;
