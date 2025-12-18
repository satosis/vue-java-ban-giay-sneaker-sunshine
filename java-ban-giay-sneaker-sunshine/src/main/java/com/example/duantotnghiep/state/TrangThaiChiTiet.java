package com.example.duantotnghiep.state;

import java.util.Calendar;
import java.util.Date;

public enum TrangThaiChiTiet {
    DANG_GIAO_DICH(-3,"Đang giao dịch"),
    HUY_DON(-2, "Hủy đơn hàng"),
    HUY_GIAO_DICH(-1, "Hủy giao dịch"),
    CHO_XU_LY(0, "Chờ xử lý"),
    DA_XU_LY(1, "Đã xử lý"),
    CHO_GIAO_HANG(2, "Chờ giao hàng"),
    DANG_GIAO_HANG(3, "Đang giao hàng"),
    GIAO_THANH_CONG(4, "Giao hàng thành công"),
    GIAO_THAT_BAI(5, "Giao hàng thất bại"),
    MAT_HANG(6, "Mất hàng"),
    DA_HOAN_TIEN(7, "Đã hoàn tiền"),
    DA_HOAN_THANH(8, "Đã hoàn thành"),
    YEU_CAU_HOAN(9,"Yêu cầu hoàn");

    private final int ma;
    private final String moTa;

    TrangThaiChiTiet(int ma, String moTa) {
        this.ma = ma;
        this.moTa = moTa;
    }

    public int getMa() {
        return ma;
    }

    public String getMoTa() {
        return moTa;
    }

    public static TrangThaiChiTiet tuMa(int ma) {
        for (TrangThaiChiTiet t : values()) {
            if (t.ma == ma) return t;
        }
        throw new IllegalArgumentException("Mã trạng thái chi tiết không hợp lệ: " + ma);
    }

    public boolean canTransitionTo(TrangThaiChiTiet next) {
        if (next == HUY_DON) {
            return this == CHO_XU_LY;
        }

        if(next == HUY_GIAO_DICH){
            return this == DANG_GIAO_DICH;
        }

        return switch (this) {
            case CHO_XU_LY -> next == DA_XU_LY;
            case DA_XU_LY ->  next == CHO_GIAO_HANG;
            case CHO_GIAO_HANG -> next == DANG_GIAO_HANG;
            case DANG_GIAO_HANG -> next == GIAO_THANH_CONG || next == GIAO_THAT_BAI || next == MAT_HANG;
            case GIAO_THAT_BAI, MAT_HANG -> next == DA_HOAN_TIEN;
            case DA_HOAN_TIEN -> next == DA_HOAN_THANH;
            case GIAO_THANH_CONG -> next == DA_HOAN_THANH;
            default -> false;
        };
    }

    public TrangThaiTong toTrangThaiTong() {
        return switch (this) {
            case HUY_DON, HUY_GIAO_DICH, MAT_HANG -> TrangThaiTong.DA_HUY;
            case DA_HOAN_TIEN, GIAO_THANH_CONG, DA_HOAN_THANH -> TrangThaiTong.THANH_CONG;
            default -> TrangThaiTong.DANG_XU_LY;
        };
    }

    public boolean coTheKhieuNai(Date deliveredAt) {
        if (this != GIAO_THANH_CONG || deliveredAt == null) return false;

        Calendar cal = Calendar.getInstance();
        cal.setTime(deliveredAt);
        cal.add(Calendar.DAY_OF_YEAR, 7);
        Date hanKhieuNai = cal.getTime();

        return new Date().before(hanKhieuNai);
    }
}
