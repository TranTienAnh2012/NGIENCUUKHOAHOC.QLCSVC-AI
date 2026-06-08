"""
=============================================================================
  Flask AI Gateway — Hệ thống Quản lý Cơ sở Vật chất (QLCSVC)
=============================================================================
  Server này chạy SONG SONG với Spring Boot (port 8080).
  - Port mặc định: 5000
  - Vai trò: Làm cầu nối giữa Frontend và Google Gemini AI.

  Luồng tổng quát:
    Browser/Spring Boot → Flask (port 5000) → Gemini AI
                                             ↕
                                  Spring Boot REST API (port 8080)
                                  (lấy data thiết bị, báo hỏng, mượn trả)

  Cách chạy:
    1. Tạo file .env chứa GEMINI_API_KEY=<your_key>
    2. pip install flask flask-cors google-generativeai python-dotenv
       requests Pillow qrcode[pil]
    3. python ai_api.py
=============================================================================
"""

# ============================================================
# 1. IMPORTS & KHỞI TẠO THƯ VIỆN
# ============================================================
from flask import Flask, request, jsonify, render_template, send_file, redirect
from flask_cors import CORS
import google.generativeai as genai
import os
from dotenv import load_dotenv
from datetime import datetime
import json
import io
import requests as req_lib
import urllib.parse
import unicodedata

# Pillow — xử lý ảnh (resize). Optional: fallback nếu không cài.
try:
    from PIL import Image
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False
    print("[WARN] Pillow chua duoc cai. Chay: pip install Pillow")

# qrcode — sinh QR code local. Optional: fallback dùng API online.
try:
    import qrcode
    import qrcode.constants
    QR_AVAILABLE = True
except ImportError:
    QR_AVAILABLE = False
    print("[WARN] qrcode chua duoc cai. Chay: pip install qrcode[pil]")

# ============================================================
# 2. CẤU HÌNH ỨNG DỤNG
# ============================================================
load_dotenv()

app = Flask(__name__)

# CORS: Cho phép Spring Boot (port 8080) gọi cross-origin sang Flask (port 5000)
CORS(app)

# --- Google Gemini AI (Cấu hình danh sách API Keys dự phòng) ---
API_KEYS = []
keys_env = os.getenv('GEMINI_API_KEYS')
if keys_env:
    API_KEYS = [k.strip() for k in keys_env.split(',') if k.strip()]
else:
    primary_key = os.getenv('GEMINI_API_KEY')
    backup_key = os.getenv('GEMINI_API_KEY_BACKUP')
    if primary_key:
        API_KEYS.append(primary_key.strip())
    if backup_key:
        API_KEYS.append(backup_key.strip())

if not API_KEYS:
    raise ValueError("Không tìm thấy GEMINI_API_KEY hoặc GEMINI_API_KEY_BACKUP trong file .env")

current_key_idx = 0

def generate_gemini_content(contents):
    """
    Gọi API Gemini với cơ chế tự động xoay vòng API Keys dự phòng khi bị Rate Limit (429).
    """
    global current_key_idx
    num_keys = len(API_KEYS)
    last_error = None

    for attempt in range(num_keys):
        active_key = API_KEYS[current_key_idx]
        try:
            genai.configure(api_key=active_key)
            m = genai.GenerativeModel('gemini-flash-latest')
            # Thực hiện cuộc gọi API
            return m.generate_content(contents)
        except Exception as e:
            err_str = str(e)
            print(f"[WARN] Loi khi goi Gemini bang Key index {current_key_idx}: {err_str}")
            if "429" in err_str or "ResourceExhausted" in err_str or "quota" in err_str.lower():
                next_idx = (current_key_idx + 1) % num_keys
                print(f"[INFO] Tu dong chuyen API Key tu index {current_key_idx} sang {next_idx}")
                current_key_idx = next_idx
                last_error = e
                continue
            else:
                raise e

    raise Exception(f"Tất cả các API Key Gemini đều gặp lỗi hoặc hết hạn mức. Lỗi cuối: {last_error}")


