-- ==============================================================================
-- CTMS (Container Terminal Management System) Database Script
-- Hệ thống Quản lý Khai thác Cảng Container - Dự án SWP
-- Hỗ trợ: Microsoft SQL Server (SSMS) 2017 / 2019 / 2022 / Azure SQL
-- Bảng mã: NVARCHAR / Unicode (Hỗ trợ tiếng Việt có dấu 100%)
-- ==============================================================================

USE master;
GO

-- 1. Tạo Database nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'ctms_db')
BEGIN
    CREATE DATABASE ctms_db COLLATE Vietnamese_CI_AS;
END
GO

USE ctms_db;
GO

-- ------------------------------------------------------------------------------
-- 2. Xóa các bảng cũ theo thứ tự ràng buộc khóa ngoại (nếu đã tồn tại)
-- ------------------------------------------------------------------------------
IF OBJECT_ID('dbo.movement_history', 'U') IS NOT NULL DROP TABLE dbo.movement_history;
IF OBJECT_ID('dbo.appointments', 'U') IS NOT NULL DROP TABLE dbo.appointments;
IF OBJECT_ID('dbo.yard_slots', 'U') IS NOT NULL DROP TABLE dbo.yard_slots;
IF OBJECT_ID('dbo.containers', 'U') IS NOT NULL DROP TABLE dbo.containers;

IF OBJECT_ID('dbo.berths', 'U') IS NOT NULL 
BEGIN
    IF OBJECT_ID('dbo.fk_berth_vessel', 'F') IS NOT NULL 
        ALTER TABLE dbo.berths DROP CONSTRAINT fk_berth_vessel;
END
GO

IF OBJECT_ID('dbo.vessels', 'U') IS NOT NULL DROP TABLE dbo.vessels;
IF OBJECT_ID('dbo.berths', 'U') IS NOT NULL DROP TABLE dbo.berths;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.shipping_lines', 'U') IS NOT NULL DROP TABLE dbo.shipping_lines;
GO

-- ------------------------------------------------------------------------------
-- 3. Tạo các bảng thực thể
-- ------------------------------------------------------------------------------

-- 3.1. Bảng: shipping_lines (Danh sách Hãng tàu đối tác)
CREATE TABLE dbo.shipping_lines (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(20) NOT NULL UNIQUE, -- Mã hãng tàu: MAE, MSC, CMA
    name NVARCHAR(255) NOT NULL,        -- Tên đầy đủ hãng tàu
    contact_email NVARCHAR(255) NULL,
    contact_phone NVARCHAR(255) NULL,
    address NVARCHAR(255) NULL
);
GO

-- 3.2. Bảng: users (Tài khoản người dùng và 4 Actors)
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,    -- Mật khẩu mã hóa BCrypt
    full_name NVARCHAR(255) NOT NULL,
    role NVARCHAR(50) NOT NULL,         -- ROLE_MANAGER, ROLE_OPERATOR, ROLE_SHIPPING_LINE, ROLE_TRUCK_DRIVER
    email NVARCHAR(255) NULL,
    phone NVARCHAR(255) NULL,
    license_plate NVARCHAR(50) NULL,    -- Biển số xe tải (dành cho tài xế)
    shipping_line_id BIGINT NULL,
    CONSTRAINT fk_user_shipping_line FOREIGN KEY (shipping_line_id) REFERENCES dbo.shipping_lines(id) ON DELETE SET NULL
);
GO

-- 3.3. Bảng: berths (Cầu cảng neo đậu tàu)
CREATE TABLE dbo.berths (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    berth_code NVARCHAR(20) NOT NULL UNIQUE, -- B-01, B-02
    name NVARCHAR(255) NOT NULL,
    length_meters FLOAT NULL,
    max_draft_meters FLOAT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, MAINTENANCE
    current_vessel_id BIGINT NULL
);
GO

