package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.DiscountCampaignProductDetailRequest;
import com.example.duantotnghiep.dto.request.DiscountCampaignProductRequest;
import com.example.duantotnghiep.dto.request.DiscountCampaignRequest;
import com.example.duantotnghiep.dto.response.DiscountCampaignProductDetailResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignProductResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignStatisticsResponse;
import com.example.duantotnghiep.mapper.DiscountCampaignMapper;
import com.example.duantotnghiep.model.DiscountCampaign;
import com.example.duantotnghiep.model.DiscountCampaignProduct;
import com.example.duantotnghiep.model.DiscountCampaignProductDetail;
import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductDetail;
import com.example.duantotnghiep.repository.DiscountCampaignProductRepository;
import com.example.duantotnghiep.repository.DiscountCampaignRepository;
import com.example.duantotnghiep.repository.InvoiceRepository;
import com.example.duantotnghiep.repository.ProductDetailRepository;
import com.example.duantotnghiep.repository.ProductRepository;
import com.example.duantotnghiep.service.DiscountCampaignService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountCampaignServiceImpl implements DiscountCampaignService {

    // Repos & Mapper
    private final DiscountCampaignRepository repository; // có sẵn trong code cũ
    private final DiscountCampaignMapper discountCampaignMapper;
    private final DiscountCampaignRepository discountCampaignRepository; // giữ nguyên như code cũ đang có
    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final DiscountCampaignProductRepository discountCampaignProductRepository;

    // ===== Cấu hình validate trong service =====
    private static final int MAX_CAMPAIGN_DAYS = 180;
    private static final int MIN_PERCENT = 1;
    private static final int MAX_PERCENT = 99;

    // ================== Public APIs ==================

    @Override
    public Page<DiscountCampaignResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return repository.findActiveCampaigns(pageable)
                .map(discountCampaignMapper::toResponse);
    }


    @Override
    @Transactional
    public DiscountCampaignResponse getDetail(Long id) {
        // 1) Lấy campaign + fetch đầy đủ
        DiscountCampaign campaign = repository.findDetailGraph(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt giảm giá với ID: " + id));

        // 2) Map phần thông tin chung
        DiscountCampaignResponse res = discountCampaignMapper.toResponse(campaign);

        // 3) Map danh sách SPCT (giữ nguyên như bạn đang có)
        BigDecimal globalPercent = campaign.getDiscountPercentage();

        List<DiscountCampaignProductDetailResponse> items =
                campaign.getProductDetails() == null ? List.of() :
                        campaign.getProductDetails().stream().map(link -> {
                            ProductDetail pd = link.getProductDetail();

                            DiscountCampaignProductDetailResponse dto = new DiscountCampaignProductDetailResponse();
                            dto.setId(link.getId());
                            if (pd != null) {
                                var product = pd.getProduct();
                                var color   = pd.getColor();
                                var size    = pd.getSize();

                                dto.setProductDetailId(pd.getId());
                                dto.setProductName(product != null ? product.getProductName() : null);
                                dto.setProductCode(product != null ? product.getProductCode() : null);
                                dto.setColorName(color != null
                                        ? (color.getColorName() != null ? color.getColorName() : color.getColorCode())
                                        : null);
                                dto.setSizeName(size != null
                                        ? (size.getSizeName() != null ? size.getSizeName() : size.getSizeCode())
                                        : null);
                                dto.setSellPrice(pd.getSellPrice());
                                dto.setDiscountPercent(link.getDiscountPercentage() != null
                                        ? link.getDiscountPercentage()
                                        : globalPercent);
                                Integer q = pd.getQuantity();
                                dto.setStock(q != null ? q.longValue() : null);
                            } else {
                                // fallback nếu dữ liệu lỗi
                                dto.setDiscountPercent(link.getDiscountPercentage() != null
                                        ? link.getDiscountPercentage()
                                        : globalPercent);
                            }
                            return dto;
                        }).toList();

        res.setProductDetails(items);

        // 4) Map lại danh sách products để có thêm productCode
        List<DiscountCampaignProductResponse> productSummaries =
                campaign.getProducts() == null ? List.of() :
                        campaign.getProducts().stream().map(cp -> {
                            DiscountCampaignProductResponse pDto = new DiscountCampaignProductResponse();
                            pDto.setId(cp.getId());
                            if (cp.getProduct() != null) {
                                pDto.setProductId(cp.getProduct().getId());
                                pDto.setProductName(cp.getProduct().getProductName());
                                pDto.setProductCode(cp.getProduct().getProductCode()); // <<-- gán mã SP
                            }
                            return pDto;
                        }).toList();

        res.setProducts(productSummaries);

        return res;
    }

    @Transactional
    @Override
    public void delete(Long id) {
        DiscountCampaign campaign = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt giảm giá với ID: " + id));

        if (campaign.getStatus() != null && campaign.getStatus() == 2) {
            throw new IllegalStateException("Đợt giảm giá này đã bị vô hiệu hoá trước đó.");
        }
        campaign.setStatus(2); // vô hiệu hoá
        campaign.setUpdatedDate(LocalDateTime.now());
        repository.save(campaign);
    }

    @Transactional
    @Override
    public DiscountCampaignResponse createDiscountCampaign(DiscountCampaignRequest request) {
        // 1) Validate cơ bản & phần trăm
        validateBasic(request);


        List<DiscountCampaign> optional = discountCampaignRepository.findConflictCampaign(request.getStartDate(),request.getEndDate(),null);
        if (!optional.isEmpty()) {
            throw new IllegalArgumentException("Khoảng thời gian đã bị trùng với campaign khác");
        }

            // 2) Code unique
        String campaignCode = generateOrUseCampaignCode(request.getCampaignCode());
        ensureCampaignCodeUniqueOrThrow(campaignCode, null);

        // 4) Bắt buộc >= 1 product
        List<Long> productIds = distinctValidIds(request.getProducts(), DiscountCampaignProductRequest::getProductId);
        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm cho đợt giảm giá.");
        }
        Map<Long, Product> productMap = loadAndValidateProducts(productIds);

        // 5) Map entity
        LocalDateTime now = LocalDateTime.now();
        DiscountCampaign campaign = new DiscountCampaign();
        campaign.setCampaignCode(campaignCode);
        campaign.setName(request.getName().trim());
        campaign.setDescription(trimOrNull(request.getDescription()));
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());

        // === NEW: tính status theo thời gian bắt đầu ===
        Integer initialStatus;
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian bắt đầu (startDate).");
        } else if (request.getStartDate().isAfter(now)) {
            initialStatus = 0; // CHỜ BẮT ĐẦU
        } else {
            initialStatus = 1; // ĐANG CHẠY
        }
        // Nếu client truyền status thì ưu tiên luật thời gian (khuyến nghị):
        campaign.setStatus(initialStatus);

        campaign.setDiscountPercentage(normalizePercentageOrNull(request.getDiscountPercentage()));
        campaign.setCreatedDate(now);
        campaign.setUpdatedDate(now);

        // 6) Unique % theo ngày tạo (chỉ áp dụng global % != null, bỏ qua status=2)
