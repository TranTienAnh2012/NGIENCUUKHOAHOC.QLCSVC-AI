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
1. Nguyên nhân hoặc Tình trạng hiện tại
2. Hướng xử lý (nếu thiết bị hỏng) HOẶC Lịch bảo trì (nếu bình thường)

Yêu cầu: Trả lời RẤT NGẮN GỌN (tối đa 4-5 dòng), đi thẳng vào vấn đề chính để tiết kiệm thời gian đọc.
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
            
            # Lấy toàn bộ phiếu bảo trì (50 gần nhất)
            maint_response = requests.get(f"{SPRING_BOOT_API}/maintenance/all?limit=50", headers=INTERNAL_HEADERS, timeout=2)
            if maint_response.status_code == 200:
                real_data['all_maintenances'] = maint_response.json()
                
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
- Thiết bị hiện đang bảo trì (đang hỏng/sửa): {stats.get('maintenance_devices', 0)}
- Thiết bị hư hỏng (chờ xử lý): {stats.get('damaged_devices', 0)}
- Tổng số lượt mượn: {stats.get('total_borrows', 0)}
- Tổng số báo hỏng: {stats.get('total_damages', 0)}
- Tổng số phiếu bảo trì trong hệ thống: {stats.get('total_maintenances', 0)}
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
    
    # Phiếu bảo trì thực tế - LIỆT KÊ CHI TIẾT
    if 'all_maintenances' in real_data and real_data['all_maintenances']:
        maint_list = "\n".join([
            f"  {i+1}. {m.get('device_name','?')} (Mã: {m.get('device_code','?')}) - Loại: {m.get('type','N/A')} - Ngày: {m.get('date','N/A')} - Người thực hiện: {m.get('performer','N/A')} - Chi phí: {m.get('cost','N/A')} - Kết quả: {m.get('result','N/A')}"
            for i, m in enumerate(real_data['all_maintenances'])
        ])
        context_parts.append(f"""
TOÀN BỘ PHIẾU BẢO TRÌ ({len(real_data['all_maintenances'])} phiếu, gần nhất trước):
{maint_list}
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
        
        if not data:
            return jsonify({"error": "Missing request body", "success": False}), 400

        # Accept both old format (equipment_name/damage_description)
        # and new format (device_name/description) from QR report form
        equipment_name     = data.get('equipment_name') or data.get('device_name') or 'Không rõ'
        damage_description = data.get('damage_description') or data.get('description') or ''

        if not damage_description.strip():
            return jsonify({"error": "Missing required fields: description", "success": False}), 400

        equipment_name = equipment_name
        damage_description = damage_description
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
        
        if not data or ('equipment_name' not in data and 'device_name' not in data):
            return jsonify({
                "error": "Missing required fields",
                "success": False
            }), 400
        
        equipment_name = data.get('equipment_name') or data.get('device_name') or 'Khong ro'
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


# ============================================================
# XÁC MINH THIẾT BỊ BẰNG AI (Gemini Vision)
# Chup anh thiet bi thuc → Gemini Vision so sanh voi thong tin DB
# ============================================================
VERIFY_DEVICE_PROMPT = """
Bạn là hệ thống AI xác minh thiết bị trong hệ thống quản lý cơ sở vật chất.

THÔNG TIN THIẾT BỊ TRONG DATABASE:
- Tên: {device_name}
- Hãng sản xuất: {manufacturer}  
- Model: {model}
- Loại: {category}
- Mã: {code}

NHIỆM VỤ: Nhìn ảnh chụp thiết bị thật và so sánh với thông tin trên.

Trả lời CHÍNH XÁC bằng JSON (không markdown):
{{
  "match": true/false,
  "confidence": 0.0-1.0,
  "detected_device": "Tên thiết bị bạn nhìn thấy trong ảnh",
  "detected_brand": "Hãng sản xuất bạn nhìn thấy",
  "detected_model": "Model bạn nhìn thấy (nếu thấy)",
  "detected_type": "Loại thiết bị (máy chiếu/laptop/loa/micro/etc)",
  "reason": "Giải thích ngắn gọn bằng tiếng Việt vì sao khớp hoặc không khớp",
  "details": "Chi tiết quan sát: màu sắc, logo, nhãn, tình trạng bên ngoài"
}}

LƯU Ý:
- match=true nếu thiết bị trong ảnh CHẮC CHẮN khớp với thông tin DB (cùng hãng, cùng loại)
- match=false nếu thiết bị khác loại hoặc khác hãng rõ ràng
- Nếu ảnh mờ/không rõ, confidence thấp nhưng đừng đoán bừa
- Ưu tiên kiểm tra: logo hãng, loại thiết bị, model number trên nhãn
"""

@app.route('/api/ai/verify-device', methods=['POST'])
def verify_device():
    """
    AI xác minh thiết bị: chụp ảnh thực → Gemini Vision so sánh với DB.
    Input: { image_base64: "...", device_id: 1 }
    Output: { match: true/false, confidence: 0.8, reason: "...", ... }
    """
    import requests as req_lib

    try:
        data = request.get_json()
        if not data or 'image_base64' not in data or 'device_id' not in data:
            return jsonify({'success': False, 'error': 'Thiếu image_base64 hoặc device_id'}), 400

        image_base64 = data['image_base64']
        device_id = data['device_id']
        image_mime = data.get('image_mime', 'image/jpeg')

        # Strip data URL prefix
        if ',' in image_base64:
            image_base64 = image_base64.split(',', 1)[1]

        # Decode + resize ảnh
        image_bytes = base64.b64decode(image_base64)
        if PIL_AVAILABLE:
            try:
                img = Image.open(io.BytesIO(image_bytes))
                max_size = 1024
                if max(img.size) > max_size:
                    img.thumbnail((max_size, max_size), Image.LANCZOS)
                    buffer = io.BytesIO()
                    img.save(buffer, format='JPEG', quality=85)
                    image_bytes = buffer.getvalue()
                    image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            except Exception:
                pass

        # Lấy thông tin thiết bị từ DB
        SPRING = 'http://localhost:8080/api/ai-data'
        device = None
        try:
            r = req_lib.get(f'{SPRING}/devices', headers=INTERNAL_HEADERS, timeout=4)
            if r.status_code == 200:
                found = [d for d in r.json() if str(d.get('id')) == str(device_id)]
                if found:
                    device = found[0]
        except Exception as e:
            print(f'verify-device DB error: {e}')

        if not device:
            return jsonify({'success': False, 'error': f'Không tìm thấy thiết bị ID={device_id}'}), 404

        # Tạo prompt với thông tin thiết bị
        prompt = VERIFY_DEVICE_PROMPT.format(
            device_name=device.get('name', ''),
            manufacturer=device.get('manufacturer', ''),
            model=device.get('model', ''),
            category=device.get('category', ''),
            code=device.get('code', '')
        )

        # Gọi Gemini Vision
        image_part = {'inline_data': {'mime_type': image_mime, 'data': image_base64}}
        gemini_response = model.generate_content([prompt, image_part])
        raw_text = gemini_response.text.strip()

        # Parse JSON
        result = {}
        try:
            clean = raw_text
            if '```' in clean:
                start = clean.find('{')
                end = clean.rfind('}') + 1
                clean = clean[start:end]
            result = json.loads(clean)
        except json.JSONDecodeError:
            result = {
                'match': False,
                'confidence': 0.0,
                'reason': 'Không thể phân tích kết quả từ AI',
                'details': raw_text[:300]
            }

        result['success'] = True
        result['device_db'] = {
            'name': device.get('name'),
            'manufacturer': device.get('manufacturer'),
            'model': device.get('model'),
            'category': device.get('category'),
            'code': device.get('code')
        }
        return jsonify(result)

    except Exception as e:
        print(f'verify-device error: {e}')
        return jsonify({'success': False, 'error': str(e)}), 500


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
    # URL ben trong QR
    host = request.host_url.rstrip('/')
    device_info_url = f"{host}/api/ai/device-info/{device_id}"

    if not QR_AVAILABLE:
        # Fallback: dùng API online tạo QR nếu chưa cài thư viện
        from flask import redirect
        import urllib.parse
        return redirect(f"https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={urllib.parse.quote(device_info_url)}")



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

    # === THIẾT BỊ KHÔNG TỒN TẠI ===
    # Nếu kết nối Spring Boot thành công nhưng không tìm thấy device → trang lỗi
    if device is None:
        not_found_html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Thiết bị không tồn tại — QLCSVC</title>
<style>
  * {{ box-sizing:border-box; margin:0; padding:0; }}
  body {{
    font-family:-apple-system,BlinkMacSystemFont,'Inter','Segoe UI',sans-serif;
    background:linear-gradient(160deg,#0f172a 0%,#1e293b 100%);
    min-height:100vh; display:flex; flex-direction:column;
    align-items:center; justify-content:center; padding:24px 16px; color:white;
  }}
  .card {{
    background:rgba(30,41,59,0.8); border:1.5px solid #334155;
    border-radius:24px; padding:48px 32px; text-align:center;
    max-width:420px; width:100%; backdrop-filter:blur(20px);
    box-shadow:0 20px 60px rgba(0,0,0,.5);
  }}
  .icon {{ font-size:4rem; margin-bottom:16px; }}
  h1 {{ font-size:1.5rem; font-weight:700; margin-bottom:8px; color:#f87171; }}
  .msg {{ font-size:0.9rem; color:#94a3b8; line-height:1.6; margin-bottom:24px; }}
  .device-id {{ 
    display:inline-block; background:#1e293b; border:1px solid #475569;
    padding:6px 16px; border-radius:8px; font-family:monospace;
    font-size:0.85rem; color:#f59e0b; margin:8px 0 16px;
  }}
  .info-box {{
    background:rgba(59,130,246,0.1); border:1px solid rgba(59,130,246,0.2);
    border-radius:12px; padding:16px; margin-bottom:24px; text-align:left;
  }}
  .info-box h3 {{ font-size:0.85rem; color:#60a5fa; margin-bottom:8px; }}
  .info-box ul {{ list-style:none; padding:0; }}
  .info-box li {{ 
    font-size:0.8rem; color:#94a3b8; padding:4px 0;
    padding-left:20px; position:relative;
  }}
  .info-box li::before {{ content:'•'; position:absolute; left:6px; color:#3b82f6; }}
  .btn-group {{ display:flex; flex-direction:column; gap:10px; }}
  .btn {{
    display:block; padding:14px 24px; border-radius:12px;
    font-size:0.9rem; font-weight:600; text-decoration:none;
    text-align:center; cursor:pointer; border:none;
    transition:all .2s ease;
  }}
  .btn-primary {{
    background:linear-gradient(135deg,#3b82f6,#2563eb);
    color:white; box-shadow:0 4px 15px rgba(59,130,246,.3);
  }}
  .btn-primary:hover {{ transform:translateY(-2px); box-shadow:0 6px 20px rgba(59,130,246,.4); }}
  .btn-secondary {{
    background:rgba(51,65,85,0.6); color:#94a3b8;
    border:1.5px solid #334155;
  }}
  .btn-secondary:hover {{ background:rgba(51,65,85,0.9); color:white; }}
  .footer {{ margin-top:24px; font-size:0.7rem; color:#475569; }}
</style>
</head>
<body>
  <div class="card">
    <div class="icon">🔍</div>
    <h1>Thiết bị không tồn tại</h1>
    <div class="device-id">ID: {device_id}</div>
    <p class="msg">
      Mã QR này trỏ đến thiết bị <strong>không có trong hệ thống</strong>.<br>
      Có thể thiết bị đã bị xóa hoặc mã QR không đúng.
    </p>
    <div class="info-box">
      <h3>💡 Bạn nên làm gì?</h3>
      <ul>
        <li>Kiểm tra lại mã QR có đúng là của hệ thống QLCSVC không</li>
        <li>Liên hệ quản trị viên để đăng ký thiết bị mới</li>
        <li>Thử quét lại mã QR khác trên thiết bị</li>
      </ul>
    </div>
    <div class="btn-group">
      <a href="{host}/api/ai/scan" class="btn btn-primary">📷 Quét mã QR khác</a>
      <a href="javascript:history.back()" class="btn btn-secondary">← Quay lại</a>
    </div>
  </div>
  <p class="footer">QLCSVC-AI · Hệ thống Quản lý Cơ sở Vật chất</p>
</body>
</html>'''
        resp = make_response(not_found_html, 404)
        resp.headers['Content-Type'] = 'text/html; charset=utf-8'
        return resp

    db_ok = True

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
    dev_mfr   = (device.get('manufacturer') or '—') if device else '—'
    dev_model = (device.get('model') or '—') if device else '—'
    dev_year  = str(device.get('year') or '—') if device else '—'

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

  /* SCAN FLOATING BUTTON */
  .fab-scan {{
    position: fixed; bottom: 24px; right: 18px;
    width: 52px; height: 52px; border-radius: 50%; z-index: 999;
    background: linear-gradient(135deg, #10b981, #059669);
    color: white; border: none; cursor: pointer; font-size: 1.3rem;
    box-shadow: 0 4px 18px rgba(16,185,129,.5);
    display: flex; align-items: center; justify-content: center;
    transition: transform .2s;
  }}
  .fab-scan:active {{ transform: scale(.93); }}
  .fab-scan-label {{
    position: fixed; bottom: 80px; right: 12px;
    background: #10b981; color: white; font-size: 0.6rem;
    font-weight: 700; padding: 2px 7px; border-radius: 10px;
    letter-spacing: .04em; z-index: 999; pointer-events: none;
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

  <!-- Khung XÁC MINH THIẾT BỊ BẰNG AI - Gemini Vision -->
  <div class="card" style="border:1.5px solid #f59e0b30; background:linear-gradient(135deg,#fefce810,#ffffff);">
    <div class="card-label" style="color:#d97706;">🔐 Xác minh thiết bị</div>
    <p style="font-size:0.75rem;color:#92400e;margin-bottom:12px;line-height:1.5;">
      ⚠️ <strong>Đối chiếu thông tin dưới đây với thiết bị thật</strong> trước khi thao tác.
      Nếu không khớp, QR có thể đã bị dán nhầm hoặc tráo đổi.
    </p>
    <div class="info-grid">
      <div><span class="lbl">🏭 Hãng sản xuất</span><span class="val" style="font-weight:700;">{dev_mfr}</span></div>
      <div><span class="lbl">📦 Model</span><span class="val" style="font-weight:700;">{dev_model}</span></div>
      <div><span class="lbl">📅 Năm sản xuất</span><span class="val" style="font-weight:700;">{dev_year}</span></div>
      <div><span class="lbl">🏷️ Mã hệ thống</span><span class="val" style="font-weight:700;color:#0f3460;">{dev_code}</span></div>
    </div>

    <!-- NÚT AI XÁC MINH — redirect sang trang scan thống nhất -->
    <div style="margin-top:16px;text-align:center;">
      <a href="{host}/api/ai/scan?verify={device_id}" style="
        padding:12px 28px; border-radius:12px; border:none;
        background:linear-gradient(135deg,#7c3aed,#6d28d9); color:white;
        font-size:0.88rem; font-weight:700; cursor:pointer;
        box-shadow:0 4px 15px rgba(124,58,237,0.4);
        transition:all .3s; display:inline-flex; align-items:center; gap:8px;
        text-decoration:none;
      ">
        📸 AI Xác minh bằng Camera
      </a>
      <p style="font-size:0.68rem;color:#78716c;margin-top:6px;">
        Chụp ảnh thiết bị thật → AI Gemini Vision so sánh tự động
      </p>
    </div>

    <p style="font-size:0.7rem;color:#78716c;margin-top:10px;text-align:center;">
      Nếu thông tin <strong>không khớp</strong> → 
      <a href="{host}/api/ai/device-lookup" style="color:#2563eb;font-weight:600;text-decoration:underline;">tra cứu thiết bị đúng</a>
    </p>
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

  <!-- AI Đánh giá sức khoẻ thiết bị -->
  <div class="card" id="aiHealthCard">
    <div class="card-label">🤖 Đánh giá AI</div>
    <div id="aiHealthLoading" style="text-align:center;padding:16px;">
      <div class="ai-spinner"></div>
      <div style="font-size:0.78rem;color:#64748b;margin-top:8px;">AI đang phân tích thiết bị...</div>
    </div>
    <div id="aiHealthResult" style="display:none;"></div>
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
<script>
// Phase 2: AI Health Assessment - tự động gọi khi trang load
(async function loadAiHealth() {{
  const loadEl = document.getElementById('aiHealthLoading');
  const resEl  = document.getElementById('aiHealthResult');
  try {{
    const r = await fetch('http://localhost:5000/api/ai/suggest-maintenance', {{
      method: 'POST',
      headers: {{ 'Content-Type': 'application/json' }},
      body: JSON.stringify({{
        equipment_name: '{dev_name}',
        device_id:   {device_id},
        current_status: '{status_raw}',
        status:      '{status_label}',
        category:    '{dev_cat}',
        room:        '{dev_room}',
        maintenance_history: {json.dumps(damages)}
      }})
    }});
    const data = await r.json();
    loadEl.style.display = 'none';
    let msgRaw = data.recommendations || data.suggestion || data.response || data.message || JSON.stringify(data);
    let msg = typeof msgRaw === 'string' ? msgRaw.replace(/\\n/g, '<br/>').replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>') : JSON.stringify(msgRaw);
    // Determine health color
    const txt = msg.toString().toLowerCase();
    const isGood = txt.includes('tốt') || txt.includes('bình thường') || txt.includes('không cần');
    const isWarn = txt.includes('lưu ý') || txt.includes('chú ý') || txt.includes('theo dõi');
    const color  = isGood ? '#16a34a' : isWarn ? '#f59e0b' : '#ef4444';
    const bg     = isGood ? '#f0fdf4' : isWarn ? '#fffbeb' : '#fef2f2';
    const border = isGood ? '#86efac' : isWarn ? '#fde68a' : '#fca5a5';
    const icon   = isGood ? '✅' : isWarn ? '⚠️' : '🚨';
    resEl.innerHTML = `<div style="border:1.5px solid ${{border}};background:${{bg}};border-radius:10px;padding:14px;font-size:0.82rem;">
      <div style="font-weight:700;color:${{color}};margin-bottom:6px;">${{icon}} Đánh giá AI</div>
      <div style="color:#374151;white-space:pre-wrap;">${{msg}}</div>
    </div>`;
    resEl.style.display = 'block';
  }} catch(e) {{
    loadEl.style.display = 'none';
    resEl.innerHTML = '<div style="color:#94a3b8;font-size:0.78rem;text-align:center;">AI không khả dụng — Flask chưa chạy</div>';
    resEl.style.display = 'block';
  }}
}})();
</script>
<!-- Scan FAB button -->
<span class="fab-scan-label">SCAN</span>
<button class="fab-scan" onclick="window.location.href='{host}/api/ai/scan'" title="Scan QR thiết bị khác">📷</button>

<script>
// === AI CAMERA VERIFICATION ===
let verifyStream = null;

function openAIVerify() {{
  const panel = document.getElementById('verifyPanel');
  const video = document.getElementById('verifyVideo');
  const status = document.getElementById('verifyStatus');
  const resultDiv = document.getElementById('verifyResult');
  
  panel.style.display = 'block';
  resultDiv.style.display = 'none';
  status.innerHTML = '<span style="color:#f59e0b;">📷 Đang khởi động camera...</span>';

  navigator.mediaDevices.getUserMedia({{ 
    video: {{ width: {{ ideal: 1280 }}, height: {{ ideal: 720 }} }}
  }}).then(stream => {{
    verifyStream = stream;
    video.srcObject = stream;
    status.innerHTML = '<span style="color:#22c55e;">✅ Camera sẵn sàng — Hướng vào thiết bị rồi nhấn "Chụp & Xác minh"</span>';
  }}).catch(err => {{
    status.innerHTML = '<span style="color:#ef4444;">❌ Không truy cập được camera: ' + err.message + '</span>';
  }});
}}

function closeVerify() {{
  const panel = document.getElementById('verifyPanel');
  const video = document.getElementById('verifyVideo');
  
  if (verifyStream) {{
    verifyStream.getTracks().forEach(t => t.stop());
    verifyStream = null;
  }}
  video.srcObject = null;
  panel.style.display = 'none';
}}

async function captureAndVerify() {{
  const video = document.getElementById('verifyVideo');
  const canvas = document.getElementById('verifyCanvas');
  const status = document.getElementById('verifyStatus');
  const resultDiv = document.getElementById('verifyResult');
  const captureBtn = document.getElementById('captureBtn');

  // Chụp ảnh từ video
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  canvas.getContext('2d').drawImage(video, 0, 0);
  const imageBase64 = canvas.toDataURL('image/jpeg', 0.85);

  // Đóng camera
  closeVerify();

  // Hiển thị loading
  captureBtn.disabled = true;
  status.innerHTML = '';
  resultDiv.style.display = 'block';
  resultDiv.innerHTML = `
    <div style="text-align:center;padding:20px;background:#f5f3ff;border-radius:12px;border:1.5px solid #c4b5fd;">
      <div style="font-size:2rem;margin-bottom:8px;">🤖</div>
      <div style="font-weight:700;color:#7c3aed;font-size:0.9rem;">AI Gemini Vision đang phân tích...</div>
      <div style="font-size:0.75rem;color:#8b5cf6;margin-top:4px;">So sánh ảnh chụp với thông tin trong hệ thống</div>
      <div style="margin-top:12px;">
        <div style="width:60px;height:4px;background:#e9d5ff;border-radius:2px;margin:0 auto;overflow:hidden;">
          <div style="width:100%;height:100%;background:linear-gradient(90deg,#7c3aed,#a78bfa);animation:loading 1.2s ease-in-out infinite;"></div>
        </div>
      </div>
    </div>
    <style>@keyframes loading {{ 0%{{transform:translateX(-100%)}} 100%{{transform:translateX(100%)}} }}</style>
  `;

  try {{
    const resp = await fetch('{host}/api/ai/verify-device', {{
      method: 'POST',
      headers: {{ 'Content-Type': 'application/json' }},
      body: JSON.stringify({{
        image_base64: imageBase64,
        device_id: {device_id},
        image_mime: 'image/jpeg'
      }})
    }});

    const data = await resp.json();

    if (!data.success) {{
      resultDiv.innerHTML = `
        <div style="padding:16px;background:#fef2f2;border-radius:12px;border:1.5px solid #fca5a5;">
          <div style="font-weight:700;color:#ef4444;">❌ Lỗi: ${{data.error || 'Không xác định'}}</div>
        </div>`;
      return;
    }}

    const isMatch = data.match === true;
    const confidence = Math.round((data.confidence || 0) * 100);
    const bg = isMatch ? '#f0fdf4' : '#fef2f2';
    const border = isMatch ? '#86efac' : '#fca5a5';
    const color = isMatch ? '#16a34a' : '#dc2626';
    const icon = isMatch ? '✅' : '❌';
    const title = isMatch ? 'THIẾT BỊ KHỚP' : 'KHÔNG KHỚP — CÓ THỂ QR ĐÃ BỊ TRÁO';

    resultDiv.innerHTML = `
      <div style="padding:16px;background:${{bg}};border-radius:12px;border:2px solid ${{border}};
        animation:fadeIn .4s ease;">
        <div style="text-align:center;margin-bottom:12px;">
          <div style="font-size:2.5rem;margin-bottom:4px;">${{icon}}</div>
          <div style="font-weight:800;color:${{color}};font-size:1rem;">${{title}}</div>
          <div style="font-size:0.78rem;color:#6b7280;margin-top:2px;">Độ tin cậy: <strong style="color:${{color}};">${{confidence}}%</strong></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:0.78rem;">
          <div style="background:white;padding:8px;border-radius:8px;">
            <div style="color:#6b7280;font-size:0.68rem;">AI nhận diện:</div>
            <div style="font-weight:700;">${{data.detected_device || '—'}}</div>
          </div>
          <div style="background:white;padding:8px;border-radius:8px;">
            <div style="color:#6b7280;font-size:0.68rem;">Hãng phát hiện:</div>
            <div style="font-weight:700;">${{data.detected_brand || '—'}}</div>
          </div>
          <div style="background:white;padding:8px;border-radius:8px;">
            <div style="color:#6b7280;font-size:0.68rem;">Model phát hiện:</div>
            <div style="font-weight:700;">${{data.detected_model || '—'}}</div>
          </div>
          <div style="background:white;padding:8px;border-radius:8px;">
            <div style="color:#6b7280;font-size:0.68rem;">Loại:</div>
            <div style="font-weight:700;">${{data.detected_type || '—'}}</div>
          </div>
        </div>
        <div style="margin-top:10px;padding:10px;background:white;border-radius:8px;font-size:0.78rem;">
          <div style="color:#6b7280;font-size:0.68rem;margin-bottom:4px;">💬 Nhận xét AI:</div>
          <div style="color:#374151;line-height:1.5;">${{data.reason || ''}}</div>
        </div>
        ${{data.details ? `<div style="margin-top:6px;padding:8px;background:white;border-radius:8px;font-size:0.72rem;color:#6b7280;">📋 ${{data.details}}</div>` : ''}}
        <div style="text-align:center;margin-top:12px;">
          <button onclick="openAIVerify()" style="padding:8px 20px;border-radius:8px;border:none;
            background:linear-gradient(135deg,#7c3aed,#6d28d9);color:white;font-weight:600;
            font-size:0.8rem;cursor:pointer;">🔄 Xác minh lại</button>
        </div>
      </div>
      <style>@keyframes fadeIn {{ from{{opacity:0;transform:translateY(10px)}} to{{opacity:1;transform:translateY(0)}} }}</style>
    `;
  }} catch(err) {{
    resultDiv.innerHTML = `
      <div style="padding:16px;background:#fef2f2;border-radius:12px;border:1.5px solid #fca5a5;">
        <div style="font-weight:700;color:#ef4444;">❌ Lỗi kết nối: ${{err.message}}</div>
        <div style="font-size:0.75rem;color:#6b7280;margin-top:4px;">Kiểm tra Flask API đang chạy tại port 5000</div>
      </div>`;
  }}

  captureBtn.disabled = false;
}}
</script>
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
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:6px;">
          <button type="button" id="aiAnalyzeBtn" onclick="aiAnalyze()"
            style="display:inline-flex;align-items:center;gap:5px;padding:6px 12px;
                   background:linear-gradient(135deg,#6366f1,#4f46e5);color:white;
                   border:none;border-radius:8px;font-size:0.75rem;font-weight:700;
                   cursor:pointer;box-shadow:0 2px 8px #6366f133;transition:opacity .15s;">
            🤖 AI Phân tích
          </button>
          <div class="char-count"><span id="charCount">0</span>/1000</div>
        </div>
        <!-- AI Result Banner -->
        <div id="aiResult" style="display:none;margin-top:10px;"></div>
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

  // ===== AI ANALYZE FEATURE =====
  async function aiAnalyze() {{
    const moTa = document.getElementById('moTa').value.trim();
    if (moTa.length < 10) {{
      alert('Vui lòng mô tả sự cố trước (ít nhất 10 ký tự) để AI phân tích.');
      return;
    }}

    const btn = document.getElementById('aiAnalyzeBtn');
    const result = document.getElementById('aiResult');
    btn.disabled = true;
    btn.innerHTML = '⏳ AI đang phân tích...';
    result.style.display = 'none';

    try {{
      const r = await fetch('http://localhost:5000/api/ai/analyze-damage', {{
        method: 'POST',
        headers: {{ 'Content-Type': 'application/json' }},
        body: JSON.stringify({{
          description: moTa,
          device_name: document.querySelector('[name=device_id]')?.value || ''
        }})
      }});
      const data = await r.json();

      if (data.success || data.analysis) {{
        const analysis = data.analysis || data.response || JSON.stringify(data);
        // Show result banner
        result.innerHTML = `
          <div style="background:#f0fdf4;border:1.5px solid #86efac;border-radius:10px;padding:14px;font-size:0.82rem;">
            <div style="font-weight:700;color:#16a34a;margin-bottom:6px;">✅ AI đã phân tích</div>
            <div style="color:#374151;white-space:pre-wrap;">${{analysis}}</div>
          </div>`;
        result.style.display = 'block';

        // Try to auto-select severity from AI response
        const txt = analysis.toLowerCase();
        if (txt.includes('khẩn cấp') || txt.includes('khan_cap') || txt.includes('nguy hiểm')) {{
          document.getElementById('s4').checked = true;
        }} else if (txt.includes('nghiêm trọng') || txt.includes('cao') || txt.includes('không dùng')) {{
          document.getElementById('s3').checked = true;
        }} else if (txt.includes('nhẹ') || txt.includes('thap') || txt.includes('vẫn dùng')) {{
          document.getElementById('s1').checked = true;
        }} else {{
          document.getElementById('s2').checked = true;
        }}
      }} else {{
        result.innerHTML = `<div style="background:#fef2f2;border:1.5px solid #fca5a5;border-radius:10px;padding:12px;font-size:0.8rem;color:#dc2626;">❌ ${{data.error || 'Không phân tích được. Thử lại sau.'}}</div>`;
        result.style.display = 'block';
      }}
    }} catch(err) {{
      result.innerHTML = `<div style="background:#fef2f2;border:1.5px solid #fca5a5;border-radius:10px;padding:12px;font-size:0.8rem;color:#dc2626;">❌ Không kết nối được Flask AI: ${{err.message}}</div>`;
      result.style.display = 'block';
    }} finally {{
      btn.disabled = false;
      btn.innerHTML = '🤖 AI Phân tích';
    }}
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

@app.route('/api/ai/device-lookup')
def device_lookup_page():
    """Trang tra cứu thiết bị — khi mất QR, nhân viên tìm theo tên/mã/phòng và in lại QR."""
    import requests as req_lib
    from flask import make_response

    SPRING = 'http://localhost:8080/api/ai-data'
    devices = []
    try:
        r = req_lib.get(f'{SPRING}/devices', headers=INTERNAL_HEADERS, timeout=5)
        if r.status_code == 200:
            devices = r.json()
    except Exception as e:
        print(f'device-lookup error: {e}')

    host = request.host_url.rstrip('/')

    # Build device rows JSON for search
    device_rows = ''
    for d in devices:
        did = d.get('id','')
        name = (d.get('name') or '').replace("'","\\'")
        code = (d.get('code') or '').replace("'","\\'")
        room = (d.get('room') or '—').replace("'","\\'")
        cat = (d.get('category') or '—').replace("'","\\'")
        mfr = (d.get('manufacturer') or '—').replace("'","\\'")
        model = (d.get('model') or '—').replace("'","\\'")
        status = d.get('status','')
        status_labels = {'TOT':'Tốt','BAO_TRI':'Bảo trì','HONG':'Hỏng','THANH_LY':'Thanh lý'}
        status_label = status_labels.get(status, status)
        status_colors = {'TOT':'#22c55e','BAO_TRI':'#f59e0b','HONG':'#ef4444','THANH_LY':'#6b7280'}
        scolor = status_colors.get(status,'#6b7280')
        device_rows += f"""
        <tr class="dev-row" data-search="{name.lower()} {code.lower()} {room.lower()} {cat.lower()} {mfr.lower()} {model.lower()}">
          <td style="font-weight:600;">{did}</td>
          <td>{name}</td>
          <td style="font-family:monospace;color:#2563eb;">{code}</td>
          <td>{room}</td>
          <td>{cat}</td>
          <td>{mfr}</td>
          <td>{model}</td>
          <td><span style="color:{scolor};font-weight:600;">{status_label}</span></td>
          <td style="white-space:nowrap;">
            <a href="{host}/api/ai/device-info/{did}" style="color:#2563eb;text-decoration:none;font-weight:600;margin-right:8px;">📋 Xem</a>
            <a href="{host}/api/ai/device-qr/{did}" target="_blank" style="color:#059669;text-decoration:none;font-weight:600;">🖨️ In QR</a>
          </td>
        </tr>"""

    html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Tra cứu thiết bị — QLCSVC</title>
<style>
  * {{ box-sizing:border-box; margin:0; padding:0; }}
  body {{
    font-family:-apple-system,BlinkMacSystemFont,'Inter','Segoe UI',sans-serif;
    background:linear-gradient(160deg,#0f172a 0%,#1e293b 100%);
    min-height:100vh; padding:24px 16px; color:white;
  }}
  .container {{ max-width:1100px; margin:0 auto; }}
  .header {{ text-align:center; margin-bottom:28px; }}
  .header h1 {{ font-size:1.6rem; font-weight:800; margin-bottom:4px; }}
  .header p {{ font-size:0.85rem; color:#94a3b8; }}
  .search-box {{
    width:100%; padding:14px 20px; border-radius:14px;
    border:1.5px solid #334155; background:#1e293b; color:white;
    font-size:0.95rem; margin-bottom:20px; outline:none;
    transition:border-color .2s;
  }}
  .search-box:focus {{ border-color:#3b82f6; }}
  .search-box::placeholder {{ color:#64748b; }}
  .card {{
    background:rgba(30,41,59,0.8); border:1.5px solid #334155;
    border-radius:16px; overflow:hidden; backdrop-filter:blur(20px);
  }}
  table {{ width:100%; border-collapse:collapse; font-size:0.8rem; }}
  th {{
    background:#0f172a; color:#94a3b8; padding:12px 10px;
    text-align:left; font-weight:600; font-size:0.72rem;
    text-transform:uppercase; letter-spacing:.05em;
    position:sticky; top:0; z-index:2;
  }}
  td {{ padding:10px; border-top:1px solid #1e293b; color:#cbd5e1; }}
  tr:hover td {{ background:#1e293b80; }}
  .no-result {{
    text-align:center; padding:40px; color:#64748b; font-size:0.9rem;
    display:none;
  }}
  .info-banner {{
    background:rgba(59,130,246,0.1); border:1px solid rgba(59,130,246,0.2);
    border-radius:12px; padding:14px 18px; margin-bottom:20px;
    font-size:0.82rem; color:#93c5fd; line-height:1.6;
  }}
  .info-banner strong {{ color:#60a5fa; }}
  .btn-group {{ display:flex; gap:10px; margin-bottom:20px; flex-wrap:wrap; }}
  .btn {{
    padding:10px 20px; border-radius:10px; font-size:0.82rem;
    font-weight:600; text-decoration:none; border:1.5px solid #334155;
    color:#94a3b8; background:#1e293b; cursor:pointer; transition:all .2s;
  }}
  .btn:hover {{ background:#334155; color:white; }}
  .btn-primary {{ background:#2563eb; color:white; border-color:#2563eb; }}
  .btn-primary:hover {{ background:#1d4ed8; }}
  .count {{ font-size:0.75rem; color:#64748b; margin-bottom:10px; padding:0 10px; }}
</style>
</head>
<body>
<div class="container">
  <div class="header">
    <h1>🔍 Tra cứu thiết bị</h1>
    <p>Tìm thiết bị theo tên, mã, phòng, hãng SX — In lại QR khi mất nhãn</p>
  </div>

  <div class="info-banner">
    <strong>💡 Khi nào dùng trang này?</strong><br>
    • Mã QR trên thiết bị bị mất, hỏng, hoặc không quét được<br>
    • Cần xác minh thiết bị nào ứng với mã QR nào<br>
    • Nhân viên bảo trì cần tìm nhanh thiết bị theo phòng/loại
  </div>

  <div class="btn-group">
    <a href="{host}/api/ai/scan" class="btn btn-primary">📷 Quét QR</a>
    <a href="{host}/api/ai/qr-print" class="btn">🖨️ In tất cả QR</a>
    <a href="javascript:history.back()" class="btn">← Quay lại</a>
  </div>

  <div style="display:flex;gap:10px;margin-bottom:20px;">
    <input type="text" class="search-box" id="searchInput" style="margin-bottom:0;flex:1;"
      placeholder="🔍 Tìm theo tên thiết bị, mã, phòng, hãng sản xuất...">
    <button id="voiceBtn" onclick="startVoice()" title="Tìm bằng giọng nói" style="
      min-width:52px; height:52px; border-radius:14px; border:1.5px solid #334155;
      background:linear-gradient(135deg,#1e293b,#0f172a); color:#94a3b8;
      font-size:1.4rem; cursor:pointer; transition:all .3s; display:flex;
      align-items:center; justify-content:center;
    ">🎤</button>
  </div>
  <div id="voiceStatus" style="font-size:0.75rem;color:#64748b;margin:-12px 0 12px 4px;min-height:18px;"></div>

  <div class="count" id="resultCount">Tìm thấy {len(devices)} thiết bị</div>

  <div class="card">
    <div style="overflow-x:auto;">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Tên thiết bị</th><th>Mã</th><th>Phòng</th>
            <th>Loại</th><th>Hãng SX</th><th>Model</th><th>Trạng thái</th><th>Thao tác</th>
          </tr>
        </thead>
        <tbody id="deviceTable">
          {device_rows}
        </tbody>
      </table>
    </div>
    <div class="no-result" id="noResult">
      😔 Không tìm thấy thiết bị nào khớp
    </div>
  </div>
</div>

<script>
  const input = document.getElementById('searchInput');
  const rows = document.querySelectorAll('.dev-row');
  const noResult = document.getElementById('noResult');
  const countEl = document.getElementById('resultCount');

  input.addEventListener('input', function() {{
    const q = this.value.toLowerCase().trim();
    let visible = 0;
    rows.forEach(row => {{
      const match = !q || row.dataset.search.includes(q);
      row.style.display = match ? '' : 'none';
      if (match) visible++;
    }});
    noResult.style.display = visible === 0 ? 'block' : 'none';
    countEl.textContent = `Tìm thấy ${{visible}} thiết bị`;
  }});

  input.focus();

  // === VOICE SEARCH (Web Speech API) ===
  let recognition = null;
  let isListening = false;

  function startVoice() {{
    const btn = document.getElementById('voiceBtn');
    const status = document.getElementById('voiceStatus');

    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {{
      status.innerHTML = '<span style="color:#f87171;">❌ Trình duyệt không hỗ trợ giọng nói. Dùng Chrome/Edge.</span>';
      return;
    }}

    if (isListening && recognition) {{
      recognition.stop();
      return;
    }}

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    recognition = new SpeechRecognition();
    recognition.lang = 'vi-VN';
    recognition.continuous = false;
    recognition.interimResults = true;

    recognition.onstart = () => {{
      isListening = true;
      btn.style.background = 'linear-gradient(135deg,#dc2626,#b91c1c)';
      btn.style.color = 'white';
      btn.style.borderColor = '#dc2626';
      btn.innerHTML = '🔴';
      status.innerHTML = '<span style="color:#f59e0b;">🎙️ Đang nghe... Nói tên thiết bị, phòng, hoặc hãng sản xuất</span>';
    }};

    recognition.onresult = (e) => {{
      let transcript = '';
      for (let i = e.resultIndex; i < e.results.length; i++) {{
        transcript += e.results[i][0].transcript;
      }}
      input.value = transcript;
      input.dispatchEvent(new Event('input'));
      if (e.results[e.results.length - 1].isFinal) {{
        status.innerHTML = `<span style="color:#22c55e;">✅ Đã nhận: "${{transcript}}"</span>`;
      }}
    }};

    recognition.onerror = (e) => {{
      isListening = false;
      btn.style.background = 'linear-gradient(135deg,#1e293b,#0f172a)';
      btn.style.color = '#94a3b8';
      btn.style.borderColor = '#334155';
      btn.innerHTML = '🎤';
      if (e.error === 'no-speech') {{
        status.innerHTML = '<span style="color:#f59e0b;">⚠️ Không nghe thấy gì. Nhấn 🎤 và nói lại.</span>';
      }} else if (e.error === 'not-allowed') {{
        status.innerHTML = '<span style="color:#f87171;">❌ Cho phép quyền microphone rồi thử lại.</span>';
      }} else {{
        status.innerHTML = `<span style="color:#f87171;">❌ Lỗi: ${{e.error}}</span>`;
      }}
    }};

    recognition.onend = () => {{
      isListening = false;
      btn.style.background = 'linear-gradient(135deg,#1e293b,#0f172a)';
      btn.style.color = '#94a3b8';
      btn.style.borderColor = '#334155';
      btn.innerHTML = '🎤';
    }};

    recognition.start();
  }}
</script>
</body>
</html>'''
    resp = make_response(html)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    return resp

@app.route('/api/ai/scan')
def scan_page():
    """Trang SCAN THỐNG NHẤT: Tab QR + Tab AI Camera (nhận diện / xác minh)."""
    from flask import make_response
    host = request.host_url.rstrip('/')
    verify_id = request.args.get('verify', '0')
    default_tab = 'ai' if verify_id != '0' else 'qr'

    # Banner xác minh nếu đang ở mode verify
    verify_banner = ''
    if verify_id != '0':
        verify_banner = f'''<div class="verify-banner">
          🔐 Đang xác minh thiết bị <strong>ID #{verify_id}</strong><br>
          Chụp ảnh thiết bị thật → AI so sánh với dữ liệu hệ thống
        </div>'''

    tab_qr_active = 'active' if default_tab == 'qr' else ''
    tab_ai_active = 'active' if default_tab == 'ai' else ''
    qr_hidden = '' if default_tab == 'qr' else 'hidden'
    ai_hidden = '' if default_tab == 'ai' else 'hidden'

    html = f'''<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Scan & AI — QLCSVC</title>
  <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
  <style>
    * {{ box-sizing:border-box; margin:0; padding:0; }}
    body {{
      font-family:-apple-system,BlinkMacSystemFont,'Inter',sans-serif;
      background:linear-gradient(160deg,#0f172a 0%,#1e293b 100%);
      min-height:100vh; display:flex; flex-direction:column;
      align-items:center; padding:20px 16px; color:white;
    }}
    h1 {{ font-size:1.4rem; font-weight:800; margin:8px 0 4px; }}
    .sub {{ font-size:0.78rem; color:#94a3b8; margin-bottom:20px; text-align:center; }}
    .tabs {{
      display:flex; gap:4px; width:100%; max-width:420px;
      background:#1e293b; border-radius:14px; padding:4px;
      border:1.5px solid #334155; margin-bottom:16px;
    }}
    .tab {{
      flex:1; padding:12px 8px; border-radius:11px; border:none;
      font-size:0.82rem; font-weight:700; cursor:pointer;
      background:transparent; color:#64748b; transition:all .3s;
      display:flex; align-items:center; justify-content:center; gap:6px;
    }}
    .tab.active {{
      background:linear-gradient(135deg,#7c3aed,#6d28d9);
      color:white; box-shadow:0 4px 15px rgba(124,58,237,.4);
    }}
    .tab:not(.active):hover {{ color:#cbd5e1; }}
    .panel {{
      width:100%; max-width:420px; background:#1e293b;
      border-radius:20px; padding:16px; border:1.5px solid #334155;
      box-shadow:0 20px 60px rgba(0,0,0,.5);
    }}
    .hidden {{ display:none !important; }}
    #qr-reader {{ width:100%; border-radius:12px; overflow:hidden; }}
    #qr-reader video {{ border-radius:12px !important; }}
    .camera-wrap {{
      position:relative; border-radius:12px; overflow:hidden;
      background:#000; min-height:240px;
    }}
    .camera-wrap video {{ width:100%; display:block; border-radius:12px; }}
    .status {{ margin-top:12px; text-align:center; font-size:0.78rem; color:#94a3b8; min-height:22px; }}
    .btn-capture {{
      width:100%; padding:14px; border-radius:12px; border:none;
      background:linear-gradient(135deg,#7c3aed,#6d28d9); color:white;
      font-size:0.9rem; font-weight:700; cursor:pointer; margin-top:12px;
      display:flex; align-items:center; justify-content:center; gap:8px;
      box-shadow:0 4px 15px rgba(124,58,237,.4); transition:all .3s;
    }}
    .btn-capture:hover {{ transform:translateY(-1px); }}
    .btn-capture:disabled {{ opacity:.5; cursor:not-allowed; transform:none; }}
    .result-card {{ margin-top:16px; border-radius:14px; padding:16px; animation:fadeIn .4s ease; }}
    .result-match {{ background:#f0fdf4; border:2px solid #86efac; }}
    .result-mismatch {{ background:#fef2f2; border:2px solid #fca5a5; }}
    .result-info {{ background:#eff6ff; border:2px solid #93c5fd; }}
    .result-grid {{ display:grid; grid-template-columns:1fr 1fr; gap:6px; font-size:0.75rem; margin-top:10px; }}
    .result-grid > div {{ background:white; padding:8px; border-radius:8px; }}
    .result-grid .lbl {{ color:#6b7280; font-size:0.65rem; display:block; }}
    .result-grid .val {{ font-weight:700; color:#1e293b; }}
    .action-btns {{ display:flex; gap:8px; margin-top:12px; }}
    .action-btns a, .action-btns button {{
      flex:1; padding:10px; border-radius:10px; border:none;
      font-size:0.8rem; font-weight:600; cursor:pointer;
      text-decoration:none; text-align:center;
    }}
    .btn-view {{ background:#2563eb; color:white; }}
    .btn-report {{ background:#dc2626; color:white; }}
    .btn-retry {{ background:#334155; color:#94a3b8; border:1.5px solid #475569 !important; }}
    .bottom-links {{ display:flex; gap:8px; margin-top:16px; width:100%; max-width:420px; }}
    .bottom-links a {{
      flex:1; padding:12px; border-radius:12px; font-size:0.82rem;
      text-decoration:none; text-align:center; font-weight:600;
      border:1.5px solid #334155; color:#94a3b8;
    }}
    .bottom-links a:hover {{ border-color:#7c3aed; color:#c4b5fd; }}
    .verify-banner {{
      width:100%; max-width:420px; padding:10px 14px; border-radius:12px;
      background:linear-gradient(135deg,#7c3aed20,#6d28d920);
      border:1.5px solid #7c3aed50; margin-bottom:12px;
      font-size:0.75rem; color:#c4b5fd; text-align:center;
    }}
    .verify-banner strong {{ color:#a78bfa; }}
    .loading-bar {{ width:60px; height:4px; background:#e9d5ff; border-radius:2px; margin:8px auto 0; overflow:hidden; }}
    .loading-bar > div {{ width:100%; height:100%; background:linear-gradient(90deg,#7c3aed,#a78bfa); animation:slide 1.2s ease-in-out infinite; }}
    @keyframes slide {{ 0%{{transform:translateX(-100%)}} 100%{{transform:translateX(100%)}} }}
    @keyframes fadeIn {{ from{{opacity:0;transform:translateY(8px)}} to{{opacity:1;transform:translateY(0)}} }}
  </style>
</head>
<body>
  <h1>📷 Scan & AI Hỗ trợ</h1>
  <p class="sub">Quét mã QR hoặc chụp ảnh thiết bị — AI nhận diện tự động</p>

  {verify_banner}

  <div class="tabs">
    <button class="tab {tab_qr_active}" onclick="switchTab('qr')" id="tabQR">🔲 Quét QR</button>
    <button class="tab {tab_ai_active}" onclick="switchTab('ai')" id="tabAI">🤖 Chụp ảnh AI</button>
  </div>

  <div class="panel {qr_hidden}" id="panelQR">
    <select id="camera-select" style="display:none; width:100%; padding:10px; margin-bottom:12px; border-radius:8px; background:#1e293b; color:#fff; border:1px solid #334155; font-size:0.9rem;"></select>
    <div id="qr-reader"></div>
    <div class="status" id="qr-status">Đang khởi động camera...</div>
  </div>

  <div class="panel {ai_hidden}" id="panelAI">
    <select id="camera-select-ai" style="display:none; width:100%; padding:10px; margin-bottom:12px; border-radius:8px; background:#1e293b; color:#fff; border:1px solid #334155; font-size:0.9rem;"></select>
    <div class="camera-wrap">
      <video id="aiVideo" autoplay playsinline muted></video>
      <canvas id="aiCanvas" style="display:none;"></canvas>
    </div>
    <div class="status" id="ai-status">Nhấn nút bên dưới để mở camera</div>
    <button class="btn-capture" id="captureBtn" onclick="handleCapture()">
      📸 Chụp & AI Phân tích
    </button>
    <div id="aiResult"></div>
  </div>

  <div class="bottom-links">
    <a href="{host}/api/ai/device-lookup">🔍 Tra cứu</a>
    <a href="javascript:history.back()">← Quay lại</a>
  </div>

<script>
const HOST = '{host}';
const VERIFY_ID = {verify_id};
let currentTab = '{default_tab}';
let qrScanner = null;
let aiStream = null;
let cameraStarted = false;

function switchTab(tab) {{
  currentTab = tab;
  document.getElementById('panelQR').classList.toggle('hidden', tab !== 'qr');
  document.getElementById('panelAI').classList.toggle('hidden', tab !== 'ai');
  document.getElementById('tabQR').classList.toggle('active', tab === 'qr');
  document.getElementById('tabAI').classList.toggle('active', tab === 'ai');
  if (tab === 'qr') {{ stopAICamera(); startQR(); }}
  if (tab === 'ai') {{ stopQR(); startAICamera(); }}
}}

function startQR(preferredCamId = null) {{
  if (qrScanner) return;
  try {{
    Html5Qrcode.getCameras().then(devices => {{
      if (devices && devices.length > 0) {{
        const cameraSelect = document.getElementById('camera-select');
        cameraSelect.style.display = 'block';
        cameraSelect.innerHTML = '';
        
        let defaultCameraId = devices[0].id;
        devices.forEach((cam, index) => {{
          let option = document.createElement('option');
          option.value = cam.id;
          option.text = cam.label || `Camera ${{index + 1}}`;
          cameraSelect.appendChild(option);
          
          let label = cam.label.toLowerCase();
          if (label.includes('integrated') || label.includes('webcam') || label.includes('back') || label.includes('sau')) {{
            defaultCameraId = cam.id;
          }}
        }});
        
        let camId = preferredCamId || defaultCameraId;
        cameraSelect.value = camId;
        cameraSelect.onchange = (e) => {{
          stopQR();
          setTimeout(() => startQR(e.target.value), 300);
        }};

        qrScanner = new Html5Qrcode('qr-reader');
        qrScanner.start(
          camId,
          {{ fps:10, qrbox:{{ width:220, height:220 }} }},
          (decoded) => {{
            document.getElementById('qr-status').innerHTML =
              '<span style="color:#10b981;font-weight:700;">✅ Đã scan! Đang chuyển...</span>';
            qrScanner.stop().then(() => {{
              qrScanner = null;
              window.location.href = decoded.startsWith('http') ? decoded : HOST + '/api/ai/device-info/' + decoded;
            }});
          }},
          () => {{}}
        ).then(() => {{
          document.getElementById('qr-status').textContent = 'Hướng camera vào mã QR trên thiết bị';
        }}).catch(() => {{
          document.getElementById('qr-status').innerHTML = '<span style="color:#f87171;">❌ Không truy cập camera</span>';
        }});
      }} else {{
        document.getElementById('qr-status').innerHTML = '<span style="color:#f87171;">❌ Không tìm thấy camera</span>';
      }}
    }}).catch(err => {{
      document.getElementById('qr-status').innerHTML = '<span style="color:#f87171;">❌ Lỗi quyền Camera: ' + err + '</span>';
    }});
  }} catch(e) {{ console.error(e); }}
}}

function stopQR() {{
  if (qrScanner) {{ qrScanner.stop().catch(() => {{}}); qrScanner = null; }}
}}

function startAICamera(preferredCamId = null) {{
  if (cameraStarted && !preferredCamId) return;
  const video = document.getElementById('aiVideo');
  const status = document.getElementById('ai-status');
  status.innerHTML = '<span style="color:#f59e0b;">📷 Đang mở camera...</span>';
  
  Html5Qrcode.getCameras().then(devices => {{
    if (devices && devices.length > 0) {{
      const cameraSelect = document.getElementById('camera-select-ai');
      cameraSelect.style.display = 'block';
      
      let defaultCameraId = devices[0].id;
      if (cameraSelect.options.length === 0) {{
        devices.forEach((cam, index) => {{
          let option = document.createElement('option');
          option.value = cam.id;
          option.text = cam.label || `Camera ${{index + 1}}`;
          cameraSelect.appendChild(option);
          
          let label = cam.label.toLowerCase();
          if (label.includes('integrated') || label.includes('webcam') || label.includes('back') || label.includes('sau')) {{
            defaultCameraId = cam.id;
          }}
        }});
        cameraSelect.onchange = (e) => {{
          stopAICamera();
          setTimeout(() => startAICamera(e.target.value), 300);
        }};
      }}
      
      let camId = preferredCamId || defaultCameraId;
      if (!preferredCamId && cameraSelect.value) camId = cameraSelect.value;
      cameraSelect.value = camId;
      
      navigator.mediaDevices.getUserMedia({{
        video: {{ deviceId: {{ exact: camId }}, width: {{ ideal:1280 }}, height: {{ ideal:720 }} }}
      }}).then(stream => {{
        aiStream = stream; video.srcObject = stream; cameraStarted = true;
        status.innerHTML = '<span style="color:#22c55e;">✅ Sẵn sàng — Hướng vào thiết bị rồi nhấn Chụp</span>';
      }}).catch(err => {{
        status.innerHTML = '<span style="color:#f87171;">❌ ' + err.message + '</span>';
      }});
    }} else {{
      status.innerHTML = '<span style="color:#f87171;">❌ Không tìm thấy camera</span>';
    }}
  }}).catch(err => {{
    status.innerHTML = '<span style="color:#f87171;">❌ Lỗi quyền Camera: ' + err + '</span>';
  }});
}}

function stopAICamera() {{
  if (aiStream) {{
    aiStream.getTracks().forEach(t => t.stop());
    aiStream = null; cameraStarted = false;
    document.getElementById('aiVideo').srcObject = null;
  }}
}}

async function handleCapture() {{
  const video = document.getElementById('aiVideo');
  const canvas = document.getElementById('aiCanvas');
  const btn = document.getElementById('captureBtn');
  const result = document.getElementById('aiResult');

  if (!cameraStarted) {{ startAICamera(); return; }}

  canvas.width = video.videoWidth; canvas.height = video.videoHeight;
  canvas.getContext('2d').drawImage(video, 0, 0);
  const base64 = canvas.toDataURL('image/jpeg', 0.85);

  btn.disabled = true;
  btn.innerHTML = '🤖 AI đang phân tích...';
  result.innerHTML = `<div style="text-align:center;padding:20px;margin-top:12px;background:#1e1b4b;border-radius:14px;border:1.5px solid #4c1d95;">
    <div style="font-size:1.8rem;">🤖</div>
    <div style="color:#a78bfa;font-weight:700;font-size:0.85rem;margin-top:4px;">
      ${{VERIFY_ID ? 'AI đang xác minh thiết bị...' : 'AI Gemini Vision đang nhận diện...'}}
    </div><div class="loading-bar"><div></div></div></div>`;

  try {{
    let data;
    if (VERIFY_ID) {{
      const r = await fetch(HOST + '/api/ai/verify-device', {{
        method:'POST', headers:{{'Content-Type':'application/json'}},
        body: JSON.stringify({{ image_base64:base64, device_id:VERIFY_ID, image_mime:'image/jpeg' }})
      }});
      data = await r.json();
      showVerifyResult(data);
    }} else {{
      const r = await fetch(HOST + '/api/ai/scan-image', {{
        method:'POST', headers:{{'Content-Type':'application/json'}},
        body: JSON.stringify({{ image_base64:base64, image_mime:'image/jpeg' }})
      }});
      data = await r.json();
      showScanResult(data);
    }}
  }} catch(err) {{
    result.innerHTML = '<div class="result-card result-mismatch"><div style="font-weight:700;color:#dc2626;">❌ ' + err.message + '</div></div>';
  }}
  btn.disabled = false;
  btn.innerHTML = '📸 Chụp & AI Phân tích';
}}

function showVerifyResult(data) {{
  const result = document.getElementById('aiResult');
  if (!data.success) {{ result.innerHTML = '<div class="result-card result-mismatch"><div style="font-weight:700;color:#dc2626;">❌ '+data.error+'</div></div>'; return; }}
  const ok = data.match === true;
  const pct = Math.round((data.confidence||0)*100);
  result.innerHTML = `
    <div class="result-card ${{ok?'result-match':'result-mismatch'}}">
      <div style="text-align:center;">
        <div style="font-size:2.2rem;">${{ok?'✅':'❌'}}</div>
        <div style="font-weight:800;color:${{ok?'#16a34a':'#dc2626'}};font-size:0.95rem;">${{ok?'THIẾT BỊ KHỚP':'KHÔNG KHỚP — QR CÓ THỂ BỊ TRÁO'}}</div>
        <div style="font-size:0.75rem;color:#6b7280;">Độ tin cậy: <strong style="color:${{ok?'#16a34a':'#dc2626'}}">${{pct}}%</strong></div>
      </div>
      <div class="result-grid">
        <div><span class="lbl">AI nhận diện</span><span class="val">${{data.detected_device||'—'}}</span></div>
        <div><span class="lbl">Hãng</span><span class="val">${{data.detected_brand||'—'}}</span></div>
        <div><span class="lbl">Model</span><span class="val">${{data.detected_model||'—'}}</span></div>
        <div><span class="lbl">Loại</span><span class="val">${{data.detected_type||'—'}}</span></div>
      </div>
      <div style="margin-top:8px;padding:10px;background:white;border-radius:8px;font-size:0.76rem;">
        <div style="color:#6b7280;font-size:0.65rem;">💬 AI nhận xét:</div>
        <div style="color:#374151;line-height:1.5;">${{data.reason||''}}</div>
      </div>
      <div class="action-btns">
        <a href="${{HOST}}/api/ai/device-info/${{VERIFY_ID}}" class="btn-view">📋 Xem thiết bị</a>
        <button onclick="handleCapture()" class="btn-retry">🔄 Chụp lại</button>
      </div>
    </div>`;
}}

function showScanResult(data) {{
  const result = document.getElementById('aiResult');
  if (!data.success) {{ result.innerHTML = '<div class="result-card result-mismatch"><div style="font-weight:700;color:#dc2626;">❌ '+(data.error||'Không nhận diện được')+'</div></div>'; return; }}
  const scan = data.scan_result || {{}};
  const m = data.matched_device;
  const conf = Math.round((data.match_confidence||scan.confidence||0)*100);
  let acts = '';
  if (m) {{
    acts = `<div class="action-btns">
      <a href="${{HOST}}/api/ai/device-info/${{m.id}}" class="btn-view">📋 Xem</a>
      <a href="${{HOST}}/api/ai/report-form?device_id=${{m.id}}&device_name=${{encodeURIComponent(m.name||'')}}" class="btn-report">🚨 Báo hỏng</a>
    </div>`;
  }}
  result.innerHTML = `
    <div class="result-card result-info">
      <div style="text-align:center;">
        <div style="font-size:2rem;">🤖</div>
        <div style="font-weight:800;color:#1d4ed8;font-size:0.95rem;">${{m ? m.name : (scan.device_name||'Không xác định')}}</div>
        <div style="font-size:0.75rem;color:#6b7280;">Confidence: <strong>${{conf}}%</strong></div>
      </div>
      <div class="result-grid">
        <div><span class="lbl">Hãng SX</span><span class="val">${{scan.brand||'—'}}</span></div>
        <div><span class="lbl">Model</span><span class="val">${{scan.model||'—'}}</span></div>
        <div><span class="lbl">Loại</span><span class="val">${{scan.device_type||'—'}}</span></div>
        <div><span class="lbl">Tình trạng</span><span class="val">${{scan.condition||'—'}}</span></div>
      </div>
      ${{m ? '<div style="margin-top:6px;padding:8px;background:#f0fdf4;border-radius:8px;font-size:0.72rem;color:#16a34a;">✅ Khớp DB: '+m.name+' ('+m.code+') — '+m.room+'</div>' : '<div style="margin-top:6px;padding:8px;background:#fffbeb;border-radius:8px;font-size:0.72rem;color:#d97706;">⚠️ Không tìm thấy thiết bị khớp trong hệ thống</div>'}}
      ${{acts}}
      <div class="action-btns" style="margin-top:4px;">
        <button onclick="handleCapture()" class="btn-retry">🔄 Chụp lại</button>
      </div>
    </div>`;
}}

switchTab(currentTab);
</script>
</body>
</html>'''
    resp = make_response(html)
    resp.headers['Content-Type'] = 'text/html; charset=utf-8'
    return resp

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=debug)