-- 3.4. Bảng: vessels (Tàu container cập/rời bến)
CREATE TABLE dbo.vessels (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    imo_number NVARCHAR(30) NULL,
    shipping_line_id BIGINT NOT NULL,
    capacity_teu INT NULL,
    length_meters FLOAT NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, BERTHED, WORKING, DEPARTED
    eta DATETIME2 NULL,
    etd DATETIME2 NULL,
    berth_id BIGINT NULL,
    CONSTRAINT fk_vessel_shipping_line FOREIGN KEY (shipping_line_id) REFERENCES dbo.shipping_lines(id),
    CONSTRAINT fk_vessel_berth FOREIGN KEY (berth_id) REFERENCES dbo.berths(id) ON DELETE SET NULL
);
GO

-- Filtered Unique Index cho imo_number (cho phép nhiều giá trị NULL)
CREATE UNIQUE NONCLUSTERED INDEX uq_vessels_imo_number 
    ON dbo.vessels(imo_number) 
    WHERE imo_number IS NOT NULL;
GO

-- Khóa ngoại vòng giữa berths và vessels
ALTER TABLE dbo.berths 
    ADD CONSTRAINT fk_berth_vessel FOREIGN KEY (current_vessel_id) REFERENCES dbo.vessels(id) ON DELETE NO ACTION;
GO

-- 3.5. Bảng: containers (Danh mục & Theo dõi Container)
CREATE TABLE dbo.containers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    container_number NVARCHAR(20) NOT NULL UNIQUE, -- MSKU9182736
    iso_type NVARCHAR(10) NULL,                    -- 20GP, 40HC, 45HC
    container_type NVARCHAR(50) NOT NULL,          -- DRY, REEFER
    cargo_status NVARCHAR(50) NOT NULL,            -- FULL, EMPTY
    condition NVARCHAR(50) NOT NULL,              -- GOOD, DAMAGED
    current_status NVARCHAR(50) NOT NULL DEFAULT 'EXPECTED', -- EXPECTED, GATE_IN, IN_YARD, GATE_OUT
    seal_number NVARCHAR(255) NULL,
    gross_weight_kg FLOAT NULL,
    shipping_line_id BIGINT NOT NULL,
    gate_in_time DATETIME2 NULL,
    gate_out_time DATETIME2 NULL,
    CONSTRAINT fk_container_shipping_line FOREIGN KEY (shipping_line_id) REFERENCES dbo.shipping_lines(id)
);
GO

-- 3.6. Bảng: yard_slots (Vị trí ô bãi Container - Yard Map)
CREATE TABLE dbo.yard_slots (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    block NVARCHAR(20) NOT NULL,                   -- BLOCK_A, BLOCK_B, BLOCK_C
    slot_code NVARCHAR(20) NOT NULL UNIQUE,        -- A-01, B-08, C-16
    slot_index INT NULL,                           -- 1 -> 16
    container_id BIGINT NULL,
    CONSTRAINT fk_yard_slot_container FOREIGN KEY (container_id) REFERENCES dbo.containers(id) ON DELETE SET NULL
);
GO

-- Filtered Unique Index cho container_id (cho phép nhiều ô trống có container_id = NULL)
CREATE UNIQUE NONCLUSTERED INDEX uq_yard_slots_container_id 
    ON dbo.yard_slots(container_id) 
    WHERE container_id IS NOT NULL;
GO

-- 3.7. Bảng: appointments (Phiếu hẹn xe tải qua cổng - Gate Appointments)
CREATE TABLE dbo.appointments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    driver_id BIGINT NOT NULL,
    appointment_type NVARCHAR(50) NOT NULL,        -- IMPORT_PICKUP, EXPORT_DROPOFF
    booking_number NVARCHAR(50) NOT NULL,
    container_number NVARCHAR(20) NOT NULL,
    container_id BIGINT NULL,
    license_plate NVARCHAR(20) NOT NULL,
    driver_phone NVARCHAR(255) NULL,
    appointment_time DATETIME2 NOT NULL,
    seal_number NVARCHAR(255) NULL,
    status NVARCHAR(50) NOT NULL DEFAULT 'CONFIRMED', -- PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED
    notes NVARCHAR(MAX) NULL,
    CONSTRAINT uq_appointment_booking UNIQUE (booking_number),
    CONSTRAINT fk_appointment_driver FOREIGN KEY (driver_id) REFERENCES dbo.users(id),
    CONSTRAINT fk_appointment_container FOREIGN KEY (container_id) REFERENCES dbo.containers(id) ON DELETE SET NULL
);
GO

