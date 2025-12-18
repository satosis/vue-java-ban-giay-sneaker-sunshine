package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.dto.response.InvoiceDetailResponse;
import com.example.duantotnghiep.dto.response.InvoiceDisplayResponse;
import com.example.duantotnghiep.dto.response.InvoiceResponse;
import com.example.duantotnghiep.dto.response.ProductAttributeResponse;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.model.ProductDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {SizeMapper.class, ColorMapper.class})
public interface InvoiceMapper {

    // ============ Invoice -> InvoiceResponse ============
    @Mapping(target = "customerId", expression = "java(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)")
    @Mapping(target = "customerName", expression = "java(invoice.getCustomer() != null ? invoice.getCustomer().getCustomerName() : \"Khách lẻ\")")
    @Mapping(target = "phone", source = "phone")
//    @Mapping(target = "phone", expression = "java(invoice.getCustomer() != null ? invoice.getCustomer().getPhone() : \"\")")
    @Mapping(target = "employeeName", source = "employee.employeeName")
    @Mapping(target = "shippingFee", source = "shippingFee")
    @Mapping(target = "deliveryAddress", source = "deliveryAddress") // THÊM DÒNG NÀY
    InvoiceResponse toInvoiceResponse(Invoice invoice);

    // ============ InvoiceDetail -> InvoiceDetailResponse ============
    @Mapping(target = "productName", source = "productDetail.product.productName")
    @Mapping(target = "productCode", source = "productDetail.product.productCode")
    @Mapping(target = "categoryName", source = "productDetail.product.productCategories", qualifiedByName = "getFirstCategoryName")
    @Mapping(target = "size", source = "productDetail.size")
    @Mapping(target = "color", source = "productDetail.color")
    @Mapping(target = "sellPrice", source = "sellPrice")
    @Mapping(target = "discountedPrice", source = "discountedPrice")
    @Mapping(target = "discountPercentage", source = "discountPercentage")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "invoiceCodeDetail", source = "invoiceCodeDetail")
    @Mapping(target = "customerName", expression = "java(invoiceDetail.getInvoice().getCustomer() != null ? invoiceDetail.getInvoice().getCustomer().getCustomerName() : \"Khách lẻ\")")
    @Mapping(target = "totalPrice", expression = "java(invoiceDetail.getSellPrice() != null ? invoiceDetail.getSellPrice().multiply(java.math.BigDecimal.valueOf(invoiceDetail.getQuantity())) : null)")
    @Mapping(target = "discountAmount", expression = "java((invoiceDetail.getSellPrice() != null && invoiceDetail.getDiscountPercentage() != null) ? invoiceDetail.getSellPrice().multiply(java.math.BigDecimal.valueOf(invoiceDetail.getDiscountPercentage())).divide(java.math.BigDecimal.valueOf(100)) : null)")
    @Mapping(target = "finalTotalPrice", expression = "java(invoiceDetail.getDiscountedPrice() != null ? invoiceDetail.getDiscountedPrice().multiply(java.math.BigDecimal.valueOf(invoiceDetail.getQuantity())) : null)")
    @Mapping(target = "deliveryAddress", source = "invoice.deliveryAddress")
    InvoiceDetailResponse toInvoiceDetailResponse(InvoiceDetail invoiceDetail);

    // ============ ProductDetail -> ProductAttributeResponse ============
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "categoryName", source = "product.productCategories", qualifiedByName = "getFirstCategoryName")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "availableQuantity", source = "quantity")
    @Mapping(target = "price", source = "sellPrice")
    ProductAttributeResponse toProductAttributeResponse(ProductDetail productDetail);

    // ============ List mappings ============
    List<InvoiceResponse> toInvoiceResponseList(List<Invoice> invoices);
    List<InvoiceDetailResponse> toInvoiceDetailResponseList(List<InvoiceDetail> details);
    List<ProductAttributeResponse> toProductAttributeResponseList(List<ProductDetail> details);

    CustomerResponse toCustomerResponse(Customer customer);

    // ============ Helper method ============
    @Named("getFirstCategoryName")
    static String getFirstCategoryName(List<?> categories) {
        if (categories != null && !categories.isEmpty()) {
            Object firstCategory = categories.get(0);
            try {
                Object category = firstCategory.getClass().getMethod("getCategory").invoke(firstCategory);
                return (String) category.getClass().getMethod("getCategoryName").invoke(category);
            } catch (Exception e) {
                return "Không xác định";
            }
        }
        return "Không xác định";
    }

    // ============ Invoice + Details -> InvoiceDisplayResponse ============
    default InvoiceDisplayResponse toInvoiceDisplayResponse(Invoice invoice, List<InvoiceDetail> details) {
        InvoiceResponse invoiceResponse = toInvoiceResponse(invoice);
        List<InvoiceDetailResponse> detailResponses = toInvoiceDetailResponseList(details);
        return new InvoiceDisplayResponse(invoiceResponse, detailResponses);
    }
}



