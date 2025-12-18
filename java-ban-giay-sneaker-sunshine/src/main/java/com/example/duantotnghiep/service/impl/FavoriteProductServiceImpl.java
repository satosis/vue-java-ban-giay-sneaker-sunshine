package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.FavoriteProductRequest;
import com.example.duantotnghiep.dto.response.FavoriteProductResponse;
import com.example.duantotnghiep.dto.response.RatingProductResponse;
import com.example.duantotnghiep.mapper.FavoriteProductMapper;
import com.example.duantotnghiep.model.*;
import com.example.duantotnghiep.repository.*;
import com.example.duantotnghiep.service.FavoriteProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteProductServiceImpl implements FavoriteProductService {
    private final FavoriteProductRepository favoriteRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final FavoriteProductMapper mapper;
    private final UserRepository userRepo;
    private final InvoiceDetailRepository invoiceDetailRepo;


    @Override
    public FavoriteProductResponse addFavorite(FavoriteProductRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Tìm user theo username
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        // Lấy employee từ user
        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là khách hàng.");
        }
        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

//        favoriteRepo.findByCustomerIdAndProductIdAndStatus(customer.getId(), product.getId(),1)
//                .ifPresent(fp -> {
//                    throw new RuntimeException("Product already favorited");
//                });

        FavoriteProduct favorite = new FavoriteProduct();
        favorite.setCustomer(customer);
        favorite.setProduct(product);
        favorite.setInvoiceId(request.getInvoiceId());
        favorite.setCreatedAt(new Date());
        favorite.setComment(request.getComment());
        favorite.setRate(request.getRate());
        favorite.setStatus(1);
        FavoriteProduct saved = favoriteRepo.save(favorite);
        return mapper.toDto(saved);
    }


    @Override
    public void removeFavorite(Long customerId, Long productId) {
        FavoriteProduct favorite = favoriteRepo.findByCustomerIdAndProductIdAndStatus(customerId,productId,1).orElseThrow(() -> new RuntimeException("Customer not found"));
        favorite.setStatus(0);
        favoriteRepo.save(favorite);
    }

    @Override
    public List<FavoriteProductResponse> getFavoritesByCustomer(Long customerId) {
        return favoriteRepo.getFavoritesByCustomer(customerId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RatingProductResponse> getFavoritesByCustomer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là khách hàng.");
        }

        List<Object[]> list = invoiceDetailRepo.findAllByCustomerId(customer.getId());
        return  list.stream().map(obj -> new RatingProductResponse(
                ((Number) obj[0]).longValue(),
                ((Number) obj[1]).longValue(),
                (String) obj[2],
                (byte[]) obj[3],
                ((Number) obj[4]).intValue() == 1
        )).collect(Collectors.toList());
    }

}
