"""
Flask API với Google Gemini AI cho Hệ thống Quản lý Cơ sở Vật chất
Tác giả: AI Assistant
Ngày tạo: 2026-01-26
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import google.generativeai as genai
import os
from dotenv import load_dotenv
from datetime import datetime
import json
import base64
import io

# Thư viện xử lý ảnh (cần cài: pip install Pillow)
try:
    from PIL import Image
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False
    print("Warning: Pillow not installed. Image resizing disabled. Run: pip install Pillow")

# Load environment variables
load_dotenv()

# Khởi tạo Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS cho phép Spring Boot gọi API

# Cấu hình Google Gemini
GEMINI_API_KEY = os.getenv('GEMINI_API_KEY')
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY không được tìm thấy trong file .env")

genai.configure(api_key=GEMINI_API_KEY)

# Internal API Key để gọi Spring Boot /api/ai-data/**
# Phải khớp với internal.api-key trong application.properties
INTERNAL_API_KEY = os.getenv('INTERNAL_API_KEY', 'qlcsvc-internal-key-dhnt-2026-flask-secret')
if not INTERNAL_API_KEY:
    raise ValueError("INTERNAL_API_KEY không được tìm thấy trong file .env")

# Header bảo mật dùng khi gọi Spring Boot API
INTERNAL_HEADERS = {
    'X-Internal-API-Key': INTERNAL_API_KEY,
    'Content-Type': 'application/json'
}

# Khởi tạo Gemini model
model = genai.GenerativeModel('gemini-flash-latest')

# ============================================================
# SESSION MANAGEMENT - In-memory storage cho conversation history
# ============================================================
user_sessions = {}  # {"user_id_session_id": [{"role": "user", "content": "..."}, ...]}

def get_session_key(user_id, session_id):
    """Tạo key duy nhất cho session"""
    return f"{user_id}_{session_id}"

def get_conversation_history(user_id, session_id):
    """Lấy lịch sử chat từ session"""
    key = get_session_key(user_id, session_id)
    return user_sessions.get(key, [])

def save_conversation(user_id, session_id, user_msg, ai_response):
    """Lưu conversation vào session"""
    key = get_session_key(user_id, session_id)
    if key not in user_sessions:
        user_sessions[key] = []
    
    # Thêm tin nhắn mới
    user_sessions[key].append({"role": "user", "content": user_msg})
    user_sessions[key].append({"role": "assistant", "content": ai_response})
    
    # Giới hạn history (giữ 20 tin nhắn gần nhất = 10 cặp hỏi-đáp)
    if len(user_sessions[key]) > 20:
        user_sessions[key] = user_sessions[key][-20:]

def build_prompt_with_history(user_message, history, context):
    """Xây dựng prompt với conversation history"""
    if not history:
        return user_message
    
    # Tạo context từ lịch sử
    history_text = "\n".join([
        f"{msg['role'].upper()}: {msg['content']}"
        for msg in history[-10:]  # Chỉ lấy 5 cặp gần nhất
    ])
    
    return f"""LỊCH SỬ HỘI THOẠI:
{history_text}

CÂU HỎI MỚI: {user_message}

Hãy trả lời dựa trên ngữ cảnh hội thoại trên."""
# ============================================================



# System prompts cho các chức năng khác nhau
CHATBOT_SYSTEM_PROMPT = """
Bạn là trợ lý AI chuyên về quản lý cơ sở vật chất trường học tại Việt Nam.
Nhiệm vụ của bạn là tư vấn, hỗ trợ về:
- Quản lý thiết bị, phòng học
- Bảo trì, sửa chữa thiết bị
- Quy trình báo hỏng, mượn trả thiết bị
- Tối ưu hóa sử dụng cơ sở vật chất

Hãy trả lời ngắn gọn, chuyên nghiệp, bằng tiếng Việt.
"""

DAMAGE_ANALYSIS_PROMPT = """
Bạn là chuyên gia phân tích hư hỏng thiết bị. 
Nhiệm vụ: Phân tích mô tả hư hỏng và đưa ra:
1. Nguyên nhân có thể xảy ra
2. Các bước xử lý cụ thể
3. Ước tính chi phí sửa chữa (nếu có thể)
4. Mức độ ưu tiên xử lý
5. Thời gian dự kiến hoàn thành

Trả lời bằng tiếng Việt, có cấu trúc rõ ràng.
"""

MAINTENANCE_SUGGESTION_PROMPT = """
Bạn là chuyên gia bảo trì thiết bị.
Dựa trên lịch sử bảo trì và tình trạng hiện tại, hãy đề xuất:
1. Lịch bảo trì tiếp theo (ngày cụ thể)
2. Loại bảo trì cần thực hiện
3. Các công việc cần làm
4. Vật tư cần chuẩn bị
5. Lưu ý đặc biệt

Trả lời bằng tiếng Việt, chuyên nghiệp.
"""

CATEGORIZATION_PROMPT = """
Bạn là chuyên gia phân loại thiết bị giáo dục.
Dựa trên tên và mô tả thiết bị, hãy phân loại vào các danh mục:
- Thiết bị điện tử (máy chiếu, máy tính, loa, micro...)
- Thiết bị văn phòng (bàn, ghế, tủ, bảng...)
- Thiết bị thí nghiệm (dụng cụ thí nghiệm, mô hình...)
- Thiết bị thể thao (bóng, lưới, dụng cụ tập...)
- Thiết bị khác

Trả về JSON format: {"category": "...", "subcategory": "...", "confidence": 0.0-1.0}
"""

# ============================================================
# IMAGE SCAN PROMPT - Dùng cho Gemini Vision (nhận diện ảnh)
# Gemini sẽ nhìn ảnh và trả lời theo JSON chuẩn này
# ============================================================
IMAGE_SCAN_PROMPT = """
Bạn là chuyên gia nhận dạng thiết bị cơ sở vật chất trường học qua hình ảnh.

Hãy phân tích kỹ ảnh được cung cấp và trả lời CHÍNH XÁC theo JSON format sau:
{
  "device_name": "Tên đầy đủ thiết bị (ví dụ: Máy chiếu Epson EB-X05)",
  "brand": "Hãng sản xuất (ví dụ: Epson, Dell, JBL)",
  "model": "Model hoặc mã thiết bị nhìn thấy trên ảnh (nếu không thấy thì để trống)",
  "device_type": "Loại thiết bị (ví dụ: Máy chiếu, Laptop, Loa, Máy tính bàn...)",
  "condition": "Mô tả tình trạng tổng thể nhìn thấy được (ví dụ: Bình thường, Có vết trầy xước...)",
  "damage_signs": "Liệt kê các dấu hiệu hư hỏng nếu có (để trống nếu không phát hiện)",
  "confidence": 0.85
}

CHÚ Ý:
- Chỉ trả về JSON, KHÔNG giải thích thêm
- Nếu không nhận ra thiết bị rõ ràng, confidence < 0.5
- confidence từ 0.0 đến 1.0
- Trả lời bằng tiếng Việt
"""


@app.route('/api/ai/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    try:
        # Test Gemini API connection
        test_response = model.generate_content("Hello")
        
        return jsonify({
            "status": "healthy",
            "gemini_api": "connected",
            "timestamp": datetime.now().isoformat(),
            "version": "1.0.0"
        }), 200
    except Exception as e:
        return jsonify({
            "status": "unhealthy",
            "gemini_api": "disconnected",
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 500


@app.route('/api/ai/chatbot', methods=['POST'])
def chatbot():
    """
    Chatbot tư vấn về cơ sở vật chất với session management
    
    Request body:
    {
        "message": "Câu hỏi của người dùng",
        "user_id": 123,              # BẮT BUỘC
        "session_id": "uuid-string", # BẮT BUỘC
        "context": {
            "user_role": "GIAO_VIEN"
        }
    }
    """
    try:
        import requests
        
        data = request.get_json()
        
        # Validate required fields
        if not data or 'message' not in data:
            return jsonify({
                "error": "Missing 'message' field",
                "success": False
            }), 400
        
        user_message = data['message']
        user_id = data.get('user_id')
        session_id = data.get('session_id')
        context = data.get('context', {})
        user_role = context.get('user_role', 'GIAO_VIEN')
        user_name = context.get('user_name', '')   # Tên thật của người dùng
        user_email = context.get('user_email', '') # Email tài khoản
        
        # Validate user_id và session_id (BẢO MẬT)
        if not user_id or not session_id:
            return jsonify({
                "error": "Missing 'user_id' or 'session_id'",
                "success": False
            }), 400
        
        # Lấy conversation history từ session
        conversation_history = get_conversation_history(user_id, session_id)
        
        # Lấy dữ liệu thực từ Spring Boot API
        SPRING_BOOT_API = "http://localhost:8080/api/ai-data"
        real_data = {}
        
        try:
            # Lấy thống kê tổng quan
            stats_response = requests.get(f"{SPRING_BOOT_API}/statistics", headers=INTERNAL_HEADERS, timeout=2)
            if stats_response.status_code == 200:
                real_data['statistics'] = stats_response.json()
            
            # Lấy TẤT CẢ thiết bị
            all_devices_response = requests.get(f"{SPRING_BOOT_API}/devices", headers=INTERNAL_HEADERS, timeout=2)
            if all_devices_response.status_code == 200:
                real_data['all_devices'] = all_devices_response.json()
            
            # Lấy thiết bị sẵn sàng
            devices_response = requests.get(f"{SPRING_BOOT_API}/devices?status=TOT", headers=INTERNAL_HEADERS, timeout=2)
            if devices_response.status_code == 200:
                real_data['available_devices'] = devices_response.json()
            
            # Lấy thiết bị cần bảo trì
            maintenance_response = requests.get(f"{SPRING_BOOT_API}/devices/maintenance-needed", headers=INTERNAL_HEADERS, timeout=2)
            if maintenance_response.status_code == 200:
                real_data['maintenance_devices'] = maintenance_response.json()
            
            # Lấy báo hỏng gần đây
            damages_response = requests.get(f"{SPRING_BOOT_API}/damages/recent?limit=5", headers=INTERNAL_HEADERS, timeout=2)
            if damages_response.status_code == 200:
                real_data['recent_damages'] = damages_response.json()
            
            # Lấy lượt mượn đang hoạt động (DANG_MUON)
            borrows_response = requests.get(f"{SPRING_BOOT_API}/borrows/active", headers=INTERNAL_HEADERS, timeout=2)
            if borrows_response.status_code == 200:
                real_data['active_borrows'] = borrows_response.json()
            
            # Lấy lượt đã trả (DA_TRA) – 20 gần nhất
            returned_response = requests.get(f"{SPRING_BOOT_API}/borrows/returned?limit=20", headers=INTERNAL_HEADERS, timeout=2)
            if returned_response.status_code == 200:
                real_data['returned_borrows'] = returned_response.json()
            
            # Lấy toàn bộ lịch sử mượn trả (50 gần nhất)
            all_borrows_response = requests.get(f"{SPRING_BOOT_API}/borrows/all?limit=50", headers=INTERNAL_HEADERS, timeout=2)
            if all_borrows_response.status_code == 200:
                real_data['all_borrows'] = all_borrows_response.json()
                
        except requests.exceptions.RequestException as e:
            print(f"Warning: Could not fetch data from Spring Boot API: {e}")
        
        # Tạo context với dữ liệu thực
        data_context = build_data_context(real_data, user_role)
        
        # Xây dựng prompt với conversation history
        message_with_history = build_prompt_with_history(user_message, conversation_history, context)
        
        # Map role codes sang tên tiếng Việt thân thiện
        role_name_map = {
            'ADMIN': 'Quản trị viên',
            'GIAO_VIEN': 'Giáo viên',
            'NHAN_VIEN_CSVC': 'Nhân viên CSVC',
        }
        role_display_name = role_name_map.get(user_role, user_role)

        # Xây dựng dòng giới thiệu người dùng rõ ràng (cả tên + email)
        identity_parts = []
        if user_name:
            identity_parts.append(f"Tên: {user_name}")
        if user_email:
            identity_parts.append(f"Email/Tài khoản: {user_email}")
        identity_parts.append(f"Vai trò: {role_display_name}")
        current_user_line = "DANH TÍNH NGƯỜI DÙNG ĐANG CHAT:\n" + "\n".join(f"  - {p}" for p in identity_parts)
        address_name = user_name if user_name else role_display_name

        # Tạo full prompt
        full_prompt = f"""{CHATBOT_SYSTEM_PROMPT}

