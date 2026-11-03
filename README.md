# CTMS – Container Terminal Management System (SWP Group 5)

Hệ thống Quản lý Khai thác Cảng Container viết bằng **Java 17+ / Spring Boot 4 + Spring Security + Thymeleaf + SQL Server**.

Hệ thống phục vụ 4 đối tượng nghiệp vụ chính tại cảng:
- **Ban Quản lý Cảng (Port Manager)**: KPI dashboard, phân bổ cầu bến, quản lý hãng tàu, quản lý tài khoản tài xế.
- **Điều hành Cảng (Terminal Operator)**: Điều độ cầu bến & xếp dỡ tàu, kiểm soát cổng ra/vào (Gate In/Out), quản lý sơ đồ bãi (Yard slot).
- **Hãng tàu đối tác (Shipping Line)**: Tra cứu lịch trình đội tàu và tồn bãi container của riêng hãng.
- **Tài xế xe tải (Truck Driver)**: Đăng ký hẹn giờ nhận/hạ container, tích hợp quét mã VietQR thanh toán tự động phí nâng hạ.

---

## 🚀 Hướng dẫn nhanh cho thành viên trong nhóm (Quick Start)

### 1. Yêu cầu môi trường
- **Java**: JDK 17 hoặc mới hơn (JDK 21, 24 đều tương thích).
- **Cơ sở dữ liệu**: Microsoft SQL Server (bản 2017, 2019, 2022 hoặc Express).
- **IDE**: IntelliJ IDEA (khuyến nghị), Eclipse hoặc VS Code.

---

### 2. Khởi tạo Cơ sở dữ liệu (Database Setup)
1. Mở **SQL Server Management Studio (SSMS)** hoặc Azure Data Studio.
2. Mở file [ctms_database.sql](ctms_database.sql) trong thư mục gốc của dự án.
3. Bấm **Execute (F5)** để chạy toàn bộ script.
   > Script sẽ tự động:
   > - Tạo cơ sở dữ liệu `ctms_db` với đầy đủ bảng và quan hệ ràng buộc.
   > - Chèn dữ liệu mẫu (tàu, cầu bến, sơ đồ bãi, container, lịch hẹn, lịch sử di chuyển).
   > - Tạo sẵn SQL Login `ctms_app` với mật khẩu `Ctms@123456` và cấp quyền `db_owner`.

---

### 3. Cấu hình kết nối (Tự động & Tùy chỉnh)

Mặc định dự án đã được thiết lập sẵn trong `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=ctms_db;encrypt=true;trustServerCertificate=true;
spring.datasource.username=${DB_USERNAME:ctms_app}
spring.datasource.password=${DB_PASSWORD:Ctms@123456}
```
👉 **Bạn KHÔNG cần đổi cấu hình gì cả** nếu đã chạy script `ctms_database.sql` ở bước 2!

*(Nếu máy bạn muốn dùng tài khoản `sa` riêng, bạn chỉ cần truyền biến môi trường `DB_USERNAME=sa` và `DB_PASSWORD=<mật-khẩu-của-bạn>` hoặc sửa trực tiếp trong `application.properties`)*.

---

### 4. Chạy ứng dụng

- **Cách 1 (Bằng IntelliJ IDEA):**
  - Mở thư mục dự án trong IntelliJ.
  - Tìm file `src/main/java/com/ctms/CtmsApplication.java`.
  - Nhấp chuột phải chọn **Run 'CtmsApplication'** (hoặc bấm `Shift + F10`).

- **Cách 2 (Bằng Terminal/Command Line):**
  ```bash
  # Windows CMD / PowerShell
  .\mvnw spring-boot:run

  # Linux / macOS
  ./mvnw spring-boot:run
  ```

Sau khi ứng dụng khởi động thành công (`Started CtmsApplication in ... seconds`), truy cập trình duyệt tại:
👉 **http://localhost:8080**

---

## 🔑 Tài khoản thử nghiệm (Mặc định)

Tất cả tài khoản đều sử dụng mật khẩu chung: `password123`

| Vai trò (Role) | Tên đăng nhập (Username) | Số điện thoại | Chức năng chính |
| :--- | :--- | :--- | :--- |
| **Quản lý Cảng** | `manager` | `0901234567` | Dashboard tổng quan, Quản lý Hãng tàu, Quản lý Tài xế (`/drivers`), Cầu bến |
| **Điều hành Cảng** | `operator` | `0902345678` | Trực cổng Check-in/Check-out (`/gate`), Quản lý bãi Cont (`/yard`), Cầu bến & Tàu |
| **Hãng tàu Maersk** | `shipping_maersk` | `0903456789` | Cổng thông tin riêng của Maersk (`/portal/shipping-line`), xem cont & tàu của hãng |
| **Tài xế xe tải** | `driver` | `0904567890` | Cổng dịch vụ tài xế (`/portal/driver`), đặt hẹn nhận/hạ cont, quét mã VietQR |

*Lưu ý: Đối với tài xế, có thể đăng nhập bằng **Username** (`driver`) hoặc bằng **Số điện thoại** (`0904567890`).*

---

## 🧪 Chạy Kiểm thử (Unit Tests)

Dự án có sẵn bộ test tự động sử dụng H2 In-Memory Database (không phụ thuộc vào SQL Server thật):
```bash
.\mvnw test
```
Toàn bộ test cases cho Gate In/Out, Sắp xếp Yard Slot, Phân bổ Berth, Quản lý tài xế đều được tự động xác minh.
