package com.example.duantotnghiep.state;

public enum TrangThaiKhieuNai {
    CHO_XU_LY(0, "Chờ xử lý"),
    DANG_XU_LY(1, "Đang xử lý"),
    THANH_CONG(2, "Khiếu nại thành công"),
    THAT_BAI(3, "Khiếu nại thất bại"),
    DA_HUY(4, "Đã hủy khiếu nại"),
    DA_HOAN_TIEN(5, "Đã hoàn tiền sau khiếu nại"),
    DA_DOI_HANG(6, "Đã đổi hàng");
    private final int ma;
    private final String moTa;

    TrangThaiKhieuNai(int ma, String moTa) {
        this.ma = ma;
        this.moTa = moTa;
    }

    public int getMa() {
        return ma;
    }

    public String getMoTa() {
        return moTa;
    }

    public static TrangThaiKhieuNai tuMa(int ma) {
        for (TrangThaiKhieuNai t : values()) {
            if (t.ma == ma) {
                return t;
            }
        }
        throw new IllegalArgumentException("Mã trạng thái khiếu nại không hợp lệ: " + ma);
    }

    public boolean canTransitionTo(TrangThaiKhieuNai next) {
        return switch (this) {
            case CHO_XU_LY -> next == DANG_XU_LY || next == DA_HUY;
            case DANG_XU_LY -> next == THANH_CONG || next == THAT_BAI || next == DA_HUY;
            case THANH_CONG -> next == DA_HOAN_TIEN || next == DA_DOI_HANG;
            case THAT_BAI, DA_HUY, DA_HOAN_TIEN, DA_DOI_HANG -> false; // Kết thúc
        };
    }
}
