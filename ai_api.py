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


@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors"""
    return jsonify({
        "success": False,
        "error": "Endpoint not found",
        "timestamp": datetime.now().isoformat()
    }), 404


@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors"""
    return jsonify({
        "success": False,
        "error": "Internal server error",
        "timestamp": datetime.now().isoformat()
    }), 500


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