-- 3.8. Bảng: movement_history (Nhật ký di chuyển & nâng hạ container)
CREATE TABLE dbo.movement_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    container_id BIGINT NOT NULL,
    event_type NVARCHAR(50) NOT NULL,              -- GATE_IN, ASSIGN_SLOT, MOVE_SLOT, GATE_OUT
    from_location NVARCHAR(100) NULL,
    to_location NVARCHAR(100) NULL,
    timestamp DATETIME2 NOT NULL DEFAULT GETDATE(),
    operator_name NVARCHAR(255) NULL,
    truck_plate NVARCHAR(255) NULL,
    notes NVARCHAR(MAX) NULL,
    CONSTRAINT fk_history_container FOREIGN KEY (container_id) REFERENCES dbo.containers(id) ON DELETE CASCADE
);
GO


-- ==============================================================================
-- 4. DỮ LIỆU MẪU (SEED DATA - TƯƠNG THÍCH HOÀN TOÀN VỚI HỆ THỐNG CTMS)
-- ==============================================================================

-- 4.1. Hãng tàu (Shipping Lines)
SET IDENTITY_INSERT dbo.shipping_lines ON;
INSERT INTO dbo.shipping_lines (id, code, name, contact_email, contact_phone, address) VALUES
(1, 'MAE', N'Maersk Line', 'operations@maersk.com', '028-3823-0001', N'Tòa nhà Bitexco, Q.1, TP.HCM'),
(2, 'MSC', N'Mediterranean Shipping Company', 'portops@msc.com', '028-3823-0002', N'Tòa nhà Sunwah, Q.1, TP.HCM'),
(3, 'CMA', N'CMA CGM Group', 'contact@cma-cgm.com', '028-3823-0003', N'Tòa nhà Saigon Tower, Q.1, TP.HCM');
SET IDENTITY_INSERT dbo.shipping_lines OFF;
GO

-- 4.2. Người dùng (Users - Mật khẩu mặc định: password123, mã hóa BCrypt)
SET IDENTITY_INSERT dbo.users ON;
INSERT INTO dbo.users (id, username, password, full_name, role, email, phone, license_plate, shipping_line_id) VALUES
(1, 'manager', '$2a$10$K92Ex1NOiEhxoRiqflcmGeo1zEW.ymT16lSGK.rkItOJfdBj3N..i', N'Nguyễn Văn Quản Lý', 'ROLE_MANAGER', 'manager@ctms.port.vn', '0901234567', NULL, NULL),
(2, 'operator', '$2a$10$K92Ex1NOiEhxoRiqflcmGeo1zEW.ymT16lSGK.rkItOJfdBj3N..i', N'Trần Điều Hành', 'ROLE_OPERATOR', 'operator@ctms.port.vn', '0902345678', NULL, NULL),
(3, 'shipping_maersk', '$2a$10$K92Ex1NOiEhxoRiqflcmGeo1zEW.ymT16lSGK.rkItOJfdBj3N..i', N'Đại Diện Hãng Tàu Maersk', 'ROLE_SHIPPING_LINE', 'agent@maersk.com', '0903456789', NULL, 1),
(4, 'driver', '$2a$10$K92Ex1NOiEhxoRiqflcmGeo1zEW.ymT16lSGK.rkItOJfdBj3N..i', N'Lê Văn Tài Xế', 'ROLE_TRUCK_DRIVER', 'driver01@transport.vn', '0904567890', '51C-889.92', NULL);
SET IDENTITY_INSERT dbo.users OFF;
GO

