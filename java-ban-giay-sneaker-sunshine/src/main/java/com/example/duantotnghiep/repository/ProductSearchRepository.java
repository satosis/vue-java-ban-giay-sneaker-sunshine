package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.request.ProductSearchRequest;
import com.example.duantotnghiep.dto.response.PaginationDTO;
import com.example.duantotnghiep.dto.response.ProductSearchResponse;
import com.example.duantotnghiep.mapper.PaginationMapper;
import com.example.duantotnghiep.mapper.ProductMapper;
import com.example.duantotnghiep.model.DiscountCampaignProduct;
import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductCategory;
import com.example.duantotnghiep.model.ProductDetail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class ProductSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PaginationMapper paginationMapper;

    public PaginationDTO<ProductSearchResponse> searchProducts(ProductSearchRequest request, Pageable pageable) {
        String baseSql = "FROM Product p WHERE 1=1";
        StringBuilder whereClause = new StringBuilder(" AND p.status = :status");
        Map<String, Object> params = new HashMap<>();
        params.put("status", 1);

        String keyword = request.getKeyword() != null ? request.getKeyword().trim() : null;
        if (keyword != null && !keyword.isEmpty()) {
            whereClause.append(" AND (LOWER(p.productName) LIKE LOWER(:keyword) OR LOWER(p.productCode) LIKE LOWER(:keyword))");
            params.put("keyword", "%" + keyword + "%");
        }

        if (request.getBrandId() != null) {
            whereClause.append(" AND (p.brand.id IS NULL OR p.brand.id = :brandId)");
            params.put("brandId", request.getBrandId());
        }
        if (request.getGenderId() != null) {
            whereClause.append(" AND (p.gender.id IS NULL OR p.gender.id = :genderId)");
            params.put("genderId", request.getGenderId());
        }
        if (request.getStyleId() != null) {
            whereClause.append(" AND (p.style.id IS NULL OR p.style.id = :styleId)");
            params.put("styleId", request.getStyleId());
        }
        if (request.getSoleId() != null) {
            whereClause.append(" AND (p.sole.id IS NULL OR p.sole.id = :soleId)");
            params.put("soleId", request.getSoleId());
        }
        if (request.getMaterialId() != null) {
            whereClause.append(" AND (p.material.id IS NULL OR p.material.id = :materialId)");
            params.put("materialId", request.getMaterialId());
        }
        if (request.getCreatedFrom() != null) {
            whereClause.append(" AND (p.createdDate IS NULL OR p.createdDate >= :createdFrom)");
            params.put("createdFrom", request.getCreatedFrom());
        }
        if (request.getCreatedTo() != null) {
            whereClause.append(" AND (p.createdDate IS NULL OR p.createdDate <= :createdTo)");
            params.put("createdTo", request.getCreatedTo());
        }
        if (request.getPriceMin() != null) {
            whereClause.append(" AND (p.sellPrice IS NULL OR p.sellPrice >= :priceMin)");
            params.put("priceMin", request.getPriceMin());
        }
        if (request.getPriceMax() != null) {
            whereClause.append(" AND (p.sellPrice IS NULL OR p.sellPrice <= :priceMax)");
            params.put("priceMax", request.getPriceMax());
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            whereClause.append(" AND EXISTS (SELECT 1 FROM ProductCategory pc WHERE pc.product = p AND (pc.category.id IS NULL OR pc.category.id IN :categoryIds) AND pc.status = 1)");
            params.put("categoryIds", request.getCategoryIds());
        }

        if (request.getColorId() != null || request.getSizeId() != null) {
            whereClause.append(" AND EXISTS (SELECT 1 FROM ProductDetail pd WHERE pd.product = p AND pd.status = 1");
            if (request.getColorId() != null) {
                whereClause.append(" AND pd.color.id = :colorId");
                params.put("colorId", request.getColorId());
            }
            if (request.getSizeId() != null) {
                whereClause.append(" AND pd.size.id = :sizeId");
                params.put("sizeId", request.getSizeId());
            }
            whereClause.append(")");
        }

        String dataSql = "SELECT DISTINCT p " + baseSql + whereClause + " ORDER BY p.id DESC";
        TypedQuery<Product> query = entityManager.createQuery(dataSql, Product.class);
        params.forEach(query::setParameter);

        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<Product> products = query.getResultList();

        final Map<Long, Long> productToMaxDiscountCampaignMap;

        if (!products.isEmpty()) {
            Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());

            // Lấy ProductCategory
            String productCategorySql = "SELECT pc FROM ProductCategory pc LEFT JOIN FETCH pc.category WHERE pc.product.id IN :productIds AND pc.status = 1";
            List<ProductCategory> productCategories = entityManager.createQuery(productCategorySql, ProductCategory.class)
                    .setParameter("productIds", productIds)
                    .getResultList();
            Map<Long, List<ProductCategory>> categoryMap = productCategories.stream()
                    .collect(Collectors.groupingBy(pc -> pc.getProduct().getId()));

            // Lấy ProductDetail
            StringBuilder productDetailSql = new StringBuilder("SELECT pd FROM ProductDetail pd LEFT JOIN FETCH pd.color LEFT JOIN FETCH pd.size WHERE pd.product.id IN :productIds AND pd.status = 1");
            if (request.getColorId() != null) {
                productDetailSql.append(" AND pd.color.id = :colorId");
            }
            if (request.getSizeId() != null) {
                productDetailSql.append(" AND pd.size.id = :sizeId");
            }

            TypedQuery<ProductDetail> detailQuery = entityManager.createQuery(productDetailSql.toString(), ProductDetail.class);
            detailQuery.setParameter("productIds", productIds);
            if (request.getColorId() != null) {
                detailQuery.setParameter("colorId", request.getColorId());
            }
            if (request.getSizeId() != null) {
                detailQuery.setParameter("sizeId", request.getSizeId());
            }
            List<ProductDetail> productDetails = detailQuery.getResultList();
            Map<Long, List<ProductDetail>> detailMap = productDetails.stream()
                    .collect(Collectors.groupingBy(pd -> pd.getProduct().getId()));

            for (Product p : products) {
                p.setProductCategories(categoryMap.getOrDefault(p.getId(), new ArrayList<>()));
                p.setProductDetails(detailMap.getOrDefault(p.getId(), new ArrayList<>()));
            }

            // Lấy DiscountCampaign có discount lớn nhất
            String discountSql = """
            SELECT dcp FROM DiscountCampaignProduct dcp
            JOIN FETCH dcp.campaign
            WHERE dcp.product.id IN :productIds and dcp.campaign.status = 1
        """;

            List<DiscountCampaignProduct> discountList = entityManager.createQuery(discountSql, DiscountCampaignProduct.class)
                    .setParameter("productIds", productIds)
                    .getResultList();

            productToMaxDiscountCampaignMap = discountList.stream()
                    .filter(dcp -> dcp.getCampaign() != null && dcp.getCampaign().getDiscountPercentage() != null)
                    .collect(Collectors.groupingBy(
                            dcp -> dcp.getProduct().getId(),
                            Collectors.collectingAndThen(
                                    Collectors.maxBy(Comparator.comparing(dcp -> dcp.getCampaign().getDiscountPercentage())),
                                    opt -> opt.map(dcp -> dcp.getCampaign().getId()).orElse(null)
                            )
                    ));
        } else {
            productToMaxDiscountCampaignMap = Collections.emptyMap();
        }

        String countSql = "SELECT COUNT(DISTINCT p) " + baseSql + whereClause;
        TypedQuery<Long> countQuery = entityManager.createQuery(countSql, Long.class);
        params.forEach(countQuery::setParameter);
        Long total = countQuery.getSingleResult();

        List<ProductSearchResponse> productResponses = products.stream()
                .map(productMapper::toResponseSearch)
                .peek(resp -> {
                    Long discountId = productToMaxDiscountCampaignMap.get(resp.getId());
                    resp.setDiscountCampaignId(discountId);
                })
                .collect(Collectors.toList());

        Page<ProductSearchResponse> page = new PageImpl<>(productResponses, pageable, total);
        return paginationMapper.toPaginationDTO(page);
    }

    public PaginationDTO<ProductSearchResponse> getProductRemoved(ProductSearchRequest request, Pageable pageable) {
        String baseSql = "FROM Product p WHERE p.status = :status";
        Map<String, Object> params = new HashMap<>();
        params.put("status", 0);

        // Filter keyword: tìm theo productName hoặc productCode
        String keyword = request.getKeyword() != null ? request.getKeyword().trim() : null;
        if (keyword != null && !keyword.isEmpty()) {
            baseSql += " AND (LOWER(p.productName) LIKE LOWER(:keyword) OR LOWER(p.productCode) LIKE LOWER(:keyword))";
            params.put("keyword", "%" + keyword + "%");
        }

        // Filter theo khoảng giá sellPrice
        if (request.getPriceMin() != null) {
            baseSql += " AND p.sellPrice >= :priceMin";
            params.put("priceMin", request.getPriceMin());
        }
        if (request.getPriceMax() != null) {
            baseSql += " AND p.sellPrice <= :priceMax";
            params.put("priceMax", request.getPriceMax());
        }

        String dataSql = "SELECT p " + baseSql + " ORDER BY p.updatedDate DESC";
        TypedQuery<Product> query = entityManager.createQuery(dataSql, Product.class);
        params.forEach(query::setParameter);

        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<Product> products = query.getResultList();

        // Đếm tổng số sản phẩm thỏa mãn
        String countSql = "SELECT COUNT(p) " + baseSql;
        TypedQuery<Long> countQuery = entityManager.createQuery(countSql, Long.class);
        params.forEach(countQuery::setParameter);
        Long total = countQuery.getSingleResult();

        // Chuyển đổi sang DTO và trả về PaginationDTO
        List<ProductSearchResponse> productResponses = products.stream()
                .map(productMapper::toResponseSearch)
                .collect(Collectors.toList());

        Page<ProductSearchResponse> page = new PageImpl<>(productResponses, pageable, total);
        return paginationMapper.toPaginationDTO(page);
    }

    public List<ProductSearchResponse> searchProductWithoutPaging(ProductSearchRequest request) {
        Pageable pageable = Pageable.unpaged();
        List<ProductSearchResponse> result = searchProducts(request, pageable).getData();

        if (!result.isEmpty()) {
            System.out.println("Đã tìm thấy " + result.size() + " sản phẩm.");
        }

        return result;
    }

}