# --- Internal API Key ---
# Dùng để Flask gọi ngược vào Spring Boot endpoint /api/ai-data/**
# Cơ chế xác thực Machine-to-Machine (M2M)
INTERNAL_API_KEY = os.getenv('INTERNAL_API_KEY', 'qlcsvc-internal-key-dhnt-2026-flask-secret')
INTERNAL_HEADERS = {
    'X-Internal-API-Key': INTERNAL_API_KEY,
    'Content-Type': 'application/json'
}

# Base URL của Spring Boot REST API
SPRING_BASE = 'http://localhost:8080/api/ai-data'


# ============================================================
# 3. QUẢN LÝ PHIÊN HỘI THOẠI (SESSION)
# ============================================================
# Lưu lịch sử chat trên RAM (dict).
# Key = "userId_sessionId", Value = list các tin nhắn.
# Giới hạn: 20 tin nhắn gần nhất / session (sliding window).
# Lưu ý: Mất toàn bộ khi restart Flask. Production nên dùng Redis.
# ============================================================

user_sessions = {}


def get_session_key(user_id: str, session_id: str) -> str:
    """Tạo key duy nhất cho mỗi phiên chat."""
    return f"{user_id}_{session_id}"


def get_conversation_history(user_id: str, session_id: str) -> list:
    """Lấy lịch sử hội thoại của 1 phiên."""
    key = get_session_key(user_id, session_id)
    return user_sessions.get(key, [])


def save_conversation(user_id: str, session_id: str, user_msg: str, ai_response: str):
    """
    Lưu cặp tin nhắn (user + AI) vào session.
    Tự động cắt bớt nếu vượt quá 20 tin nhắn.
    """
    key = get_session_key(user_id, session_id)
    if key not in user_sessions:
        user_sessions[key] = []
    user_sessions[key].append({"role": "user", "content": user_msg})
    user_sessions[key].append({"role": "assistant", "content": ai_response})
    # Sliding window: giữ 20 tin nhắn gần nhất
    if len(user_sessions[key]) > 20:
        user_sessions[key] = user_sessions[key][-20:]


# ============================================================
# 4. SYSTEM PROMPTS — Prompt gốc cho từng loại tác vụ AI
# ============================================================
# Mỗi prompt định hướng cho Gemini trả lời đúng vai trò.
# Sửa prompt ở đây sẽ thay đổi hành vi AI trên toàn hệ thống.
# ============================================================

# Prompt cho Chatbot widget (góc phải màn hình)
CHATBOT_SYSTEM_PROMPT = """
Bạn là trợ lý AI chuyên về quản lý cơ sở vật chất trường học tại Việt Nam.
Nhiệm vụ của bạn là tư vấn, hỗ trợ về:
- Quản lý thiết bị, phòng học
- Bảo trì, sửa chữa thiết bị
- Quy trình báo hỏng, mượn trả thiết bị
Hãy trả lời ngắn gọn, chuyên nghiệp, bằng tiếng Việt.
"""

# Prompt phân tích hư hỏng — trả về nguyên nhân + mức ưu tiên
DAMAGE_ANALYSIS_PROMPT = """
Bạn là chuyên gia phân tích hư hỏng thiết bị. 
Nhiệm vụ: Phân tích mô tả hư hỏng và đưa ra:
1. Nguyên nhân có thể xảy ra
2. Các bước xử lý cụ thể
3. Mức độ ưu tiên xử lý
Trả lời bằng tiếng Việt, có cấu trúc rõ ràng.
"""

# Prompt gợi ý bảo trì — trả lời RẤT NGẮN
MAINTENANCE_SUGGESTION_PROMPT = """
Bạn là chuyên gia bảo trì thiết bị.
Yêu cầu: Trả lời RẤT NGẮN GỌN (tối đa 4-5 dòng).
Trả lời bằng tiếng Việt, chuyên nghiệp.
"""

# Prompt phân loại thiết bị → JSON
CATEGORIZATION_PROMPT = """
Phân loại thiết bị giáo dục. Trả về JSON: {"category": "...", "subcategory": "...", "confidence": 0.0-1.0}
"""