-- 4.3. Cầu cảng (Berths)
SET IDENTITY_INSERT dbo.berths ON;
INSERT INTO dbo.berths (id, berth_code, name, length_meters, max_draft_meters, status, current_vessel_id) VALUES
(1, 'B-01', N'Cầu cảng số 1 (Ngoại thương)', 380.0, 14.5, 'OCCUPIED', NULL),
(2, 'B-02', N'Cầu cảng số 2 (Nội địa & trung chuyển)', 420.0, 16.0, 'AVAILABLE', NULL);
SET IDENTITY_INSERT dbo.berths OFF;
GO

-- 4.4. Tàu cập cảng (Vessels)
SET IDENTITY_INSERT dbo.vessels ON;
INSERT INTO dbo.vessels (id, name, imo_number, shipping_line_id, capacity_teu, length_meters, status, eta, etd, berth_id) VALUES
(1, 'MSC OSCAR', 'IMO9703291', 2, 19224, 395.4, 'BERTHED', DATEADD(HOUR, -8, GETDATE()), DATEADD(HOUR, 16, GETDATE()), 1),
(2, 'MAERSK MC-KINNEY MOLLER', 'IMO9619907', 1, 18270, 399.0, 'SCHEDULED', DATEADD(DAY, 1, GETDATE()), DATEADD(DAY, 2, GETDATE()), NULL),
(3, 'CMA CGM ANTOINE DE SAINT EXUPERY', 'IMO9776418', 3, 20600, 400.0, 'SCHEDULED', DATEADD(DAY, 2, GETDATE()), DATEADD(DAY, 3, GETDATE()), NULL);
SET IDENTITY_INSERT dbo.vessels OFF;
GO

-- Cập nhật liên kết tàu đang cập cầu bến 1
UPDATE dbo.berths SET current_vessel_id = 1 WHERE id = 1;
GO

-- 4.5. Khởi tạo 48 ô bãi (Yard Slots: Khu A 16 ô, Khu B 16 ô, Khu C 16 ô)
SET IDENTITY_INSERT dbo.yard_slots ON;
INSERT INTO dbo.yard_slots (id, block, slot_code, slot_index, container_id) VALUES
-- Khu A (Block A: A-01 -> A-16)
(1, 'BLOCK_A', 'A-01', 1, NULL),
(2, 'BLOCK_A', 'A-02', 2, NULL),
(3, 'BLOCK_A', 'A-03', 3, NULL),
(4, 'BLOCK_A', 'A-04', 4, NULL),
(5, 'BLOCK_A', 'A-05', 5, NULL),
(6, 'BLOCK_A', 'A-06', 6, NULL),
(7, 'BLOCK_A', 'A-07', 7, NULL),
(8, 'BLOCK_A', 'A-08', 8, NULL),
(9, 'BLOCK_A', 'A-09', 9, NULL),
(10, 'BLOCK_A', 'A-10', 10, NULL),
(11, 'BLOCK_A', 'A-11', 11, NULL),
(12, 'BLOCK_A', 'A-12', 12, NULL),
(13, 'BLOCK_A', 'A-13', 13, NULL),
(14, 'BLOCK_A', 'A-14', 14, NULL),
(15, 'BLOCK_A', 'A-15', 15, NULL),
(16, 'BLOCK_A', 'A-16', 16, NULL),
-- Khu B (Block B: B-01 -> B-16)
(17, 'BLOCK_B', 'B-01', 1, NULL),
(18, 'BLOCK_B', 'B-02', 2, NULL),
(19, 'BLOCK_B', 'B-03', 3, NULL),
(20, 'BLOCK_B', 'B-04', 4, NULL),
(21, 'BLOCK_B', 'B-05', 5, NULL),
(22, 'BLOCK_B', 'B-06', 6, NULL),
(23, 'BLOCK_B', 'B-07', 7, NULL),
(24, 'BLOCK_B', 'B-08', 8, NULL),
(25, 'BLOCK_B', 'B-09', 9, NULL),
(26, 'BLOCK_B', 'B-10', 10, NULL),
(27, 'BLOCK_B', 'B-11', 11, NULL),
(28, 'BLOCK_B', 'B-12', 12, NULL),
(29, 'BLOCK_B', 'B-13', 13, NULL),
(30, 'BLOCK_B', 'B-14', 14, NULL),
(31, 'BLOCK_B', 'B-15', 15, NULL),
(32, 'BLOCK_B', 'B-16', 16, NULL),
-- Khu C (Block C: C-01 -> C-16)
(33, 'BLOCK_C', 'C-01', 1, NULL),
(34, 'BLOCK_C', 'C-02', 2, NULL),
(35, 'BLOCK_C', 'C-03', 3, NULL),
(36, 'BLOCK_C', 'C-04', 4, NULL),
(37, 'BLOCK_C', 'C-05', 5, NULL),
(38, 'BLOCK_C', 'C-06', 6, NULL),
(39, 'BLOCK_C', 'C-07', 7, NULL),
(40, 'BLOCK_C', 'C-08', 8, NULL),
(41, 'BLOCK_C', 'C-09', 9, NULL),
(42, 'BLOCK_C', 'C-10', 10, NULL),
(43, 'BLOCK_C', 'C-11', 11, NULL),
(44, 'BLOCK_C', 'C-12', 12, NULL),
(45, 'BLOCK_C', 'C-13', 13, NULL),
(46, 'BLOCK_C', 'C-14', 14, NULL),
(47, 'BLOCK_C', 'C-15', 15, NULL),
(48, 'BLOCK_C', 'C-16', 16, NULL);
SET IDENTITY_INSERT dbo.yard_slots OFF;
GO

