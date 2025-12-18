package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.ProductDetailRequest;
import com.example.duantotnghiep.dto.request.ProductImageRequest;
import com.example.duantotnghiep.dto.request.ProductRequest;
import com.example.duantotnghiep.dto.request.ProductSearchRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.mapper.CategoryMapper;
import com.example.duantotnghiep.mapper.ProductDetailMapper;
import com.example.duantotnghiep.mapper.ProductImageMapper;
import com.example.duantotnghiep.mapper.ProductMapper;
import com.example.duantotnghiep.model.*;
import com.example.duantotnghiep.repository.*;
import com.example.duantotnghiep.service.ProductService;
import com.example.duantotnghiep.xuatExcel.ProductExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryRepository categoryRepository;
    private final ProductDetailMapper productDetailMapper;
    private final CategoryMapper categoryMapper;
    private final ProductHistoryRepository productHistoryRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ColorRepository colorRepository;
    private final DiscountCampaignRepository discountCampaignRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final UserRepository userRepository;
    private final ReservationOrderRepository reservationOrderRepository;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // Validate input
        if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
            throw new IllegalArgumentException("Category IDs cannot be empty");
        }

        Product product = productMapper.toEntity(request);
        product.setProductCode(generateProductCode());
        product.setStatus(1);
        product.setCreatedDate(new Date());
        product.setUpdatedBy("admin");
        product.setCreatedBy("admin");

        Product savedProduct = productRepository.save(product);

        if (request.getProductImages() != null && !request.getProductImages().isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();
            for (ProductImageRequest imageRequest : request.getProductImages()){
                if(imageRequest.getColorId() == null || imageRequest.getProductImages() == null || imageRequest.getProductImages().isEmpty()) continue;
                Color color = colorRepository.findById(imageRequest.getColorId())
                        .orElseThrow(() -> new IllegalArgumentException("Color not found: " + imageRequest.getColorId()));
                try {
                    ProductImage image = new ProductImage();
                    image.setImage(imageRequest.getProductImages().getBytes());
                    image.setImageName(imageRequest.getProductImages().getOriginalFilename());
                    image.setStatus(1);
                    image.setCreatedDate(new Date());
                    image.setCreatedBy("admin");
                    image.setUpdatedBy("admin");
                    image.setColor(color);
                    image.setProduct(savedProduct);
                    productImages.add(image);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
                }
            }
            if (!productImages.isEmpty()) {
                productImageRepository.saveAll(productImages);
            }

        }

        if (request.getProductDetails() != null && !request.getProductDetails().isEmpty()) {
            List<ProductDetail> details = productDetailMapper.mapProductDetailRequests(request.getProductDetails());
            details.forEach(detail -> {
                detail.setProduct(savedProduct);
                detail.setProductDetailCode(generateProductDetailCode());
                detail.setStatus(1);
                detail.setCreatedBy("admin");
                detail.setCreatedDate(new Date());
                detail.setUpdatedBy("admin");
                detail.setUpdatedDate(new Date());
            });
            productDetailRepository.saveAll(details);
            savedProduct.setProductDetails(details);
        }

        // Handle categories
        List<Category> categories = categoryRepository.findAllByIdInAndStatus(request.getCategoryIds(), 1);
        if (categories.size() != request.getCategoryIds().size()) {
            throw new IllegalArgumentException("Invalid or inactive categories provided");
        }

        List<ProductCategory> productCategories = categories.stream().map(category -> {
            ProductCategory pc = new ProductCategory();
            pc.setId(new ProductCategoryId(savedProduct.getId(), category.getId()));
            pc.setProduct(savedProduct);
            pc.setCategory(category);
            pc.setStatus(1);
            pc.setCreatedBy("admin");
            pc.setCreatedDate(new Date());
            pc.setUpdatedBy("admin");
            pc.setUpdatedDate(new Date());
            return pc;
        }).collect(Collectors.toList());
        productCategoryRepository.saveAll(productCategories);

        ProductResponse response = productMapper.toResponse(savedProduct);
        response.setCategories(categories.stream().map(categoryMapper::toResponse).collect(Collectors.toList()));
        response.setProductDetails(savedProduct.getProductDetails().stream()
                .map(productDetailMapper::toResponse)
                .collect(Collectors.toList()));
        return response;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existingProduct = productRepository.findByStatus(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        // Clone sản phẩm cũ để so sánh
        Product clonedOld = new Product();
        BeanUtils.copyProperties(existingProduct, clonedOld);

        // Cập nhật thông tin chung
        productMapper.updateEntityFromRequest(request, existingProduct);
        existingProduct.setUpdatedBy("admin");
        existingProduct.setUpdatedDate(new Date());
        Product updatedProduct = productRepository.save(existingProduct);

        // ---------------- ProductDetail ----------------
        List<ProductDetail> oldDetails = productDetailRepository.findByProductIdAndStatus(existingProduct.getId(), 1);
        List<ProductDetailRequest> newDetailRequests = request.getProductDetails() != null ? request.getProductDetails() : new ArrayList<>();
        List<ProductDetail> updatedDetails = new ArrayList<>();

        Set<Long> newColorIds = newDetailRequests.stream()
                .map(ProductDetailRequest::getColorId)
                .collect(Collectors.toSet());

        for (ProductDetail oldDetail : oldDetails) {
            Optional<ProductDetailRequest> matching = newDetailRequests.stream()
                    .filter(req -> Objects.equals(req.getColorId(), oldDetail.getColor()) &&
                            Objects.equals(req.getSizeId(), oldDetail.getSize()))
                    .findFirst();

            if (matching.isPresent()) {
                ProductDetailRequest req = matching.get();
                boolean changed = false;

                // Chỉ update quantity nếu thay đổi
                if (!Objects.equals(oldDetail.getQuantity(), req.getQuantity())) {
                    oldDetail.setQuantity(req.getQuantity());
                    changed = true;
                }

                if (changed) {
                    oldDetail.setUpdatedBy("admin");
                    oldDetail.setUpdatedDate(new Date());
                    productDetailRepository.save(oldDetail);
                }
                updatedDetails.add(oldDetail);
            } else {
                // Xóa mềm nếu không có trong request
                oldDetail.setStatus(0);
                oldDetail.setUpdatedBy("admin");
                oldDetail.setUpdatedDate(new Date());
                productDetailRepository.save(oldDetail);
            }
        }

        // Tạo mới ProductDetail nếu chưa có
        for (ProductDetailRequest detailRequest : newDetailRequests) {
            boolean exists = oldDetails.stream().anyMatch(old ->
                    Objects.equals(old.getColor(), detailRequest.getColorId()) &&
                            Objects.equals(old.getSize(), detailRequest.getSizeId()));
            if (!exists) {
                ProductDetail newDetail = productDetailMapper.fromRequest(detailRequest);
                newDetail.setProduct(existingProduct);
                newDetail.setStatus(1);
                newDetail.setCreatedBy("admin");
                newDetail.setCreatedDate(new Date());
                newDetail.setUpdatedBy("admin");
                newDetail.setUpdatedDate(new Date());

                // Generate code nếu chưa có
                if (newDetail.getProductDetailCode() == null) {
                    newDetail.setProductDetailCode(generateProductDetailCode());
                }

                productDetailRepository.save(newDetail);
                updatedDetails.add(newDetail);
            }
        }

        existingProduct.setProductDetails(updatedDetails);

        // ---------------- Xử lý ProductImage ----------------
        // Xử lý ảnh cũ theo oldColorIds và newColorIds
        if (request.getOldColorIds() != null && !request.getOldColorIds().isEmpty()) {
            Set<Long> oldColorIds = new HashSet<>(request.getOldColorIds());
            List<ProductImage> existingImages = productImageRepository.findByProductIdAndStatus(existingProduct.getId(), 1);

            for (ProductImage image : existingImages) {
                Long colorId = image.getColor() != null ? image.getColor().getId() : null;
                if (colorId != null && oldColorIds.contains(colorId) && !newColorIds.contains(colorId)) {
                    image.setStatus(0);
                    image.setUpdatedBy("admin");
                    image.setUpdatedDate(new Date());
                    productImageRepository.save(image);
                }
            }
        }

        // Xử lý ảnh theo oldImageIds
        if (request.getOldImageIds() != null && !request.getOldImageIds().isEmpty()) {
            List<ProductImage> allImages = productImageRepository.findAllByIdAndStatus(request.getOldImageIds(), 1);
            List<Long> missingImageIds = request.getOldImageIds().stream()
                    .filter(idImage -> allImages.stream().noneMatch(img -> img.getId().equals(idImage)))
                    .collect(Collectors.toList());
            if (!missingImageIds.isEmpty()) {
                throw new RuntimeException("Ảnh không tồn tại: " + missingImageIds);
            }
            for (ProductImage image : allImages) {
                image.setStatus(0);
                image.setUpdatedBy("admin");
                image.setUpdatedDate(new Date());
                productImageRepository.save(image);
            }
        }

        // Upload ảnh mới
        if (request.getProductImages() != null && !request.getProductImages().isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();
            for (ProductImageRequest imageRequest : request.getProductImages()) {
                if (imageRequest.getColorId() == null ||
                        imageRequest.getProductImages() == null ||
                        imageRequest.getProductImages().isEmpty()) continue;

                Color color = colorRepository.findById(imageRequest.getColorId())
                        .orElseThrow(() -> new IllegalArgumentException("Color not found: " + imageRequest.getColorId()));

                try {
                    ProductImage image = new ProductImage();
                    image.setImage(imageRequest.getProductImages().getBytes());
                    image.setImageName(imageRequest.getProductImages().getOriginalFilename());
                    image.setStatus(1);
                    image.setCreatedDate(new Date());
                    image.setCreatedBy("admin");
                    image.setUpdatedBy("admin");
                    image.setColor(color);
                    image.setProduct(existingProduct);
                    productImages.add(image);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
                }
            }
            if (!productImages.isEmpty()) {
                productImageRepository.saveAll(productImages);
            }
        }

        // ---------------- Xử lý ProductCategory ----------------
        List<Long> newCategoryIds = request.getCategoryIds();
        if (newCategoryIds != null && !newCategoryIds.isEmpty()) {
            List<ProductCategory> existingPCs = productCategoryRepository.getAllByProductAndStatus(id);
            Map<Long, ProductCategory> existingMap = existingPCs.stream()
                    .collect(Collectors.toMap(pc -> pc.getCategory().getId(), pc -> pc));

            Set<Long> newCategoryIdSet = new HashSet<>(newCategoryIds);
            List<ProductCategory> categoriesToUpdateOrCreate = new ArrayList<>();

            for (ProductCategory oldPC : existingPCs) {
                if (!newCategoryIdSet.contains(oldPC.getCategory().getId()) && oldPC.getStatus() == 1) {
                    oldPC.setStatus(0);
                    oldPC.setUpdatedBy("admin");
                    oldPC.setUpdatedDate(new Date());
                    categoriesToUpdateOrCreate.add(oldPC);
                }
            }

            for (Long categoryId : newCategoryIds) {
                if (existingMap.containsKey(categoryId)) {
                    ProductCategory existingPC = existingMap.get(categoryId);
                    if (existingPC.getStatus() == 0) {
                        existingPC.setStatus(1);
                        existingPC.setUpdatedBy("admin");
                        existingPC.setUpdatedDate(new Date());
                        categoriesToUpdateOrCreate.add(existingPC);
                    }
                } else {
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy Category với id: " + categoryId));

                    ProductCategory newPC = new ProductCategory();
                    newPC.setId(new ProductCategoryId(updatedProduct.getId(), categoryId));
                    newPC.setProduct(updatedProduct);
                    newPC.setCategory(category);
                    newPC.setStatus(1);
                    newPC.setCreatedBy("admin");
                    newPC.setCreatedDate(new Date());
                    newPC.setUpdatedBy("admin");
                    newPC.setUpdatedDate(new Date());
                    categoriesToUpdateOrCreate.add(newPC);
                }
            }

            if (!categoriesToUpdateOrCreate.isEmpty()) {
                productCategoryRepository.saveAll(categoriesToUpdateOrCreate);
            }
        }

        updatedProduct = productRepository.save(existingProduct);

        // ---------------- Lưu lịch sử thay đổi ----------------
        compareAndLogProductChanges(clonedOld, updatedProduct);

        // ---------------- Build Response ----------------
        ProductResponse response = productMapper.toResponse(updatedProduct);
        List<ProductCategory> activeProductCategories = productCategoryRepository.getAllByProductAndStatus(updatedProduct.getId());
        List<String> categoryNames = activeProductCategories.stream()
                .map(pc -> pc.getCategory().getCategoryName())
                .collect(Collectors.toList());
        response.setCategoryNames(categoryNames);

        return response;
    }

    private void compareAndLogProductChanges(Product oldProduct, Product newProduct) {
        saveHistoryIfChanged(newProduct.getId(), "Tên sản phẩm", oldProduct.getProductName(), newProduct.getProductName());
        saveHistoryIfChanged(newProduct.getId(), "Mô tả sản phẩm", oldProduct.getDescription(), newProduct.getDescription());
        saveHistoryIfChanged(newProduct.getId(), "Giá bán",oldProduct.getSellPrice(), newProduct.getSellPrice());
        saveHistoryIfChanged(newProduct.getId(), "Trọng lượng",oldProduct.getWeight(), newProduct.getWeight());

        Long oldBrandId = oldProduct.getBrand() != null ? oldProduct.getBrand().getId() : null;
        Long newBrandId = newProduct.getBrand() != null ? newProduct.getBrand().getId() : null;
        if (!Objects.equals(oldBrandId, newBrandId)) {
            String oldBrandName = oldProduct.getBrand() != null ? oldProduct.getBrand().getBrandName() : "null";
            String newBrandName = newProduct.getBrand() != null ? newProduct.getBrand().getBrandName() : "null";
            saveHistoryIfChanged(newProduct.getId(), "Thương hiệu", oldBrandName, newBrandName);
        }

        // So sánh trạng thái (Status)
        if (!Objects.equals(oldProduct.getStatus(), newProduct.getStatus())) {
            String oldStatus = oldProduct.getStatus() == 1 ? "Hoạt động" : "Không hoạt động";
            String newStatus = newProduct.getStatus() == 1 ? "Hoạt động" : "Không hoạt động";
            saveHistoryIfChanged(newProduct.getId(), "Trạng thái", oldStatus, newStatus);
        }
    }

    private <T> void saveHistoryIfChanged(Long productId, String fieldName, T oldValue, T newValue) {
        // Sử dụng Objects.equals() để so sánh an toàn, bao gồm cả trường hợp null
        String oldValueStr = String.valueOf(oldValue);
        String newValueStr = String.valueOf(newValue);
        if (!Objects.equals(oldValueStr, newValueStr)) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

            Employee employee = Optional.ofNullable(user.getEmployee())
                    .orElseThrow(() -> new RuntimeException("Người dùng không phải là nhân viên."));

            ProductHistory history = new ProductHistory();
            history.setProductId(productId);
            history.setActionType("UPDATE");
            history.setFieldName(fieldName);

            // Chuyển đổi thành chuỗi chỉ khi lưu vào database
            history.setOldValue(oldValue != null ? String.valueOf(oldValue) : "null");
            history.setNewValue(newValue != null ? String.valueOf(newValue) : "null");

            history.setEmployeeId(employee.getId());
            history.setCreatedDate(new Date());
            history.setNote("Cập nhật sản phẩm");
            productHistoryRepository.save(history);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ProductResponse restoreProduct(Long id) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        existingProduct.setStatus(1);
        if (existingProduct.getProductDetails() != null) {
            existingProduct.getProductDetails().forEach(detail -> {
                if (detail.getStatus() == 2) {
                    detail.setStatus(1);
                }
            });
        }
        productRepository.save(existingProduct);

        return productMapper.toResponse(existingProduct);
    }


    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findByStatus(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + id));

        // set status cho product
        product.setStatus(0);
        product.setUpdatedDate(new Date());
        product.setUpdatedBy("admin");

        // set status cho toàn bộ productDetail của product
        if (product.getProductDetails() != null && !product.getProductDetails().isEmpty()) {
            product.getProductDetails().forEach(detail -> {
                detail.setStatus(2);
                detail.setUpdatedDate(new Date());
                detail.setUpdatedBy("admin");
            });
        }

        productRepository.save(product); // cascade sẽ update luôn productDetail nếu đã map cascade
    }

    @Override
    public void deleteProductV2(Long id) {
        Product product = productRepository.findByStatusRemoved(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + id));

        // set status cho product
        product.setStatus(3);
        product.setUpdatedDate(new Date());
        product.setUpdatedBy("admin");

        // set status cho toàn bộ productDetail của product
        if (product.getProductDetails() != null && !product.getProductDetails().isEmpty()) {
            product.getProductDetails().forEach(detail -> {
                detail.setStatus(3);
                detail.setUpdatedDate(new Date());
                detail.setUpdatedBy("admin");
            });
        }

        productRepository.save(product); // cascade sẽ update luôn productDetail nếu đã map cascade
    }

    @Override
    public ProductResponse getProductById(Long id) {
        // 1) Lấy product + chi tiết
        Product product = productRepository.findByIdWithProductDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        // 2) Ảnh (status=1)
        List<ProductImageResponse> imageResponses = productRepository.findByIdWithProductImages(id)
                .map(Product::getProductImages)
                .orElse(List.of())
                .stream()
                .filter(pi -> pi.getStatus() == 1)
                .map(productImageMapper::toResponse)
                .collect(Collectors.toList());

        // 3) Danh mục (status=1)
        List<CategoryResponse> categoryResponses = productCategoryRepository.getAllByProductAndStatus(id).stream()
                .map(pc -> categoryMapper.toResponse(pc.getCategory()))
                .collect(Collectors.toList());

        // 4) Map sang DTO
        ProductResponse response = productMapper.toResponse(product);
        response.setProductImages(imageResponses);
        response.setCategories(categoryResponses);

        // 5) Lọc SPCT status=1
        List<ProductDetailResponse> detailResponses = product.getProductDetails().stream()
                .filter(pd -> pd.getStatus() == 1)
                .map(productDetailMapper::toResponse)
                .collect(Collectors.toList());
        response.setProductDetails(detailResponses);

        // Khuyến nghị: dùng method có fetch join nếu bạn đã có (findActiveCampaignsWithRelations)
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
        response.setDiscountPercentage((int) Math.round(productDiscount));
        response.setDiscountedPrice(calculateDiscountPrice(response.getSellPrice(), productDiscount));

        for (ProductDetailResponse d : detailResponses) {
            double detailDiscount = getBestDiscountPercentageForProductDetail(d.getId(), activeCampaigns);
            if (detailDiscount <= 0) {
                detailDiscount = productDiscount; // fallback
            }
            d.setDiscountPercentage((int) Math.round(detailDiscount));
            d.setDiscountedPrice(calculateDiscountPrice(d.getSellPrice(), detailDiscount));
        }

        return response;
    }

    @Override
    public ProductResponse getProductByIdV2(Long id) {
        // 1) Lấy product + chi tiết
        Product product = productRepository.findByIdWithProductDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        // 2) Ảnh (status=1)
        List<ProductImageResponse> imageResponses = productRepository.findByIdWithProductImages(id)
                .map(Product::getProductImages)
                .orElse(List.of())
                .stream()
                .filter(pi -> pi.getStatus() == 1)
                .map(productImageMapper::toResponse)
                .collect(Collectors.toList());

        // 3) Danh mục (status=1)
        List<CategoryResponse> categoryResponses = productCategoryRepository.getAllByProductAndStatus(id).stream()
                .map(pc -> categoryMapper.toResponse(pc.getCategory()))
                .collect(Collectors.toList());

        // 4) Map sang DTO
        ProductResponse response = productMapper.toResponse(product);
        response.setProductImages(imageResponses);
        response.setCategories(categoryResponses);

        // 5) Lọc SPCT status=1
        List<ProductDetailResponse> detailResponses = product.getProductDetails().stream()
                .filter(pd -> pd.getStatus() == 1)
                .map(productDetailMapper::toResponse)
                .collect(Collectors.toList());
        response.setProductDetails(detailResponses);

        // Khuyến nghị: dùng method có fetch join nếu bạn đã có (findActiveCampaignsWithRelations)
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
        response.setDiscountPercentage((int) Math.round(productDiscount));
        response.setDiscountedPrice(calculateDiscountPrice(response.getSellPrice(), productDiscount));

        for (ProductDetailResponse d : detailResponses) {
            double detailDiscount = getBestDiscountPercentageForProductDetail(d.getId(), activeCampaigns);
            if (detailDiscount <= 0) {
                detailDiscount = productDiscount; // fallback
            }
            d.setDiscountPercentage((int) Math.round(detailDiscount));
            d.setDiscountedPrice(calculateDiscountPrice(d.getSellPrice(), detailDiscount));

            Integer quantityReser = reservationOrderRepository.sumQuantityByProductDetailActive(d.getId());
            int availableQuantity = d.getQuantity() - (quantityReser == null ? 0 : quantityReser);

            // Nếu âm thì set = 0
            d.setQuantity(Math.max(0, availableQuantity));
        }

        return response;
    }

    @Override
    public Page<ProductDetailResponse> pageProductDetailsByProductId(
            int page, int size, Long productId, Long colorId, Long brandId) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ProductDetail> pdPage =
                productDetailRepository.pageByProductId(productId, colorId, brandId, pageable);

        return pdPage.map(productDetailMapper::toResponse);
    }


    @Override
    public Page<ProductDetailResponse> pageProductDetails(int page, int size, List<Long> productIds) {
        Pageable pageable = PageRequest.of(page, size);

        List<Long> normalizedIds = (productIds == null || productIds.isEmpty()) ? null : productIds;

        Page<ProductDetail> pdPage = productDetailRepository.pageAllActive(normalizedIds, pageable);
        return pdPage.map(productDetailMapper::toResponse);
    }


    @Override
    public List<ProductDetailResponse> getProductDetailById(Long productId) {
        List<ProductDetail> responses = productDetailRepository.findByProductIdAndStatus(productId,1);
        return responses.stream().map(productDetailMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAllWithJPQL(pageable);

        // NÊN dùng hàm fetch join (nếu bạn đã viết): findActiveCampaignsWithRelations(now)
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        List<ProductResponse> responses = products.getContent().stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            // % tốt nhất ở mức Product
            double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
            response.setDiscountPercentage((int) Math.round(productDiscount));
            response.setDiscountedPrice(calculateDiscountPrice(response.getSellPrice(), productDiscount));

            // ID campaign tốt nhất cho Product (giữ nếu bạn còn hiển thị)
            Long bestCampaignId = getBestDiscountCampaignIdForProduct(product, activeCampaigns);
            response.setDiscountCampaignId(bestCampaignId);

            // Áp dụng cho từng SPCT
            if (response.getProductDetails() != null) {
                for (ProductDetailResponse detailResponse : response.getProductDetails()) {
                    Long detailId = detailResponse.getId();

                    // Ưu tiên SPCT (nếu có % riêng sẽ dùng % riêng; nếu null thì dùng % campaign)
                    double detailDiscount = getBestDiscountPercentageForProductDetail(detailId, activeCampaigns);

                    boolean usedProductFallback = false;
                    if (detailDiscount <= 0) {
                        detailDiscount = productDiscount;
                        usedProductFallback = true;
                    }

                    detailResponse.setDiscountPercentage((int) Math.round(detailDiscount));
                    detailResponse.setDiscountedPrice(
                            calculateDiscountPrice(detailResponse.getSellPrice(), detailDiscount)
                    );

                    Integer quantityReserOrder = reservationOrderRepository.sumQuantityByProductDetailActive(detailId);
                    int availableQuantity = detailResponse.getQuantity() - (quantityReserOrder == null ? 0 : quantityReserOrder);

                    // Nếu âm thì set = 0
                    if (availableQuantity < 0) {
                        availableQuantity = 0;
                    }
                    detailResponse.setQuantity(availableQuantity);

                    if (usedProductFallback) {
                        detailResponse.setDiscountCampaignId(bestCampaignId);
                    } else {
                        detailResponse.setDiscountCampaignId(null);
                    }
                }
            }
            return response;
        }).collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }


    private Long getBestDiscountCampaignIdForProduct(Product product, List<DiscountCampaign> campaigns) {
        Long productId = product.getId();
        return campaigns.stream()
                .flatMap(campaign -> {
                    if (campaign.getProducts() == null) return Stream.<Map.Entry<Long, Double>>empty();
                    return campaign.getProducts().stream()
                            .filter(dcp -> dcp.getProduct() != null && dcp.getProduct().getId().equals(productId))
                            .map(dcp -> Map.entry(
                                    campaign.getId(),
                                    // % ở mức Product lấy từ campaign (null => 0)
                                    campaign.getDiscountPercentage() != null
                                            ? campaign.getDiscountPercentage().doubleValue()
                                            : 0.0
                            ));
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // % tốt nhất cho Product (mức Product) - không filter bỏ campaign null
    private double getBestDiscountPercentageForProduct(Product product, List<DiscountCampaign> campaigns) {
        Long productId = product.getId();
        return campaigns.stream()
                .flatMap(campaign -> {
                    if (campaign.getProducts() == null) return Stream.<Double>empty();
                    return campaign.getProducts().stream()
                            .filter(dcp -> dcp.getProduct() != null && dcp.getProduct().getId().equals(productId))
                            .map(dcp -> campaign.getDiscountPercentage() != null
                                    ? campaign.getDiscountPercentage().doubleValue()
                                    : 0.0);
                })
                .max(Double::compare)
                .orElse(0.0);
    }

    // % tốt nhất cho ProductDetail (ƯU TIÊN % SPCT; nếu null thì fallback % campaign; cả hai null => 0)
    private double getBestDiscountPercentageForProductDetail(Long productDetailId, List<DiscountCampaign> campaigns) {
        return campaigns.stream()
                .flatMap(campaign -> {
                    if (campaign.getProductDetails() == null) return Stream.<Double>empty();
                    return campaign.getProductDetails().stream()
                            .filter(dcpd -> dcpd.getProductDetail() != null
                                    && dcpd.getProductDetail().getId().equals(productDetailId))
                            .map(dcpd -> {
                                BigDecimal perItem = dcpd.getDiscountPercentage();    // % riêng SPCT (có thể null)
                                BigDecimal campPct = campaign.getDiscountPercentage();// % mặc định campaign (có thể null)
                                BigDecimal used = perItem != null
                                        ? perItem
                                        : (campPct != null ? campPct : BigDecimal.ZERO);
                                return used.doubleValue();
                            });
                })
                .max(Double::compare)
                .orElse(0.0);
    }

    // Tính giá sau giảm
    private BigDecimal calculateDiscountPrice(BigDecimal originalPrice, double discountPercent) {
        if (originalPrice == null) return null;
        if (discountPercent <= 0) return originalPrice;
        return originalPrice.multiply(
                BigDecimal.valueOf(100 - discountPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        );
    }

    // (Tuỳ bạn có dùng chỗ khác) % tốt nhất theo productId - mức Product
    private double getBestDiscountPercentageForProductId(Long productId, List<DiscountCampaign> campaigns) {
        return campaigns.stream()
                .flatMap(campaign -> {
                    if (campaign.getProducts() == null) return Stream.<Double>empty();
                    return campaign.getProducts().stream()
                            .filter(dcp -> dcp.getProduct() != null && dcp.getProduct().getId().equals(productId))
                            .map(dcp -> campaign.getDiscountPercentage() != null
                                    ? campaign.getDiscountPercentage().doubleValue()
                                    : 0.0);
                })
                .max(Double::compare)
                .orElse(0.0);
    }


    @Override
    public ProductDetailResponse scanProductDetail(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) throw new IllegalArgumentException("Mã QR trống.");

        // Ưu tiên dùng fetch-join để tránh lazy/null khi map
        ProductDetail pd = productDetailRepository.findActiveByDetailCodeFetchAll(code)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SPCT hoặc SPCT không hoạt động."));

        // Map sang DTO theo mapper bạn đã có
        ProductDetailResponse resp = productDetailMapper.toResponse(pd);

        // Tính giảm giá
        List<DiscountCampaign> active = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        double pctDetail  = getBestDiscountPercentageForProductDetail(pd.getId(), active);
        double pctProduct = (pd.getProduct() != null)
                ? getBestDiscountPercentageForProduct(pd.getProduct(), active) : 0.0;
        double bestPct    = Math.max(pctDetail, pctProduct);

        Long bestCampaignId = (pctDetail >= pctProduct)
                ? getBestDiscountCampaignIdForProductDetail(pd.getId(), active)
                : (pd.getProduct() != null ? getBestDiscountCampaignIdForProduct(pd.getProduct(), active) : null);

        // Fill vào DTO (các field này bạn không map trong mapper)
        resp.setDiscountPercentage((int) Math.round(bestPct));
        resp.setDiscountedPrice(calculateDiscountPrice(resp.getSellPrice(), bestPct));
        resp.setDiscountCampaignId(bestCampaignId);

        // Barcode đã được map = productDetailCode theo mapper của bạn
        return resp;
    }

    private Long getBestDiscountCampaignIdForProductDetail(Long productDetailId, List<DiscountCampaign> campaigns) {
        if (productDetailId == null) return null;
        return campaigns.stream()
                .filter(c -> c.getDiscountPercentage() != null)
                .flatMap(c -> {
                    if (c.getProductDetails() != null) {
                        return c.getProductDetails().stream()
                                .filter(link -> link.getProductDetail() != null
                                        && link.getProductDetail().getId().equals(productDetailId))
                                .map(link -> Map.entry(c.getId(), c.getDiscountPercentage().doubleValue()));
                    }
                    return Stream.empty();
                })
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public PaginationDTO<ProductSearchResponse> phanTrang(ProductSearchRequest request, Pageable pageable) {
        // 1) Lấy kết quả phân trang
        PaginationDTO<ProductSearchResponse> pagination = productSearchRepository.searchProducts(request, pageable);

        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        // 3) Áp dụng giảm giá
        pagination.getData().forEach(resp -> {
            // % tốt nhất mức Product
            double productDiscount = getBestDiscountPercentageForProductId(resp.getId(), activeCampaigns);
            resp.setDiscountPercentage((int) Math.round(productDiscount));
            resp.setDiscountedPrice(calculateDiscountPrice(resp.getSellPrice(), productDiscount));

            // Áp dụng cho từng SPCT
            if (resp.getProductDetails() != null) {
                resp.getProductDetails().forEach(d -> {
                    double detailDiscount = getBestDiscountPercentageForProductDetail(d.getId(), activeCampaigns);

                    // Fallback về % Product nếu SPCT không có giảm
                    if (detailDiscount <= 0) {
                        detailDiscount = productDiscount;
                    }

                    d.setDiscountPercentage((int) Math.round(detailDiscount));
                    d.setDiscountedPrice(calculateDiscountPrice(d.getSellPrice(), detailDiscount));
                });
            }
        });

        return pagination;
    }

    @Override
    public void exportProductToExcel(ProductSearchRequest dto, OutputStream outputStream) throws IOException {
        List<ProductSearchResponse> products = productSearchRepository.searchProductWithoutPaging(dto);
        try (ByteArrayInputStream excelStream = ProductExcelExporter.exportProductToExcel(products)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = excelStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOException("Failed to export Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public void exportProductToExcelByIds(List<Long> productIds, OutputStream outputStream) throws IOException {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách ID sản phẩm không được rỗng");
        }

        List<Product> productsWithCategories = productRepository.findByIdsWithCategories(productIds);

        List<Product> productsWithDetails = productRepository.findByIdsWithDetails(productIds);

        // Hợp nhất dữ liệu
        Map<Long, Product> productMap = new HashMap<>();
        for (Product p : productsWithCategories) {
            productMap.put(p.getId(), p);
        }
        for (Product p : productsWithDetails) {
            Product existing = productMap.get(p.getId());
            if (existing != null) {
                // Gán productDetails từ truy vấn thứ hai vào đối tượng hiện có
                existing.setProductDetails(p.getProductDetails());
            } else {
                productMap.put(p.getId(), p);
            }
        }

        List<Product> products = new ArrayList<>(productMap.values());

        // Ánh xạ sang ProductSearchResponse
        List<ProductSearchResponse> productResponses = products.stream()
                .map(productMapper::toResponseSearch)
                .collect(Collectors.toList());

        // Xuất Excel
        try (ByteArrayInputStream excelStream = ProductExcelExporter.exportProductToExcel(productResponses)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = excelStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOException("Không thể xuất Excel theo ID: " + e.getMessage(), e);
        }
    }

    @Override
    public PaginationDTO<ProductSearchResponse> getProductRemoved(ProductSearchRequest request, Pageable pageable) {
        return productSearchRepository.getProductRemoved(request,pageable);
    }

    @Override
    public List<ProductResponse> findProductWithImage() {
        Pageable top8 = PageRequest.of(0, 8);

        // Lấy danh sách sản phẩm
        List<Product> products = productRepository.findProductWithImage(top8);

        // Lấy danh sách đợt giảm giá đang hoạt động
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        // Ánh xạ và tính giảm giá
        return products.stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            // Tính giảm giá cho sản phẩm
            double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
            response.setDiscountPercentage((int) Math.round(productDiscount));
            response.setDiscountedPrice(calculateDiscountPrice(product.getSellPrice(), productDiscount));

            // Tính giảm giá cho các chi tiết sản phẩm nếu có
            if (response.getProductDetails() != null) {
                for (ProductDetailResponse detail : response.getProductDetails()) {
                    double detailDiscount = getBestDiscountPercentageForProductDetail(detail.getId(), activeCampaigns);

                    // Nếu không có giảm giá riêng thì dùng % của sản phẩm
                    if (detailDiscount <= 0) {
                        detailDiscount = productDiscount;
                    }

                    Integer quantityReserOrder = reservationOrderRepository
                            .sumQuantityByProductDetailActive(detail.getId());

                    // Tính lại tồn kho, nếu âm thì = 0
                    int availableQuantity = detail.getQuantity() - (quantityReserOrder == null ? 0 : quantityReserOrder);
                    detail.setQuantity(Math.max(0, availableQuantity));

                    detail.setDiscountPercentage((int) Math.round(detailDiscount));
                    detail.setDiscountedPrice(calculateDiscountPrice(detail.getSellPrice(), detailDiscount));
                }
            }
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> findProducts(List<Long> productIds) {
        List<Product> products = productRepository.findAllByIds(productIds);
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        List<ProductResponse> responses = products.stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
            response.setDiscountPercentage((int) Math.round(productDiscount));
            response.setDiscountedPrice(calculateDiscountPrice(product.getSellPrice(), productDiscount));

            if (response.getProductDetails() != null) {
                for (ProductDetailResponse detailResponse : response.getProductDetails()) {
                    double detailDiscount = getBestDiscountPercentageForProductDetail(detailResponse.getId(), activeCampaigns);

                    if (detailDiscount <= 0) {
                        detailDiscount = productDiscount; // fallback
                    }

                    Integer quantityReserOrder = reservationOrderRepository
                            .sumQuantityByProductDetailActive(detailResponse.getId());

                    // đảm bảo không âm
                    detailResponse.setQuantity(
                            Math.max(0, detailResponse.getQuantity() - (quantityReserOrder == null ? 0 : quantityReserOrder))
                    );

                    detailResponse.setDiscountPercentage((int) Math.round(detailDiscount));
                    detailResponse.setDiscountedPrice(
                            calculateDiscountPrice(detailResponse.getSellPrice(), detailDiscount)
                    );
                }
            }
            return response;
        }).collect(Collectors.toList());

        return responses;
    }

    @Override
    public List<FavoriteProductResponse> getFavoritesByProductId(Long productId) {
        List<FavoriteProductResponse> responses = favoriteProductRepository.getFavoritesByProductId(productId);
        return responses;
    }

    private String generateProductCode() {
        String prefix = "P-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    private String generateProductDetailCode() {
        String prefix = "PD-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    public Page<ProductResponse> getProductsByBrand(Long brandId, Pageable pageable) {
        Page<Product> products = productRepository.findAllByBrand(brandId, pageable);
        return getProductsFiltered(products, pageable);
    }


    private Page<ProductResponse> getProductsFiltered(Page<Product> products, Pageable pageable) {
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        List<ProductResponse> responses = products.getContent().stream()
                .map(product -> mapToProductResponse(product, activeCampaigns))
                .toList();

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    private ProductResponse mapToProductResponse(Product product, List<DiscountCampaign> activeCampaigns) {
        ProductResponse response = productMapper.toResponse(product);

        double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
        Long bestCampaignId = getBestDiscountCampaignIdForProduct(product, activeCampaigns);

        response.setDiscountPercentage((int) Math.round(productDiscount));
        response.setDiscountedPrice(calculateDiscountPrice(product.getSellPrice(), productDiscount));
        response.setDiscountCampaignId(bestCampaignId);

        if (response.getProductDetails() != null) {
            for (ProductDetailResponse detail : response.getProductDetails()) {
                double detailDiscount = getBestDiscountPercentageForProductDetail(detail.getId(), activeCampaigns);
                Long bestDetailCampaignId = getBestDiscountCampaignIdForProductDetail(detail.getId(), activeCampaigns);

                if (detailDiscount <= 0) {
                    detailDiscount = productDiscount;
                    bestDetailCampaignId = bestCampaignId; // fallback campaign
                }

                Integer quantityReserOrder = reservationOrderRepository
                        .sumQuantityByProductDetailActive(detail.getId());

                // Trừ số lượng đã giữ chỗ, nếu âm thì ép = 0
                detail.setQuantity(
                        Math.max(0, detail.getQuantity() - (quantityReserOrder == null ? 0 : quantityReserOrder))
                );

                detail.setDiscountPercentage((int) Math.round(detailDiscount));
                detail.setDiscountedPrice(calculateDiscountPrice(detail.getSellPrice(), detailDiscount));
                detail.setDiscountCampaignId(bestDetailCampaignId);
            }
        }

        return response;
    }

    private Page<ProductResponse> mapWithDiscounts(Page<Product> products, Pageable pageable) {
        List<DiscountCampaign> activeCampaigns =
                discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        List<ProductResponse> responses = products.getContent().stream().map(product -> {
            ProductResponse response = productMapper.toResponse(product);

            double productDiscount = getBestDiscountPercentageForProduct(product, activeCampaigns);
            response.setDiscountPercentage((int) Math.round(productDiscount));
            response.setDiscountedPrice(calculateDiscountPrice(product.getSellPrice(), productDiscount));

            Long bestCampaignId = getBestDiscountCampaignIdForProduct(product, activeCampaigns);
            response.setDiscountCampaignId(bestCampaignId);

            if (response.getProductDetails() != null) {
                for (ProductDetailResponse d : response.getProductDetails()) {
                    double detailDiscount = getBestDiscountPercentageForProductDetail(d.getId(), activeCampaigns);

                    Long bestDetailCampaignId = detailDiscount > 0
                            ? getBestDiscountCampaignIdForProductDetail(d.getId(), activeCampaigns)
                            : bestCampaignId;

                    if (detailDiscount <= 0) {
                        detailDiscount = productDiscount;
                    }

                    Integer quantityReserOrder = reservationOrderRepository
                            .sumQuantityByProductDetailActive(d.getId());

                    // đảm bảo số lượng không âm
                    d.setQuantity(
                            Math.max(0, d.getQuantity() - (quantityReserOrder == null ? 0 : quantityReserOrder))
                    );

                    d.setDiscountPercentage((int) Math.round(detailDiscount));
                    d.setDiscountedPrice(calculateDiscountPrice(d.getSellPrice(), detailDiscount));
                    d.setDiscountCampaignId(bestDetailCampaignId);
                }
            }
            return response;
        }).collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, products.getTotalElements());
    }

    public Page<ProductResponse> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findAllByCategoryId(categoryId, pageable);
        return mapWithDiscounts(products, pageable);
    }

    @Override
    public Page<ProductResponse> getProductsByGenderId(Long genderId, Pageable pageable) {
        Page<Product> products = productRepository.findAllByGenderId(genderId, pageable);
        return mapWithDiscounts(products, pageable);
    }

    @Override
    public Page<ProductResponse> getProductsByColorId(Long colorId, Pageable pageable) {
        Page<Product> products = productRepository.findAllByColorId(colorId, pageable);
        return mapWithDiscounts(products, pageable);
    }

    @Override
    public Page<ProductResponse> getProductsBySizeId(Long sizeId, Pageable pageable) {
        Page<Product> products = productRepository.findAllBySizeId(sizeId, pageable);
        return mapWithDiscounts(products, pageable);
    }

    @Override
    public List<ProductResponse> getProductSearch(String name) {
        List<ProductResponse> products = productRepository.findProductWithImageV2(name)
                .stream()
                .limit(5)
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getProductName(),
                        p.getSellPrice(),
                        p.getProductImages().stream()
                                .map(img -> new ProductImageResponse(img.getId(), img.getImageName(), img.getImage(), img.getColor().getColorName()))
                                .toList()
                ))
                .toList();
        return products;
    }

    @Override
    public ProductDetailResponse verifyProductDetail(Long id) {
        ProductDetail v = productDetailRepository.findById(id).orElse(null);
        if (v == null) {
            throw new RuntimeException("Ko tìm thấy spct với id: "+id);
        }
        return productDetailMapper.toResponse(v);
    }

    @Override
    public List<ProductDetailResponse> verifyListProductDetail(List<Long> id) {
        List<ProductDetail> details = productDetailRepository.findByProductIds(id);
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("Ko tìm thấy spct với id: "+id);
        }
        return productDetailMapper.toResponses(details);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> page = productRepository.searchByKeyword(keyword, pageable);
        return page.map(productMapper::toResponse);
    }

}