# Prompt quét ảnh thiết bị (Computer Vision) → JSON
IMAGE_SCAN_PROMPT = """
Phân tích ảnh thiết bị trường học. Trả về JSON:
{
  "device_name": "...",
  "brand": "...",
  "model": "...",
  "device_type": "...",
  "condition": "...",
  "damage_signs": "...",
  "confidence": 0.0-1.0
}
Trả lời bằng tiếng Việt.
"""

# Prompt xác minh thiết bị — so ảnh thật với thông tin DB
VERIFY_DEVICE_PROMPT = """
So sánh ảnh thật với thông tin DB: {device_name}, {manufacturer}, {model}, {category}, {code}.
Trả về JSON:
{{
  "match": true/false,
  "confidence": 0.0-1.0,
  "detected_device": "...",
  "detected_brand": "...",
  "detected_model": "...",
  "detected_type": "...",
  "reason": "...",
  "details": "..."
}}
"""


# ============================================================
# 5. HÀM TIỆN ÍCH (UTILS)
# ============================================================

def normalize_vn(text: str) -> str:
    """
    Chuẩn hóa chuỗi tiếng Việt: lowercase + bỏ dấu.
    Dùng để so khớp tên thiết bị (fuzzy match).
    Ví dụ: "Máy Chiếu" → "may chieu"
    """
    text = text.lower().strip()
    try:
        text = unicodedata.normalize('NFD', text)
        text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    except Exception:
        pass
    return text


def fmt_date(d) -> str:
    """
    Format ngày từ ISO string → 'dd/MM/yyyy'.
    Trả về '—' nếu None, trả raw string nếu parse lỗi.
    """
    if not d:
        return '—'
    try:
        return datetime.fromisoformat(str(d)[:19]).strftime('%d/%m/%Y')
    except Exception:
        return str(d)[:10]


def match_device_from_db(detected_name: str, detected_brand: str,
                         detected_type: str, db_devices: list) -> tuple:
    """
    Thuật toán Fuzzy Match — So khớp tên thiết bị AI nhận diện với DB.

    Cách tính điểm:
      1. Tách tên thành các từ (tokens)
      2. Đếm số từ trùng khớp với tên/mã thiết bị trong DB
      3. Cộng thêm 0.2 nếu tên hãng (brand) trùng
      4. Ngưỡng tối thiểu: score > 0.2 mới tính là match

    Returns:
        (matched_device, score) hoặc (None, 0.0) nếu không khớp
    """
    if not db_devices or not detected_name:
        return None, 0.0

    name_norm = normalize_vn(detected_name)
    brand_norm = normalize_vn(detected_brand) if detected_brand else ''
    search_terms = set(name_norm.split())

    best_match = None
    best_score = 0.0

    for device in db_devices:
        db_name = normalize_vn(str(device.get('name', '')))
        db_code = normalize_vn(str(device.get('code', '')))

        # Đếm số từ khớp
        hits = sum(1 for t in search_terms if t in db_name or t in db_code)
        score = hits / len(search_terms) if search_terms else 0

        # Bonus nếu trùng brand
        if brand_norm and brand_norm in db_name:
            score += 0.2

        if score > best_score:
            best_score = score
            best_match = device

    if best_score > 0.2:
        return best_match, min(best_score, 0.95)
    return None, 0.0


def fetch_spring_data(endpoint: str, timeout: int = 3):
    """
    Gọi Spring Boot REST API nội bộ.
    Trả về JSON data hoặc None nếu lỗi.

    Args:
        endpoint: Đường dẫn sau /api/ai-data/ (vd: 'devices', 'damages/recent?limit=50')
        timeout: Giới hạn thời gian chờ (giây)
    """
    try:
        r = req_lib.get(f'{SPRING_BASE}/{endpoint}', headers=INTERNAL_HEADERS, timeout=timeout)
        if r.status_code == 200:
            return r.json()
    except Exception as e:
        print(f"[ERROR] Khong ket noi duoc Spring Boot ({endpoint}): {e}")
    return None


# ============================================================
# 6. API ENDPOINTS — Các endpoint AI chính
# ============================================================

# --- 6.1 Health Check ---
@app.route('/api/ai/health', methods=['GET'])
def health_check():
    """Kiểm tra Flask server còn sống không. Dùng cho monitoring."""
    return jsonify({"status": "healthy", "timestamp": datetime.now().isoformat()}), 200


