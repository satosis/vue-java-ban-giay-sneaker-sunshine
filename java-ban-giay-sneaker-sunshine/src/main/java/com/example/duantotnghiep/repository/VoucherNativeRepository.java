package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.VoucherStatusDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;

@Repository
public class VoucherNativeRepository {
    @PersistenceContext
    private EntityManager em;

    public VoucherStatusDTO findStatusByVoucherId(Long voucherId, Date start, Date end) {
        String sql = """
            SELECT
              v.id AS voucher_id,
              v.voucher_code AS voucher_code,
              v.voucher_name AS voucher_name,
              COALESCE(SUM(vh.discount_value_applied), 0) AS total_discount_given,
              COALESCE(SUM(CASE WHEN vh.used_at >= ?2 AND vh.used_at < ?3 THEN COALESCE(vh.discount_value_applied,0) ELSE 0 END), 0) AS discount_today,
              COALESCE(SUM(CASE WHEN vh.used_at IS NOT NULL THEN 1 ELSE 0 END), 0) AS total_uses,
              COALESCE(SUM(CASE WHEN vh.used_at >= ?2 AND vh.used_at < ?3 THEN 1 ELSE 0 END), 0) AS uses_today
            FROM voucher v
            LEFT JOIN voucher_history vh ON vh.voucher_id = v.id
            WHERE v.id = ?1
            GROUP BY v.id, v.voucher_code, v.voucher_name
            """;

        // convert Date -> Timestamp for DB driver / native query stability
        Timestamp startTs = new Timestamp(start.getTime());
        Timestamp endTs   = new Timestamp(end.getTime());

        Query q = em.createNativeQuery(sql)
                .setParameter(1, voucherId)
                .setParameter(2, startTs)
                .setParameter(3, endTs);

        Object single = q.getSingleResult();
        if (single == null) return null;

        Object[] row = (Object[]) single;

        Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
        String code = row[1] != null ? row[1].toString() : null;
        String name = row[2] != null ? row[2].toString() : null;

        BigDecimal totalDiscount = toBigDecimal(row[3]);
        BigDecimal discountToday = toBigDecimal(row[4]);
        Long totalUses = toLong(row[5]);
        Long usesToday = toLong(row[6]);

        return new VoucherStatusDTO(
                id,
                code,
                name,
                totalDiscount != null ? totalDiscount : BigDecimal.ZERO,
                discountToday != null ? discountToday : BigDecimal.ZERO,
                totalUses != null ? totalUses : 0L,
                usesToday != null ? usesToday : 0L
        );
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof BigInteger) return ((BigInteger) o).longValue();
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
