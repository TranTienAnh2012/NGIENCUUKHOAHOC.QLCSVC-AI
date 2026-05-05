# 📋 BÁO CÁO CHUYỂN GIAO — Hệ thống QLCSVC AI

**Người thực hiện:** Luc Van Son (2310900087)  
**Branch:** `feature/luc-van-son`  
**Repository:** `TranTienAnh2012/NGIENCUUKHOAHOC.QLCSVC-AI`  
**Thời gian:** 10/03/2026 → 24/04/2026  
**Tổng cộng:** 15 commits · 17 files · **+3,803 dòng code**

---

## 1. TỔNG QUAN HỆ THỐNG

Tích hợp **AI (Gemini Vision)** vào hệ thống Quản lý Cơ sở Vật chất (QLCSVC), cung cấp:

| Tính năng | Mô tả |
|-----------|-------|
| 🤖 Nhận diện thiết bị bằng ảnh | Chụp ảnh → Gemini Vision nhận diện hãng, model, tình trạng |
| 🔐 Chống tráo QR | AI so sánh ảnh chụp thực vs dữ liệu DB → phát hiện gian lận |
| 📱 Quét QR Code | Scan mã QR trên thiết bị → xem thông tin chi tiết |
| 🚨 Báo hỏng tự động | AI phân tích mức độ hư hỏng, gợi ý bảo trì |
| 🔍 Tra cứu + Voice Search | Tìm thiết bị bằng từ khóa hoặc giọng nói |
| 🏷️ In lại QR | Nhân viên bảo trì in lại mã QR khi bị mất |

---

## 2. KIẾN TRÚC

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────┐
│  Browser/Mobile  │◄────►│  Flask (Python)   │◄────►│ Spring Boot │
│  (Frontend)      │      │  Port 5000        │      │ Port 8080   │
│                  │      │  - AI routes      │      │ - DB access │
│                  │      │  - Gemini Vision   │      │ - REST API  │
└─────────────────┘      └──────────────────┘      └─────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │  Google AI   │
                         │ Gemini Flash │
                         └──────────────┘
```

- **Spring Boot** (Java): Backend chính, quản lý DB, xác thực
- **Flask** (Python): AI proxy, Gemini Vision, render trang scan/QR
- **Gemini Vision**: Google AI nhận diện hình ảnh thiết bị

---

## 3. DANH SÁCH API ENDPOINTS

### Flask AI API (`ai_api.py` — Port 5000)

| Method | Endpoint | Chức năng |
|--------|----------|-----------|
| GET | `/api/ai/health` | Kiểm tra Flask hoạt động |
| POST | `/api/ai/chatbot` | AI chatbot hỗ trợ |
| POST | `/api/ai/analyze-damage` | AI phân tích hư hỏng |
| POST | `/api/ai/suggest-maintenance` | AI gợi ý bảo trì |
| POST | `/api/ai/categorize-equipment` | AI phân loại thiết bị |
| POST | `/api/ai/verify-device` | **AI xác minh QR** (chống tráo) |
| POST | `/api/ai/scan-image` | **AI nhận diện** thiết bị từ ảnh |
| GET | `/api/ai/device-qr/<id>` | Tạo ảnh QR cho thiết bị |
| GET | `/api/ai/device-info/<id>` | Trang thông tin thiết bị (mobile) |
| GET | `/api/ai/qr-print` | In lại QR cho nhân viên bảo trì |
| GET | `/api/ai/report-form` | Form báo hỏng (scan QR → báo) |
| POST | `/api/ai/submit-report` | Gửi báo hỏng |
| GET | `/api/ai/device-lookup` | Tra cứu thiết bị + Voice Search |
| GET | `/api/ai/scan` | **Trang scan thống nhất** (QR + AI) |

### Spring Boot API (`AiDataController.java` — Port 8080)

| Endpoint | Chức năng |
|----------|-----------|
| `/api/ai-data/devices` | Danh sách thiết bị (JSON) |
| `/api/ai-data/damage-reports` | Danh sách báo hỏng |
| `/api/ai-data/borrowings` | Lịch sử mượn trả |

---

## 4. CHI TIẾT COMMITS THEO GIAI ĐOẠN

### 🔹 Giai đoạn 1: Nền tảng AI (10-12/03/2026)

| Commit | Nội dung |
|--------|----------|
| `5c4fde3` | **Initial commit** — Tạo `ai_api.py` với Flask + Gemini, endpoints cơ bản (chatbot, analyze-damage, suggest-maintenance) |
| `4e4db78` | **AI Image Scan** — Thêm `/api/ai/scan-image`: chụp ảnh → Gemini Vision nhận diện thiết bị |
| `0165a01` | **Fix Gemini model** — Sửa lỗi `gemini-1.5-flash` không hợp lệ, dùng global model `gemini-flash-latest` |
| `e23acd3` | **Cải thiện scan result** — Hiển thị 5 section: thông tin cơ bản, DB match, lịch sử hỏng, mượn trả, gợi ý AI |
| `f11e98a` | **QR Code generation** — 3 endpoint mới: tạo QR, in nhãn QR đẹp, trang device-info mobile |

### 🔹 Giai đoạn 2: QR Workflow (13-14/04/2026)

| Commit | Nội dung |
|--------|----------|
| `9d7882b` | **QR Label đẹp** — Nhãn QR có logo, tên thiết bị, phòng — dán lên máy |
| `7e93227` | **Form báo hỏng** — Quét QR → form báo hỏng (người dùng ẩn danh cũng dùng được) |
| `7e19f0e` | **Fix route order** — Sửa lỗi routes đăng ký sau `app.run()` |
| `2f127ba` | **Full AI integration** — Tích hợp AI chatbot, phân tích hỏng, gợi ý bảo trì vào admin panel |
| `372cbf5` | **Fix field names** — API chấp nhận cả 2 format field name (cũ & mới) |

### 🔹 Giai đoạn 3: Fix & UI (14/04/2026)

| Commit | Nội dung |
|--------|----------|
| `05f1440` | **Fix DataInitializer** — Ngừng reset mật khẩu admin mỗi lần restart |
| `c8b0bdf` | **Fix edit user** — Form sửa user cập nhật password đúng |
| `b4aa567` | **Nút Scan QR** — Thêm nút 📷 vào trang admin + user |
| `dd9aafe` | **Nút Scan QR** — Thêm nút 📷 vào layout giáo viên + nhân viên |

### 🔹 Giai đoạn 4: AI Nâng cao (23-24/04/2026)

| Commit | Nội dung |
|--------|----------|
| `1bfd609` | **AI Xác minh Camera** — Gemini Vision so sánh ảnh chụp với DB (chống tráo QR), tra cứu thiết bị, voice search, sidebar QR tools |
| `d9987d6` | **Gộp Scan thống nhất** — 1 trang `/api/ai/scan` với 2 tab (QR + AI), xóa camera code rải rác |

---

## 5. CÁC FILE CHÍNH CẦN BIẾT

```
DHNT/
├── ai_api.py                          ← 🔥 FILE CHÍNH: Toàn bộ Flask AI (2800+ dòng)
├── .env                               ← GEMINI_API_KEY (BÍ MẬT)
├── requirements.txt                   ← Dependencies Python
│
├── src/main/java/.../
│   ├── api/AiDataController.java      ← Spring Boot API cho Flask gọi
│   ├── config/DataInitializer.java    ← Khởi tạo data mặc định
│   ├── entity/LoaiThietBi.java        ← Entity loại thiết bị (đã fix lazy)
│   └── areas/admin/service/
│       ├── AdminThietBiService.java   ← Service thiết bị
│       ├── AdminLoaiThietBiService.java
│       └── AdminNguoiDungService.java
│
└── src/main/resources/templates/layout/
    ├── sidebar.html                   ← Menu sidebar (có nhóm "Công cụ QR")
    ├── main.html                      ← Layout chính + nút scan
    ├── giaovien-layout.html           ← Layout giáo viên + nút scan
    └── nhanvien-layout.html           ← Layout nhân viên + nút scan