# --- 6.2 Chatbot Widget (có phân quyền RBAC) ---
@app.route('/api/ai/chatbot', methods=['POST'])
def chatbot():
    """
    Endpoint chính cho Chatbot widget (góc phải màn hình).

    Luồng:
      1. Nhận message + user_id + session_id + context (role)
      2. Kiểm tra role:
         - ADMIN/NV_CSVC → Được xem data tổng quan (gọi Spring Boot lấy thống kê)
         - GIAO_VIEN → Bị cấm xem data nhạy cảm
      3. Ghép System Prompt + Role Instructions + DB Context + User Message
      4. Gửi Gemini → Nhận response → Lưu history → Trả về client

    Request body:
      {
        "message": "Hỏi gì đó...",
        "user_id": "123",
        "session_id": "abc",
        "context": {"user_role": "ADMIN"}
      }
    """
    try:
        data = request.get_json()
        user_message = data.get('message', '')
        user_id = data.get('user_id')
        session_id = data.get('session_id')

        if not user_message or not user_id or not session_id:
            return jsonify({"error": "Thiếu trường bắt buộc (message, user_id, session_id)"}), 400

        context = data.get('context', {})
        user_role = context.get('user_role', 'GIAO_VIEN')

        # --- Phân quyền RBAC ---
        context_data = ""
        role_instructions = ""

        if user_role in ['ADMIN', 'NHAN_VIEN_CSVC', 'ROLE_ADMIN', 'ROLE_NHAN_VIEN_CSVC']:
            # Admin/NV CSVC: ĐƯỢC xem số liệu tổng quan
            role_instructions = (
                "Bạn đang nói chuyện với QUẢN TRỊ VIÊN hoặc NHÂN VIÊN CSVC. "
                "Bạn ĐƯỢC PHÉP cung cấp số liệu tổng quan hệ thống và thông tin chi tiết.\n"
            )
            # Lấy thống kê thiết bị từ Spring Boot
            devices = fetch_spring_data('devices')
            if devices:
                tot = sum(1 for d in devices if d.get('status') == 'TOT')
                hong = sum(1 for d in devices if d.get('status') == 'HONG')
                bao_tri = sum(1 for d in devices if d.get('status') == 'BAO_TRI')
                thanh_ly = sum(1 for d in devices if d.get('status') == 'THANH_LY')
                context_data = (
                    f"[Hệ thống nội bộ]: Hiện tại có {len(devices)} thiết bị. "
                    f"Trạng thái: {tot} hoạt động tốt, {bao_tri} cần bảo trì, "
                    f"{hong} đang báo hỏng, {thanh_ly} đã thanh lý.\n\n"
                )
        else:
            # Giáo viên: CẤM xem data nhạy cảm
            role_instructions = (
                "Bạn đang nói chuyện với GIÁO VIÊN. "
                "NGHIÊM CẤM cung cấp số liệu tổng quan của toàn trường, "
                "thông tin cá nhân của người dùng khác, hoặc thông tin bảo mật. "
                "Nếu họ hỏi về số lượng thiết bị tổng, lịch sử của người khác, "
                "hãy từ chối lịch sự và nói rằng họ không có quyền hạn xem thông tin này.\n"
            )

        # Ghép prompt hoàn chỉnh
        full_prompt = f"{CHATBOT_SYSTEM_PROMPT}\n{role_instructions}\n{context_data}User: {user_message}"
        response = generate_gemini_content(full_prompt)
        ai_response = response.text

        # Lưu lịch sử hội thoại
        save_conversation(user_id, session_id, user_message, ai_response)

        return jsonify({"success": True, "response": ai_response}), 200

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# --- 6.3 Chat Đơn Giản (không session, không context) ---
@app.route('/api/ai/chat', methods=['POST'])
def simple_chat():
    """
    Chat 1 lượt đơn giản — không lưu history, không phân quyền.
    Dùng bởi nút "AI Đánh Giá" trên trang Thống kê.

    Request body: {"message": "..."}
    """
    try:
        data = request.get_json()
        message = data.get('message', '')
        if not message:
            return jsonify({"error": "Thiếu message"}), 400

        response = generate_gemini_content(message)
        try:
            text = response.text
        except ValueError:
            text = "Xin lỗi, câu trả lời bị chặn bởi bộ lọc an toàn hoặc gặp lỗi từ Google Gemini."

        return jsonify({"success": True, "response": text}), 200

    except Exception as e:
        error_msg = str(e)
        # Xử lý đặc biệt lỗi Rate Limit (429) từ Gemini
        if "429" in error_msg or "ResourceExhausted" in error_msg or "quota" in error_msg.lower():
            error_msg = "Gemini API đã hết hạn mức truy cập (Rate Limit). Vui lòng thử lại sau."
        return jsonify({"success": False, "error": error_msg}), 500


