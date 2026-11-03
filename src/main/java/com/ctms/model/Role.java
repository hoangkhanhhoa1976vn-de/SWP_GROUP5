package com.ctms.model;

public enum Role {
    ROLE_MANAGER("Quản lý cảng (Manager)"),
    ROLE_OPERATOR("Điều hành viên (Terminal Operator)"),
    ROLE_SHIPPING_LINE("Đại diện Hãng tàu (Shipping Line)"),
    ROLE_TRUCK_DRIVER("Tài xế xe tải (Truck Driver)");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
