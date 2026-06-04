"""
Flask API với Google Gemini AI cho Hệ thống Quản lý Cơ sở Vật chất
Tác giả: AI Assistant
Ngày tạo: 2026-01-26
"""

from flask import Flask, request, jsonify, render_template, make_response, send_file, redirect
from flask_cors import CORS
import google.generativeai as genai
import os
from dotenv import load_dotenv
from datetime import datetime
import json
import base64
import io
import requests as req_lib
import urllib.parse
import unicodedata

# Thư viện xử lý ảnh (cần cài: pip install Pillow)
try:
    from PIL import Image
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False
    print("Warning: Pillow not installed. Image resizing disabled. Run: pip install Pillow")

# QR Code library
try:
    import qrcode
    import qrcode.constants
    QR_AVAILABLE = True
except ImportError:
    QR_AVAILABLE = False
    print("Warning: qrcode chưa được cài. Chạy: pip install qrcode[pil]")

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
INTERNAL_API_KEY = os.getenv('INTERNAL_API_KEY', 'qlcsvc-internal-key-dhnt-2026-flask-secret')
INTERNAL_HEADERS = {
    'X-Internal-API-Key': INTERNAL_API_KEY,
    'Content-Type': 'application/json'
}

# Khởi tạo Gemini model
model = genai.GenerativeModel('gemini-flash-latest')

# ============================================================
# SESSION MANAGEMENT
# ============================================================
user_sessions = {}

def get_session_key(user_id, session_id):
    return f"{user_id}_{session_id}"

def get_conversation_history(user_id, session_id):
    key = get_session_key(user_id, session_id)
    return user_sessions.get(key, [])

def save_conversation(user_id, session_id, user_msg, ai_response):
    key = get_session_key(user_id, session_id)
    if key not in user_sessions:
        user_sessions[key] = []
    user_sessions[key].append({"role": "user", "content": user_msg})
    user_sessions[key].append({"role": "assistant", "content": ai_response})
    if len(user_sessions[key]) > 20:
        user_sessions[key] = user_sessions[key][-20:]

# ============================================================
# PROMPTS
# ============================================================
CHATBOT_SYSTEM_PROMPT = """
Bạn là trợ lý AI chuyên về quản lý cơ sở vật chất trường học tại Việt Nam.
Nhiệm vụ của bạn là tư vấn, hỗ trợ về:
- Quản lý thiết bị, phòng học
- Bảo trì, sửa chữa thiết bị
- Quy trình báo hỏng, mượn trả thiết bị
Hãy trả lời ngắn gọn, chuyên nghiệp, bằng  tiếng Việt.
"""

DAMAGE_ANALYSIS_PROMPT = """
Bạn là chuyên gia phân tích hư hỏng thiết bị. 
Nhiệm vụ: Phân tích mô tả hư hỏng và đưa ra:
1. Nguyên nhân có thể xảy ra
2. Các bước xử lý cụ thể
3. Mức độ ưu tiên xử lý
Trả lời bằng tiếng Việt, có cấu trúc rõ ràng.
"""

MAINTENANCE_SUGGESTION_PROMPT = """
Bạn là chuyên gia bảo trì thiết bị.
Yêu cầu: Trả lời RẤT NGẮN GỌN (tối đa 4-5 dòng).
Trả lời bằng tiếng Việt, chuyên nghiệp.
"""

CATEGORIZATION_PROMPT = """
Phân loại thiết bị giáo dục. Trả về JSON: {"category": "...", "subcategory": "...", "confidence": 0.0-1.0}
"""

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
# UTILS
# ============================================================
def normalize_vn(text):
    text = text.lower().strip()
    try:
        text = unicodedata.normalize('NFD', text)
        text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    except: pass
    return text

def fmt_date(d):
    if not d: return '—'
    try:
        return datetime.fromisoformat(str(d)[:19]).strftime('%d/%m/%Y')
    except: return str(d)[:10]

