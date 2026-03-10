# Flask AI API - Hướng dẫn Cài đặt và Sử dụng

## 📋 Tổng quan

Flask AI API cung cấp các tính năng AI cho hệ thống quản lý cơ sở vật chất sử dụng Google Gemini API.

## 🚀 Cài đặt

### 1. Cài đặt Python

Đảm bảo Python 3.8+ đã được cài đặt:
```bash
python --version
```

### 2. Cài đặt Dependencies

```bash
# Di chuyển vào thư mục project
cd d:\NGIENCUUKHOAHOC.QLCSVC-AI\DHNT

# Cài đặt các thư viện cần thiết
pip install -r requirements.txt
```

### 3. Cấu hình API Key

File `.env` đã được tạo sẵn với API key của bạn. Nếu cần thay đổi:

```bash
# Mở file .env và chỉnh sửa
GEMINI_API_KEY=your_new_api_key_here
```

## ▶️ Chạy Server

```bash
# Chạy Flask server
python ai_api.py
```

Server sẽ chạy tại: `http://localhost:5000`

## 📡 API Endpoints

### 1. Health Check
```bash
GET http://localhost:5000/api/ai/health
```

**Response:**
```json
{
  "status": "healthy",
  "gemini_api": "connected",
  "timestamp": "2026-01-26T08:30:00",
  "version": "1.0.0"
}
```

---

### 2. Chatbot Tư vấn
```bash
POST http://localhost:5000/api/ai/chatbot
Content-Type: application/json

{
  "message": "Làm thế nào để bảo trì máy chiếu?",
  "context": {
    "user_role": "GIAO_VIEN"
  }
}
```

**Response:**
```json
{
  "success": true,
  "response": "Để bảo trì máy chiếu hiệu quả, bạn nên...",
  "suggestions": [
    "Xem hướng dẫn bảo trì thiết bị",
    "Báo cáo hư hỏng thiết bị"
  ],
  "timestamp": "2026-01-26T08:30:00"
}
```

---

### 3. Phân tích Báo hỏng
```bash
POST http://localhost:5000/api/ai/analyze-damage
Content-Type: application/json

{
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "damage_description": "Không hiển thị hình ảnh, đèn sáng nhưng màn hình trắng",
  "severity": "CAO"
}
```

**Response:**
```json
{
  "success": true,
  "analysis": "Nguyên nhân có thể:\n1. Bóng đèn hỏng...\n2. Cáp HDMI lỏng...",
  "priority": "high",
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "severity": "CAO",
  "timestamp": "2026-01-26T08:30:00"
}
```

---

### 4. Gợi ý Bảo trì
```bash
POST http://localhost:5000/api/ai/suggest-maintenance
Content-Type: application/json

{
  "equipment_id": 1,
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "maintenance_history": [
    {
      "date": "2024-01-15",
      "type": "DINH_KY",
      "result": "THANH_CONG"
    }
  ],
  "current_status": "TOT"
}
```

**Response:**
```json
{
  "success": true,
  "recommendations": "Lịch bảo trì tiếp theo:\n- Ngày: 2024-07-15\n- Loại: Bảo trì định kỳ...",
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "current_status": "TOT",
  "timestamp": "2026-01-26T08:30:00"
}
```

---

### 5. Phân loại Thiết bị
```bash
POST http://localhost:5000/api/ai/categorize-equipment
Content-Type: application/json

{
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "description": "Máy chiếu dùng cho phòng học"
}
```

**Response:**
```json
{
  "success": true,
  "equipment_name": "Máy chiếu Panasonic PT-LB360",
  "category": "Thiết bị điện tử",
  "subcategory": "Máy chiếu",
  "confidence": 0.95,
  "timestamp": "2026-01-26T08:30:00"
}
```

## 🔗 Tích hợp với Spring Boot

### Tạo REST Client trong Spring Boot

```java
@Service
public class AiApiService {
    
    private final RestTemplate restTemplate;
    private final String AI_API_URL = "http://localhost:5000/api/ai";
    
    public AiApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public String getChatbotResponse(String message) {
        String url = AI_API_URL + "/chatbot";
        
        Map<String, Object> request = new HashMap<>();
        request.put("message", message);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, request, Map.class
        );
        
        return (String) response.getBody().get("response");
    }
    
    public String analyzeDamage(String equipmentName, String description, String severity) {
        String url = AI_API_URL + "/analyze-damage";
        
        Map<String, Object> request = new HashMap<>();
        request.put("equipment_name", equipmentName);
        request.put("damage_description", description);
        request.put("severity", severity);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, request, Map.class
        );
        
        return (String) response.getBody().get("analysis");
    }
}
```

## 🧪 Testing với curl

```bash
# Test health check
curl http://localhost:5000/api/ai/health

# Test chatbot
curl -X POST http://localhost:5000/api/ai/chatbot \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"Cách bảo trì máy chiếu?\"}"

# Test analyze damage
curl -X POST http://localhost:5000/api/ai/analyze-damage \
  -H "Content-Type: application/json" \
  -d "{\"equipment_name\": \"Máy chiếu\", \"damage_description\": \"Không hiển thị\", \"severity\": \"CAO\"}"
```

## ⚠️ Lưu ý

1. **API Key**: Không commit file `.env` lên Git
2. **Port**: Đảm bảo port 5000 không bị chiếm dụng
3. **Rate Limit**: Gemini API free tier: 60 requests/phút
4. **CORS**: Đã enable CORS để Spring Boot có thể gọi API

## 🐛 Troubleshooting

### Lỗi: "GEMINI_API_KEY không được tìm thấy"
- Kiểm tra file `.env` có tồn tại và chứa API key
- Đảm bảo file `.env` nằm cùng thư mục với `ai_api.py`

### Lỗi: "Port 5000 already in use"
- Thay đổi port trong file `.env`: `FLASK_PORT=5001`
- Hoặc kill process đang dùng port 5000

### Lỗi khi cài đặt dependencies
```bash
# Nếu gặp lỗi, thử upgrade pip
python -m pip install --upgrade pip

# Sau đó cài lại
pip install -r requirements.txt
```

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Python version >= 3.8
2. API key hợp lệ
3. Internet connection (để gọi Gemini API)
4. Port 5000 available