{data_context}

{current_user_line}

{message_with_history}

QUAN TRỌNG:
- Hãy trả lời dựa trên DỮ LIỆU THỰC TẾ và LỊCH SỬ HỘI THOẠI
- Xưng hô với người dùng theo đúng tên: {address_name}
- TUYỆT ĐỐI KHÔNG NHẦM tên người dùng hiện tại với tên trong danh sách mượn/báo hỏng
- Nếu câu hỏi yêu cầu liệt kê, hãy LIỆT KÊ CHI TIẾT từng item
- Trả lời ngắn gọn nhưng đầy đủ thông tin
"""
        
        # Gọi Gemini API
        response = model.generate_content(full_prompt)
        ai_response = response.text
        
        # Lưu conversation vào session
        save_conversation(user_id, session_id, user_message, ai_response)
        
        # Parse response để tạo suggestions
        suggestions = generate_suggestions(user_role, real_data)
        
        return jsonify({
            "success": True,
            "response": ai_response,
            "suggestions": suggestions,
            "session_id": session_id,
            "has_real_data": len(real_data) > 0,
            "conversation_length": len(get_conversation_history(user_id, session_id)),
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 500


def build_data_context(real_data, user_role):
    """Xây dựng context từ dữ liệu thực"""
    if not real_data:
        return "CẢNH BÁO: Không có dữ liệu thực từ hệ thống. Hãy trả lời dựa trên kiến thức chung."
    
    context_parts = ["DỮ LIỆU THỰC TẾ TỪ HỆ THỐNG:"]
    
    # Thống kê
    if 'statistics' in real_data:
        stats = real_data['statistics']
        context_parts.append(f"""
THỐNG KÊ TỔNG QUAN:
- Tổng số thiết bị: {stats.get('total_devices', 0)}
- Thiết bị đang sẵn sàng: {stats.get('available_devices', 0)}
- Thiết bị đang được mượn: {stats.get('active_borrows', 0)} lượt
- Thiết bị đang bảo trì: {stats.get('maintenance_devices', 0)}
- Thiết bị hư hỏng: {stats.get('damaged_devices', 0)}
- Tổng số lượt mượn: {stats.get('total_borrows', 0)}
- Tổng số báo hỏng: {stats.get('total_damages', 0)}
""")
    
    # Thiết bị cần bảo trì - LIỆT KÊ CHI TIẾT
    if 'maintenance_devices' in real_data and real_data['maintenance_devices']:
        devices_list = "\n".join([
            f"  {i+1}. {d['name']} - Mã: {d['code']} - Phòng: {d.get('room', 'Chưa xác định')} - Loại: {d.get('category', 'N/A')}"
            for i, d in enumerate(real_data['maintenance_devices'][:10])
        ])
        context_parts.append(f"""
DANH SÁCH THIẾT BỊ CẦN BẢO TRÌ ({len(real_data['maintenance_devices'])} thiết bị):
{devices_list}
""")
    
    # Báo hỏng gần đây - LIỆT KÊ CHI TIẾT
    if 'recent_damages' in real_data and real_data['recent_damages']:
        damages_list = "\n".join([
            f"  {i+1}. {d['device_name']}: {d['description'][:80]}... - Trạng thái: {d['status']} - Mức độ: {d.get('severity', 'N/A')}"
            for i, d in enumerate(real_data['recent_damages'][:5])
        ])
        context_parts.append(f"""
BÁO HỎNG GẦN ĐÂY ({len(real_data['recent_damages'])} báo cáo):
{damages_list}
""")
    
    # TẤT CẢ THIẾT BỊ TRONG HỆ THỐNG - LIỆT KÊ ĐẦY ĐỦ
    if 'all_devices' in real_data and real_data['all_devices']:
        all_devices_list = "\n".join([
            f"  {i+1}. {d['name']} - Mã: {d['code']} - Loại: {d.get('category', 'N/A')} - Phòng: {d.get('room', 'Chưa xác định')} - Trạng thái: {d.get('status', 'N/A')}"
            for i, d in enumerate(real_data['all_devices'])
        ])
        context_parts.append(f"""
TẤT CẢ THIẾT BỊ TRONG HỆ THỐNG ({len(real_data['all_devices'])} thiết bị):
{all_devices_list}
""")
    
    # Thiết bị sẵn sàng - LIỆT KÊ CHI TIẾT (cho tất cả user)
    if 'available_devices' in real_data and real_data['available_devices']:
        devices_list = "\n".join([
            f"  {i+1}. {d['name']} - Mã: {d['code']} - Loại: {d.get('category', 'N/A')} - Phòng: {d.get('room', 'Chưa xác định')}"
            for i, d in enumerate(real_data['available_devices'])  # Hiển thị TẤT CẢ, không giới hạn
        ])
        context_parts.append(f"""
DANH SÁCH THIẾT BỊ CÓ THỂ MƯỢN ({len(real_data['available_devices'])} thiết bị):
{devices_list}
""")
    
    # Lượt đang mượn - CHI TIẾT
    if 'active_borrows' in real_data and real_data['active_borrows']:
        borrows_list = "\n".join([
            f"  {i+1}. {b.get('device_name','?')} (Mã: {b.get('device_code','?')}) - Người mượn: {b.get('user_name','N/A')} - Ngày mượn: {b.get('borrow_date','N/A')} - Trả dự kiến: {b.get('expected_return','N/A')}"
            for i, b in enumerate(real_data['active_borrows'])
        ])
        context_parts.append(f"""
THIẾT BỊ ĐANG ĐƯỢC MƯỢN ({len(real_data['active_borrows'])} lượt):
{borrows_list}
""")
    
    # Đã trả gần đây - CHI TIẾT
    if 'returned_borrows' in real_data and real_data['returned_borrows']:
        returned_list = "\n".join([
            f"  {i+1}. {b.get('device_name','?')} (Mã: {b.get('device_code','?')}) - Người mượn: {b.get('user_name','N/A')} - Ngày mượn: {b.get('borrow_date','N/A')} - Đã trả: {b.get('actual_return','N/A')}"
            for i, b in enumerate(real_data['returned_borrows'])
        ])
        context_parts.append(f"""
LỊCH SỬ THIẾT BỊ ĐÃ TRẢ GẦN ĐÂY ({len(real_data['returned_borrows'])} lượt):
{returned_list}
""")
    
    # Toàn bộ lịch sử mượn trả
    if 'all_borrows' in real_data and real_data['all_borrows']:
        all_list = "\n".join([
            f"  {i+1}. {b.get('device_name','?')} (Mã: {b.get('device_code','?')}) - Người mượn: {b.get('user_name','N/A')} - Trạng thái: {b.get('status','N/A')} - Ngày mượn: {b.get('borrow_date','N/A')}"
            for i, b in enumerate(real_data['all_borrows'])
        ])
        context_parts.append(f"""