def match_device_from_db(detected_name, detected_brand, detected_type, db_devices):
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
        hits = sum(1 for t in search_terms if t in db_name or t in db_code)
        score = hits / len(search_terms) if search_terms else 0
        if brand_norm and brand_norm in db_name: score += 0.2
        if score > best_score:
            best_score = score
            best_match = device
    return (best_match, min(best_score, 0.95)) if best_score > 0.2 else (None, 0.0)

# ============================================================
# API ROUTES
# ============================================================

@app.route('/api/ai/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy", "timestamp": datetime.now().isoformat()}), 200

@app.route('/api/ai/chatbot', methods=['POST'])
def chatbot():
    try:
        data = request.get_json()
        user_message = data.get('message', '')
        user_id = data.get('user_id')
        session_id = data.get('session_id')
        if not user_message or not user_id or not session_id:
            return jsonify({"error": "Missing fields"}), 400
        
        context = data.get('context', {})
        user_role = context.get('user_role', 'GIAO_VIEN')
        
        # Lấy context từ DB dựa trên Role
        context_data = ""
        role_instructions = ""
        
        if user_role in ['ADMIN', 'NHAN_VIEN_CSVC', 'ROLE_ADMIN', 'ROLE_NHAN_VIEN_CSVC']:
            role_instructions = "Bạn đang nói chuyện với QUẢN TRỊ VIÊN hoặc NHÂN VIÊN CSVC. Bạn ĐƯỢC PHÉP cung cấp số liệu tổng quan hệ thống và thông tin chi tiết.\n"
            try:
                r = req_lib.get('http://localhost:8080/api/ai-data/devices', headers=INTERNAL_HEADERS, timeout=3)
                if r.status_code == 200:
                    devices = r.json()
                    tot = sum(1 for d in devices if d.get('status') == 'TOT')
                    hong = sum(1 for d in devices if d.get('status') == 'HONG')
                    bao_tri = sum(1 for d in devices if d.get('status') == 'BAO_TRI')
                    thanh_ly = sum(1 for d in devices if d.get('status') == 'THANH_LY')
                    context_data = f"[Hệ thống nội bộ]: Hiện tại có {len(devices)} thiết bị. Trạng thái: {tot} hoạt động tốt, {bao_tri} cần bảo trì, {hong} đang báo hỏng, {thanh_ly} đã thanh lý.\n\n"
            except Exception as e:
                pass
        else:
            role_instructions = "Bạn đang nói chuyện với GIÁO VIÊN. NGHIÊM CẤM cung cấp số liệu tổng quan của toàn trường, thông tin cá nhân của người dùng khác, hoặc thông tin bảo mật. Nếu họ hỏi về số lượng thiết bị tổng, lịch sử của người khác, hãy từ chối lịch sự và nói rằng họ không có quyền hạn xem thông tin này.\n"

        history = get_conversation_history(user_id, session_id)
        full_prompt = f"{CHATBOT_SYSTEM_PROMPT}\n{role_instructions}\n{context_data}User: {user_message}"
        response = model.generate_content(full_prompt)
        ai_response = response.text
        save_conversation(user_id, session_id, user_message, ai_response)
        
        return jsonify({"success": True, "response": ai_response}), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/chat', methods=['POST'])
def simple_chat():
    try:
        data = request.get_json()
        message = data.get('message', '')
        if not message: return jsonify({"error": "Missing message"}), 400
        
        response = model.generate_content(message)
        return jsonify({"success": True, "response": response.text}), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/analyze-damage', methods=['POST'])
def analyze_damage():
    try:
        data = request.get_json()
        desc = data.get('description') or data.get('damage_description')
        if not desc: return jsonify({"error": "Missing description"}), 400
        resp = model.generate_content(f"{DAMAGE_ANALYSIS_PROMPT}\nDescription: {desc}")
        return jsonify({"success": True, "analysis": resp.text}), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/suggest-maintenance', methods=['POST'])
def suggest_maintenance():
    try:
        data = request.get_json()
        name = data.get('equipment_name') or data.get('device_name') or 'Thiết bị'
        status = data.get('current_status', 'TOT')
        resp = model.generate_content(f"{MAINTENANCE_SUGGESTION_PROMPT}\nDevice: {name}\nStatus: {status}")
        return jsonify({"success": True, "recommendations": resp.text}), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/scan-image', methods=['POST'])
def scan_image():
    try:
        data = request.get_json()
        img_base64 = data.get('image_base64')
        if not img_base64: return jsonify({"error": "Missing image"}), 400
        if ',' in img_base64: img_base64 = img_base64.split(',')[1]
        
        image_part = {'inline_data': {'mime_type': 'image/jpeg', 'data': img_base64}}
        resp = model.generate_content([IMAGE_SCAN_PROMPT, image_part])
        scan_result = {}
        try:
            txt = resp.text
            if '```' in txt:
                start = txt.find('{')
                end = txt.rfind('}') + 1
                txt = txt[start:end]
            scan_result = json.loads(txt)
        except: scan_result = {"device_name": "Unknown", "confidence": 0.1}
        
        # Match with DB
        db_devices = []
        try:
            r = req_lib.get('http://localhost:8080/api/ai-data/devices', headers=INTERNAL_HEADERS, timeout=3)
            if r.status_code == 200: db_devices = r.json()
        except: pass
        
        matched, score = match_device_from_db(scan_result.get('device_name'), scan_result.get('brand'), '', db_devices)
        
        return jsonify({
            "success": True,
            "scan_result": scan_result,
            "matched_device": matched,
            "match_score": score
        }), 200
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/verify-device', methods=['POST'])
def verify_device():
    try:
        data = request.get_json()
        img_base64 = data.get('image_base64')
        dev_id = data.get('device_id')
        if not img_base64 or not dev_id: return jsonify({"error": "Missing data"}), 400
        
        # Get device from DB
        device = None
        try:
            r = req_lib.get('http://localhost:8080/api/ai-data/devices', headers=INTERNAL_HEADERS, timeout=3)
            if r.status_code == 200:
                device = next((d for d in r.json() if str(d['id']) == str(dev_id)), None)
        except: pass
        
        if not device: return jsonify({"error": "Device not found"}), 404
        
        if ',' in img_base64: img_base64 = img_base64.split(',')[1]
        prompt = VERIFY_DEVICE_PROMPT.format(
            device_name=device.get('name',''), manufacturer=device.get('manufacturer',''),
            model=device.get('model',''), category=device.get('category',''), code=device.get('code','')
        )
        image_part = {'inline_data': {'mime_type': 'image/jpeg', 'data': img_base64}}
        resp = model.generate_content([prompt, image_part])
        
        # Parse result
        res_txt = resp.text
        try:
            if '```' in res_txt:
                start = res_txt.find('{')
                end = res_txt.rfind('}') + 1
                res_txt = res_txt[start:end]
            result_json = json.loads(res_txt)
            return jsonify({"success": True, **result_json})
        except:
            return jsonify({"success": True, "reason": res_txt, "match": False})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route('/api/ai/device-qr/<int:device_id>')
def get_device_qr(device_id):
    host = request.host_url.rstrip('/')
    info_url = f"{host}/api/ai/device-info/{device_id}"
    if not QR_AVAILABLE:
        return redirect(f"https://api.qrserver.com/v1/create-qr-code/?size=200x200&data={urllib.parse.quote(info_url)}")
    qr = qrcode.QRCode(version=1, box_size=10, border=2)
    qr.add_data(info_url); qr.make(fit=True)
    img = qr.make_image(fill_color="#1a1a2e", back_color="white")
    buf = io.BytesIO(); img.save(buf, format='PNG'); buf.seek(0)
    return send_file(buf, mimetype='image/png')

# ============================================================
# PAGE ROUTES (HTML)
# ============================================================

@app.route('/api/ai/device-info/<int:device_id>')
def device_info_page(device_id):
    SPRING = 'http://localhost:8080/api/ai-data'
    device, damages, borrower, borrow_history = None, [], None, []
    db_ok = True
    try:
        r = req_lib.get(f'{SPRING}/devices', headers=INTERNAL_HEADERS, timeout=4)
        if r.status_code == 200:
            device = next((d for d in r.json() if str(d['id']) == str(device_id)), None)
        if device:
            name = (device.get('name') or '').lower()
            dr = req_lib.get(f'{SPRING}/damages/recent?limit=50', headers=INTERNAL_HEADERS, timeout=4)
            if dr.status_code == 200:
                damages = [d for d in dr.json() if name in (d.get('device_name') or '').lower()][:5]
            br = req_lib.get(f'{SPRING}/borrows/active', headers=INTERNAL_HEADERS, timeout=4)
            if br.status_code == 200:
                borrower = next((b for b in br.json() if name in (b.get('device_name') or '').lower()), None)
            hr = req_lib.get(f'{SPRING}/borrows/all?limit=100', headers=INTERNAL_HEADERS, timeout=4)
            if hr.status_code == 200:
                borrow_history = [b for b in hr.json() if name in (b.get('device_name') or '').lower()][:5]
    except Exception as e: 
        print(f"Error fetching device info: {e}")
        db_ok = False
    
    if not device: return "Device not found", 404

    # Mapping variables for template
    STATUS_MAP = {
        'TOT':      ('Đang hoạt động tốt', '#22c55e', '✅'),
        'BAO_TRI':  ('Đang bảo trì',       '#f59e0b', '🔧'),
        'HONG':     ('Hỏng',               '#ef4444', '❌'),
        'THANH_LY': ('Đã thanh lý',        '#6b7280', '🗑️'),
    }
    status_raw = device.get('status', 'TOT')
    status_label, status_color, status_icon = STATUS_MAP.get(status_raw, (status_raw, '#6b7280', '❓'))

    damage_html = ""
    for d in damages:
        sev = (d.get('severity') or '').upper()
        bc = '#ef4444' if ('CAO' in sev or 'NGHIEM' in sev) else '#f59e0b' if ('TRUNG' in sev) else '#3b82f6'
        st_map = {'CHO_XU_LY':'Chờ xử lý','DANG_XU_LY':'Đang xử lý','HOAN_THANH':'Hoàn thành','DA_BAO_TRI':'Đã bảo trì'}
        st_label = st_map.get((d.get('status') or '').upper(), d.get('status') or '—')
        damage_html += f'<div class="history-item"><div class="hi-left"><div class="hi-title">{d.get("description","—")}</div><div class="hi-sub">📅 {fmt_date(d.get("reported_date"))}</div></div><div class="hi-badges"><span class="badge" style="color:{bc};">{sev}</span><span class="badge">{st_label}</span></div></div>'

    borrow_html = ""
    for b in borrow_history:
        is_active = b.get('status') != 'DA_TRA'
        borrow_html += f'<div class="history-item"><div class="hi-left"><div class="hi-title">👤 {b.get("user_name","—")}</div><div class="hi-sub">📅 {fmt_date(b.get("borrow_date"))}</div></div><div class="hi-badges"><span class="badge">{"Đang mượn" if is_active else "Đã trả"}</span></div></div>'

    borrower_section = ""
    if borrower:
        borrower_section = f'<div class="borrow-alert"><div class="borrow-alert-title">⚠️ Đang được mượn</div><div class="borrow-grid"><div><span class="lbl">Người mượn</span><span class="val bold">{borrower.get("user_name","—")}</span></div><div><span class="lbl">Dự kiến trả</span><span class="val">{fmt_date(borrower.get("expected_return"))}</span></div></div></div>'

    return render_template('device_info.html', 
        device=device, device_id=device_id, 
        dev_name=device.get('name', '—'), dev_code=device.get('code', '—'),
        dev_room=device.get('room', '—'), dev_cat=device.get('category', '—'),
        dev_mfr=device.get('manufacturer', '—'), dev_model=device.get('model', '—'),
        dev_year=device.get('year', '—'),
        status_label=status_label, status_color=status_color, status_icon=status_icon, status_raw=status_raw,
        db_ok=db_ok, host=request.host_url.rstrip('/'), 
        damage_html=damage_html, borrow_html=borrow_html, borrower_section=borrower_section,
        damages_json=json.dumps(damages),
        current_time=datetime.now().strftime('%H:%M - %d/%m/%Y'))

@app.route('/api/ai/qr-print')
def qr_print_page():
    devices = []
    db_ok = True
    try:
        r = req_lib.get('http://localhost:8080/api/ai-data/devices', headers=INTERNAL_HEADERS, timeout=4)
        if r.status_code == 200: devices = r.json()
    except: db_ok = False
    
    host = request.host_url.rstrip('/')
    cards_html = ""
    stats = {"tot": 0, "bao_tri": 0, "hong": 0}
    for d in devices:
        s = d.get('status', 'TOT')
        if s == 'TOT': stats['tot'] += 1
        elif s == 'BAO_TRI': stats['bao_tri'] += 1
        elif s == 'HONG': stats['hong'] += 1
        
        cards_html += f'<a class="label-card" href="{host}/api/ai/device-info/{d["id"]}" target="_blank"><div class="label-header"><div class="label-sys">QLCSVC</div></div><div class="label-body"><img class="label-qr" src="{host}/api/ai/device-qr/{d["id"]}"><div class="label-info"><div class="label-name">{d.get("name","—")}</div><div class="label-detail">Mã: {d.get("code","—")}</div><div class="label-detail">Phòng: {d.get("room","—")}</div></div></div></a>'
        
    return render_template('qr_print.html', devices=devices, stats=stats, cards_html=cards_html, db_ok=db_ok, host=host)

@app.route('/api/ai/report-form')
def report_form_page():
    device_id = request.args.get('device_id')
    device_name = request.args.get('device_name', 'Thiết bị')
    host = request.host_url.rstrip('/')
    return render_template('report_form.html', 
        device_id=device_id, device_name=device_name, 
        submit_url=f"{host}/api/ai/submit-report",
        back_url=f"{host}/api/ai/device-info/{device_id}" if device_id else f"{host}/api/ai/qr-print")

@app.route('/api/ai/submit-report', methods=['POST'])
def submit_report():
    try:
        data = request.form
        r = req_lib.post('http://localhost:8080/api/ai-data/report-damage', 
            params={
                'thiet_bi_id': data.get('device_id'),
                'mo_ta': data.get('mo_ta'),
                'muc_do': data.get('muc_do', 'TRUNG_BINH'),
                'ten_nguoi_bao': data.get('ten_nguoi_bao', 'User'),
                'so_dien_thoai': data.get('so_dien_thoai', '')
            }, headers=INTERNAL_HEADERS, timeout=5)
        return jsonify(r.json())
    except Exception as e: return jsonify({"success": False, "message": str(e)}), 500

@app.route('/api/ai/device-lookup')
def device_lookup_page():
    devices = []
    try:
        r = req_lib.get('http://localhost:8080/api/ai-data/devices', headers=INTERNAL_HEADERS, timeout=5)
        if r.status_code == 200: devices = r.json()
    except: pass
    return render_template('device_lookup.html', devices=devices, host=request.host_url.rstrip('/'))

@app.route('/api/ai/scan')
def scan_page():
    verify_id = request.args.get('verify', '0')
    return render_template('scan.html', verify_id=verify_id, host=request.host_url.rstrip('/'))

# ============================================================
# MAIN
# ============================================================
if __name__ == '__main__':
    port = int(os.getenv('FLASK_PORT', 5000))
    debug = os.getenv('FLASK_ENV', 'development') == 'development'
    app.run(host='0.0.0.0', port=port, debug=debug)