//        ensureUniquePercentPerCreatedDayOrThrow(now.toLocalDate(), campaign.getDiscountPercentage(), null);

        // 7) Link products
        List<DiscountCampaignProduct> productLinks = productIds.stream().map(pid -> {
            DiscountCampaignProduct link = new DiscountCampaignProduct();
            link.setCampaign(campaign);
            link.setProduct(productMap.get(pid));
            link.setCreatedDate(now);
            link.setUpdatedDate(now);
            return link;
        }).toList();
        campaign.setProducts(productLinks);

        // 8) SPCT (nếu có) phải thuộc tập products
        List<Long> pdIds = distinctValidIds(request.getProductDetails(), DiscountCampaignProductDetailRequest::getProductDetailId);
        if (!pdIds.isEmpty()) {
            Map<Long, ProductDetail> pdMap = loadAndValidateProductDetails(pdIds);
            ensureProductDetailsBelongToProductsOrThrow(pdIds, pdMap, productMap.keySet());

            List<DiscountCampaignProductDetail> items = new ArrayList<>();
            for (DiscountCampaignProductDetailRequest dReq : safeList(request.getProductDetails())) {
                if (dReq == null || dReq.getProductDetailId() == null) continue;
                DiscountCampaignProductDetail it = new DiscountCampaignProductDetail();
                it.setCampaign(campaign);
                it.setProductDetail(pdMap.get(dReq.getProductDetailId()));
                it.setDiscountPercentage(normalizePercentageOrNull(dReq.getDiscountPercentage())); // nullable nếu dùng global
                it.setCreatedDate(Timestamp.valueOf(now));
                it.setUpdatedDate(Timestamp.valueOf(now));
                items.add(it);
            }
            campaign.setProductDetails(items);
        }

        DiscountCampaign saved = discountCampaignRepository.save(campaign);
        return discountCampaignMapper.toResponse(saved);
    }


    @Transactional
    @Override
    public DiscountCampaignResponse updateDiscountCampaign(Long id, DiscountCampaignRequest request) {
        DiscountCampaign current = discountCampaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt giảm giá với ID: " + id));

        List<DiscountCampaign> optional = discountCampaignRepository.findConflictCampaign(request.getStartDate(),request.getEndDate(),id);
        if (!optional.isEmpty()) {
            throw new IllegalArgumentException("Khoảng thời gian đã bị trùng với campaign khác");
        }
        // --- Lấy dữ liệu mới (nếu null thì giữ nguyên) ---
        String newCode = isBlank(request.getCampaignCode()) ? current.getCampaignCode() : request.getCampaignCode().trim();
        String newName = isBlank(request.getName()) ? current.getName() : request.getName().trim();
        String newDesc = request.getDescription() == null ? current.getDescription() : trimOrNull(request.getDescription());

        LocalDateTime newStartDt = request.getStartDate() == null ? current.getStartDate() : request.getStartDate();
        LocalDateTime newEndDt   = request.getEndDate()   == null ? current.getEndDate()   : request.getEndDate();
        BigDecimal newGlobalPercent = request.getDiscountPercentage() == null
                ? current.getDiscountPercentage() : request.getDiscountPercentage();

        // --- Dựng request ảo để validate đầy đủ ---
        DiscountCampaignRequest virtual = new DiscountCampaignRequest();
        virtual.setCampaignCode(newCode);
        virtual.setName(newName);
        virtual.setDescription(newDesc);
        virtual.setStartDate(newStartDt);
        virtual.setEndDate(newEndDt);
        virtual.setDiscountPercentage(newGlobalPercent);
        virtual.setProducts(request.getProducts());       // null = giữ nguyên
        virtual.setProductDetails(request.getProductDetails());

        // Validate cơ bản & phần trăm (nên dùng LocalDateTime trong validateBasic)
        validateBasic(virtual);

        if (!equalsIgnoreCaseSafe(current.getCampaignCode(), newCode)) {
            ensureCampaignCodeUniqueOrThrow(newCode, id);
        }

        // --- Áp dụng scalar ---
        current.setCampaignCode(newCode);
        current.setName(newName);
        current.setDescription(newDesc);
        current.setStartDate(newStartDt);
        current.setEndDate(newEndDt);
        current.setDiscountPercentage(normalizePercentageOrNull(newGlobalPercent));

        // ========== NEW: Tự động tính status như create ==========
        ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        int computedStatus = newStartDt.isAfter(now) ? 0 : 1; // 0: CHỜ BẮT ĐẦU, 1: ĐANG CHẠY
        // (Tùy chọn) Nếu muốn cập nhật ngay thành hết hạn khi end < now:
        // if (newEndDt.isBefore(now)) computedStatus = 2;

        current.setStatus(computedStatus);
        // =========================================================

        current.setUpdatedDate(now);

        // Unique % theo ngày tạo của bản ghi hiện tại
        LocalDate createdDayOfCurrent = (current.getCreatedDate() != null)
                ? current.getCreatedDate().toLocalDate()
                : now.toLocalDate(); // fallback an toàn
//        ensureUniquePercentPerCreatedDayOrThrow(createdDayOfCurrent, current.getDiscountPercentage(), current.getId());

        // --- Products ---
        List<Long> productIds;
        Map<Long, Product> productMap;
        if (virtual.getProducts() != null) {
            productIds = distinctValidIds(virtual.getProducts(), DiscountCampaignProductRequest::getProductId);
            if (productIds.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm cho đợt giảm giá.");
            productMap = loadAndValidateProducts(productIds);

            if (current.getProducts() == null) current.setProducts(new ArrayList<>());
            Set<Long> existing = current.getProducts().stream().map(l -> l.getProduct().getId()).collect(Collectors.toSet());

            current.getProducts().removeIf(link -> !productIds.contains(link.getProduct().getId()));
            for (Long pid : productIds) {
                if (!existing.contains(pid)) {
                    DiscountCampaignProduct link = new DiscountCampaignProduct();
                    link.setCampaign(current);
                    link.setProduct(productMap.get(pid));
                    link.setCreatedDate(now);
                    link.setUpdatedDate(now);
                    current.getProducts().add(link);
                }
            }
            current.getProducts().forEach(l -> l.setUpdatedDate(now));
        } else {
            productIds = current.getProducts() == null ? Collections.emptyList()
                    : current.getProducts().stream().map(l -> l.getProduct().getId()).toList();
            productMap = current.getProducts() == null ? Collections.emptyMap()
                    : current.getProducts().stream().map(DiscountCampaignProduct::getProduct)
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            if (productIds.isEmpty()) throw new IllegalArgumentException("Đợt giảm giá phải có ít nhất 1 sản phẩm.");
        }

        // --- ProductDetails ---
        if (virtual.getProductDetails() != null) {
            LinkedHashMap<Long, BigDecimal> incoming = new LinkedHashMap<>();
            for (DiscountCampaignProductDetailRequest dReq : safeList(virtual.getProductDetails())) {
                if (dReq == null || dReq.getProductDetailId() == null) continue;
                incoming.put(dReq.getProductDetailId(), normalizePercentageOrNull(dReq.getDiscountPercentage()));
            }

            if (current.getProductDetails() == null) current.setProductDetails(new ArrayList<>());
            Map<Long, DiscountCampaignProductDetail> existingMap =
                    current.getProductDetails().stream().collect(Collectors.toMap(d -> d.getProductDetail().getId(), d -> d));

            if (incoming.isEmpty()) {
                current.getProductDetails().clear(); // orphanRemoval=true sẽ xoá DB
            } else {
                List<Long> newPdIds = new ArrayList<>(incoming.keySet());
                Map<Long, ProductDetail> pdMap = loadAndValidateProductDetails(newPdIds);
                ensureProductDetailsBelongToProductsOrThrow(newPdIds, pdMap, productMap.keySet());

                current.getProductDetails().removeIf(link -> !incoming.containsKey(link.getProductDetail().getId()));
                for (Map.Entry<Long, BigDecimal> e : incoming.entrySet()) {
                    Long pdId = e.getKey();
                    BigDecimal perItem = e.getValue();
                    DiscountCampaignProductDetail exist = existingMap.get(pdId);
                    if (exist != null) {
                        exist.setDiscountPercentage(perItem);
                        exist.setUpdatedDate(java.sql.Timestamp.valueOf(now));
                    } else {
                        DiscountCampaignProductDetail it = new DiscountCampaignProductDetail();
                        it.setCampaign(current);
                        it.setProductDetail(pdMap.get(pdId));
                        it.setDiscountPercentage(perItem);
                        it.setCreatedDate(java.sql.Timestamp.valueOf(now));
                        it.setUpdatedDate(java.sql.Timestamp.valueOf(now));
                        current.getProductDetails().add(it);
                    }
                }
            }
        } else {
            List<Long> keepPdIds = current.getProductDetails() == null ? Collections.emptyList()
                    : current.getProductDetails().stream().map(d -> d.getProductDetail().getId()).toList();
            if (!keepPdIds.isEmpty()) {
                Map<Long, ProductDetail> keepMap = productDetailRepository.findAllById(keepPdIds)
                        .stream().collect(Collectors.toMap(ProductDetail::getId, it -> it));
                ensureProductDetailsBelongToProductsOrThrow(keepPdIds, keepMap, productMap.keySet());
            }
        }

        current.setUpdatedDate(now);
        DiscountCampaign saved = discountCampaignRepository.save(current);
        return discountCampaignMapper.toResponse(saved);
    }


    @Override
    public DiscountCampaignStatisticsResponse getStatistics(Long campaignId) {
        return invoiceRepository.getStatisticsByCampaignId(campaignId);
    }

    @Override
    public Page<DiscountCampaignResponse> search(String keyword, Integer status, LocalDate createdDate, Pageable pageable) {
        LocalDateTime start = null, end = null;
        if (createdDate != null) {
            start = createdDate.atStartOfDay();
            end = createdDate.plusDays(1).atStartOfDay().minusNanos(1);
        }
        return discountCampaignRepository.searchByKeywordStatusCreatedDate(keyword, status, start, end, pageable);
    }

    // ================== VALIDATION & UTILITIES ==================

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private void validateBasic(DiscountCampaignRequest req) {
        if (req == null) throw new IllegalArgumentException("Yêu cầu không hợp lệ (request null).");

        if (isBlank(req.getName())) {
            throw new IllegalArgumentException("Tên đợt giảm giá không được để trống.");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
        }

        // Dùng LocalDateTime để so sánh theo 'thời điểm'
        LocalDateTime start = req.getStartDate();
        LocalDateTime end   = req.getEndDate();
        LocalDateTime now   = LocalDateTime.now(APP_ZONE);

        // Kết thúc phải sau bắt đầu (nếu cho phép bằng nhau thì đổi thành isBefore)
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
        }

        // Thời lượng theo ngày (tính inclusive theo ngày như cũ)
        long days = Duration.between(
                start.toLocalDate().atStartOfDay(),
                end.toLocalDate().plusDays(1).atStartOfDay()
        ).toDays();
        if (days > MAX_CAMPAIGN_DAYS) {
            throw new IllegalArgumentException("Thời lượng đợt giảm giá vượt quá " + MAX_CAMPAIGN_DAYS + " ngày.");
        }

        // Không cho start ở quá khứ theo 'thời điểm' (khắc phục case 00:00 hôm nay < 09:29)
        if (start.isBefore(now)) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải lớn hơn thời điểm hiện tại.");
        }

        // Không cho end ở quá khứ (thường thừa nếu đã check start>=now và end>start, nhưng để chặt chẽ)
        if (end.isBefore(now)) {
            throw new IllegalArgumentException("Thời gian kết thúc không được ở trong quá khứ.");
        }

        // % global (nếu có)
        if (req.getDiscountPercentage() != null) {
            BigDecimal v = normalizePercentageOrNull(req.getDiscountPercentage());
            ensurePercentValidOrThrow(v, "Toàn chiến dịch");
        }
        // % per-item (nếu có)
        for (DiscountCampaignProductDetailRequest d : safeList(req.getProductDetails())) {
            if (d != null && d.getDiscountPercentage() != null) {
                BigDecimal pv = normalizePercentageOrNull(d.getDiscountPercentage());
                ensurePercentValidOrThrow(pv, "Theo SPCT");
            }
        }
    }

    /** Trong cùng ngày tạo, chỉ cho phép duy nhất 1 campaign có cùng % toàn chiến dịch (bỏ qua status==2). */