TOÀN BỘ LỊCH SỬ MƯỢN TRẢ ({len(real_data['all_borrows'])} bản ghi, mượn gần nhất trước):
{all_list}
""")
    
    return "\n".join(context_parts)


def generate_suggestions(user_role, real_data):
    """Tạo suggestions dựa trên role và dữ liệu"""
    if user_role == 'ADMIN':
        suggestions = []
        
        # Dựa trên dữ liệu thực để tạo suggestions
        if real_data.get('statistics', {}).get('pending_damages', 0) > 0:
            suggestions.append(f"Xử lý {real_data['statistics']['pending_damages']} báo hỏng chưa giải quyết")
        
        if real_data.get('maintenance_devices'):
            suggestions.append(f"Lên lịch bảo trì cho {len(real_data['maintenance_devices'])} thiết bị")
        
        suggestions.extend([
            "Xem báo cáo thống kê chi tiết",
            "Quản lý người dùng hệ thống"
        ])
        
        return suggestions[:4]
    else:  # GIAO_VIEN
        return [
            "Xem thiết bị có thể mượn",
            "Lịch sử mượn trả của tôi",
            "Hướng dẫn báo hỏng thiết bị",
            "Quy trình mượn thiết bị"
        ]



@app.route('/api/ai/analyze-damage', methods=['POST'])
def analyze_damage():
    """
    Phân tích báo hỏng và đề xuất giải pháp
    
    Request body:
    {
        "equipment_name": "Tên thiết bị",
        "damage_description": "Mô tả hư hỏng",
        "severity": "THAP|TRUNG_BINH|CAO|KHAN_CAP",
        "image_url": "URL hình ảnh (optional)"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'equipment_name' not in data or 'damage_description' not in data:
            return jsonify({
                "error": "Missing required fields",
                "success": False
            }), 400
        
        equipment_name = data['equipment_name']
        damage_description = data['damage_description']
        severity = data.get('severity', 'TRUNG_BINH')
        
        # Tạo prompt phân tích
        analysis_prompt = f"""{DAMAGE_ANALYSIS_PROMPT}

Thiết bị: {equipment_name}
Mô tả hư hỏng: {damage_description}
Mức độ nghiêm trọng: {severity}

Hãy phân tích chi tiết và đưa ra giải pháp."""
        
        # Gọi Gemini API
        response = model.generate_content(analysis_prompt)
        
        # Parse response thành structured data
        analysis_text = response.text
        
        # Xác định priority dựa trên severity và phân tích
        priority_map = {
            "KHAN_CAP": "urgent",
            "CAO": "high",
            "TRUNG_BINH": "medium",
            "THAP": "low"
        }
        
        return jsonify({
            "success": True,
            "analysis": analysis_text,
            "priority": priority_map.get(severity, "medium"),
            "equipment_name": equipment_name,
            "severity": severity,
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 500


@app.route('/api/ai/suggest-maintenance', methods=['POST'])
def suggest_maintenance():
    """
    Gợi ý lịch bảo trì dựa trên lịch sử
    
    Request body:
    {
        "equipment_id": 1,
        "equipment_name": "Tên thiết bị",
        "maintenance_history": [
            {
                "date": "2024-01-15",
                "type": "DINH_KY",
                "result": "THANH_CONG"
            }
        ],
        "current_status": "TOT|HU_HONG_NHE|HU_HONG_NANG|DANG_SUA_CHUA"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'equipment_name' not in data:
            return jsonify({
                "error": "Missing required fields",
                "success": False
            }), 400
        
        equipment_name = data['equipment_name']
        maintenance_history = data.get('maintenance_history', [])
        current_status = data.get('current_status', 'TOT')
        
        # Tạo prompt gợi ý bảo trì
        history_text = "\n".join([
            f"- {h.get('date')}: {h.get('type')} - Kết quả: {h.get('result')}"
            for h in maintenance_history
        ])
        
        suggestion_prompt = f"""{MAINTENANCE_SUGGESTION_PROMPT}

Thiết bị: {equipment_name}
Tình trạng hiện tại: {current_status}

Lịch sử bảo trì:
{history_text if history_text else "Chưa có lịch sử bảo trì"}

Hãy đề xuất kế hoạch bảo trì tiếp theo."""
        
        # Gọi Gemini API
        response = model.generate_content(suggestion_prompt)
        
        return jsonify({
            "success": True,
            "recommendations": response.text,
            "equipment_name": equipment_name,
            "current_status": current_status,
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 500


@app.route('/api/ai/categorize-equipment', methods=['POST'])
def categorize_equipment():
    """
    Phân loại thiết bị tự động
    
    Request body:
    {
        "equipment_name": "Tên thiết bị",
        "description": "Mô tả thiết bị (optional)"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'equipment_name' not in data:
            return jsonify({
                "error": "Missing 'equipment_name' field",
                "success": False
            }), 400
        
        equipment_name = data['equipment_name']
        description = data.get('description', '')
        
        # Tạo prompt phân loại
        categorization_prompt = f"""{CATEGORIZATION_PROMPT}

Tên thiết bị: {equipment_name}
Mô tả: {description if description else "Không có mô tả"}

Hãy phân loại thiết bị này."""
        
        # Gọi Gemini API
        response = model.generate_content(categorization_prompt)
        
        # Cố gắng parse JSON từ response
        try:
            # Gemini có thể trả về text có chứa JSON
            response_text = response.text.strip()
            
            # Tìm JSON trong response
            if '{' in response_text and '}' in response_text:
                start = response_text.find('{')
                end = response_text.rfind('}') + 1
                json_str = response_text[start:end]
                result = json.loads(json_str)
            else:
                # Fallback: tạo response mặc định
                result = {
                    "category": "Thiết bị khác",
                    "subcategory": equipment_name,
                    "confidence": 0.5
                }
        except:
            result = {
                "category": "Thiết bị khác",
                "subcategory": equipment_name,
                "confidence": 0.5,
                "raw_response": response.text
            }
        
        return jsonify({
            "success": True,
            "equipment_name": equipment_name,
            **result,
            "timestamp": datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }), 500


# ============================================================
# SYNONYM MAP - Giai quyet van de ten thiet bi khi AI tra ve khac voi DB
# Vi du AI tra ve 'Tivi man hinh phang' nhung DB co 'Man hinh' hay 'Smart TV'
# Map nay giup ket noi cac ten tuong duong nhau
# ============================================================
DEVICE_SYNONYMS = {
    'tivi': ['man hinh', 'tv', 'television', 'screen', 'display', 'smart tv', 'led'],
    'man hinh': ['tivi', 'tv', 'monitor', 'screen', 'display', 'led'],
    'may chieu': ['projector', 'chieu', 'epson', 'panasonic'],
    'laptop': ['may tinh xach tay', 'notebook', 'dell', 'hp', 'asus', 'lenovo', 'acer'],
    'may tinh': ['computer', 'pc', 'desktop', 'may bo'],
    'loa': ['speaker', 'audio', 'jbl', 'yamaha', 'sound'],
    'micro': ['microphone', 'mic'],
    'may in': ['printer', 'canon', 'hp', 'epson'],
    'dieu hoa': ['air conditioner', 'may lanh', 'ac'],
    'tu lanh': ['refrigerator', 'fridge'],
    'bang': ['bảng', 'whiteboard', 'blackboard'],
    'ban': ['table', 'desk'],
    'ghe': ['chair', 'seat'],
    'may scan': ['scanner', 'may quet'],
}

def normalize_vn(text):
    """Chuan hoa tieng Viet don gian: lowercase, bo dau"""
    import unicodedata
    text = text.lower().strip()
    # Bo dau tieng Viet bang unicodedata
    try:
        text = unicodedata.normalize('NFD', text)
        text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    except Exception:
        pass
    return text


def match_device_from_db(detected_name, detected_brand, detected_type, db_devices):
    """
    Tim thiet bi khop nhat trong DB dua tren ten AI nhan dang.
    
    Cai tien so voi phien ban cu:
    - Chuan hoa tieng Viet (bo dau) truoc khi so sanh
    - Them synonym mapping: 'Tivi' co the khop voi 'Man hinh' trong DB
    - Tinh diem rieng cho tung truong (name, brand, type) voi trong so khac nhau
    - Ha nguong xuat tu 30% xuong 20% de bat duoc nhieu truong hop hon
    """
    if not db_devices or not detected_name:
        return None, 0.0
    
    # Chuan hoa input tu AI
    name_norm = normalize_vn(detected_name)
    brand_norm = normalize_vn(detected_brand) if detected_brand else ''
    type_norm = normalize_vn(detected_type) if detected_type else ''
    
    # Lay tu khoa tim kiem tu ca 3 truong
    search_terms = set(name_norm.split() + brand_norm.split() + type_norm.split())
    # Bo cac tu qua ngan (1 chu cai)
    search_terms = {t for t in search_terms if len(t) > 1}
    
    # Mo rong search_terms bang synonyms
    expanded_terms = set(search_terms)
    for term in search_terms:
        for key, synonyms in DEVICE_SYNONYMS.items():
            if term in key or key in term:
                expanded_terms.update(key.split())
                expanded_terms.update(' '.join(synonyms).split())
            for syn in synonyms:
                if term in syn or syn in term:
                    expanded_terms.add(key)
                    expanded_terms.update(key.split())
    
    best_match = None
    best_score = 0.0
    
    for device in db_devices:
        db_name_raw = device.get('name', device.get('tenThietBi', device.get('ten', '')))
        db_code_raw = device.get('code', device.get('maThietBi', device.get('ma', '')))
        db_category_raw = device.get('category', device.get('loai', ''))
        
        # Chuan hoa ten DB
        db_name = normalize_vn(str(db_name_raw))
        db_code = normalize_vn(str(db_code_raw))
        db_cat = normalize_vn(str(db_category_raw) if db_category_raw else '')
        
        db_text = db_name + ' ' + db_code + ' ' + db_cat
        db_words = set(db_text.split())
        
        # Tinh diem dua tren original + expanded terms
        # Original terms co trong so cao hon (x2)
        orig_hits = sum(2 for t in search_terms if any(t in w or w in t for w in db_words))
        exp_hits = sum(1 for t in expanded_terms - search_terms if any(t in w or w in t for w in db_words))
        
        total_possible = len(search_terms) * 2 + len(expanded_terms - search_terms)
        if total_possible > 0:
            score = (orig_hits + exp_hits) / total_possible
        else:
            score = 0.0
        
        # Bonus neu brand khop chinh xac
        if brand_norm and brand_norm in db_name:
            score = min(score + 0.3, 1.0)
        
        if score > best_score:
            best_score = score
            best_match = device
    
    # Ha nguong tu 30% xuong 20% va toi da 0.9 de tranh false positive cao qua
    return (best_match, min(best_score, 0.95)) if best_score > 0.2 else (None, 0.0)


@app.route('/api/ai/scan-image', methods=['POST'])
def scan_image():
    """
    [CAI TIEN] Nhan dang thiet bi tu anh, doi chieu DB, lay lich su day du
    
    Pipeline 9 buoc:
    1. Nhan image_base64 tu frontend
    2. Giai ma base64 -> bytes
    3. Resize anh (neu > 1024px) de tiet kiem Gemini token
    4. Gemini Vision phan tich anh -> JSON {device_name, brand, condition, damage...}
    5. Lay toan bo thiet bi tu Spring Boot DB
    6. match_device_from_db() -> tim thiet bi khop nhat (co synonym mapping)
    7. [MOI] Lay lich su bao hong cua thiet bi nay tu DB  
    8. [MOI] Kiem tra ai dang muon thiet bi nay tu DB
    9. Tong hop ket qua + goi y dien form
    """
    try:
        import requests as req_lib
        
        data = request.get_json()
        if not data or 'image_base64' not in data:
            return jsonify({'success': False, 'error': "Thieu truong 'image_base64'."}), 400
        
        image_base64 = data['image_base64']
        image_mime = data.get('image_mime', 'image/jpeg')
        
        # Strip data URL prefix neu co
        if ',' in image_base64:
            image_base64 = image_base64.split(',', 1)[1]
        
        # BUOC 1: Giai ma base64
        try:
            image_bytes = base64.b64decode(image_base64)
        except Exception:
            return jsonify({'success': False, 'error': 'Base64 khong hop le.'}), 400
        
        # BUOC 2: Resize anh neu > 1024px
        if PIL_AVAILABLE:
            try:
                img = Image.open(io.BytesIO(image_bytes))
                max_size = 1024
                if max(img.size) > max_size:
                    img.thumbnail((max_size, max_size), Image.LANCZOS)
                    buffer = io.BytesIO()
                    img_format = 'JPEG' if 'jpeg' in image_mime else 'PNG'
                    img.save(buffer, format=img_format, quality=85)
                    image_bytes = buffer.getvalue()
                    image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            except Exception as e:
                print(f'Warning resize: {e}')
        
        # BUOC 3: Gemini Vision nhan dang thiet bi
        image_part = {'inline_data': {'mime_type': image_mime, 'data': image_base64}}
        gemini_response = model.generate_content([IMAGE_SCAN_PROMPT, image_part])
        raw_text = gemini_response.text.strip()
        
        # BUOC 4: Parse JSON tu Gemini (xu ly ca truong hop Gemini boc trong ```json```)
        scan_result = {}
        try:
            clean = raw_text
            if '```' in clean:
                start = clean.find('{')
                end = clean.rfind('}') + 1
                clean = clean[start:end]
            scan_result = json.loads(clean)
        except json.JSONDecodeError:
            scan_result = {
                'device_name': 'Khong xac dinh',
                'brand': '', 'model': '', 'device_type': '',
                'condition': raw_text[:200], 'damage_signs': '', 'confidence': 0.3
            }
        
        SPRING_BOOT_API = 'http://localhost:8080/api/ai-data'
        db_connected = False
        
        # BUOC 5: Lay toan bo thiet bi tu DB
        db_devices = []
        try:
            r = req_lib.get(f'{SPRING_BOOT_API}/devices', headers=INTERNAL_HEADERS, timeout=3)
            if r.status_code == 200:
                db_devices = r.json()
                db_connected = True
        except Exception as e:
            print(f'Warning DB devices: {e}')
        
        # BUOC 6: Match thiet bi bang thuat toan cai tien (synonym + normalize)
        matched_device = None
        match_score = 0.0
        if db_devices:
            matched_device, match_score = match_device_from_db(
                scan_result.get('device_name', ''),
                scan_result.get('brand', ''),
                scan_result.get('device_type', ''),
                db_devices
            )
        
        # BUOC 7: Lay lich su bao hong cua thiet bi nay
        # Loc tu danh sach bao hong chung, tim nhung bao cao lien quan thiet bi da match
        device_damage_history = []
        if matched_device and db_connected:
            try:
                dr = req_lib.get(f'{SPRING_BOOT_API}/damages/recent?limit=50', headers=INTERNAL_HEADERS, timeout=3)
                if dr.status_code == 200:
                    all_damages = dr.json()
                    dev_name = matched_device.get('name', '').lower()
                    dev_code = matched_device.get('code', '').lower()
                    # Loc chi lay bao hong cua thiet bi nay
                    device_damage_history = [
                        d for d in all_damages
                        if (d.get('device_name', '') or '').lower() in dev_name
                        or dev_name in (d.get('device_name', '') or '').lower()
                        or dev_code in (d.get('device_code', '') or '').lower()
                    ][:5]  # Chi lay 5 lan gan nhat
            except Exception as e:
                print(f'Warning damage history: {e}')
        
        # BUOC 8: Kiem tra ai dang muon thiet bi nay
        current_borrower = None
        borrow_history = []
        if matched_device and db_connected:
            try:
                br = req_lib.get(f'{SPRING_BOOT_API}/borrows/active', headers=INTERNAL_HEADERS, timeout=3)
                if br.status_code == 200:
                    active_borrows = br.json()
                    dev_name = matched_device.get('name', '').lower()
                    # Tim xem thiet bi nay co dang duoc muon khong
                    for borrow in active_borrows:
                        b_dev = (borrow.get('device_name', '') or '').lower()
                        if dev_name in b_dev or b_dev in dev_name:
                            current_borrower = borrow
                            break
            except Exception as e:
                print(f'Warning borrow check: {e}')
            
            # Lay 5 lan muon gan nhat cua thiet bi nay
            try:
                hr = req_lib.get(f'{SPRING_BOOT_API}/borrows/all?limit=100', headers=INTERNAL_HEADERS, timeout=3)
                if hr.status_code == 200:
                    all_borrows = hr.json()
                    dev_name = matched_device.get('name', '').lower()
                    borrow_history = [
                        b for b in all_borrows
                        if dev_name in (b.get('device_name', '') or '').lower()
                        or (b.get('device_name', '') or '').lower() in dev_name
                    ][:5]
            except Exception as e:
                print(f'Warning borrow history: {e}')
        
        # BUOC 9: Tong hop va tra ket qua
        form_suggestion = {}
        if matched_device:
            damage_hint = scan_result.get('damage_signs', '') or scan_result.get('condition', '')
            form_suggestion = {
                'equipment_id': matched_device.get('id'),
                'equipment_name': matched_device.get('name', ''),
                'description_hint': f"[Scan AI] {damage_hint}",
                'severity_hint': 'CAO' if scan_result.get('damage_signs') else 'TRUNG_BINH'
            }
        
        return jsonify({
            'success': True,
            # Ket qua Gemini Vision nhan dang tu anh
            'scan_result': {
                'detected_name': scan_result.get('device_name', 'Khong xac dinh'),
                'detected_brand': scan_result.get('brand', ''),
                'detected_model': scan_result.get('model', ''),
                'detected_type': scan_result.get('device_type', ''),
                'condition': scan_result.get('condition', ''),
                'damage_signs': scan_result.get('damage_signs', ''),
                'confidence': float(scan_result.get('confidence', 0.0))
            },
            # Thiet bi khop trong DB (null neu khong tim thay)
            'matched_device': matched_device,
            'match_score': round(match_score, 2),
            # Trang thai ket noi Spring Boot
            'db_connected': db_connected,
            'db_total_devices': len(db_devices),
            # [MOI] Lich su bao hong cua thiet bi nay tu DB
            'device_damage_history': device_damage_history,
            # [MOI] Thong tin nguoi dang muon thiet bi (null neu khong ai muon)
            'current_borrower': current_borrower,
            # [MOI] 5 lan muon gan nhat
            'borrow_history': borrow_history,
            # Goi y dien form bao hong
            'form_suggestion': form_suggestion,
            'ai_notes': f"Gemini nhan dang: {scan_result.get('device_name', '?')} "
                        f"(Do tin cay: {int(float(scan_result.get('confidence', 0)) * 100)}%)",
            'timestamp': datetime.now().isoformat()
        }), 200
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e), 'timestamp': datetime.now().isoformat()}), 500



# ==============================================================
# QR CODE FEATURE - Tao ma QR cho tung thiet bi
# - /api/ai/device-qr/<id>   → anh PNG ma QR (co dinh theo id)
# - /api/ai/device-info/<id> → trang HTML dep khi scan QR
# - /api/ai/qr-print         → trang admin in nhan dan QR
# ==============================================================

try:
    import qrcode
    import qrcode.constants
    QR_AVAILABLE = True
except ImportError:
    QR_AVAILABLE = False
    print("Warning: qrcode chua duoc cai. Chay: pip install qrcode[pil]")


@app.route('/api/ai/device-qr/<int:device_id>')
def get_device_qr(device_id):
    """
    Tra ve anh PNG ma QR cho thiet bi.
    QR ma hoa URL /api/ai/device-info/<id> - co dinh vinh vien theo device_id.
    Cach dung: <img src="/api/ai/device-qr/1"> de nhung truc tiep vao HTML.
    """
    if not QR_AVAILABLE:
        return jsonify({'error': 'Cai qrcode: pip install qrcode[pil]'}), 500

    # URL ben trong QR: trang thong tin thiet bi thoi gian thuc
    host = request.host_url.rstrip('/')
    device_info_url = f"{host}/api/ai/device-info/{device_id}"

    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=2,
    )
    qr.add_data(device_info_url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="#1a1a2e", back_color="white")

    buf = io.BytesIO()
    img.save(buf, format='PNG')
    buf.seek(0)

    from flask import send_file
    return send_file(buf, mimetype='image/png',
                     download_name=f'QR_ThietBi_{device_id}.png')


