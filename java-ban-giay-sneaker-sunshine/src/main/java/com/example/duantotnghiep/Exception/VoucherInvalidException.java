package com.example.duantotnghiep.Exception;

import java.io.Serial;

/**
 * Ném ra khi voucher đang áp dụng không còn hợp lệ
 * (đã bị xoá/tắt, hết hạn, hoặc chưa đến thời gian áp dụng).
 *
 * Sử dụng cùng GlobalExceptionHandler để trả về 409 CONFLICT + { code, message } cho FE.
 *
 * Ví dụ dùng:
 *   throw VoucherInvalidException.removed();     // voucher bị xoá/tắt
 *   throw VoucherInvalidException.expired();     // voucher hết hạn
 *   throw VoucherInvalidException.notYetActive();// chưa tới thời gian áp dụng
 *   throw new VoucherInvalidException("VOUCHER_REMOVED", "Voucher đã bị vô hiệu hoá.");
 */
public class VoucherInvalidException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Mã lỗi phục vụ FE hiển thị/logic (ví dụ: VOUCHER_REMOVED, VOUCHER_EXPIRED, VOUCHER_NOT_YET_ACTIVE) */
    private final String code;

    // ===== Constructors =====

    public VoucherInvalidException(String message) {
        super(message);
        this.code = "VOUCHER_INVALID";
    }

    public VoucherInvalidException(String code, String message) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "VOUCHER_INVALID" : code;
    }

    public VoucherInvalidException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = (code == null || code.isBlank()) ? "VOUCHER_INVALID" : code;
    }

    // ===== Factory methods tiện dùng =====

    /** Voucher đã bị xoá/tắt (status = 0 hoặc deleted = 1) */
    public static VoucherInvalidException removed() {
        return new VoucherInvalidException(
                "VOUCHER_REMOVED",
                "Voucher đang áp dụng đã bị xoá/tắt. Hệ thống đã tự bỏ voucher khỏi hoá đơn."
        );
    }

    /** Voucher đã hết hạn (now > endDate) */
    public static VoucherInvalidException expired() {
        return new VoucherInvalidException(
                "VOUCHER_EXPIRED",
                "Voucher đã hết hạn. Hệ thống đã tự bỏ voucher khỏi hoá đơn."
        );
    }

    /** Voucher chưa tới thời gian áp dụng (now < startDate) */
    public static VoucherInvalidException notYetActive() {
        return new VoucherInvalidException(
                "VOUCHER_NOT_YET_ACTIVE",
                "Voucher chưa đến thời gian áp dụng. Hệ thống đã tự bỏ voucher khỏi hoá đơn."
        );
    }

    // ===== Getter =====

    public String getCode() {
        return code;
    }
}