-- 4.6. Danh sách Container mẫu
SET IDENTITY_INSERT dbo.containers ON;
INSERT INTO dbo.containers (id, container_number, iso_type, container_type, cargo_status, condition, current_status, seal_number, gross_weight_kg, shipping_line_id, gate_in_time, gate_out_time) VALUES
(1, 'MSKU9182736', '40HC', 'DRY', 'FULL', 'GOOD', 'IN_YARD', 'SL-99881', 28400.0, 1, DATEADD(HOUR, -4, GETDATE()), NULL),
(2, 'MSKU3451290', '20GP', 'REEFER', 'FULL', 'GOOD', 'IN_YARD', 'SL-99882', 22100.0, 1, DATEADD(HOUR, -3, GETDATE()), NULL),
(3, 'MSCU1122334', '40HC', 'DRY', 'EMPTY', 'GOOD', 'IN_YARD', 'SL-77661', 3800.0, 2, DATEADD(HOUR, -2, GETDATE()), NULL),
(4, 'MSCU5566778', '40HC', 'REEFER', 'FULL', 'DAMAGED', 'IN_YARD', 'SL-77662', 29100.0, 2, DATEADD(HOUR, -1, GETDATE()), NULL),
(5, 'CMAU9988776', '20GP', 'DRY', 'FULL', 'GOOD', 'IN_YARD', 'SL-55441', 21500.0, 3, DATEADD(MINUTE, -30, GETDATE()), NULL),
(6, 'CMAU3344556', '45HC', 'DRY', 'EMPTY', 'GOOD', 'IN_YARD', 'SL-55442', 4200.0, 3, DATEADD(MINUTE, -15, GETDATE()), NULL),
(7, 'MSKU7711223', '40HC', 'DRY', 'FULL', 'GOOD', 'GATE_IN', 'SL-99883', 27800.0, 1, DATEADD(MINUTE, -10, GETDATE()), NULL),
(8, 'MSCU9900112', '20GP', 'REEFER', 'EMPTY', 'GOOD', 'EXPECTED', 'SL-77663', 2900.0, 2, NULL, NULL);
SET IDENTITY_INSERT dbo.containers OFF;
GO

