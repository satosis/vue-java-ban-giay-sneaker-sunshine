package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.request.VoucherSearchRequest;
import com.example.duantotnghiep.dto.response.PaginationDTO;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.mapper.PaginationMapper;
import com.example.duantotnghiep.mapper.VoucherMapper;
import com.example.duantotnghiep.model.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class VoucherSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PaginationMapper paginationMapper;

    @Autowired
    private VoucherMapper voucherMapper;

    public PaginationDTO<VoucherResponse> searchVouchers(VoucherSearchRequest request, Pageable pageable) {
        String baseSql = "FROM Voucher v WHERE 1=1";
        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        // Keyword filter
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().trim().toLowerCase() + "%";

            // Tìm các productId có tên gần giống keyword
            List<Long> productIds = entityManager.createQuery(
                            "SELECT p.id FROM Product p WHERE LOWER(p.productName) LIKE :kw", Long.class)
                    .setParameter("kw", keyword)
                    .getResultList();

            if (!productIds.isEmpty()) {
                where.append(" AND (LOWER(v.voucherName) LIKE :kw OR LOWER(v.voucherCode) LIKE :kw OR v.product.id IN :productIds)");
                params.put("kw", keyword);
                params.put("productIds", productIds);
            } else {
                where.append(" AND (LOWER(v.voucherName) LIKE :kw OR LOWER(v.voucherCode) LIKE :kw)");
                params.put("kw", keyword);
            }
        }

        where.append(" AND v.customer IS NULL");
        if (request.getStatus() == null) {
            where.append(" AND v.status IN (1, 2)");
        } else {
            where.append(" AND v.status = :status");
            params.put("status", request.getStatus());
        }

        if (request.getOrderType() != null) {
            where.append(" AND v.orderType = :orderType");
            params.put("orderType", request.getOrderType());
        }

        // Voucher type
        if (request.getVoucherType() != null) {
            where.append(" AND v.voucherType = :voucherType");
            params.put("voucherType", request.getVoucherType());
        }

        // Category
        if (request.getCategoryId() != null) {
            where.append(" AND (v.category.id IS NULL OR v.category.id = :catId)");
            params.put("catId", request.getCategoryId());
        }

        // Main query with pagination
        String dataQuery = "SELECT v " + baseSql + where + " ORDER BY v.id DESC";
        TypedQuery<Voucher> vm = entityManager.createQuery(dataQuery, Voucher.class);
        params.forEach(vm::setParameter);
        if (pageable.isPaged()) {
            vm.setFirstResult((int) pageable.getOffset());
            vm.setMaxResults(pageable.getPageSize());
        }
        List<Voucher> vouchers = vm.getResultList();

        // Count total
        String countQuery = "SELECT COUNT(v) " + baseSql + where;
        TypedQuery<Long> cm = entityManager.createQuery(countQuery, Long.class);
        params.forEach(cm::setParameter);
        Long total = cm.getSingleResult();

        // Map to response
        List<VoucherResponse> responses = vouchers.stream()
                .map(voucherMapper::toDto)
                .collect(Collectors.toList());

        return paginationMapper.toPaginationDTO(new PageImpl<>(responses, pageable, total));
    }


}