//    private void ensureUniquePercentPerCreatedDayOrThrow(LocalDate createdDay, BigDecimal percent, Long excludeId) {
//        if (percent == null) return; // chỉ kiểm tra cho % toàn chiến dịch
//        BigDecimal p = percent.setScale(2, RoundingMode.HALF_UP);
//
//        List<DiscountCampaign> all = discountCampaignRepository.findAll();
//        boolean duplicated = all.stream().anyMatch(c -> {
//            if (c.getStatus() != null && c.getStatus() == 2) return false; // bỏ qua đã vô hiệu hoá
//            if (c.getCreatedDate() == null) return false;
//            LocalDate d = c.getCreatedDate().toLocalDate();
//            BigDecimal cp = c.getDiscountPercentage();
//            boolean sameDay = createdDay.equals(d);
//            boolean samePercent = (cp != null && cp.setScale(2, RoundingMode.HALF_UP).compareTo(p) == 0);
//            boolean notSelf = (excludeId == null || !Objects.equals(c.getId(), excludeId));
//            return sameDay && samePercent && notSelf;
//        });
//
//        if (duplicated) {
//            throw new IllegalArgumentException(
//                    "Trong ngày " + createdDay + " đã tồn tại một đợt giảm giá với cùng phần trăm: "
//                            + p.stripTrailingZeros().toPlainString() + "%."
//            );
//        }
//    }