@app.route('/api/ai/device-info/<int:device_id>')
def device_info_page(device_id):
    """
    Trang HTML dep hien thi toan bo thong tin thiet bi khi scan QR.
    Du lieu lay thoi gian thuc tu Spring Boot DB (no-cache).
    Chay duoc tren moi thiet bi: dien thoai, may tinh bang, laptop.
    """
    import requests as req_lib
    from flask import make_response

    SPRING = 'http://localhost:8080/api/ai-data'
    device = None
    damages = []
    borrower = None
    borrow_history = []

    try:
        r = req_lib.get(f'{SPRING}/devices', headers=INTERNAL_HEADERS, timeout=4)
        if r.status_code == 200:
            all_dev = r.json()
            found = [d for d in all_dev if str(d.get('id')) == str(device_id)]
            if found:
                device = found[0]

        if device:
            dev_name = (device.get('name') or '').lower()
            # Lich su bao hong
            dr = req_lib.get(f'{SPRING}/damages/recent?limit=50', headers=INTERNAL_HEADERS, timeout=4)
            if dr.status_code == 200:
                damages = [d for d in dr.json()
                           if dev_name in (d.get('device_name') or '').lower()
                           or (d.get('device_name') or '').lower() in dev_name][:5]
            # Nguoi dang muon
            br = req_lib.get(f'{SPRING}/borrows/active', headers=INTERNAL_HEADERS, timeout=4)
            if br.status_code == 200:
                for b in br.json():
                    if dev_name in (b.get('device_name') or '').lower():
                        borrower = b
                        break
            # 5 lan muon gan nhat
            hr = req_lib.get(f'{SPRING}/borrows/all?limit=100', headers=INTERNAL_HEADERS, timeout=4)
            if hr.status_code == 200:
                borrow_history = [b for b in hr.json()
                                  if dev_name in (b.get('device_name') or '').lower()][:5]
    except Exception as e:
        print(f'device-info error: {e}')

    host = request.host_url.rstrip('/')
    db_ok = device is not None

    STATUS_MAP = {
        'TOT':      ('Đang hoạt động tốt', '#22c55e', '✅'),
        'BAO_TRI':  ('Đang bảo trì',       '#f59e0b', '🔧'),
        'HONG':     ('Hỏng',               '#ef4444', '❌'),
        'THANH_LY': ('Đã thanh lý',        '#6b7280', '🗑️'),
    }
    status_raw = (device.get('status') or 'TOT') if device else 'TOT'
    status_label, status_color, status_icon = STATUS_MAP.get(status_raw, (status_raw, '#6b7280', '❓'))

    dev_name  = (device.get('name') or f'Thiết bị #{device_id}') if device else f'Thiết bị #{device_id}'
    dev_code  = (device.get('code') or '—') if device else '—'
    dev_room  = (device.get('room') or '—') if device else '—'
    dev_cat   = (device.get('category') or '—') if device else '—'

    # Build HTML sections
    def damage_badge(sev):
        s = (sev or '').upper()
        if 'CAO' in s or 'NGHIEM' in s: return '#ef4444', sev
        if 'TRUNG' in s or 'MED' in s:  return '#f59e0b', sev
        return '#3b82f6', sev or '—'

    def status_badge_dmg(st):
        m = {'CHO_XU_LY':'Chờ xử lý','DANG_XU_LY':'Đang xử lý','HOAN_THANH':'Hoàn thành','DA_BAO_TRI':'Đã bảo trì'}
        return m.get((st or '').upper(), st or '—')

    def fmt_date(d):
        if not d: return '—'
        try:
            from datetime import datetime as dt
            return dt.fromisoformat(str(d)[:19]).strftime('%d/%m/%Y')
        except: return str(d)[:10]

    damage_html = ''
    for d in damages:
        bc, bl = damage_badge(d.get('severity'))
        damage_html += f'''
        <div class="history-item">
          <div class="hi-left">
            <div class="hi-title">{d.get('description') or 'Không có mô tả'}</div>
            <div class="hi-sub">{('👤 ' + d['reporter_name'] + '  ·  ') if d.get('reporter_name') else ''}📅 {fmt_date(d.get('reported_date'))}</div>
          </div>
          <div class="hi-badges">
            <span class="badge" style="background:{bc}15;color:{bc};border-color:{bc}40;">{bl}</span>
            <span class="badge" style="background:#6b728015;color:#6b7280;border-color:#6b728040;">{status_badge_dmg(d.get('status'))}</span>
          </div>
        </div>'''

    borrow_html = ''
    for b in borrow_history:
        is_active = (b.get('status') or '') != 'DA_TRA'
        bc = '#f59e0b' if is_active else '#6b7280'
        bl = 'Đang mượn' if is_active else 'Đã trả'
        borrow_html += f'''
        <div class="history-item">
          <div class="hi-left">
            <div class="hi-title">👤 {b.get('user_name') or 'Không rõ'}</div>
            <div class="hi-sub">📅 Mượn: {fmt_date(b.get('borrow_date'))}{('  ·  Trả: ' + fmt_date(b.get('actual_return'))) if b.get('actual_return') else ''}</div>
          </div>
          <div class="hi-badges">
            <span class="badge" style="background:{bc}15;color:{bc};border-color:{bc}40;">{bl}</span>
          </div>
        </div>'''

    borrower_section = ''
    if borrower:
        borrower_section = f'''
    <div class="borrow-alert">
      <div class="borrow-alert-title">⚠️ Thiết bị đang được mượn</div>
      <div class="borrow-grid">
        <div><span class="lbl">Người mượn</span><span class="val bold">{borrower.get('user_name','Không rõ')}</span></div>
        <div><span class="lbl">Ngày mượn</span><span class="val">{fmt_date(borrower.get('borrow_date'))}</span></div>
        <div><span class="lbl">Dự kiến trả</span><span class="val">{fmt_date(borrower.get('expected_return'))}</span></div>
        <div><span class="lbl">Ghi chú</span><span class="val">{borrower.get('ghi_chu') or '—'}</span></div>
      </div>
    </div>'''

    no_db_banner = '' if db_ok else '''
    <div class="no-db-banner">⚠️ Không kết nối được Spring Boot — thông tin có thể chưa đầy đủ</div>'''

    html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<meta name="description" content="Thông tin thiết bị {dev_name} - Hệ thống QLCSVC">
