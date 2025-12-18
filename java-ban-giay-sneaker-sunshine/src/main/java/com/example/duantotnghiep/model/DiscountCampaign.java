package com.example.duantotnghiep.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discount_campaign")
public class DiscountCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "campaign_code", length = 100)
    private String campaignCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "name")
    private String name;

    @Size(max = 500)
    @Nationalized
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    private Integer status;

    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Size(max = 100)
    @Nationalized
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100)
    @Nationalized
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscountCampaignProduct> products = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscountCampaignProductDetail> productDetails = new ArrayList<>();

    /* ------ Lifecycle ------ */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdDate == null) createdDate = now;
        if (updatedDate == null) updatedDate = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }

    /* ------ Helpers (không gán list mới!) ------ */
    public void addProduct(Product product) {
        DiscountCampaignProduct link = new DiscountCampaignProduct();
        link.setCampaign(this);
        link.setProduct(product);
        products.add(link);
    }

    public void removeProductById(Long productId) {
        products.removeIf(l -> l.getProduct() != null && productId.equals(l.getProduct().getId()));
    }

    public void clearProducts() {
        products.clear(); // orphanRemoval sẽ xóa rows liên quan khi flush
    }
}