# --- 6.4 Phân Tích Hư Hỏng ---
@app.route('/api/ai/analyze-damage', methods=['POST'])
def analyze_damage():
    """
    AI phân tích mô tả hư hỏng → trả về nguyên nhân + bước xử lý + mức ưu tiên.

    Request body: {"description": "Máy chiếu không lên hình..."}
    """
    try:
        data = request.get_json()
        desc = data.get('description') or data.get('damage_description')
        if not desc:
            return jsonify({"error": "Thiếu mô tả hư hỏng"}), 400

        resp = generate_gemini_content(f"{DAMAGE_ANALYSIS_PROMPT}\nDescription: {desc}")
        return jsonify({"success": True, "analysis": resp.text}), 200

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# --- 6.5 Gợi Ý Bảo Trì ---
@app.route('/api/ai/suggest-maintenance', methods=['POST'])
def suggest_maintenance():
    """
    AI gợi ý bảo trì cho 1 thiết bị cụ thể (ngắn gọn 4-5 dòng).
    Được gọi tự động khi mở trang chi tiết thiết bị (device_info).

    Request body: {"equipment_name": "Máy chiếu Epson", "current_status": "TOT"}
    """
    try:
        data = request.get_json()
        name = data.get('equipment_name') or data.get('device_name') or 'Thiết bị'
        status = data.get('current_status', 'TOT')

        resp = generate_gemini_content(f"{MAINTENANCE_SUGGESTION_PROMPT}\nDevice: {name}\nStatus: {status}")
        return jsonify({"success": True, "recommendations": resp.text}), 200

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# --- 6.6 Quét Ảnh Nhận Diện Thiết Bị (Computer Vision) ---
@app.route('/api/ai/scan-image', methods=['POST'])
def scan_image():
    """
    Chụp ảnh thiết bị → Gemini Vision nhận diện → Fuzzy match với DB.

    Luồng:
      1. Client gửi ảnh base64
      2. Gửi ảnh + IMAGE_SCAN_PROMPT → Gemini Vision
      3. Gemini trả JSON (device_name, brand, model, condition...)
      4. Lấy danh sách thiết bị từ Spring Boot DB
      5. Chạy fuzzy match: tên AI nhận diện ↔ tên trong DB
      6. Trả về: scan_result (AI) + matched_device (DB) + match_score

    Request body: {"image_base64": "data:image/jpeg;base64,..."}
    """
    try:
        data = request.get_json()
        img_base64 = data.get('image_base64')
        if not img_base64:
            return jsonify({"error": "Thiếu ảnh (image_base64)"}), 400

        # Bỏ prefix "data:image/jpeg;base64," nếu có
        if ',' in img_base64:
            img_base64 = img_base64.split(',')[1]

        # Gửi ảnh cho Gemini Vision
        image_part = {'inline_data': {'mime_type': 'image/jpeg', 'data': img_base64}}
        resp = generate_gemini_content([IMAGE_SCAN_PROMPT, image_part])

        # Parse JSON từ response (Gemini đôi khi wrap trong ```)
        scan_result = {}
        try:
            txt = resp.text
            if '```' in txt:
                start = txt.find('{')
                end = txt.rfind('}') + 1
                txt = txt[start:end]
            scan_result = json.loads(txt)
        except Exception:
            scan_result = {"device_name": "Unknown", "confidence": 0.1}

        # Fuzzy match với DB
        db_devices = fetch_spring_data('devices') or []
        matched, score = match_device_from_db(
            scan_result.get('device_name'),
            scan_result.get('brand'),
            '',
            db_devices
        )

        return jsonify({
            "success": True,
            "scan_result": scan_result,
            "matched_device": matched,
            "match_score": score
        }), 200

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# --- 6.7 Xác Minh Thiết Bị (So ảnh thật ↔ thông tin DB) ---
@app.route('/api/ai/verify-device', methods=['POST'])
def verify_device():
    """
    Dùng khi kiểm kê: Chụp ảnh thiết bị → AI so sánh với thông tin đã đăng ký trong DB.

    Luồng:
      1. Client gửi ảnh base64 + device_id
      2. Flask lấy thông tin thiết bị từ DB theo device_id
      3. Gửi ảnh + thông tin DB → Gemini so sánh
      4. Trả về: match (true/false) + confidence + reason

    Request body: {"image_base64": "...", "device_id": 5}
    """
    try:
        data = request.get_json()
        img_base64 = data.get('image_base64')
        dev_id = data.get('device_id')
        if not img_base64 or not dev_id:
            return jsonify({"error": "Thiếu ảnh hoặc device_id"}), 400

        # Lấy thông tin thiết bị từ DB
        devices = fetch_spring_data('devices')
        device = None
        if devices:
            device = next((d for d in devices if str(d['id']) == str(dev_id)), None)

        if not device:
            return jsonify({"error": "Không tìm thấy thiết bị trong DB"}), 404

        # Bỏ prefix base64
        if ',' in img_base64:
            img_base64 = img_base64.split(',')[1]

        # Ghép prompt với thông tin DB
        prompt = VERIFY_DEVICE_PROMPT.format(
            device_name=device.get('name', ''),
            manufacturer=device.get('manufacturer', ''),
            model=device.get('model', ''),
            category=device.get('category', ''),
            code=device.get('code', '')
        )
        image_part = {'inline_data': {'mime_type': 'image/jpeg', 'data': img_base64}}
        resp = generate_gemini_content([prompt, image_part])

        # Parse JSON response
        res_txt = resp.text
        try:
            if '```' in res_txt:
                start = res_txt.find('{')
                end = res_txt.rfind('}') + 1
                res_txt = res_txt[start:end]
            result_json = json.loads(res_txt)
            return jsonify({"success": True, **result_json})
        except Exception:
            return jsonify({"success": True, "reason": res_txt, "match": False})

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# --- 6.8 Sinh QR Code cho Thiết Bị ---
@app.route('/api/ai/device-qr/<int:device_id>')
def get_device_qr(device_id):
    """
    Trả về ảnh QR Code (PNG) chứa URL đến trang chi tiết thiết bị.
    QR khi quét sẽ mở: /api/ai/device-info/{device_id}

    Fallback: Nếu không cài thư viện qrcode → redirect sang API online.
    """
    host = request.host_url.rstrip('/')
    info_url = f"{host}/api/ai/device-info/{device_id}"

    if not QR_AVAILABLE or not PIL_AVAILABLE:
        # Fallback: dùng API online
        return redirect(
            f"https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={urllib.parse.quote(info_url)}"
        )

    # Sinh QR local
    qr = qrcode.QRCode(version=1, box_size=10, border=2)
    qr.add_data(info_url)
    qr.make(fit=True)
    img = qr.make_image(fill_color="#1a1a2e", back_color="white")

    buf = io.BytesIO()
    img.save(buf, 'PNG')
    buf.seek(0)
    return send_file(buf, mimetype='image/png')


