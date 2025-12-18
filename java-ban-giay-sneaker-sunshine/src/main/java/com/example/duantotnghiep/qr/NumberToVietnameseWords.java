package com.example.duantotnghiep.qr;


import java.math.BigDecimal;


public class NumberToVietnameseWords {

    private static final String[] NUMBERS = {
            "không", "một", "hai", "ba", "bốn",
            "năm", "sáu", "bảy", "tám", "chín"
    };

    public static String convert(BigDecimal number) {
        if (number == null) return "";
        long n = number.longValue();
        if (n == 0) return "không";

        StringBuilder result = new StringBuilder();

        long million = n / 1_000_000;
        long thousand = (n % 1_000_000) / 1_000;
        long hundred = n % 1_000;

        if (million > 0) {
            result.append(readThreeDigits((int) million, true)).append(" triệu ");
        }
        if (thousand > 0) {
            // Nếu không có triệu thì nhóm nghìn là nhóm đầu tiên
            result.append(readThreeDigits((int) thousand, million == 0)).append(" nghìn ");
        }
        if (hundred > 0) {
            // Nếu triệu và nghìn đều bằng 0 thì nhóm trăm là nhóm đầu tiên
            boolean isFirstGroup = (million == 0 && thousand == 0);
            result.append(readThreeDigits((int) hundred, isFirstGroup));
        }

        return result.toString().trim();
    }

    private static String readThreeDigits(int number, boolean isFirstGroup) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int units = number % 10;

        StringBuilder sb = new StringBuilder();

        if (hundreds > 0) {
            sb.append(NUMBERS[hundreds]).append(" trăm ");
        } else if ((tens > 0 || units > 0) && !isFirstGroup) {
            // Nếu không có trăm mà có chục hoặc đơn vị và không phải nhóm đầu tiên, đọc "không trăm"
            sb.append("không trăm ");
        }

        if (tens > 1) {
            sb.append(NUMBERS[tens]).append(" mươi ");
            if (units == 1) sb.append("mốt ");
            else if (units == 5) sb.append("lăm ");
            else if (units > 0) sb.append(NUMBERS[units]).append(" ");
        } else if (tens == 1) {
            sb.append("mười ");
            if (units == 1) sb.append("một ");
            else if (units == 5) sb.append("lăm ");
            else if (units > 0) sb.append(NUMBERS[units]).append(" ");
        } else if (tens == 0 && units > 0) {
            if (hundreds > 0) sb.append("lẻ ");
            if (units == 5) sb.append("năm ");
            else sb.append(NUMBERS[units]).append(" ");
        }

        return sb.toString().trim();
    }
}


