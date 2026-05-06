# Kế hoạch Triển khai Tính năng Nhân viên CSVC

Tính năng yêu cầu tập trung vào việc hoàn thiện quy trình xử lý của Nhân viên CSVC, bao gồm: nhận báo hỏng, lên lịch bảo trì, cập nhật tiến độ liên tục, hiển thị thống kê tổng quan, danh sách thiết bị (với liên kết đến báo cáo hỏng/bảo trì hiện tại), và danh sách phòng học (với thống kê số lượng & tình trạng thiết bị).

## User Review Required
> [!IMPORTANT]
> Vui lòng xác nhận luồng "Cập nhật tiến độ bảo trì": Nhân viên sẽ có thể thêm ghi chú/cập nhật kết quả bảo trì (Thành công, Thất bại, Cần thay thế) ngay trên danh sách hoặc chi tiết Lịch bảo trì. Các ghi chú cũ sẽ được nối tiếp hoặc ghi đè tùy thiết kế, nhưng tôi đề xuất nối tiếp (append) với timestamp để dễ theo dõi. Bạn có đồng ý với phương án này không?

## Proposed Changes

### 1. Luồng Báo Hỏng & Lịch Bảo Trì (Staff Flow)
* **Chấp nhận Báo Hỏng:** Đảm bảo trang danh sách và chi tiết Báo hỏng (`areas/nhanvien_csvc/bao-hong/list.html` và `detail.html`) có nút "Chấp nhận & Lên lịch" để chuyển hướng sang form tạo lịch bảo trì.
* **Cập nhật tiến độ liên tục:**
  * Thêm endpoint `POST /nhanvien-csvc/bao-tri/{id}/update` trong `CSVCViewController` để cập nhật trạng thái `ketQua` và nội dung ghi chú tiến độ.
  * Thêm UI cập nhật (Modal) trong danh sách bảo trì (`areas/nhanvien_csvc/bao-tri/list.html`).

---

### 2. Thống kê & Báo cáo
#### [NEW] `com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThongKeService.java`
* Lấy các số liệu tổng quan: Tổng thiết bị, Thiết bị đang hỏng/bảo trì, Số báo hỏng chờ xử lý, Số lịch bảo trì đang diễn ra.
#### [MODIFY] `CSVCViewController.java`
* Inject `CSVCThongKeService` và truyền dữ liệu vào trang `/thong-ke`.
#### [MODIFY] `areas/nhanvien_csvc/thong-ke.html`
* Thay thế placeholder bằng giao diện hiển thị Dashboard/Thống kê trực quan.

---

### 3. Quản lý Thiết Bị
#### [NEW] `com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCThietBiService.java`
* Cung cấp dữ liệu thiết bị kèm theo các thông tin liên quan đến Báo Hỏng/Bảo Trì đang diễn ra.
#### [MODIFY] `CSVCViewController.java`
* Lấy danh sách thiết bị và truyền vào model cho trang `/thiet-bi`.
#### [MODIFY] `areas/nhanvien_csvc/thiet-bi.html`
* Xây dựng giao diện danh sách thiết bị. Hiển thị badge trạng thái.
* Nếu thiết bị đang có báo hỏng hoặc đang bảo trì, hiển thị thêm nút "Xem báo cáo" (link trực tiếp đến mã báo hỏng/bảo trì đó).

---

### 4. Quản lý Phòng Học
#### [NEW] `com.Tta.QLCSVC.DHNT.areas.nhanvien_csvc.service.CSVCPhongHocService.java`
* Cung cấp danh sách phòng học và tính toán tổng số thiết bị cũng như phân loại theo trạng thái (Tốt, Hỏng, Bảo trì) cho mỗi phòng.
#### [MODIFY] `CSVCViewController.java`
* Lấy danh sách phòng học kèm thống kê và truyền vào model cho trang `/phong-hoc`.
#### [MODIFY] `areas/nhanvien_csvc/phong-hoc.html`
* Xây dựng giao diện card/danh sách phòng học, hiển thị rõ số lượng thiết bị bên trong và tỷ lệ tình trạng.

## Verification Plan
### Automated Tests
* Không yêu cầu do hệ thống chưa có setup Unit Test cụ thể, nhưng sẽ kiểm tra bằng cách build code Java không lỗi.
### Manual Verification
* Đăng nhập với tài khoản nhân viên.
* Kiểm tra việc "Chấp nhận đơn" từ Báo Hỏng sang Bảo Trì.
* Thử cập nhật tiến độ trên Lịch bảo trì.
* Kiểm tra trang Thống kê hiển thị đúng số liệu.
* Kiểm tra trang Thiết bị xem nút "Xem báo cáo" có xuất hiện đúng trên thiết bị hỏng không.
* Kiểm tra trang Phòng học có hiển thị đúng số lượng thiết bị Tốt/Hỏng.