# ============================================================
# 7. PAGE ROUTES — Các trang HTML được Flask render
# ============================================================
# Các trang này phục vụ cho luồng "Quét QR" — người dùng quét QR
# trên thiết bị → mở trình duyệt → xem thông tin + báo hỏng.
# Template HTML nằm trong thư mục templates/
# ============================================================

# Mapping trạng thái thiết bị → (label, color, icon)
STATUS_MAP = {
    'TOT':      ('Đang hoạt động tốt', '#22c55e', '✅'),
    'BAO_TRI':  ('Đang bảo trì',       '#f59e0b', '🔧'),
    'HONG':     ('Hỏng',               '#ef4444', '❌'),
    'THANH_LY': ('Đã thanh lý',        '#6b7280', '🗑️'),
}

# Mapping trạng thái báo hỏng → label hiển thị
DAMAGE_STATUS_MAP = {
    'CHO_XU_LY':  'Chờ xử lý',
    'DANG_XU_LY': 'Đang xử lý',
    'HOAN_THANH':  'Hoàn thành',
    'DA_BAO_TRI':  'Đã bảo trì',
}


@app.route('/api/ai/device-info/<int:device_id>')
def device_info_page(device_id):
    """
    Trang chi tiết thiết bị — mở khi quét QR.

    Hiển thị: Tên, mã, phòng, trạng thái, lịch sử hỏng, lịch sử mượn,
              AI đánh giá sức khỏe, nút báo hỏng, nút AI xác minh.

    Luồng lấy dữ liệu:
      1. GET /api/ai-data/devices → tìm thiết bị theo ID
      2. GET /api/ai-data/damages/recent → lọc theo tên thiết bị
      3. GET /api/ai-data/borrows/active → tìm ai đang mượn
      4. GET /api/ai-data/borrows/all → lịch sử mượn trả
    """
    device = None
    damages = []
    borrower = None
    borrow_history = []
    db_ok = True

    try:
        # Lấy thông tin thiết bị
        devices = fetch_spring_data('devices', timeout=4)
        if devices:
            device = next((d for d in devices if str(d['id']) == str(device_id)), None)

        if device:
            name = (device.get('name') or '').lower()

            # Lịch sử báo hỏng gần đây (lọc theo tên thiết bị)
            all_damages = fetch_spring_data('damages/recent?limit=50', timeout=4)
            if all_damages:
                damages = [d for d in all_damages if name in (d.get('device_name') or '').lower()][:5]

            # Ai đang mượn thiết bị này?
            active_borrows = fetch_spring_data('borrows/active', timeout=4)
            if active_borrows:
                borrower = next(
                    (b for b in active_borrows if name in (b.get('device_name') or '').lower()),
                    None
                )

            # Lịch sử mượn trả
            all_borrows = fetch_spring_data('borrows/all?limit=100', timeout=4)
            if all_borrows:
                borrow_history = [b for b in all_borrows if name in (b.get('device_name') or '').lower()][:5]

    except Exception as e:
        print(f"[ERROR] Lay thong tin thiet bi {device_id}: {e}")
        db_ok = False

    if not device:
        return "Không tìm thấy thiết bị", 404

    # Xử lý trạng thái
    status_raw = device.get('status', 'TOT')
    status_label, status_color, status_icon = STATUS_MAP.get(status_raw, (status_raw, '#6b7280', '❓'))

    # Xử lý dữ liệu báo hỏng cho template
    for d in damages:
        sev = (d.get('severity') or '').upper()
        d['severity_color'] = '#ef4444' if ('CAO' in sev or 'NGHIEM' in sev) else '#f59e0b' if 'TRUNG' in sev else '#3b82f6'
        d['status_label'] = DAMAGE_STATUS_MAP.get((d.get('status') or '').upper(), d.get('status') or '—')
        d['date_fmt'] = fmt_date(d.get('reported_date'))

    # Xử lý dữ liệu mượn trả cho template
    for b in borrow_history:
        b['is_active'] = b.get('status') != 'DA_TRA'
        b['date_fmt'] = fmt_date(b.get('borrow_date'))

    # Xử lý borrower
    borrower_data = None
    if borrower:
        borrower_data = {
            'user_name': borrower.get('user_name', '—'),
            'expected_return': fmt_date(borrower.get('expected_return'))
        }

    return render_template('device_info.html',
        device=device, device_id=device_id,
        dev_name=device.get('name', '—'),
        dev_code=device.get('code', '—'),
        dev_room=device.get('room', '—'),
        dev_cat=device.get('category', '—'),
        dev_mfr=device.get('manufacturer', '—'),
        dev_model=device.get('model', '—'),
        dev_year=device.get('year', '—'),
        status_label=status_label,
        status_color=status_color,
        status_icon=status_icon,
        status_raw=status_raw,
        db_ok=db_ok,
        host=request.host_url.rstrip('/'),
        damages=damages,
        borrow_history=borrow_history,
        borrower=borrower_data,
        damages_json=json.dumps(damages),
        current_time=datetime.now().strftime('%H:%M - %d/%m/%Y')
    )


