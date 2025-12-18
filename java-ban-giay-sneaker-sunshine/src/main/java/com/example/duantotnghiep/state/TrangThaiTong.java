package com.example.duantotnghiep.state;

public enum TrangThaiTong {
    HUY_GIAO_DICH(-1,"Hủy giao dịch"),
    DANG_XU_LY(0, "Đang xử lý"),
    THANH_CONG(1, "Thành công"),
    DA_HUY(2, "Đã hủy"),
    TRA_HANG(3, "Trả hàng"),
    KHIEU_NAI(4, "Khiếu nại");

    private final int ma;
    private final String moTa;

    TrangThaiTong(int ma, String moTa) {
        this.ma = ma;
        this.moTa = moTa;
    }

    public int getMa() {
        return ma;
    }

    public String getMoTa() {
        return moTa;
    }

    public static TrangThaiTong tuMa(int ma) {
        for (TrangThaiTong t : values()) {
            if (t.ma == ma) return t;
        }
        throw new IllegalArgumentException("Mã trạng thái tổng không hợp lệ: " + ma);
    }

    public static String fromValue(int value) {
        for (TrangThaiTong status : TrangThaiTong.values()) {
            if (status.getMa() == value) {
                return status.getMoTa();
            }
        }
        return "Không xác định";
    }
}