-- Gán 6 container vào các ô bãi toạ độ: A-01, A-03, A-05, B-01, B-03, C-01
UPDATE dbo.yard_slots SET container_id = 1 WHERE slot_code = 'A-01';
UPDATE dbo.yard_slots SET container_id = 2 WHERE slot_code = 'A-03';
UPDATE dbo.yard_slots SET container_id = 3 WHERE slot_code = 'A-05';
UPDATE dbo.yard_slots SET container_id = 4 WHERE slot_code = 'B-01';
UPDATE dbo.yard_slots SET container_id = 5 WHERE slot_code = 'B-03';
UPDATE dbo.yard_slots SET container_id = 6 WHERE slot_code = 'C-01';
GO

-- 4.7. Phiếu hẹn tài xế qua cổng (Appointments)
SET IDENTITY_INSERT dbo.appointments ON;
INSERT INTO dbo.appointments (id, driver_id, appointment_type, booking_number, container_number, container_id, license_plate, driver_phone, appointment_time, seal_number, status, notes) VALUES
(1, 4, 'IMPORT_PICKUP', 'BK-IMP-2026-001', 'MSKU9182736', 1, '51C-889.92', '0904567890', DATEADD(HOUR, 3, GETDATE()), 'SL-99881', 'CONFIRMED', N'Xe tải hẹn đến lấy cont nhập khẩu số MSKU9182736'),
(2, 4, 'EXPORT_DROPOFF', 'BK-EXP-2026-002', 'MSKU7711223', 7, '51D-123.45', '0904567890', DATEADD(MINUTE, -30, GETDATE()), 'SL-99883', 'CHECKED_IN', N'Hạ cont xuất khẩu đi tàu MSC Oscar');
SET IDENTITY_INSERT dbo.appointments OFF;
GO

-- 4.8. Lịch sử dịch chuyển container (Movement History)
SET IDENTITY_INSERT dbo.movement_history ON;
INSERT INTO dbo.movement_history (id, container_id, event_type, from_location, to_location, timestamp, operator_name, truck_plate, notes) VALUES
(1, 1, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi A-01 (Khu A (Block A))', DATEADD(HOUR, -4, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí'),
(2, 2, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi A-03 (Khu A (Block A))', DATEADD(HOUR, -3, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí'),
(3, 3, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi A-05 (Khu A (Block A))', DATEADD(HOUR, -2, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí'),
(4, 4, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi B-01 (Khu B (Block B))', DATEADD(HOUR, -1, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí'),
(5, 5, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi B-03 (Khu B (Block B))', DATEADD(MINUTE, -30, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí'),
(6, 6, 'ASSIGN_SLOT', N'Cổng vào (Gate-01)', N'Ô bãi C-01 (Khu C (Block C))', DATEADD(MINUTE, -15, GETDATE()), N'Trần Điều Hành', NULL, N'Hạ bãi phân bổ vị trí');
SET IDENTITY_INSERT dbo.movement_history OFF;
GO

-- 4.9. Cấp quyền kết nối cho ứng dụng CTMS (Tài khoản mặc định ctms_app / Ctms@123456)
USE master;
GO
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'ctms_app')
BEGIN
    CREATE LOGIN [ctms_app] WITH PASSWORD = 'Ctms@123456', CHECK_POLICY = OFF;
END
GO
USE ctms_db;
GO
IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'ctms_app')
BEGIN
    CREATE USER [ctms_app] FOR LOGIN [ctms_app];
END
GO
ALTER ROLE db_owner ADD MEMBER [ctms_app];
GO

-- ==============================================================================
-- HOÀN TẤT! ĐÃ TẠO TOÀN BỘ CƠ SỞ DỮ LIỆU CTMS TRÊN MICROSOFT SQL SERVER!
-- ==============================================================================