@app.route('/api/ai/qr-print')
def qr_print_page():
    """
    Trang in nhãn QR hàng loạt cho tất cả thiết bị.
    Mỗi nhãn gồm: QR code + tên + mã + phòng.
    Ấn Ctrl+P → in ra giấy → dán lên thiết bị.
    """
    devices = []
    db_ok = True

    all_devices = fetch_spring_data('devices', timeout=4)
    if all_devices is not None:
        devices = all_devices
    else:
        db_ok = False

    # Đếm thống kê
    stats = {"tot": 0, "bao_tri": 0, "hong": 0}
    for d in devices:
        s = d.get('status', 'TOT')
        if s == 'TOT':
            stats['tot'] += 1
        elif s == 'BAO_TRI':
            stats['bao_tri'] += 1
        elif s == 'HONG':
            stats['hong'] += 1

    host = request.host_url.rstrip('/')
    return render_template('qr_print.html',
        devices=devices, stats=stats,
        db_ok=db_ok, host=host
    )


@app.route('/api/ai/report-form')
def report_form_page():
    """
    Form báo hỏng nhanh — mở khi ấn "Báo hỏng" trên trang chi tiết thiết bị.
    Người dùng điền mô tả hư hỏng → submit → tạo phiếu báo hỏng trong DB.
    """
    device_id = request.args.get('device_id')
    device_name = request.args.get('device_name', 'Thiết bị')
    host = request.host_url.rstrip('/')

    return render_template('report_form.html',
        device_id=device_id,
        device_name=device_name,
        submit_url=f"{host}/api/ai/submit-report",
        back_url=f"{host}/api/ai/device-info/{device_id}" if device_id else f"{host}/api/ai/qr-print"
    )