```

---

## 6. HƯỚNG DẪN CHẠY

### Bước 1: Chạy Spring Boot (Backend)
```bash
cd d:\K23CNT3-LucVanSon-2310900087\DHNT
.\mvnw.cmd spring-boot:run
# → Chạy tại http://localhost:8080
```

### Bước 2: Chạy Flask AI (Frontend AI)
```bash
cd d:\K23CNT3-LucVanSon-2310900087\DHNT
python ai_api.py
# → Chạy tại http://localhost:5000
```

### Bước 3: Truy cập
| URL | Nơi |
|-----|-----|
| `http://localhost:8080` | Trang admin/user chính |
| `http://localhost:5000/api/ai/scan` | Scan QR + AI Camera |
| `http://localhost:5000/api/ai/device-lookup` | Tra cứu thiết bị |
| `http://localhost:5000/api/ai/device-info/1` | Xem thiết bị ID=1 |

> [!IMPORTANT]
> File `.env` phải có `GEMINI_API_KEY=...` (lấy từ Google AI Studio)

---

## 7. LUỒNG CHÍNH NGƯỜI DÙNG SỬ DỤNG

### Luồng 1: Quét QR → Xem thông tin
```
📱 Mở Scan → Tab "Quét QR" → Quét mã QR trên thiết bị
    → Tự động chuyển đến trang thông tin thiết bị
    → Xem: tên, phòng, trạng thái, lịch sử hỏng, AI đánh giá
```

### Luồng 2: Chụp ảnh → AI nhận diện
```
📸 Mở Scan → Tab "Chụp ảnh AI" → Chụp thiết bị
    → Gemini Vision nhận diện: hãng, model, tình trạng
    → Đối chiếu DB → Xem chi tiết hoặc Báo hỏng
```

### Luồng 3: Xác minh thiết bị (chống tráo QR)
```
🔐 Trên trang device-info → Nhấn "AI Xác minh bằng Camera"
    → Chuyển sang Scan?verify=ID → Tab AI tự bật
    → Chụp ảnh thiết bị thật → AI so sánh với DB
    → ✅ KHỚP (85%) hoặc ❌ KHÔNG KHỚP — QR bị tráo!
```

### Luồng 4: Nhân viên bảo trì tra cứu
```
🔍 Sidebar → "Tra cứu thiết bị" → Gõ tên hoặc 🎤 nói giọng
    → Bảng kết quả → "In lại QR" khi mã bị mất
```

---

## 8. GHI CHÚ KỸ THUẬT

> [!NOTE]
> - **Flask giao tiếp Spring Boot** qua `INTERNAL_HEADERS` (không public)
> - **Gemini Vision** dùng model `gemini-flash-latest` (miễn phí, nhanh)
> - **PIL** resize ảnh trước khi gửi AI (tiết kiệm token)
> - **Voice Search** dùng Web Speech API (chỉ Chrome/Edge)
> - **QR Code** sinh bằng thư viện `qrcode` Python

> [!WARNING]
> - Trường `hinh_anh_chinh` trong DB vẫn `NULL` → UI dùng placeholder
> - Lint IDE báo "Cannot find flask/PIL" → do `.venv` path, **KHÔNG ảnh hưởng runtime**

---

*Tài liệu này được tạo tự động ngày 24/04/2026*
