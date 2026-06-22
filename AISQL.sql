CREATE DATABASE  IF NOT EXISTS `qlcsvc_ai` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `qlcsvc_ai`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: qlcsvc_ai
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_du_doan_bao_tri`
--

DROP TABLE IF EXISTS `ai_du_doan_bao_tri`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_du_doan_bao_tri` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `ngay_du_doan` date NOT NULL,
  `xac_suat_hong` decimal(5,2) DEFAULT NULL COMMENT 'Xác suất 0.00 - 1.00',
  `muc_do_rui_ro` enum('THAP','TRUNG_BINH','CAO','NGUY_HIEM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ngay_du_kien_hong` date DEFAULT NULL,
  `chi_phi_uoc_tinh` decimal(15,2) DEFAULT NULL,
  `hanh_dong_de_xuat` text COLLATE utf8mb4_unicode_ci,
  `do_tin_cay` decimal(5,2) DEFAULT NULL COMMENT 'Độ tin cậy 0.00 - 1.00',
  `phien_ban_model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_thiet_bi_ngay` (`thiet_bi_id`,`ngay_du_doan` DESC),
  KEY `idx_rui_ro` (`muc_do_rui_ro`),
  KEY `idx_ngay_du_kien` (`ngay_du_kien_hong`),
  CONSTRAINT `ai_du_doan_bao_tri_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kết quả dự đoán từ AI';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_du_doan_bao_tri`
--

LOCK TABLES `ai_du_doan_bao_tri` WRITE;
/*!40000 ALTER TABLE `ai_du_doan_bao_tri` DISABLE KEYS */;
INSERT INTO `ai_du_doan_bao_tri` VALUES (1,1,'2026-01-11',0.78,'CAO','2026-03-15',1500000.00,'Nên bảo trì phòng ngừa trong tháng 2/2026. Kiểm tra bóng đèn và hệ thống làm mát.',0.85,'v1.0.0','2026-03-08 08:09:32'),(2,2,'2026-01-11',0.45,'TRUNG_BINH','2026-05-20',1200000.00,'Theo dõi thêm 2 tháng, nếu có dấu hiệu bất thường thì bảo trì.',0.72,'v1.0.0','2026-03-08 08:09:32'),(3,8,'2026-01-11',0.25,'THAP','2026-08-10',800000.00,'Thiết bị hoạt động tốt, bảo trì định kỳ theo lịch.',0.90,'v1.0.0','2026-03-08 08:09:32');
/*!40000 ALTER TABLE `ai_du_doan_bao_tri` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_du_lieu_huan_luyen`
--

DROP TABLE IF EXISTS `ai_du_lieu_huan_luyen`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_du_lieu_huan_luyen` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `features` json NOT NULL COMMENT 'Features đã được tính toán',
  `label` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Nhãn: 0/1 hoặc TOT/HONG',
  `loai_du_lieu` enum('TRAIN','TEST','VALIDATION') COLLATE utf8mb4_unicode_ci DEFAULT 'TRAIN',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loai_du_lieu` (`loai_du_lieu`),
  KEY `idx_thiet_bi` (`thiet_bi_id`),
  CONSTRAINT `ai_du_lieu_huan_luyen_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dữ liệu training cho ML model';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_du_lieu_huan_luyen`
--

LOCK TABLES `ai_du_lieu_huan_luyen` WRITE;
/*!40000 ALTER TABLE `ai_du_lieu_huan_luyen` DISABLE KEYS */;
INSERT INTO `ai_du_lieu_huan_luyen` VALUES (1,1,'{\"repair_cost_avg\": 300000, \"device_age_months\": 12, \"failure_count_12m\": 0, \"usage_frequency_per_month\": 15.5, \"days_since_last_maintenance\": 30}','0','TRAIN','2026-03-08 08:09:32'),(2,5,'{\"repair_cost_avg\": 1200000, \"device_age_months\": 34, \"failure_count_12m\": 2, \"usage_frequency_per_month\": 8.2, \"days_since_last_maintenance\": 90}','1','TRAIN','2026-03-08 08:09:32'),(3,10,'{\"repair_cost_avg\": 500000, \"device_age_months\": 16, \"failure_count_12m\": 1, \"usage_frequency_per_month\": 20.0, \"days_since_last_maintenance\": 120}','1','TEST','2026-03-08 08:09:32');
/*!40000 ALTER TABLE `ai_du_lieu_huan_luyen` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_model_metrics`
--

DROP TABLE IF EXISTS `ai_model_metrics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_metrics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ten_model` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phien_ban` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `loai_metric` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'accuracy, precision, recall, f1, etc.',
  `gia_tri` decimal(10,4) DEFAULT NULL,
  `ngay_danh_gia` date DEFAULT NULL,
  `ghi_chu` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_model_version` (`ten_model`,`phien_ban`),
  KEY `idx_ngay` (`ngay_danh_gia` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Theo dõi hiệu suất AI models';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_model_metrics`
--

LOCK TABLES `ai_model_metrics` WRITE;
/*!40000 ALTER TABLE `ai_model_metrics` DISABLE KEYS */;
INSERT INTO `ai_model_metrics` VALUES (1,'predictive_maintenance','v1.0.0','accuracy',0.8542,'2026-01-11','Model đầu tiên, baseline','2026-03-08 08:09:32'),(2,'predictive_maintenance','v1.0.0','precision',0.8234,'2026-01-11','Precision cho class HONG','2026-03-08 08:09:32'),(3,'predictive_maintenance','v1.0.0','recall',0.7891,'2026-01-11','Recall cho class HONG','2026-03-08 08:09:32'),(4,'predictive_maintenance','v1.0.0','f1_score',0.8058,'2026-01-11','F1 score tổng thể','2026-03-08 08:09:32');
/*!40000 ALTER TABLE `ai_model_metrics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bao_hong`
--

DROP TABLE IF EXISTS `bao_hong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bao_hong` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `nguoi_bao_id` bigint NOT NULL,
  `ngay_bao` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `mo_ta_loi` text COLLATE utf8mb4_unicode_ci,
  `muc_do_nghiem_trong` enum('THAP','TRUNG_BINH','CAO','KHAN_CAP') COLLATE utf8mb4_unicode_ci DEFAULT 'TRUNG_BINH',
  `trang_thai` enum('CHO_XU_LY','DANG_XU_LY','HOAN_THANH','HUY') COLLATE utf8mb4_unicode_ci DEFAULT 'CHO_XU_LY',
  `hinh_anh_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ghi_chu` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `nguoi_bao_id` (`nguoi_bao_id`),
  KEY `idx_thiet_bi_trang_thai` (`thiet_bi_id`,`trang_thai`),
  KEY `idx_muc_do` (`muc_do_nghiem_trong`),
  KEY `idx_ngay_bao` (`ngay_bao` DESC),
  KEY `idx_trang_thai` (`trang_thai`),
  KEY `idx_bao_hong_composite` (`thiet_bi_id`,`trang_thai`,`ngay_bao`),
  FULLTEXT KEY `ft_mo_ta_loi` (`mo_ta_loi`),
  CONSTRAINT `bao_hong_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`),
  CONSTRAINT `bao_hong_ibfk_2` FOREIGN KEY (`nguoi_bao_id`) REFERENCES `nguoi_dung` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phiếu báo hỏng thiết bị';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bao_hong`
--

LOCK TABLES `bao_hong` WRITE;
/*!40000 ALTER TABLE `bao_hong` DISABLE KEYS */;
INSERT INTO `bao_hong` VALUES (1,5,2,'2026-03-08 08:09:32','Loa bị rè, không phát ra âm thanh rõ ràng. Có tiếng kêu lạ khi bật nguồn.','CAO','DANG_XU_LY',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL),(2,10,4,'2026-03-08 08:09:32','Máy in không nhận lệnh in từ máy tính. Đèn báo lỗi nhấp nháy.','KHAN_CAP','HOAN_THANH',NULL,'2026-03-08 08:09:32','2026-06-04 17:15:27',NULL),(3,3,5,'2026-03-08 08:09:32','Laptop bị nóng máy, quạt kêu to bất thường.','TRUNG_BINH','HOAN_THANH',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL),(4,7,2,'2026-06-04 16:02:49','gà','THAP','CHO_XU_LY',NULL,'2026-06-04 16:02:49','2026-06-04 16:02:49',NULL),(9,10,2,'2026-06-04 16:48:46','sdfff','THAP','CHO_XU_LY',NULL,'2026-06-04 16:48:46','2026-06-04 16:48:46',NULL),(10,11,2,'2026-06-04 17:00:38','aqafaf','TRUNG_BINH','CHO_XU_LY',NULL,'2026-06-04 17:00:38','2026-06-04 17:00:38',NULL);
/*!40000 ALTER TABLE `bao_hong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bao_tri`
--

DROP TABLE IF EXISTS `bao_tri`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bao_tri` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `bao_hong_id` bigint DEFAULT NULL,
  `loai_bao_tri` enum('DINH_KY','SUA_CHUA','PHONG_NGUA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `ngay_bao_tri` date NOT NULL,
  `noi_dung` text COLLATE utf8mb4_unicode_ci,
  `chi_phi` decimal(15,2) DEFAULT NULL,
  `nguoi_thuc_hien` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ket_qua` enum('THANH_CONG','THAT_BAI','CAN_THAY_THE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `linh_kien_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `bao_hong_id` (`bao_hong_id`),
  KEY `idx_thiet_bi` (`thiet_bi_id`),
  KEY `idx_ngay_bao_tri` (`ngay_bao_tri` DESC),
  KEY `idx_loai` (`loai_bao_tri`),
  KEY `idx_bao_tri_composite` (`thiet_bi_id`,`loai_bao_tri`,`ngay_bao_tri`),
  KEY `FKg7eec47q6cfn4r892k6f2bu05` (`linh_kien_id`),
  CONSTRAINT `bao_tri_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`),
  CONSTRAINT `bao_tri_ibfk_2` FOREIGN KEY (`bao_hong_id`) REFERENCES `bao_hong` (`id`) ON DELETE SET NULL,
  CONSTRAINT `FKg7eec47q6cfn4r892k6f2bu05` FOREIGN KEY (`linh_kien_id`) REFERENCES `linh_kien` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử bảo trì/sửa chữa';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bao_tri`
--

LOCK TABLES `bao_tri` WRITE;
/*!40000 ALTER TABLE `bao_tri` DISABLE KEYS */;
INSERT INTO `bao_tri` VALUES (1,5,1,'SUA_CHUA','2026-01-11','Thay loa bass mới, kiểm tra mạch khuếch đại',1500000.00,'Nguyễn Văn Tèo','THANH_CONG','2026-03-08 08:09:32',NULL),(2,3,3,'SUA_CHUA','2026-01-10','Vệ sinh quạt tản nhiệt, thay keo tản nhiệt',500000.00,'Trần Văn Tý','THANH_CONG','2026-03-08 08:09:32',NULL),(3,1,NULL,'DINH_KY','2026-01-05','Vệ sinh ống kính, kiểm tra bóng đèn\n[06/05/2026 16:41] Cho 3k ddi',300000.00,'Lê Văn Tèo','THANH_CONG','2026-03-08 08:09:32',NULL),(4,10,2,'SUA_CHUA','2026-05-06','Test\n[06/05/2026 16:03] Kho day \n[06/05/2026 16:14] KHo nha\n[06/05/2026 16:16] Khó Nha\n[06/05/2026 16:24] Khó vl',12000.00,'Qua Chuoi to bu','CAN_THAY_THE','2026-05-06 08:45:47',NULL);
/*!40000 ALTER TABLE `bao_tri` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chatbot_hoi_thoai`
--

DROP TABLE IF EXISTS `chatbot_hoi_thoai`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chatbot_hoi_thoai` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nguoi_dung_id` bigint DEFAULT NULL,
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tin_nhan` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `intent` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entities` json DEFAULT NULL,
  `phan_hoi` text COLLATE utf8mb4_unicode_ci,
  `do_tin_cay` decimal(5,2) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `nguoi_dung_id` (`nguoi_dung_id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_intent` (`intent`),
  KEY `idx_ngay_tao` (`created_at` DESC),
  CONSTRAINT `chatbot_hoi_thoai_ibfk_1` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `nguoi_dung` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử hội thoại với chatbot';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chatbot_hoi_thoai`
--

LOCK TABLES `chatbot_hoi_thoai` WRITE;
/*!40000 ALTER TABLE `chatbot_hoi_thoai` DISABLE KEYS */;
INSERT INTO `chatbot_hoi_thoai` VALUES (1,2,'sess_20260111_001','Phòng 301 còn máy chiếu không?','check_availability','{\"room_number\": \"301\", \"equipment_type\": \"máy chiếu\"}','Có 1 máy chiếu khả dụng tại phòng 301: Máy chiếu Epson EB-X05 (TB001)',0.95,'2026-03-08 08:09:32'),(2,4,'sess_20260111_002','Tôi muốn mượn laptop ngày mai','book_equipment','{\"date\": \"2026-01-12\", \"equipment_type\": \"laptop\"}','Hiện có 2 laptop khả dụng. Bạn muốn mượn vào khung giờ nào?',0.88,'2026-03-08 08:09:32'),(3,5,'sess_20260111_003','Máy in phòng 205 bị lỗi','report_damage','{\"room_number\": \"205\", \"equipment_type\": \"máy in\"}','Tôi đã ghi nhận báo hỏng. Nhân viên CSVC sẽ kiểm tra trong vòng 24h.',0.92,'2026-03-08 08:09:32');
/*!40000 ALTER TABLE `chatbot_hoi_thoai` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hinh_anh_thiet_bi`
--

DROP TABLE IF EXISTS `hinh_anh_thiet_bi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hinh_anh_thiet_bi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `url_hinh_anh` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `loai_hinh_anh` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `danh_gia_ai` json DEFAULT NULL COMMENT 'Kết quả phân tích từ CV model',
  `nguoi_chup_id` bigint DEFAULT NULL,
  `ngay_chup` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `nguoi_chup_id` (`nguoi_chup_id`),
  KEY `idx_thiet_bi` (`thiet_bi_id`),
  KEY `idx_loai` (`loai_hinh_anh`),
  KEY `idx_ngay_chup` (`ngay_chup` DESC),
  CONSTRAINT `hinh_anh_thiet_bi_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`),
  CONSTRAINT `hinh_anh_thiet_bi_ibfk_2` FOREIGN KEY (`nguoi_chup_id`) REFERENCES `nguoi_dung` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lưu trữ hình ảnh và kết quả CV';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hinh_anh_thiet_bi`
--

LOCK TABLES `hinh_anh_thiet_bi` WRITE;
/*!40000 ALTER TABLE `hinh_anh_thiet_bi` DISABLE KEYS */;
INSERT INTO `hinh_anh_thiet_bi` VALUES (1,1,'/images/equipment/TB001_qr.jpg','QR_CODE',NULL,3,'2026-03-08 08:09:32'),(2,5,'/images/equipment/TB005_damage.jpg','BAO_HONG','{\"damages\": [{\"type\": \"SCRATCH\", \"location\": \"speaker_grill\", \"severity\": \"MEDIUM\"}], \"condition\": \"BAO_TRI\", \"confidence\": 0.92}',2,'2026-03-08 08:09:32'),(3,3,'/images/equipment/TB003_check.jpg','KIEM_TRA_TINH_TRANG','{\"damages\": [], \"condition\": \"TOT\", \"confidence\": 0.95}',4,'2026-03-08 08:09:32');
/*!40000 ALTER TABLE `hinh_anh_thiet_bi` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `iot_du_lieu_cam_bien`
--

DROP TABLE IF EXISTS `iot_du_lieu_cam_bien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `iot_du_lieu_cam_bien` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phong_id` bigint NOT NULL,
  `nhiet_do` decimal(5,2) DEFAULT NULL COMMENT 'Nhiệt độ (°C)',
  `do_am` decimal(5,2) DEFAULT NULL COMMENT 'Độ ẩm (%)',
  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_phong_timestamp` (`phong_id`,`timestamp` DESC),
  CONSTRAINT `iot_du_lieu_cam_bien_ibfk_1` FOREIGN KEY (`phong_id`) REFERENCES `phong_hoc` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dữ liệu từ cảm biến IoT';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `iot_du_lieu_cam_bien`
--

LOCK TABLES `iot_du_lieu_cam_bien` WRITE;
/*!40000 ALTER TABLE `iot_du_lieu_cam_bien` DISABLE KEYS */;
INSERT INTO `iot_du_lieu_cam_bien` VALUES (1,3,24.50,65.20,'2026-01-11 01:00:00'),(2,3,25.10,64.80,'2026-01-11 02:00:00'),(3,3,26.30,63.50,'2026-01-11 03:00:00'),(4,4,23.80,66.10,'2026-01-11 01:00:00'),(5,5,22.50,58.30,'2026-01-11 01:00:00');
/*!40000 ALTER TABLE `iot_du_lieu_cam_bien` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `linh_kien`
--

DROP TABLE IF EXISTS `linh_kien`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `linh_kien` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `don_vi_tinh` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `han_bao_hanh` date DEFAULT NULL,
  `ma_linh_kien` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ngay_mua` date DEFAULT NULL,
  `ten_linh_kien` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thoi_gian_da_su_dung` int DEFAULT NULL,
  `thong_so_ky_thuat` text COLLATE utf8mb4_unicode_ci,
  `trang_thai` enum('HOAT_DONG','CAN_THAY_THE','DANG_BAO_HANH','DA_HU_HONG') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tuoi_tho_toi_da` int DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `thiet_bi_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7l4wdmjduqu06i13l8dwydq56` (`ma_linh_kien`),
  KEY `FKjhuno2rwtnt7h8htwneaqaoxg` (`thiet_bi_id`),
  CONSTRAINT `FKjhuno2rwtnt7h8htwneaqaoxg` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `linh_kien`
--

LOCK TABLES `linh_kien` WRITE;
/*!40000 ALTER TABLE `linh_kien` DISABLE KEYS */;
INSERT INTO `linh_kien` VALUES (1,'2026-06-04 21:45:55.503635','Trang','2026-09-08','LK-BinhMuc-001','2025-01-20','Bình Chứa Mực',31,'Bình Chứa Mực, Dung tích 4 lít','HOAT_DONG',2000,'2026-06-04 22:18:25.678395',10),(2,'2026-06-04 21:49:47.594594','Giờ','2026-06-04','LK-Bong-001','2025-01-15','Bóng máy chiếu',0,'Bóng AmongUS 240W, Ước tính 3000h giờ tuổi thọ','HOAT_DONG',3000,'2026-06-04 21:49:47.594594',2);
/*!40000 ALTER TABLE `linh_kien` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loai_phong`
--

DROP TABLE IF EXISTS `loai_phong`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loai_phong` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `mo_ta` text COLLATE utf8mb4_unicode_ci,
  `so_phong` int DEFAULT NULL,
  `ten_loai` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_2oxb80tsbyljiuw9f00yxtjn8` (`ten_loai`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loai_phong`
--

LOCK TABLES `loai_phong` WRITE;
/*!40000 ALTER TABLE `loai_phong` DISABLE KEYS */;
/*!40000 ALTER TABLE `loai_phong` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loai_thiet_bi`
--

DROP TABLE IF EXISTS `loai_thiet_bi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loai_thiet_bi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ten_loai` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mo_ta` text COLLATE utf8mb4_unicode_ci,
  `thoi_gian_bao_hanh_mac_dinh` int DEFAULT NULL COMMENT 'Thời gian bảo hành mặc định (tháng)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Phân loại thiết bị';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loai_thiet_bi`
--

LOCK TABLES `loai_thiet_bi` WRITE;
/*!40000 ALTER TABLE `loai_thiet_bi` DISABLE KEYS */;
INSERT INTO `loai_thiet_bi` VALUES (1,'Máy chiếu','Máy chiếu đa năng phục vụ giảng dạy',24,'2026-03-08 08:09:32'),(2,'Laptop','Laptop phục vụ giảng dạy và học tập',36,'2026-03-08 08:09:32'),(3,'Loa','Loa hội trường và phòng học',12,'2026-03-08 08:09:32'),(4,'Micro','Micro không dây',12,'2026-03-08 08:09:32'),(5,'Bảng tương tác','Bảng tương tác thông minh',24,'2026-03-08 08:09:32'),(6,'Máy tính để bàn','PC phòng máy tính',36,'2026-03-08 08:09:32'),(7,'Máy in','Máy in văn phòng',12,'2026-03-08 08:09:32');
/*!40000 ALTER TABLE `loai_thiet_bi` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `muon_tra_thiet_bi`
--

DROP TABLE IF EXISTS `muon_tra_thiet_bi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `muon_tra_thiet_bi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `thiet_bi_id` bigint NOT NULL,
  `nguoi_muon_id` bigint NOT NULL,
  `ngay_muon` datetime NOT NULL,
  `ngay_tra_du_kien` datetime DEFAULT NULL,
  `ngay_tra_thuc_te` datetime DEFAULT NULL,
  `trang_thai` enum('DANG_MUON','DA_TRA','QUA_HAN') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ghi_chu` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_thiet_bi_trang_thai` (`thiet_bi_id`,`trang_thai`),
  KEY `idx_nguoi_muon` (`nguoi_muon_id`),
  KEY `idx_ngay_muon` (`ngay_muon` DESC),
  KEY `idx_trang_thai` (`trang_thai`),
  KEY `idx_muon_tra_composite` (`nguoi_muon_id`,`trang_thai`,`ngay_muon`),
  CONSTRAINT `muon_tra_thiet_bi_ibfk_1` FOREIGN KEY (`thiet_bi_id`) REFERENCES `thiet_bi` (`id`),
  CONSTRAINT `muon_tra_thiet_bi_ibfk_2` FOREIGN KEY (`nguoi_muon_id`) REFERENCES `nguoi_dung` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử mượn/trả thiết bị';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `muon_tra_thiet_bi`
--

LOCK TABLES `muon_tra_thiet_bi` WRITE;
/*!40000 ALTER TABLE `muon_tra_thiet_bi` DISABLE KEYS */;
INSERT INTO `muon_tra_thiet_bi` VALUES (1,1,2,'2026-01-10 08:00:00','2026-01-10 12:00:00','2026-01-10 11:45:00','DA_TRA',NULL,'2026-03-08 08:09:32'),(2,3,4,'2026-01-11 14:00:00','2026-01-11 16:00:00',NULL,'QUA_HAN',NULL,'2026-03-08 08:09:32'),(3,2,5,'2026-01-09 09:00:00','2026-01-09 11:00:00','2026-01-09 11:30:00','DA_TRA',NULL,'2026-03-08 08:09:32'),(4,6,2,'2026-01-08 13:00:00','2026-01-08 15:00:00','2026-01-08 15:20:00','DA_TRA',NULL,'2026-03-08 08:09:32'),(5,9,2,'2026-05-06 22:56:46','2026-05-12 00:00:00','2026-05-06 23:01:13','DA_TRA','mượn á','2026-05-06 15:56:46'),(6,2,2,'2026-05-06 23:04:52','2026-05-06 00:00:00','2026-05-06 23:10:08','DA_TRA','adad','2026-05-06 16:04:52'),(7,2,2,'2026-05-12 14:52:29','2026-05-16 00:00:00','2026-06-04 22:00:28','DA_TRA','ads','2026-05-12 07:52:29'),(8,2,2,'2026-06-04 22:00:03','2026-06-17 00:00:00','2026-06-04 22:00:35','DA_TRA','','2026-06-04 15:00:03'),(9,10,2,'2026-06-04 22:02:45','2026-06-08 00:00:00','2026-06-04 22:03:03','DA_TRA','in túi','2026-06-04 15:02:45'),(10,10,2,'2026-06-04 22:17:28','2026-06-08 00:00:00','2026-06-04 22:18:05','DA_TRA','jkw;rv; ldv','2026-06-04 15:17:28'),(11,10,2,'2026-06-04 22:27:10','2026-06-08 00:00:00','2026-06-04 22:27:30','DA_TRA','','2026-06-04 15:27:10'),(12,3,2,'2026-06-04 22:27:21','2026-06-10 00:00:00',NULL,'DANG_MUON','','2026-06-04 15:27:21'),(13,7,2,'2026-06-04 23:02:27','2026-06-25 00:00:00',NULL,'DANG_MUON','','2026-06-04 16:02:27');
/*!40000 ALTER TABLE `muon_tra_thiet_bi` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nguoi_dung`
--

DROP TABLE IF EXISTS `nguoi_dung`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nguoi_dung` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ho_ten` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mat_khau` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt hashed password',
  `so_dien_thoai` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vai_tro` enum('ADMIN','GIAO_VIEN','NHAN_VIEN_CSVC') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GIAO_VIEN',
  `trang_thai` enum('ACTIVE','INACTIVE') COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_email` (`email`),
  KEY `idx_vai_tro` (`vai_tro`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý người dùng hệ thống';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nguoi_dung`
--

LOCK TABLES `nguoi_dung` WRITE;
/*!40000 ALTER TABLE `nguoi_dung` DISABLE KEYS */;
INSERT INTO `nguoi_dung` VALUES (1,'Admin Hệ thống','admin@dhnt.edu.vn','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi','0123456789','ADMIN','ACTIVE','2026-03-08 08:09:32','2026-03-08 08:09:32'),(2,'Nguyễn Văn An','nguyenvanan@dhnt.edu.vn','$2a$10$ogo.Drt8chVljM.r5a6Xhef0aMHhLoG84u.nuZK3AzfNLD5zRM.3.','0987654321','GIAO_VIEN','ACTIVE','2026-03-08 08:09:32','2026-04-22 17:13:01'),(3,'Trần Thị Bình','tranthib@dhnt.edu.vn','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi','0912345678','NHAN_VIEN_CSVC','ACTIVE','2026-03-08 08:09:32','2026-03-08 08:09:32'),(4,'Lê Văn Cường','levancuong@dhnt.edu.vn','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi','0909123456','GIAO_VIEN','ACTIVE','2026-03-08 08:09:32','2026-03-08 08:09:32'),(5,'Phạm Thị Dung','phamthidung@dhnt.edu.vn','$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIq/fRh.Oi','0898765432','GIAO_VIEN','ACTIVE','2026-03-08 08:09:32','2026-03-08 08:09:32'),(6,'Admin System','admin@example.com','$2a$10$xED9sHLoj2VMraSAbZaJcOgmKoZPqT3aPGn4mCqjEkZD8v9VqtH1W',NULL,'ADMIN','ACTIVE','2026-03-08 08:12:41','2026-04-13 17:53:25'),(7,'Lục Văn Sơn','lvson2005@gmail.com','$2a$10$K6PC3wp6VRUhYCDh5caEUOOqDKt1TNIIo1Pnry5xvj8oDlqhOdsoK','0111111111','NHAN_VIEN_CSVC','ACTIVE','2026-03-08 08:14:47','2026-04-13 18:33:22'),(8,'Lục Văn Sơn','shadyfyrix@gmail.com','$2a$10$mv7asqjIxpvHyMLGliWByup4fI0I4wI5frbK/zTx/GcoKeZflZJB2','08237768','ADMIN','ACTIVE','2026-03-08 08:17:19','2026-04-13 18:33:30');
/*!40000 ALTER TABLE `nguoi_dung` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `phong_hoc`
--

DROP TABLE IF EXISTS `phong_hoc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `phong_hoc` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ma_phong` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã phòng: P301, PTN101...',
  `ten_phong` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `toa_nha` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tang` int DEFAULT NULL,
  `suc_chua` int DEFAULT NULL COMMENT 'Số lượng sinh viên tối đa',
  `loai_phong` enum('LY_THUYET','THI_NGHIEM','MAY_TINH') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `loai_phong_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ma_phong` (`ma_phong`),
  KEY `idx_ma_phong` (`ma_phong`),
  KEY `idx_loai_phong` (`loai_phong`),
  KEY `FKbc1vh6dypciwyrv41ede9b0fs` (`loai_phong_id`),
  CONSTRAINT `FKbc1vh6dypciwyrv41ede9b0fs` FOREIGN KEY (`loai_phong_id`) REFERENCES `loai_phong` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý phòng học/phòng thí nghiệm';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `phong_hoc`
--

LOCK TABLES `phong_hoc` WRITE;
/*!40000 ALTER TABLE `phong_hoc` DISABLE KEYS */;
INSERT INTO `phong_hoc` VALUES (1,'P301','Phòng 301','Tòa A',3,60,'LY_THUYET','2026-03-08 08:09:32',NULL),(2,'P302','Phòng 302','Tòa A',3,60,'LY_THUYET','2026-03-08 08:09:32',NULL),(3,'P205','Phòng máy tính 205','Tòa B',2,40,'MAY_TINH','2026-03-08 08:09:32',NULL),(4,'P206','Phòng máy tính 206','Tòa B',2,40,'MAY_TINH','2026-03-08 08:09:32',NULL),(5,'PTN101','Phòng thí nghiệm Vật lý','Tòa C',1,30,'THI_NGHIEM','2026-03-08 08:09:32',NULL),(6,'PTN102','Phòng thí nghiệm Hóa học','Tòa C',1,30,'THI_NGHIEM','2026-03-08 08:09:32',NULL),(7,'P401','Hội trường A','Tòa A',4,200,'LY_THUYET','2026-03-08 08:09:32',NULL);
/*!40000 ALTER TABLE `phong_hoc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `thiet_bi`
--

DROP TABLE IF EXISTS `thiet_bi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `thiet_bi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ma_thiet_bi` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Mã QR code',
  `ten_thiet_bi` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `loai_thiet_bi_id` bigint NOT NULL,
  `phong_id` bigint DEFAULT NULL,
  `hang_san_xuat` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `model` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nam_san_xuat` int DEFAULT NULL,
  `ngay_mua` date DEFAULT NULL,
  `gia_mua` decimal(15,2) DEFAULT NULL,
  `trang_thai` enum('TOT','BAO_TRI','HONG','THANH_LY') COLLATE utf8mb4_unicode_ci DEFAULT 'TOT',
  `ghi_chu` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `hinh_anh_chinh` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `han_bao_hanh` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ma_thiet_bi` (`ma_thiet_bi`),
  KEY `idx_ma_thiet_bi` (`ma_thiet_bi`),
  KEY `idx_loai_trang_thai` (`loai_thiet_bi_id`,`trang_thai`),
  KEY `idx_phong` (`phong_id`),
  KEY `idx_trang_thai` (`trang_thai`),
  FULLTEXT KEY `ft_ten_thiet_bi` (`ten_thiet_bi`),
  CONSTRAINT `thiet_bi_ibfk_1` FOREIGN KEY (`loai_thiet_bi_id`) REFERENCES `loai_thiet_bi` (`id`),
  CONSTRAINT `thiet_bi_ibfk_2` FOREIGN KEY (`phong_id`) REFERENCES `phong_hoc` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý thiết bị chi tiết';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thiet_bi`
--

LOCK TABLES `thiet_bi` WRITE;
/*!40000 ALTER TABLE `thiet_bi` DISABLE KEYS */;
INSERT INTO `thiet_bi` VALUES (1,'TB001','Máy chiếu Epson EB-X05',1,1,'Epson','EB-X05',2023,'2023-01-15',8500000.00,'TOT',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL,NULL),(2,'TB002','Máy chiếu BenQ MH535A',1,2,'BenQ','MH535A',2023,'2023-02-20',9200000.00,'TOT',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL,NULL),(3,'TB003','Laptop Dell Latitude 5420',2,3,'Dell','Latitude 5420',2022,'2022-06-20',18000000.00,'HONG',NULL,'2026-03-08 08:09:32','2026-06-04 16:36:57',NULL,NULL),(4,'TB004','Laptop HP ProBook 450 G8',2,3,'HP','ProBook 450 G8',2022,'2022-07-15',17500000.00,'HONG',NULL,'2026-03-08 08:09:32','2026-06-04 16:23:10',NULL,NULL),(5,'TB005','Loa JBL EON615',3,7,'JBL','EON615',2021,'2021-03-10',12000000.00,'BAO_TRI',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL,NULL),(6,'TB006','Micro Shure SM58',4,7,'Shure','SM58',2022,'2022-04-05',3500000.00,'TOT',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL,NULL),(7,'TB007','Bảng tương tác Samsung Flip 2',5,1,'Samsung','Flip 2 WM55R',2023,'2023-05-10',45000000.00,'HONG',NULL,'2026-03-08 08:09:32','2026-06-04 16:02:49',NULL,NULL),(8,'TB008','PC Dell OptiPlex 7090',6,3,'Dell','OptiPlex 7090',2023,'2023-08-01',15000000.00,'TOT',NULL,'2026-03-08 08:09:32','2026-03-08 08:09:32',NULL,NULL),(9,'TB009','PC HP EliteDesk 800 G8',6,4,NULL,NULL,NULL,'2023-08-01',NULL,'BAO_TRI','','2026-03-08 08:09:32','2026-06-04 18:27:10',NULL,NULL),(10,'TB010','Máy in HP LaserJet Pro M404dn',7,NULL,NULL,NULL,NULL,'2026-06-02',NULL,'HONG','','2026-03-08 08:09:32','2026-06-04 17:31:39',NULL,'2026-06-11'),(11,'TB11C7A8EF','TEst 1',4,4,NULL,NULL,NULL,'2026-06-04',NULL,'HONG','313','2026-06-04 15:30:30','2026-06-04 17:00:38',NULL,NULL);
/*!40000 ALTER TABLE `thiet_bi` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `thong_bao`
--

DROP TABLE IF EXISTS `thong_bao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `thong_bao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `da_doc` bit(1) DEFAULT NULL,
  `loai_thong_bao` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ngay_tao` datetime(6) DEFAULT NULL,
  `noi_dung` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_nhan` enum('ADMIN','GIAO_VIEN','NHAN_VIEN_CSVC') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tieu_de` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url_detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nguoi_nhan_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `nguoi_dung_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKuqt61uibe8dxw2aw7rn5gq6l` (`nguoi_nhan_id`),
  KEY `FK2883elhrppa005tc6cla0sm6y` (`nguoi_dung_id`),
  CONSTRAINT `FK2883elhrppa005tc6cla0sm6y` FOREIGN KEY (`nguoi_dung_id`) REFERENCES `nguoi_dung` (`id`),
  CONSTRAINT `FKuqt61uibe8dxw2aw7rn5gq6l` FOREIGN KEY (`nguoi_nhan_id`) REFERENCES `nguoi_dung` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thong_bao`
--

LOCK TABLES `thong_bao` WRITE;
/*!40000 ALTER TABLE `thong_bao` DISABLE KEYS */;
INSERT INTO `thong_bao` VALUES (1,_binary '','MUON_TRA','2026-05-06 22:56:45.985787','Giảng viên Nguyễn Văn An vừa gửi yêu cầu mượn thiết bị PC HP EliteDesk 800 G8','ADMIN','Yêu cầu mượn thiết bị mới','/admin/muon-tra/5',NULL,NULL,NULL),(2,_binary '\0','MUON_TRA','2026-05-06 23:00:39.008529','Yêu cầu mượn thiết bị PC HP EliteDesk 800 G8 của bạn đã được duyệt. Bạn có thể đến nhận thiết bị.',NULL,'Đơn mượn thiết bị đã được duyệt','/giaovien/muon-tra',2,NULL,NULL),(3,_binary '','MUON_TRA','2026-05-06 23:04:52.106665','Giảng viên Nguyễn Văn An vừa gửi yêu cầu mượn thiết bị Máy chiếu BenQ MH535A','ADMIN','Yêu cầu mượn thiết bị mới','/admin/muon-tra/6',NULL,NULL,NULL),(4,_binary '','MUON_TRA','2026-05-06 23:05:10.241672','Yêu cầu mượn thiết bị Máy chiếu BenQ MH535A của bạn đã được duyệt. Bạn có thể đến nhận thiết bị.',NULL,'Đơn mượn thiết bị đã được duyệt','/giaovien/muon-tra',2,NULL,NULL),(5,_binary '\0','OVERDUE',NULL,'Chào Lê Văn Cường, thiết bị \'Laptop Dell Latitude 5420\' bạn mượn vào ngày 2026-01-11 đã quá thời hạn trả dự kiến (2026-01-11). Vui lòng hoàn trả sớm nhất có thể. Cảm ơn!',NULL,'Thông báo: Thiết bị quá hạn trả',NULL,NULL,'2026-05-12 14:41:25.648060',4),(6,_binary '','MUON_TRA',NULL,'Thiết bị \"Máy chiếu BenQ MH535A\" sẽ hết hạn mượn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn.',NULL,'⏰ Nhắc nhở: Sắp đến hạn trả thiết bị','/giao-vien/muon-tra',NULL,'2026-05-12 15:00:05.305271',2),(7,_binary '','HE_THONG',NULL,'Tổng thiết bị: 10 | Đang mượn: 1 | Quá hạn: 1 | Báo hỏng chờ xử lý: 0.','ADMIN','? Báo cáo hệ thống sáng nay','/admin',NULL,'2026-05-12 15:00:05.347673',NULL),(8,_binary '\0','HE_THONG',NULL,'Thiết bị đang mượn: 1 | Quá hạn chưa trả: 1 | Đơn báo hỏng cần xử lý: 0.','NHAN_VIEN_CSVC','? Tóm tắt công việc hôm nay','/nhanvien-csvc/bao-hong',NULL,'2026-05-12 15:00:05.355040',NULL),(9,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn thiết bị đã quá ngày trả dự kiến. Đề nghị liên hệ người mượn để thu hồi.','NHAN_VIEN_CSVC','? Cảnh báo: 1 thiết bị mượn quá hạn','/nhanvien-csvc/muon-tra',NULL,'2026-05-12 15:00:05.362027',NULL),(10,_binary '','BAO_TRI',NULL,'Tỉ lệ thiết bị bị báo hỏng đã vượt 10% (3/10). Đề xuất tổ chức đợt bảo trì tổng thể.','ADMIN','⚠️ AI Cảnh báo: Tần suất hỏng hóc cao','/admin/bao-tri',NULL,'2026-05-12 15:00:05.367535',NULL),(11,_binary '','MUON_TRA',NULL,'Thiết bị \"Máy chiếu BenQ MH535A\" sẽ hết hạn mượn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn.',NULL,'⏰ Nhắc nhở: Sắp đến hạn trả thiết bị','/giao-vien/muon-tra',NULL,'2026-05-12 15:00:05.382601',2),(12,_binary '','MUON_TRA',NULL,'Thiết bị \"Máy chiếu BenQ MH535A\" sẽ hết hạn mượn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn.',NULL,'⏰ Nhắc nhở: Sắp đến hạn trả thiết bị','/giao-vien/muon-tra',NULL,'2026-05-12 15:00:50.551313',2),(13,_binary '','HE_THONG',NULL,'Tổng thiết bị: 10 | Đang mượn: 1 | Quá hạn: 1 | Báo hỏng chờ xử lý: 0.','ADMIN','? Báo cáo hệ thống sáng nay','/admin',NULL,'2026-05-12 15:00:50.601466',NULL),(14,_binary '\0','HE_THONG',NULL,'Thiết bị đang mượn: 1 | Quá hạn chưa trả: 1 | Đơn báo hỏng cần xử lý: 0.','NHAN_VIEN_CSVC','? Tóm tắt công việc hôm nay','/nhanvien-csvc/bao-hong',NULL,'2026-05-12 15:00:50.607787',NULL),(15,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn thiết bị đã quá ngày trả dự kiến. Đề nghị liên hệ người mượn để thu hồi.','NHAN_VIEN_CSVC','? Cảnh báo: 1 thiết bị mượn quá hạn','/nhanvien-csvc/muon-tra',NULL,'2026-05-12 15:00:50.616507',NULL),(16,_binary '','BAO_TRI',NULL,'Tỉ lệ thiết bị bị báo hỏng đã vượt 10% (3/10). Đề xuất tổ chức đợt bảo trì tổng thể.','ADMIN','⚠️ AI Cảnh báo: Tần suất hỏng hóc cao','/admin/bao-tri',NULL,'2026-05-12 15:00:50.623104',NULL),(17,_binary '','MUON_TRA',NULL,'Thiết bị \"Máy chiếu BenQ MH535A\" sẽ hết hạn mượn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn.',NULL,'⏰ Nhắc nhở: Sắp đến hạn trả thiết bị','/giao-vien/muon-tra',NULL,'2026-05-12 15:00:50.641068',2),(18,_binary '','MUON_TRA',NULL,'Giáo viên \'Nguyễn Văn An\' đang mượn thiết bị \'Máy chiếu BenQ MH535A\', hạn trả ngày 16/05/2026 (còn 4 ngày). Viết 2 câu nhắc nhở lịch sự, thân thiện để gửi cho giáo viên này.',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 4 ngày','/giao-vien/muon-tra',NULL,'2026-05-12 15:06:49.271902',2),(19,_binary '','HE_THONG',NULL,'Dựa vào dữ liệu sau, hãy viết một báo cáo ngắn gọn (3-4 câu) cho Quản trị viên hệ thống QLCSVC, nêu bật các vấn đề cần chú ý:\nDữ liệu hệ thống QLCSVC:\n- Tổng thiết bị: 10\n- Đang được mượn: 1\n- Mượn qu...','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-05-12 15:06:49.271902',NULL),(20,_binary '','MUON_TRA',NULL,'Có 1 thiết bị mượn đã quá ngày trả. Viết 2 câu nhắc nhở cho quản trị viên về việc theo dõi và thu hồi.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-05-12 15:06:49.300160',NULL),(21,_binary '\0','HE_THONG',NULL,'Dựa vào tình trạng công việc nhân viên CSVC:\n- Đơn báo hỏng chờ xử lý: 0\n- Đơn đang xử lý: 2\n- Thiết bị hỏng khẩn cấp/cao: 0\nViết tóm tắt công việc hôm nay (3-4 câu), nêu ưu tiên cần xử lý.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-05-12 15:06:49.311070',NULL),(22,_binary '','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Máy chiếu BenQ MH535A\" bạn đang mượn sẽ hết hạn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 4 ngày','/giao-vien/muon-tra',NULL,'2026-05-12 15:11:17.032752',2),(23,_binary '','HE_THONG',NULL,'Hệ thống hiện có 10 thiết bị, đang mượn: 1, quá hạn: 1, báo hỏng chờ xử lý: 0.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-05-12 15:11:17.055178',NULL),(24,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-05-12 15:11:17.063361',NULL),(25,_binary '\0','HE_THONG',NULL,'Hôm nay có 0 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-05-12 15:11:17.071457',NULL),(26,_binary '','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Máy chiếu BenQ MH535A\" bạn đang mượn sẽ hết hạn vào ngày 16/05/2026 (còn 4 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 4 ngày','/giao-vien/muon-tra',NULL,'2026-05-12 15:11:44.253464',2),(27,_binary '','HE_THONG',NULL,'Hệ thống hiện có 10 thiết bị, đang mượn: 1, quá hạn: 1, báo hỏng chờ xử lý: 0.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-05-12 15:11:44.268515',NULL),(28,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-05-12 15:11:44.273543',NULL),(29,_binary '','HE_THONG',NULL,'Hôm nay có 0 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-05-12 15:11:44.278627',NULL),(30,_binary '','HE_THONG',NULL,'Hệ thống hiện có 10 thiết bị, đang mượn: 1, quá hạn: 1, báo hỏng chờ xử lý: 0.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-05-14 15:58:29.592651',NULL),(31,_binary '','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Máy chiếu BenQ MH535A\" bạn đang mượn sẽ hết hạn vào ngày 16/05/2026 (còn 2 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 2 ngày','/giao-vien/muon-tra',NULL,'2026-05-14 15:58:29.585050',2),(32,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-05-14 15:58:29.613631',NULL),(33,_binary '','HE_THONG',NULL,'Hôm nay có 0 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-05-14 15:58:29.622748',NULL),(34,_binary '','HE_THONG',NULL,'Hệ thống hiện có 10 thiết bị, đang mượn: 1, quá hạn: 1, báo hỏng chờ xử lý: 0.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 21:40:41.770393',NULL),(35,_binary '','OVERDUE',NULL,'Chào Nguyễn Văn An, thiết bị \'Máy chiếu BenQ MH535A\' bạn mượn vào ngày 2026-05-12 đã quá thời hạn trả dự kiến (2026-05-16). Vui lòng hoàn trả sớm nhất có thể. Cảm ơn!',NULL,'Thông báo: Thiết bị quá hạn trả',NULL,NULL,'2026-06-04 21:40:41.767837',2),(36,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 21:40:41.790775',NULL),(37,_binary '','HE_THONG',NULL,'Hôm nay có 0 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 21:40:41.801733',NULL),(38,_binary '','MUON_TRA',NULL,'Có 2 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 2 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 21:57:32.755865',NULL),(39,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị TEst 1 (Mức độ: KHAN_CAP). Lỗi: vc ','ADMIN','? Báo hỏng mới: TEst 1','/admin/bao-hong',NULL,'2026-06-04 23:09:50.364874',NULL),(40,_binary '\0','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị TEst 1 (Mức độ: KHAN_CAP). Lỗi: vc ','NHAN_VIEN_CSVC','? Báo hỏng mới: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:09:50.368835',NULL),(41,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 2.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:12:07.796624',NULL),(42,_binary '','BAO_HONG',NULL,'Phát hiện 1 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 1 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:12:07.824143',NULL),(43,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:12:07.844656',NULL),(44,_binary '','HE_THONG',NULL,'Hôm nay có 2 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:12:07.861820',NULL),(45,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị PC HP EliteDesk 800 G8 (Mức độ: CAO). Lỗi: âffaf','ADMIN','? Báo hỏng mới: PC HP EliteDesk 800 G8','/admin/bao-hong',NULL,'2026-06-04 23:18:40.317058',NULL),(46,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị PC HP EliteDesk 800 G8 (Mức độ: CAO). Lỗi: âffaf','NHAN_VIEN_CSVC','? Báo hỏng mới: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:18:40.327843',NULL),(47,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:21:46.945810',NULL),(48,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:21:46.967206',NULL),(49,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:21:46.984051',NULL),(50,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:21:46.997019',NULL),(51,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Laptop HP ProBook 450 G8 (Mức độ: THAP). Lỗi: vc','ADMIN','? Báo hỏng mới: Laptop HP ProBook 450 G8','/admin/bao-hong',NULL,'2026-06-04 23:23:09.905288',NULL),(52,_binary '\0','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Laptop HP ProBook 450 G8 (Mức độ: THAP). Lỗi: vc','NHAN_VIEN_CSVC','? Báo hỏng mới: Laptop HP ProBook 450 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:23:09.910284',NULL),(53,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 4.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:30:35.392619',NULL),(54,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:30:35.428057',NULL),(55,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:30:35.439948',NULL),(56,_binary '\0','HE_THONG',NULL,'Hôm nay có 4 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:30:35.451953',NULL),(57,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 4.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:31:42.842986',NULL),(58,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:31:42.849304',NULL),(59,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:31:42.860665',NULL),(60,_binary '\0','HE_THONG',NULL,'Hôm nay có 4 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:31:42.872300',NULL),(61,_binary '\0','BAO_HONG',NULL,'Thiết bị TEst 1 vừa được báo hỏng (mức KHAN_CAP): vc . Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:31:42.893954',NULL),(62,_binary '\0','BAO_HONG',NULL,'Thiết bị PC HP EliteDesk 800 G8 vừa được báo hỏng (mức CAO): âffaf. Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:31:42.908668',NULL),(63,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:31:42.923608',NULL),(64,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:31:42.945134',NULL),(65,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:31:42.961288',NULL),(66,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 4.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:33:00.214056',NULL),(67,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:33:00.238171',NULL),(68,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:33:00.246253',NULL),(69,_binary '\0','HE_THONG',NULL,'Hôm nay có 4 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:33:00.252233',NULL),(70,_binary '\0','BAO_HONG',NULL,'Thiết bị TEst 1 vừa được báo hỏng (mức KHAN_CAP): vc . Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:33:00.284947',NULL),(71,_binary '\0','BAO_HONG',NULL,'Thiết bị PC HP EliteDesk 800 G8 vừa được báo hỏng (mức CAO): âffaf. Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:33:00.302338',NULL),(72,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:33:00.318128',NULL),(73,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:33:00.326236',NULL),(74,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:33:00.340067',NULL),(75,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 4.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:35:31.256289',NULL),(76,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:35:31.268162',NULL),(77,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:35:31.287140',NULL),(78,_binary '\0','HE_THONG',NULL,'Hôm nay có 4 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:35:31.294123',NULL),(79,_binary '\0','BAO_HONG',NULL,'Thiết bị TEst 1 vừa được báo hỏng (mức KHAN_CAP): vc . Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:35:31.307545',NULL),(80,_binary '\0','BAO_HONG',NULL,'Thiết bị PC HP EliteDesk 800 G8 vừa được báo hỏng (mức CAO): âffaf. Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:35:31.318776',NULL),(81,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:35:31.327815',NULL),(82,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:35:31.338466',NULL),(83,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:35:31.354604',NULL),(84,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Laptop Dell Latitude 5420 (Mức độ: TRUNG_BINH). Lỗi: vfvdfdfg','ADMIN','? Báo hỏng mới: Laptop Dell Latitude 5420','/admin/bao-hong',NULL,'2026-06-04 23:36:56.604918',NULL),(85,_binary '\0','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Laptop Dell Latitude 5420 (Mức độ: TRUNG_BINH). Lỗi: vfvdfdfg','NHAN_VIEN_CSVC','? Báo hỏng mới: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:36:56.610664',NULL),(86,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 5.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:45:35.215875',NULL),(87,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:45:35.228324',NULL),(88,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:45:35.241060',NULL),(89,_binary '\0','HE_THONG',NULL,'Hôm nay có 5 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:45:35.253041',NULL),(90,_binary '\0','BAO_HONG',NULL,'Thiết bị TEst 1 vừa được báo hỏng (mức KHAN_CAP): vc . Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:45:35.268742',NULL),(91,_binary '\0','BAO_HONG',NULL,'Thiết bị PC HP EliteDesk 800 G8 vừa được báo hỏng (mức CAO): âffaf. Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:45:35.281673',NULL),(92,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:45:35.295764',NULL),(93,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:45:35.310640',NULL),(94,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:45:35.324345',NULL),(95,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 5.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:46:47.531437',NULL),(96,_binary '','BAO_HONG',NULL,'Phát hiện 2 thiết bị hỏng mức nghiêm trọng. Vui lòng kiểm tra và xử lý ngay để đảm bảo hoạt động giảng dạy.','ADMIN','? AI Cảnh báo: 2 thiết bị hỏng khẩn cấp','/admin/bao-hong',NULL,'2026-06-04 23:46:47.547033',NULL),(97,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:46:47.560166',NULL),(98,_binary '\0','HE_THONG',NULL,'Hôm nay có 5 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:46:47.581070',NULL),(99,_binary '\0','BAO_HONG',NULL,'Thiết bị TEst 1 vừa được báo hỏng (mức KHAN_CAP): vc . Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:46:47.594064',NULL),(100,_binary '\0','BAO_HONG',NULL,'Thiết bị PC HP EliteDesk 800 G8 vừa được báo hỏng (mức CAO): âffaf. Vui lòng kiểm tra và xử lý sớm.','NHAN_VIEN_CSVC','? Báo hỏng khẩn: PC HP EliteDesk 800 G8','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:46:47.606925',NULL),(101,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:46:47.623993',NULL),(102,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:46:47.640562',NULL),(103,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:46:47.653036',NULL),(104,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Máy in HP LaserJet Pro M404dn (Mức độ: THAP). Lỗi: sdfff','ADMIN','? Báo hỏng mới: Máy in HP LaserJet Pro M404dn','/admin/bao-hong',NULL,'2026-06-04 23:48:45.885759',NULL),(105,_binary '\0','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị Máy in HP LaserJet Pro M404dn (Mức độ: THAP). Lỗi: sdfff','NHAN_VIEN_CSVC','? Báo hỏng mới: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-hong',NULL,'2026-06-04 23:48:45.888272',NULL),(106,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 2.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:57:20.197433',NULL),(107,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:57:20.207699',NULL),(108,_binary '\0','HE_THONG',NULL,'Hôm nay có 2 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:57:20.218199',NULL),(109,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:57:20.229582',NULL),(110,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:57:20.239800',NULL),(111,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:57:20.249106',NULL),(112,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 2.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-04 23:58:40.910405',NULL),(113,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-04 23:58:40.938645',NULL),(114,_binary '\0','HE_THONG',NULL,'Hôm nay có 2 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-04 23:58:40.950344',NULL),(115,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:58:40.983346',NULL),(116,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:58:41.007765',NULL),(117,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-04 23:58:41.021677',NULL),(118,_binary '','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị TEst 1 (Mức độ: TRUNG_BINH). Lỗi: aqafaf','ADMIN','? Báo hỏng mới: TEst 1','/admin/bao-hong',NULL,'2026-06-05 00:00:38.403864',NULL),(119,_binary '\0','BAO_HONG',NULL,'Giáo viên Nguyễn Văn An vừa báo hỏng thiết bị TEst 1 (Mức độ: TRUNG_BINH). Lỗi: aqafaf','NHAN_VIEN_CSVC','? Báo hỏng mới: TEst 1','/nhanvien-csvc/bao-hong',NULL,'2026-06-05 00:00:38.407374',NULL),(120,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:04:44.081766',2),(121,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:04:44.129954',NULL),(122,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:04:44.138680',NULL),(123,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:04:44.156321',NULL),(124,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:04:44.170167',NULL),(125,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:04:44.180585',NULL),(126,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:04:44.198911',NULL),(127,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:12:15.375995',2),(128,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:12:15.415556',NULL),(129,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:12:15.424402',NULL),(130,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 2 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:12:15.433205',NULL),(131,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Máy in HP LaserJet Pro M404dn đã hoàn tất với kết quả: CAN_THAY_THE. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Máy in HP LaserJet Pro M404dn','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:12:15.442278',NULL),(132,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Loa JBL EON615 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Loa JBL EON615','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:12:15.452390',NULL),(133,_binary '\0','BAO_TRI',NULL,'Bảo trì thiết bị Laptop Dell Latitude 5420 đã hoàn tất với kết quả: THANH_CONG. Cập nhật trạng thái thiết bị nếu cần thiết.','NHAN_VIEN_CSVC','? Bảo trì hoàn tất: Laptop Dell Latitude 5420','/nhanvien-csvc/bao-tri',NULL,'2026-06-05 00:12:15.462945',NULL),(134,_binary '','BAO_HONG',NULL,'Phiếu báo hỏng thiết bị Máy in HP LaserJet Pro M404dn vừa được cập nhật.','ADMIN','? Hệ thống: Cập nhật báo hỏng','/admin/bao-hong/view/2',NULL,'2026-06-05 00:15:27.139131',NULL),(135,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:26:33.918316',NULL),(136,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:26:33.933371',NULL),(137,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:26:33.943687',NULL),(138,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:26:33.963415',2),(139,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:29:02.597424',NULL),(140,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:29:02.610821',NULL),(141,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:29:02.623128',NULL),(142,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:29:02.651375',2),(143,_binary '','THIET_BI',NULL,'Thiết bị Máy in HP LaserJet Pro M404dn (Mã: TB010) vừa được chỉnh sửa.','ADMIN','? Hệ thống: Cập nhật thiết bị','/admin/thiet-bi',NULL,'2026-06-05 00:31:38.608127',NULL),(144,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:35:13.393717',NULL),(145,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:35:13.411751',NULL),(146,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:35:13.424998',NULL),(147,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:35:13.451302',2),(148,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:43:05.100047',NULL),(149,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:43:05.120259',NULL),(150,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:43:05.126486',NULL),(151,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:43:05.156597',2),(152,_binary '','THIET_BI',NULL,'Thiết bị PC HP EliteDesk 800 G8 (Mã: TB009) vừa được chỉnh sửa.','ADMIN','? Hệ thống: Cập nhật thiết bị','/admin/thiet-bi',NULL,'2026-06-05 00:43:42.109830',NULL),(153,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:45:02.764120',NULL),(154,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:45:02.774758',NULL),(155,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:45:02.784577',NULL),(156,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:45:02.809714',2),(157,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:46:16.960697',NULL),(158,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:46:16.980319',NULL),(159,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:46:16.985702',NULL),(160,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:46:17.009369',2),(161,_binary '','THIET_BI',NULL,'Thiết bị PC HP EliteDesk 800 G8 (Mã: TB009) vừa được chỉnh sửa.','ADMIN','? Hệ thống: Cập nhật thiết bị','/admin/thiet-bi',NULL,'2026-06-05 00:46:36.019216',NULL),(162,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 00:52:40.305641',NULL),(163,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 00:52:40.336201',NULL),(164,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 00:52:40.344592',NULL),(165,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 00:52:40.375147',2),(166,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 01:04:51.554978',NULL),(167,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 01:04:51.590652',NULL),(168,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 01:04:51.605600',NULL),(169,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 01:04:51.634841',2),(170,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 01:13:53.411501',NULL),(171,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 01:13:53.440323',NULL),(172,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 01:13:53.449427',NULL),(173,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 01:13:53.485056',2),(174,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 01:23:53.543194',NULL),(175,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 01:23:53.556596',NULL),(176,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 01:23:53.567398',NULL),(177,_binary '','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 01:23:53.590936',2),(178,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 01:25:57.724747',NULL),(179,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 01:25:57.746596',NULL),(180,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 01:25:57.759706',NULL),(181,_binary '','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 01:25:57.789735',2),(182,_binary '','THIET_BI',NULL,'Thiết bị PC HP EliteDesk 800 G8 (Mã: TB009) vừa được chỉnh sửa.','ADMIN','? Hệ thống: Cập nhật thiết bị','/admin/thiet-bi',NULL,'2026-06-05 01:27:09.788478',NULL),(183,_binary '','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 01:54:02.138473',NULL),(184,_binary '','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 01:54:02.149946',NULL),(185,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 01:54:02.165947',NULL),(186,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 01:54:02.214174',2),(187,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:02:35.554047',NULL),(188,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:02:35.567775',NULL),(189,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:02:35.581044',NULL),(190,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:02:35.615014',2),(191,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:07:41.277629',NULL),(192,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:07:41.301040',NULL),(193,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:07:41.306781',NULL),(194,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:07:41.339657',2),(195,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:15:09.400924',NULL),(196,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:15:09.438866',NULL),(197,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:15:09.450667',NULL),(198,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:15:09.492196',2),(199,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:20:45.851869',NULL),(200,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:20:45.862229',NULL),(201,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:20:45.873272',NULL),(202,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:20:45.894217',2),(203,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:23:24.095246',NULL),(204,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:23:24.114563',NULL),(205,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:23:24.118606',NULL),(206,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:23:24.144849',2),(207,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:27:34.401807',NULL),(208,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:27:34.408907',NULL),(209,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:27:34.432182',NULL),(210,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:27:34.446609',2),(211,_binary '\0','HE_THONG',NULL,'Hệ thống hiện có 11 thiết bị, đang mượn: 2, quá hạn: 1, báo hỏng chờ xử lý: 3.','ADMIN','? AI Báo cáo hệ thống tự động','/admin',NULL,'2026-06-05 02:28:41.261059',NULL),(212,_binary '\0','MUON_TRA',NULL,'Có 1 phiếu mượn đã quá hạn trả. Đề nghị liên hệ người mượn để thu hồi thiết bị kịp thời.','ADMIN','⚠️ AI: 1 thiết bị mượn quá hạn','/admin/muon-tra',NULL,'2026-06-05 02:28:41.295050',NULL),(213,_binary '\0','HE_THONG',NULL,'Hôm nay có 3 đơn báo hỏng chờ xử lý, 1 đơn đang xử lý. Ưu tiên xử lý các thiết bị khẩn cấp trước.','NHAN_VIEN_CSVC','? AI Tóm tắt công việc hôm nay','/nhanvien-csvc',NULL,'2026-06-05 02:28:41.306959',NULL),(214,_binary '\0','MUON_TRA',NULL,'Nhắc nhở: Thiết bị \"Laptop Dell Latitude 5420\" bạn đang mượn sẽ hết hạn vào ngày 10/06/2026 (còn 5 ngày). Vui lòng hoàn trả đúng hạn, cảm ơn bạn!',NULL,'⏰ Nhắc nhở: Hạn trả thiết bị còn 5 ngày','/giao-vien/muon-tra',NULL,'2026-06-05 02:28:41.353000',2);
/*!40000 ALTER TABLE `thong_bao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `v_lich_su_muon_tra`
--

DROP TABLE IF EXISTS `v_lich_su_muon_tra`;
/*!50001 DROP VIEW IF EXISTS `v_lich_su_muon_tra`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_lich_su_muon_tra` AS SELECT 
 1 AS `id`,
 1 AS `nguoi_muon`,
 1 AS `ma_thiet_bi`,
 1 AS `ten_thiet_bi`,
 1 AS `ma_phong`,
 1 AS `ngay_muon`,
 1 AS `ngay_tra_du_kien`,
 1 AS `ngay_tra_thuc_te`,
 1 AS `trang_thai`,
 1 AS `gio_muon`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_thiet_bi_can_bao_tri`
--

DROP TABLE IF EXISTS `v_thiet_bi_can_bao_tri`;
/*!50001 DROP VIEW IF EXISTS `v_thiet_bi_can_bao_tri`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_thiet_bi_can_bao_tri` AS SELECT 
 1 AS `id`,
 1 AS `ma_thiet_bi`,
 1 AS `ten_thiet_bi`,
 1 AS `ma_phong`,
 1 AS `xac_suat_hong`,
 1 AS `muc_do_rui_ro`,
 1 AS `ngay_du_kien_hong`,
 1 AS `hanh_dong_de_xuat`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_thong_ke_thiet_bi`
--

DROP TABLE IF EXISTS `v_thong_ke_thiet_bi`;
/*!50001 DROP VIEW IF EXISTS `v_thong_ke_thiet_bi`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_thong_ke_thiet_bi` AS SELECT 
 1 AS `ten_loai`,
 1 AS `tong_so`,
 1 AS `so_tot`,
 1 AS `so_bao_tri`,
 1 AS `so_hong`,
 1 AS `so_thanh_ly`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_lich_su_muon_tra`
--

/*!50001 DROP VIEW IF EXISTS `v_lich_su_muon_tra`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_lich_su_muon_tra` AS select `mt`.`id` AS `id`,`nd`.`ho_ten` AS `nguoi_muon`,`tb`.`ma_thiet_bi` AS `ma_thiet_bi`,`tb`.`ten_thiet_bi` AS `ten_thiet_bi`,`ph`.`ma_phong` AS `ma_phong`,`mt`.`ngay_muon` AS `ngay_muon`,`mt`.`ngay_tra_du_kien` AS `ngay_tra_du_kien`,`mt`.`ngay_tra_thuc_te` AS `ngay_tra_thuc_te`,`mt`.`trang_thai` AS `trang_thai`,(case when (`mt`.`trang_thai` = 'DA_TRA') then timestampdiff(HOUR,`mt`.`ngay_muon`,`mt`.`ngay_tra_thuc_te`) else NULL end) AS `gio_muon` from (((`muon_tra_thiet_bi` `mt` join `nguoi_dung` `nd` on((`mt`.`nguoi_muon_id` = `nd`.`id`))) join `thiet_bi` `tb` on((`mt`.`thiet_bi_id` = `tb`.`id`))) left join `phong_hoc` `ph` on((`tb`.`phong_id` = `ph`.`id`))) order by `mt`.`ngay_muon` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_thiet_bi_can_bao_tri`
--

/*!50001 DROP VIEW IF EXISTS `v_thiet_bi_can_bao_tri`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_thiet_bi_can_bao_tri` AS select `tb`.`id` AS `id`,`tb`.`ma_thiet_bi` AS `ma_thiet_bi`,`tb`.`ten_thiet_bi` AS `ten_thiet_bi`,`ph`.`ma_phong` AS `ma_phong`,`ai`.`xac_suat_hong` AS `xac_suat_hong`,`ai`.`muc_do_rui_ro` AS `muc_do_rui_ro`,`ai`.`ngay_du_kien_hong` AS `ngay_du_kien_hong`,`ai`.`hanh_dong_de_xuat` AS `hanh_dong_de_xuat` from ((`thiet_bi` `tb` join `ai_du_doan_bao_tri` `ai` on((`tb`.`id` = `ai`.`thiet_bi_id`))) left join `phong_hoc` `ph` on((`tb`.`phong_id` = `ph`.`id`))) where ((`ai`.`muc_do_rui_ro` in ('CAO','NGUY_HIEM')) and (`ai`.`ngay_du_doan` = (select max(`ai_du_doan_bao_tri`.`ngay_du_doan`) from `ai_du_doan_bao_tri` where (`ai_du_doan_bao_tri`.`thiet_bi_id` = `tb`.`id`)))) order by `ai`.`xac_suat_hong` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_thong_ke_thiet_bi`
--

/*!50001 DROP VIEW IF EXISTS `v_thong_ke_thiet_bi`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_unicode_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_thong_ke_thiet_bi` AS select `ltb`.`ten_loai` AS `ten_loai`,count(0) AS `tong_so`,sum((case when (`tb`.`trang_thai` = 'TOT') then 1 else 0 end)) AS `so_tot`,sum((case when (`tb`.`trang_thai` = 'BAO_TRI') then 1 else 0 end)) AS `so_bao_tri`,sum((case when (`tb`.`trang_thai` = 'HONG') then 1 else 0 end)) AS `so_hong`,sum((case when (`tb`.`trang_thai` = 'THANH_LY') then 1 else 0 end)) AS `so_thanh_ly` from (`thiet_bi` `tb` join `loai_thiet_bi` `ltb` on((`tb`.`loai_thiet_bi_id` = `ltb`.`id`))) group by `ltb`.`id`,`ltb`.`ten_loai` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-05 18:28:06