@app.route('/api/ai/submit-report', methods=['POST'])
def submit_report():
    """
    Nhận form data từ report_form → forward sang Spring Boot để tạo phiếu báo hỏng.

    Luồng:
      Browser (form submit) → Flask → POST /api/ai-data/report-damage (Spring Boot) → DB
    """
    try:
        data = request.form
        r = req_lib.post(
            f'{SPRING_BASE}/report-damage',
            params={
                'thiet_bi_id': data.get('device_id'),
                'mo_ta': data.get('mo_ta'),
                'muc_do': data.get('muc_do', 'TRUNG_BINH'),
                'ten_nguoi_bao': data.get('ten_nguoi_bao', 'User'),
                'so_dien_thoai': data.get('so_dien_thoai', '')
            },
            headers=INTERNAL_HEADERS,
            timeout=5
        )
        return jsonify(r.json())
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/api/ai/device-lookup')
def device_lookup_page():
    """Trang tra cứu thiết bị — tìm kiếm theo tên/mã."""
    devices = fetch_spring_data('devices', timeout=5) or []
    return render_template('device_lookup.html',
        devices=devices,
        host=request.host_url.rstrip('/')
    )


@app.route('/api/ai/scan')
def scan_page():
    """
    Trang quét ảnh camera — nhận diện thiết bị bằng Gemini Vision.
    Nếu có param ?verify=<id> → chế độ xác minh (so sánh với DB).
    """
    verify_id = request.args.get('verify', '0')
    default_tab = 'ai' if verify_id and verify_id != '0' else 'qr'
    return render_template('scan.html',
        verify_id=verify_id,
        default_tab=default_tab,
        host=request.host_url.rstrip('/')
    )



# ============================================================
# 8. KHỞI CHẠY SERVER
# ============================================================
if __name__ == '__main__':
    port = int(os.getenv('FLASK_PORT', 5000))
    debug = os.getenv('FLASK_ENV', 'development') == 'development'
    print(f"[INFO] Flask AI Gateway dang chay tai http://0.0.0.0:{port}")
    print(f"[INFO] Spring Boot API: {SPRING_BASE}")
    print(f"[INFO] Debug mode: {debug}")
    app.run(host='0.0.0.0', port=port, debug=debug)