//    private void ensureDiscountModeConsistencyOrThrow(DiscountCampaignRequest req) {
//        boolean hasGlobal = req.getDiscountPercentage() != null;
//        boolean hasAnyItem = safeList(req.getProductDetails())
//                .stream().anyMatch(d -> d != null && d.getDiscountPercentage() != null);
//        if (hasGlobal && hasAnyItem) {
//            throw new IllegalArgumentException("Chỉ được chọn một hình thức giảm giá: hoặc % toàn chiến dịch, hoặc % theo từng SPCT (không được đồng thời).");
//        }
//    }

    private void ensureCampaignCodeUniqueOrThrow(String code, Long excludeId) {
        // Không thêm repo method mới: duyệt findAll và so sánh ignore-case
        List<DiscountCampaign> all = discountCampaignRepository.findAll();
        boolean duplicated = all.stream().anyMatch(c ->
                c.getCampaignCode() != null
                        && c.getCampaignCode().equalsIgnoreCase(code)
                        && (excludeId == null || !Objects.equals(c.getId(), excludeId)));
        if (duplicated) throw new IllegalArgumentException("Mã chiến dịch đã tồn tại: " + code);
    }

    private Map<Long, Product> loadAndValidateProducts(List<Long> ids) {
        Map<Long, Product> map = productRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(Product::getId, it -> it));
        if (map.size() != ids.size()) {
            List<Long> notFound = ids.stream().filter(id -> !map.containsKey(id)).toList();
            throw new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + notFound);
        }
        List<Long> inactive = map.values().stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 0)
                .map(Product::getId).toList();
        if (!inactive.isEmpty()) {
            throw new IllegalArgumentException("Một số sản phẩm đang không hoạt động: " + inactive);
        }
        return map;
    }

    private Map<Long, ProductDetail> loadAndValidateProductDetails(List<Long> ids) {
        Map<Long, ProductDetail> map = productDetailRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(ProductDetail::getId, it -> it));
        if (map.size() != ids.size()) {
            List<Long> notFound = ids.stream().filter(id -> !map.containsKey(id)).toList();
            throw new IllegalArgumentException("Không tìm thấy sản phẩm chi tiết (SPCT) với ID: " + notFound);
        }
        List<Long> inactive = map.values().stream()
                .filter(d -> d.getStatus() != null && d.getStatus() == 0)
                .map(ProductDetail::getId).toList();
        if (!inactive.isEmpty()) {
            throw new IllegalArgumentException("Một số SPCT đang không hoạt động: " + inactive);
        }
        return map;
    }

    private void ensureProductDetailsBelongToProductsOrThrow(
            List<Long> pdIds, Map<Long, ProductDetail> pdMap, Set<Long> allowedProductIds
    ) {
        List<Long> violated = new ArrayList<>();
        for (Long pdId : pdIds) {
            ProductDetail pd = pdMap.get(pdId);
            Long productIdOfPd = (pd.getProduct() != null ? pd.getProduct().getId() : null);
            if (productIdOfPd == null || !allowedProductIds.contains(productIdOfPd)) {
                violated.add(pdId);
            }
        }
        if (!violated.isEmpty()) {
            throw new IllegalArgumentException(
                    "Các SPCT sau không thuộc các sản phẩm đã chọn trong chiến dịch: " + violated
                            + ". Vui lòng thêm đúng Sản phẩm trước khi chọn SPCT."
            );
        }
    }

    // ================== Utilities ==================

    private String generateOrUseCampaignCode(String input) {
        if (isBlank(input)) {
            return "CAMPAIGN_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "_" + ((int) (Math.random() * 9000) + 1000);
        }
        return input.trim();
    }

    /** Chuẩn hoá %: scale=2; chỉ cho phép trong [1..99]; null => null */
    private BigDecimal normalizePercentageOrNull(BigDecimal percent) {
        if (percent == null) return null;
        BigDecimal p = percent.setScale(2, RoundingMode.HALF_UP);
        if (p.compareTo(BigDecimal.valueOf(MIN_PERCENT)) < 0
                || p.compareTo(BigDecimal.valueOf(MAX_PERCENT)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm chỉ cho phép từ " + MIN_PERCENT + "% đến " + MAX_PERCENT + "%.");
        }
        return p;
    }

    /** Kiểm tra %: chỉ cho phép [1..99] */
    private void ensurePercentValidOrThrow(BigDecimal v, String where) {
        if (v == null) return;
        if (v.compareTo(BigDecimal.valueOf(MIN_PERCENT)) < 0
                || v.compareTo(BigDecimal.valueOf(MAX_PERCENT)) > 0) {
            throw new IllegalArgumentException(where + ": chỉ cho phép từ " + MIN_PERCENT + "% đến " + MAX_PERCENT + "%.");
        }
    }

    private <T> List<Long> distinctValidIds(List<T> list, Function<T, Long> extractor) {
        return safeList(list).stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList(); // nếu JDK <16 -> .collect(Collectors.toList())
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
