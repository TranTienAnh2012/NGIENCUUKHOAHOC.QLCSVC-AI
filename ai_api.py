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
    # Lấy port từ environment variable hoặc dùng 5000 mặc định
    port = int(os.getenv('FLASK_PORT', 5000))
    debug = os.getenv('FLASK_ENV', 'development') == 'development'
    
    print(f"""
    ╔═══════════════════════════════════════════════════════════╗
    ║  Flask AI API - Hệ thống Quản lý Cơ sở Vật chất          ║
    ╠═══════════════════════════════════════════════════════════╣
    ║  Server đang chạy tại: http://localhost:{port}            ║
    ║  Gemini API: {'✓ Connected' if GEMINI_API_KEY else '✗ Not configured'}                                    ║
    ║                                                           ║
    ║  Endpoints:                                               ║
    ║  - GET  /api/ai/health                                    ║
    ║  - POST /api/ai/chatbot                                   ║
    ║  - POST /api/ai/analyze-damage                            ║
    ║  - POST /api/ai/suggest-maintenance                       ║
    ║  - POST /api/ai/categorize-equipment                      ║
    ╚═══════════════════════════════════════════════════════════╝
    """)
    
    app.run(host='0.0.0.0', port=port, debug=debug)