<title>{dev_name} — QLCSVC</title>
<style>
  :root {{
    --bg: #f0f2f7;
    --card: #ffffff;
    --primary: #1a1a2e;
    --accent: #0f3460;
    --text: #1e293b;
    --sub: #64748b;
    --border: #e2e8f0;
    --radius: 16px;
    --shadow: 0 4px 24px rgba(0,0,0,0.08);
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ background: var(--bg); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: var(--text); min-height: 100vh; }}

  /* HEADER */
  .hero {{ background: linear-gradient(145deg, var(--primary) 0%, var(--accent) 60%, #16213e 100%);
           color: white; padding: 28px 20px 96px; text-align: center; position: relative; overflow: hidden; }}
  .hero::before {{ content:''; position:absolute; top:-40px; right:-40px; width:200px; height:200px;
                   background:rgba(255,255,255,0.04); border-radius:50%; }}
  .hero::after  {{ content:''; position:absolute; bottom:-60px; left:-30px; width:150px; height:150px;
                   background:rgba(255,255,255,0.03); border-radius:50%; }}
  .hero-badge {{ display:inline-block; background:rgba(255,255,255,0.12); border:1px solid rgba(255,255,255,0.2);
                 padding:4px 14px; border-radius:20px; font-size:0.72rem; font-weight:600;
                 letter-spacing:0.08em; text-transform:uppercase; margin-bottom:10px; }}
  .hero h1 {{ font-size:clamp(1.2rem,4vw,1.6rem); font-weight:700; line-height:1.3; margin-bottom:6px; }}
  .hero .code {{ font-size:0.82rem; opacity:0.65; margin-bottom:16px; }}
  .status-pill {{ display:inline-flex; align-items:center; gap:6px; padding:8px 20px; border-radius:30px;
                  font-weight:700; font-size:0.88rem; border:2px solid;
                  background:rgba(255,255,255,0.1); color:white; border-color:rgba(255,255,255,0.3); }}

  /* BODY */
  .body {{ padding: 0 14px 24px; margin-top: -72px; position: relative; z-index: 2; }}

  /* CARD */
  .card {{ background: var(--card); border-radius: var(--radius); padding: 18px;
           box-shadow: var(--shadow); margin-bottom: 14px; }}
  .card-label {{ font-size: 0.68rem; font-weight: 700; text-transform: uppercase;
                 letter-spacing: 0.1em; color: var(--sub); margin-bottom: 14px;
                 display: flex; align-items: center; gap: 6px; }}

  /* INFO GRID */
  .info-grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }}
  .lbl {{ display: block; font-size: 0.68rem; color: var(--sub); margin-bottom: 3px; }}
  .val {{ display: block; font-size: 0.92rem; font-weight: 600; color: var(--text); }}
  .val.bold {{ font-size: 1rem; color: var(--primary); }}
  .val.status {{ font-weight: 700; }}

  /* BORROW ALERT */
  .borrow-alert {{ background: #fffbeb; border: 1.5px solid #fcd34d; border-radius: var(--radius);
                   padding: 16px; margin-bottom: 14px; box-shadow: 0 2px 12px #fcd34d30; }}
  .borrow-alert-title {{ font-weight: 700; color: #92400e; font-size: 0.92rem; margin-bottom: 12px; }}
  .borrow-grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }}
  .borrow-grid div {{ display: flex; flex-direction: column; }}

  /* HISTORY */
  .history-item {{ display: flex; justify-content: space-between; align-items: flex-start;
                   padding: 10px 0; border-bottom: 1px solid var(--border); }}
  .history-item:last-child {{ border-bottom: none; }}
  .hi-title {{ font-weight: 600; font-size: 0.85rem; line-height: 1.4; color: var(--text); }}
  .hi-sub {{ font-size: 0.72rem; color: var(--sub); margin-top: 3px; }}
  .hi-badges {{ display: flex; flex-direction: column; gap: 4px; align-items: flex-end; flex-shrink: 0; margin-left: 10px; }}
  .badge {{ font-size: 0.68rem; font-weight: 700; padding: 2px 8px; border-radius: 10px; border: 1px solid; white-space: nowrap; }}

  /* EMPTY STATE */
  .empty {{ text-align: center; color: var(--sub); font-size: 0.82rem; padding: 12px 0; }}

  /* QR MINI */
  .qr-mini {{ text-align: center; margin-top: 4px; }}
  .qr-mini img {{ width: 80px; height: 80px; border-radius: 8px; border: 2px solid var(--border); }}
  .qr-mini p {{ font-size: 0.65rem; color: var(--sub); margin-top: 4px; }}

  /* CTA BUTTON */
  .btn-report {{ display: block; background: linear-gradient(135deg, #ef4444, #dc2626);
                 color: white; text-align: center; padding: 16px; border-radius: 14px;
                 text-decoration: none; font-weight: 700; font-size: 1rem; margin-bottom: 14px;
                 box-shadow: 0 6px 20px #ef444440; letter-spacing: 0.02em;
                 transition: transform 0.15s, box-shadow 0.15s; }}
  .btn-report:active {{ transform: scale(0.98); }}

  /* NO DB BANNER */
  .no-db-banner {{ background: #fef2f2; color: #b91c1c; border: 1px solid #fca5a5;
                   border-radius: 10px; padding: 10px 14px; font-size: 0.78rem;
                   margin-bottom: 12px; font-weight: 500; }}

  /* FOOTER */
  .footer {{ text-align: center; font-size: 0.68rem; color: var(--sub); padding: 8px 0 20px; }}

  @media (max-width: 360px) {{
    .info-grid, .borrow-grid {{ grid-template-columns: 1fr; }}
  }}
</style>
</head>
<body>

<!-- HEADER -->
<div class="hero">
  <div class="hero-badge">{dev_cat}</div>
  <h1>{dev_name}</h1>
  <div class="code">Mã: {dev_code}</div>
  <div class="status-pill">{status_icon} {status_label}</div>
</div>

<!-- BODY -->
<div class="body">

  {no_db_banner}

  <!-- Thông tin cơ bản -->
  <div class="card">
    <div class="card-label">📋 Thông tin thiết bị</div>
    <div class="info-grid">
      <div><span class="lbl">Mã thiết bị</span><span class="val">{dev_code}</span></div>
      <div><span class="lbl">Phòng / Vị trí</span><span class="val">{dev_room}</span></div>
      <div><span class="lbl">Loại thiết bị</span><span class="val">{dev_cat}</span></div>
      <div><span class="lbl">Trạng thái</span><span class="val status" style="color:{status_color};">{status_icon} {status_label}</span></div>
    </div>

    <!-- QR nhỏ góc dưới -->
    <div class="qr-mini" style="margin-top:14px;">
      <img src="{host}/api/ai/device-qr/{device_id}" alt="QR {dev_name}">
      <p>Scan để xem trang này</p>
    </div>
  </div>

  {borrower_section}

  <!-- Lịch sử báo hỏng -->
  <div class="card">
    <div class="card-label">🔧 Lịch sử báo hỏng gần đây</div>
    {damage_html if damage_html else '<div class="empty">Chưa có báo cáo hỏng hóc ✅</div>'}
  </div>

  <!-- Lịch sử mượn trả -->
  <div class="card">
    <div class="card-label">📋 Lịch sử mượn trả</div>
    {borrow_html if borrow_html else '<div class="empty">Chưa có lịch sử mượn trả</div>'}
  </div>

  <!-- Nút báo hỏng -->
  <a class="btn-report" href="{host}/api/ai/report-form?device_id={device_id}&amp;device_name={dev_name}">
    🚨 Báo hỏng thiết bị này
  </a>

  <div class="footer">
    {dev_name} &nbsp;·&nbsp; Hệ thống QLCSVC<br>
    Cập nhật: {datetime.now().strftime('%H:%M — %d/%m/%Y')}
  </div>

</div>
</body>
</html>'''

    resp = make_response(html)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    resp.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
    return resp


@app.route('/api/ai/qr-print')
def qr_print_page():
    """
    Trang admin in nhan QR cho tat ca thiet bi.
    Moi nhan in duoc: QR + ten thiet bi + ma + phong + trang thai.
    Co nut in an va layout chuan cho may in A4.
    """
    import requests as req_lib
    from flask import make_response

    devices = []
    db_ok = False
    try:
        r = req_lib.get('http://localhost:8080/api/ai-data/devices',
                        headers=INTERNAL_HEADERS, timeout=4)
        if r.status_code == 200:
            devices = r.json()
            db_ok = True
    except Exception as e:
        print(f'qr-print error: {e}')

    host = request.host_url.rstrip('/')

    STATUS_MAP = {
        'TOT':      ('#22c55e', '✅ Tốt'),
        'BAO_TRI':  ('#f59e0b', '🔧 Bảo trì'),
        'HONG':     ('#ef4444', '❌ Hỏng'),
        'THANH_LY': ('#6b7280', '🗑️ Thanh lý'),
    }

    cards_html = ''
    for d in devices:
        did   = d.get('id', '')
        dname = d.get('name', '—')
        dcode = d.get('code', '—')
        droom = d.get('room') or '—'
        dstat = d.get('status', 'TOT')
        s_color, s_label = STATUS_MAP.get(dstat, ('#6b7280', dstat))
        qr_url   = f"{host}/api/ai/device-qr/{did}"
        info_url = f"{host}/api/ai/device-info/{did}"

        cards_html += f'''
        <a class="label-card" href="{info_url}" target="_blank" title="Xem thông tin {dname}">
          <div class="label-header">
            <div class="label-sys">QLCSVC</div>
            <div class="label-cat">{d.get('category','—')}</div>
          </div>
          <div class="label-body">
            <img class="label-qr" src="{qr_url}" alt="QR {dname}" loading="lazy">
            <div class="label-info">
              <div class="label-name">{dname}</div>
              <div class="label-detail">📌 Mã: <b>{dcode}</b></div>
              <div class="label-detail">📍 Phòng: {droom}</div>
              <div class="label-status" style="color:{s_color};">{s_label}</div>
            </div>
          </div>
          <div class="label-footer">Scan để xem thông tin đầy đủ</div>
        </a>'''

    no_db = '' if db_ok else '''
    <div style="background:#fef2f2;color:#b91c1c;border:1px solid #fca5a5;border-radius:12px;
                padding:16px 20px;margin:20px;font-weight:500;">
      ⚠️ Spring Boot chưa chạy — không thể tải danh sách thiết bị.
    </div>'''

    html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>In Nhãn QR Thiết Bị — QLCSVC Admin</title>
<style>
  :root {{
    --primary: #1a1a2e;
    --accent: #0f3460;
    --bg: #f0f2f7;
    --card: #ffffff;
    --border: #e2e8f0;
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ background: var(--bg); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }}

  /* TOP NAV */
  .topbar {{ background: linear-gradient(135deg, var(--primary), var(--accent));
             color: white; padding: 0 24px; height: 58px;
             display: flex; align-items: center; justify-content: space-between;
             position: sticky; top: 0; z-index: 100; box-shadow: 0 2px 12px rgba(0,0,0,0.2); }}
  .topbar h1 {{ font-size: 1rem; font-weight: 700; }}
  .topbar-right {{ display: flex; gap: 10px; align-items: center; }}
  .btn {{ padding: 8px 18px; border-radius: 8px; font-weight: 700; font-size: 0.82rem;
          cursor: pointer; border: none; transition: transform 0.15s; }}
  .btn:active {{ transform: scale(0.97); }}
  .btn-print {{ background: #22c55e; color: white; }}
  .btn-outline {{ background: transparent; color: white; border: 1.5px solid rgba(255,255,255,0.4); }}

  /* STATS BAR */
  .statsbar {{ background: white; border-bottom: 1px solid var(--border);
               padding: 10px 24px; font-size: 0.82rem; color: #64748b;
               display: flex; align-items: center; gap: 20px; }}
  .stat-item {{ display: flex; align-items: center; gap: 5px; }}
  .stat-dot {{ width: 8px; height: 8px; border-radius: 50%; }}

  /* GRID NHAN */
  .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(240px,1fr));
           gap: 16px; padding: 20px 24px; max-width: 1400px; margin: 0 auto; }}

  /* NHAN DAN */
  .label-card {{ background: white; border-radius: 12px; overflow: hidden; text-decoration: none;
                 color: inherit; border: 1.5px solid var(--border);
                 box-shadow: 0 2px 10px rgba(0,0,0,0.06); transition: transform 0.15s, box-shadow 0.15s;
                 display: flex; flex-direction: column; }}
  .label-card:hover {{ transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }}

  .label-header {{ background: linear-gradient(135deg, var(--primary), var(--accent));
                   color: white; padding: 8px 12px;
                   display: flex; justify-content: space-between; align-items: center; }}
  .label-sys {{ font-weight: 800; font-size: 0.8rem; letter-spacing: 0.1em; }}
  .label-cat {{ font-size: 0.68rem; opacity: 0.75; background: rgba(255,255,255,0.15);
                padding: 2px 8px; border-radius: 10px; }}

  .label-body {{ display: flex; gap: 10px; padding: 12px; flex: 1; }}
  .label-qr {{ width: 90px; height: 90px; flex-shrink: 0; border-radius: 8px;
               border: 2px solid var(--border); object-fit: contain; }}
  .label-info {{ flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: center; gap: 4px; }}
  .label-name {{ font-weight: 700; font-size: 0.85rem; color: var(--primary); line-height: 1.3;
                 display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }}
  .label-detail {{ font-size: 0.72rem; color: #64748b; }}
  .label-status {{ font-size: 0.72rem; font-weight: 700; margin-top: 2px; }}

  .label-footer {{ background: #f8fafc; border-top: 1px solid var(--border);
                   padding: 6px 12px; font-size: 0.62rem; color: #94a3b8;
                   text-align: center; }}

  /* PRINT */
  @media print {{
    .topbar, .statsbar {{ display: none !important; }}
    body {{ background: white; }}
    .grid {{ padding: 0; gap: 10px; grid-template-columns: repeat(4, 1fr); }}
    .label-card {{ break-inside: avoid; page-break-inside: avoid; box-shadow: none;
                   border: 1px solid #ccc; }}
    .label-card:hover {{ transform: none; box-shadow: none; }}
  }}
</style>
</head>
<body>

<div class="topbar">
  <h1>🖨️ In Nhãn QR Thiết Bị — QLCSVC</h1>
  <div class="topbar-right">
    <span style="font-size:0.78rem;opacity:0.7;">{len(devices)} thiết bị</span>
    <button class="btn btn-outline" onclick="location.reload()">🔄 Làm mới</button>
    <button class="btn btn-print" onclick="window.print()">🖨️ In trang này</button>
  </div>
</div>

<div class="statsbar">
  <div class="stat-item"><div class="stat-dot" style="background:#22c55e;"></div>
    Tốt: {sum(1 for d in devices if d.get('status') == 'TOT')}</div>
  <div class="stat-item"><div class="stat-dot" style="background:#f59e0b;"></div>
    Bảo trì: {sum(1 for d in devices if d.get('status') == 'BAO_TRI')}</div>
  <div class="stat-item"><div class="stat-dot" style="background:#ef4444;"></div>
    Hỏng: {sum(1 for d in devices if d.get('status') == 'HONG')}</div>
  <span style="margin-left:auto;font-size:0.72rem;">Click nhãn → xem chi tiết &nbsp;|&nbsp; Ctrl+P / Nút In → in ấn</span>
</div>

{no_db}

<div class="grid">
{cards_html}
</div>

</body>
</html>'''

    resp = make_response(html)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    resp.headers['Cache-Control'] = 'no-cache, no-store'
    return resp


@app.route('/api/ai/report-form')
def report_form_page():
    """
    Trang form bao hong dep - mo ra khi nhan nut 'Bao hong thiet bi nay' tren device-info.
    Pre-fill san thong tin thiet bi tu device_id.
    Nguoi dung chi can:
      1. Chon muc do nghiem trong
      2. Mo ta loi (bat buoc >= 10 ky tu)
      3. Nhap ten + SDT (khong bat buoc - de xac nhan lai)
      4. Bam Gui
    """
    import requests as req_lib
    from flask import make_response

    device_id   = request.args.get('device_id', '')
    device_name = request.args.get('device_name', '')

    # Lay thong tin thiet bi neu chua co ten
    device_code = ''
    device_room = ''
    if device_id and not device_name:
        try:
            r = req_lib.get('http://localhost:8080/api/ai-data/devices',
                            headers=INTERNAL_HEADERS, timeout=3)
            if r.status_code == 200:
                found = [d for d in r.json() if str(d.get('id')) == str(device_id)]
                if found:
                    device_name = found[0].get('name', '')
                    device_code = found[0].get('code', '')
                    device_room = found[0].get('room', '')
        except Exception:
            pass

    host = request.host_url.rstrip('/')
    submit_url = f"{host}/api/ai/submit-report"
    back_url   = f"{host}/api/ai/device-info/{device_id}" if device_id else f"{host}/api/ai/qr-print"

    html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<title>Báo hỏng — {device_name or 'Thiết bị'}</title>
<style>
  :root {{
    --primary: #1a1a2e;
    --accent:  #0f3460;
    --danger:  #ef4444;
    --bg:      #f0f2f7;
    --card:    #ffffff;
    --border:  #e2e8f0;
    --text:    #1e293b;
    --sub:     #64748b;
    --radius:  14px;
  }}
  * {{ box-sizing:border-box; margin:0; padding:0; }}
  body {{ background:var(--bg); font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
         color:var(--text); min-height:100vh; }}

  /* HEADER */
  .hero {{ background:linear-gradient(145deg,#ef4444,#dc2626,#b91c1c);
           color:white; padding:22px 20px 84px; text-align:center; position:relative; overflow:hidden; }}
  .hero::before {{ content:''; position:absolute; top:-30px; right:-30px; width:160px; height:160px;
                   background:rgba(255,255,255,0.06); border-radius:50%; }}
  .hero .back {{ position:absolute; top:16px; left:16px; background:rgba(255,255,255,0.15);
                 border:1px solid rgba(255,255,255,0.25); color:white; padding:6px 12px;
                 border-radius:8px; font-size:0.78rem; font-weight:600; text-decoration:none; }}
  .hero-icon {{ font-size:2.4rem; margin-bottom:6px; }}
  .hero h1   {{ font-size:1.2rem; font-weight:700; margin-bottom:4px; }}
  .hero .sub {{ font-size:0.78rem; opacity:0.8; }}

  /* DEVICE CHIP */
  .device-chip {{ display:inline-flex; align-items:center; gap:8px; background:rgba(255,255,255,0.15);
                  border:1px solid rgba(255,255,255,0.25); padding:6px 14px; border-radius:20px;
                  font-size:0.8rem; font-weight:600; margin-top:10px; }}

  /* BODY */
  .body {{ padding:0 14px 32px; margin-top:-68px; position:relative; z-index:2; }}

  /* CARD */
  .card {{ background:var(--card); border-radius:var(--radius); padding:18px;
           box-shadow:0 4px 20px rgba(0,0,0,0.08); margin-bottom:14px; }}
  .card-label {{ font-size:0.68rem; font-weight:700; text-transform:uppercase;
                 letter-spacing:0.1em; color:var(--sub); margin-bottom:14px; }}

  /* SEVERITY SELECTOR */
  .severity-grid {{ display:grid; grid-template-columns:repeat(2,1fr); gap:8px; }}
  .sev-option {{ position:relative; }}
  .sev-option input {{ position:absolute; opacity:0; width:0; height:0; }}
  .sev-label {{ display:flex; flex-direction:column; align-items:center; gap:4px; padding:12px 8px;
                border:2px solid var(--border); border-radius:10px; cursor:pointer; transition:all .15s; }}
  .sev-icon  {{ font-size:1.5rem; }}
  .sev-text  {{ font-size:0.72rem; font-weight:700; text-align:center; }}
  .sev-option input:checked + .sev-label {{ border-color:currentColor; background:currentColor; }}
  .sev-option input:checked + .sev-label * {{ color:white !important; }}
  .sev-thap   .sev-label {{ color:#22c55e; }}
  .sev-trung  .sev-label {{ color:#f59e0b; }}
  .sev-cao    .sev-label {{ color:#ef4444; }}
  .sev-khan   .sev-label {{ color:#7c3aed; }}

  /* FORM FIELDS */
  label.field {{ display:block; font-size:0.72rem; font-weight:700; color:var(--sub);
                 text-transform:uppercase; letter-spacing:0.06em; margin-bottom:5px; }}
  .field-group {{ margin-bottom:14px; }}
  textarea, input[type=text], input[type=tel] {{
    width:100%; padding:12px 14px; border:1.5px solid var(--border); border-radius:10px;
    font-size:0.9rem; font-family:inherit; color:var(--text); background:white;
    resize:vertical; transition:border-color .15s; outline:none; }}
  textarea:focus, input:focus {{ border-color:#ef4444; box-shadow:0 0 0 3px #ef444415; }}
  .char-count {{ font-size:0.68rem; color:var(--sub); text-align:right; margin-top:3px; }}

  /* SUBMIT */
  .btn-submit {{ width:100%; padding:15px; background:linear-gradient(135deg,#ef4444,#dc2626);
                 color:white; border:none; border-radius:var(--radius); font-size:1rem;
                 font-weight:700; cursor:pointer; box-shadow:0 6px 20px #ef444440;
                 transition:transform .15s, opacity .15s; }}
  .btn-submit:active {{ transform:scale(0.98); }}
  .btn-submit:disabled {{ opacity:0.6; cursor:not-allowed; }}

  /* SUCCESS / ERROR STATE */
  .result-box {{ display:none; border-radius:var(--radius); padding:20px; text-align:center; margin-top:14px; }}
  .result-box.success {{ background:#f0fdf4; border:1.5px solid #86efac; }}
  .result-box.error   {{ background:#fef2f2; border:1.5px solid #fca5a5; }}
  .result-icon  {{ font-size:2.4rem; margin-bottom:8px; }}
  .result-title {{ font-size:1.1rem; font-weight:700; margin-bottom:6px; }}
  .result-msg   {{ font-size:0.82rem; color:var(--sub); }}
  .btn-back {{ display:inline-block; margin-top:14px; padding:10px 24px; background:var(--primary);
               color:white; border-radius:10px; text-decoration:none; font-weight:700; font-size:0.88rem; }}

  /* REQUIRED STAR */
  .req {{ color:#ef4444; }}

  @media(max-width:360px) {{ .severity-grid {{ grid-template-columns:repeat(2,1fr); }} }}
</style>
</head>
<body>

<div class="hero">
  <a class="back" href="{back_url}">← Quay lại</a>
  <div class="hero-icon">🚨</div>
  <h1>Báo hỏng thiết bị</h1>
  <div class="sub">Phản ánh sự cố để nhân viên kỹ thuật xử lý nhanh nhất</div>
  {f'<div class="device-chip">🖥️ {device_name}{(" — " + device_code) if device_code else ""}{(" · " + device_room) if device_room else ""}</div>' if device_name else ''}
</div>

<div class="body">

  <form id="reportForm" onsubmit="submitReport(event)">
    <input type="hidden" name="device_id" value="{device_id}">

    <!-- Mức độ nghiêm trọng -->
    <div class="card">
      <div class="card-label">⚡ Mức độ nghiêm trọng <span class="req">*</span></div>
      <div class="severity-grid">
        <div class="sev-option sev-thap">
          <input type="radio" name="muc_do" id="s1" value="THAP">
          <label class="sev-label" for="s1">
            <span class="sev-icon">🟢</span>
            <span class="sev-text" style="color:#22c55e;">Nhẹ<br><small>Vẫn dùng được</small></span>
          </label>
        </div>
        <div class="sev-option sev-trung">
          <input type="radio" name="muc_do" id="s2" value="TRUNG_BINH" checked>
          <label class="sev-label" for="s2">
            <span class="sev-icon">🟡</span>
            <span class="sev-text" style="color:#f59e0b;">Trung bình<br><small>Hạn chế sử dụng</small></span>
          </label>
        </div>
        <div class="sev-option sev-cao">
          <input type="radio" name="muc_do" id="s3" value="CAO">
          <label class="sev-label" for="s3">
            <span class="sev-icon">🔴</span>
            <span class="sev-text" style="color:#ef4444;">Nghiêm trọng<br><small>Không dùng được</small></span>
          </label>
        </div>
        <div class="sev-option sev-khan">
          <input type="radio" name="muc_do" id="s4" value="KHAN_CAP">
          <label class="sev-label" for="s4">
            <span class="sev-icon">🆘</span>
            <span class="sev-text" style="color:#7c3aed;">Khẩn cấp<br><small>Nguy hiểm/mất DL</small></span>
          </label>
        </div>
      </div>
    </div>

    <!-- Mô tả lỗi -->
    <div class="card">
      <div class="card-label">📝 Mô tả sự cố <span class="req">*</span></div>
      <div class="field-group">
        <textarea id="moTa" name="mo_ta" rows="4" placeholder="Mô tả chi tiết sự cố, ví dụ:&#10;• Bàn phím không gõ được phím A, B&#10;• Máy không lên nguồn sau khi cắm sạc&#10;• Màn hình bị sọc ngang..." maxlength="1000" oninput="updateCount(this)"></textarea>
        <div class="char-count"><span id="charCount">0</span>/1000</div>
      </div>
    </div>

    <!-- Thông tin người báo -->
    <div class="card">
      <div class="card-label">👤 Thông tin người báo <span style="font-weight:400;text-transform:none;letter-spacing:0;">(tùy chọn)</span></div>
      <div class="field-group">
        <label class="field" for="tenNguoiBao">Họ tên</label>
        <input type="text" id="tenNguoiBao" name="ten_nguoi_bao" placeholder="Nguyễn Văn A" maxlength="100">
      </div>
      <div class="field-group" style="margin-bottom:0;">
        <label class="field" for="sdtNguoiBao">Số điện thoại</label>
        <input type="tel" id="sdtNguoiBao" name="so_dien_thoai" placeholder="0901234567" maxlength="15">
      </div>
    </div>

    <button type="submit" class="btn-submit" id="submitBtn">
      🚨 Gửi báo cáo hỏng
    </button>
  </form>

  <!-- Kết quả -->
  <div class="result-box success" id="resultSuccess">
    <div class="result-icon">✅</div>
    <div class="result-title">Đã gửi báo cáo!</div>
    <div class="result-msg" id="successMsg">Nhân viên kỹ thuật sẽ xử lý sớm nhất có thể.</div>
    <a href="{back_url}" class="btn-back">← Quay lại thiết bị</a>
  </div>

  <div class="result-box error" id="resultError">
    <div class="result-icon">❌</div>
    <div class="result-title">Gửi thất bại</div>
    <div class="result-msg" id="errorMsg">Vui lòng thử lại hoặc liên hệ trực tiếp nhân viên kỹ thuật.</div>
    <a href="javascript:void(0)" onclick="resetForm()" class="btn-back" style="background:#ef4444;">Thử lại</a>
  </div>

</div>

<script>
  function updateCount(el) {{
    document.getElementById('charCount').textContent = el.value.length;
  }}

  async function submitReport(e) {{
    e.preventDefault();
    const moTa = document.getElementById('moTa').value.trim();
    if (moTa.length < 10) {{
      alert('Vui lòng mô tả chi tiết hơn (ít nhất 10 ký tự).');
      return;
    }}

    const btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.textContent = '⏳ Đang gửi...';

    const fd = new FormData(document.getElementById('reportForm'));
    const params = new URLSearchParams();
    for (const [k,v] of fd.entries()) params.append(k, v);

    try {{
      const r = await fetch('{submit_url}', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/x-www-form-urlencoded' }},
        body: params.toString()
      }});
      const data = await r.json();
      if (data.success) {{
        document.getElementById('reportForm').style.display = 'none';
        document.getElementById('successMsg').textContent =
          data.message + (data.bao_hong_id ? ' (Mã phiếu: #' + data.bao_hong_id + ')' : '');
        document.getElementById('resultSuccess').style.display = 'block';
      }} else {{
        document.getElementById('errorMsg').textContent = data.message || 'Lỗi không xác định.';
        document.getElementById('resultError').style.display = 'block';
        btn.disabled = false;
        btn.textContent = '🚨 Gửi báo cáo hỏng';
      }}
    }} catch(err) {{
      document.getElementById('errorMsg').textContent = 'Không kết nối được server. Lỗi: ' + err.message;
      document.getElementById('resultError').style.display = 'block';
      btn.disabled = false;
      btn.textContent = '🚨 Gửi báo cáo hỏng';
    }}
  }}

  function resetForm() {{
    document.getElementById('resultError').style.display = 'none';
  }}
</script>
</body>
</html>'''

    resp = make_response(html)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    resp.headers['Cache-Control'] = 'no-cache'
    return resp


@app.route('/api/ai/submit-report', methods=['POST'])
def submit_report():
    """
    Nhan form bao hong tu /api/ai/report-form va forward sang Spring Boot.
    Tra ve JSON { success, message, bao_hong_id }.
    """
    import requests as req_lib

    device_id     = request.form.get('device_id', '').strip()
    mo_ta         = request.form.get('mo_ta', '').strip()
    muc_do        = request.form.get('muc_do', 'TRUNG_BINH').strip()
    ten_nguoi_bao = request.form.get('ten_nguoi_bao', '').strip() or 'Khách/Người dùng'
    so_dien_thoai = request.form.get('so_dien_thoai', '').strip()

    if not device_id:
        return jsonify({'success': False, 'message': 'Thiếu thông tin thiết bị.'}), 400
    if not mo_ta or len(mo_ta) < 5:
        return jsonify({'success': False, 'message': 'Mô tả quá ngắn.'}), 400

    try:
        r = req_lib.post(
            'http://localhost:8080/api/ai-data/report-damage',
            params={{
                'thiet_bi_id':   device_id,
                'mo_ta':         mo_ta,
                'muc_do':        muc_do,
                'ten_nguoi_bao': ten_nguoi_bao,
                'so_dien_thoai': so_dien_thoai,
            }},
            headers=INTERNAL_HEADERS,
            timeout=5
        )
        data = r.json()
        return jsonify(data)
    except Exception as e:
        return jsonify({{'success': False, 'message': f'Lỗi kết nối Spring Boot: {{e}}'}}), 500
if __name__ == '__main__':
    # Fix encoding cho Windows terminal (cp1252 khong ho tro Unicode box chars)
    import sys
    if sys.stdout.encoding and sys.stdout.encoding.lower() != 'utf-8':
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass

    # Lay port tu environment variable hoac dung 5000 mac dinh
    port = int(os.getenv('FLASK_PORT', 5000))
    debug = os.getenv('FLASK_ENV', 'development') == 'development'

    gemini_status = 'OK' if GEMINI_API_KEY else 'NOT CONFIGURED'

    print(f"""
    +-----------------------------------------------------------+
    |  Flask AI API - He thong Quan ly Co so Vat chat           |
    +-----------------------------------------------------------+
    |  Server: http://localhost:{port}                           |
    |  Gemini API: {gemini_status:<48}|
    |                                                           |
    |  Endpoints:                                               |
    |  - GET  /api/ai/health                                    |
    |  - POST /api/ai/chatbot                                   |
    |  - POST /api/ai/analyze-damage                            |
    |  - POST /api/ai/suggest-maintenance                       |
    |  - POST /api/ai/categorize-equipment                      |
    |  - POST /api/ai/scan-image  [NEW - Gemini Vision]         |
    +-----------------------------------------------------------+
    """)

    app.run(host='0.0.0.0', port=port, debug=debug)

