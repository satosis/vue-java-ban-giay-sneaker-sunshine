<template>
  <nav class="sidebar-container">
    <!-- Logo đặt ngoài el-menu để không bị coi như menu item -->
    <div class="logo-container">
      <img :src="logoSrc" alt="Logo" class="logo-img" />
      <span class="logo-text">Sunshine Sneakers</span>
    </div>

    <el-menu :default-active="route.path" class="el-menu-vertical" router>
      <el-menu-item index="/dashboard">
        <i class="menu-icon bi bi-house-door" aria-hidden="true"></i>
        <span class="menu-label">Trang chủ</span>
      </el-menu-item>

      <el-sub-menu index="sales">
        <template #title>
          <i class="menu-icon bi bi-cart-fill" aria-hidden="true"></i>
          <span class="menu-label">Bán hàng</span>
        </template>
        <el-menu-item index="/sales-counter/list">
          <i class="menu-icon bi bi-cash" aria-hidden="true"></i>
          <span class="menu-label">Bán hàng tại quầy</span>
        </el-menu-item>
        <el-menu-item index="/sales-online">
          <i class="menu-icon bi bi-laptop" aria-hidden="true"></i>
          <span class="menu-label">Bán hàng online</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="product-management">
        <template #title>
          <i class="menu-icon bi bi-box-seam" aria-hidden="true"></i>
          <span class="menu-label">Quản lý sản phẩm</span>
        </template>

        <el-menu-item index="/product">
          <i class="menu-icon bi bi-box" aria-hidden="true"></i>
          <span class="menu-label">Sản phẩm</span>
        </el-menu-item>

        <el-menu-item index="/supplier">
          <i class="menu-icon bi bi-truck" aria-hidden="true"></i>
          <span class="menu-label">Nhà cung cấp</span>
        </el-menu-item>

        <el-menu-item index="/sole">
          <i class="menu-icon bi bi-grid" aria-hidden="true"></i>
          <span class="menu-label">Đế giày</span>
        </el-menu-item>

        <el-menu-item index="/style">
          <i class="menu-icon bi bi-circle-half" aria-hidden="true"></i>
          <span class="menu-label">Cổ giày</span>
        </el-menu-item>

        <el-menu-item index="/material">
          <i class="menu-icon bi bi-layers" aria-hidden="true"></i>
          <span class="menu-label">Chất liệu</span>
        </el-menu-item>

        <el-menu-item index="/categories">
          <i class="menu-icon bi bi-list" aria-hidden="true"></i>
          <span class="menu-label">Danh mục</span>
        </el-menu-item>

        <el-menu-item index="/size">
          <i class="menu-icon bi bi-arrows-angle-contract" aria-hidden="true"></i>
          <span class="menu-label">Kích thước</span>
        </el-menu-item>

        <el-menu-item index="/color">
          <i class="menu-icon bi bi-palette" aria-hidden="true"></i>
          <span class="menu-label">Màu sắc</span>
        </el-menu-item>

        <el-menu-item index="/brand">
          <i class="menu-icon bi bi-tags" aria-hidden="true"></i>
          <span class="menu-label">Thương hiệu</span>
        </el-menu-item>
      </el-sub-menu>

        <el-menu-item index="/voucher">
          <i class="menu-icon bi bi-tag" aria-hidden="true"></i>
          <span class="menu-label">Mã giảm giá</span>
        </el-menu-item>

      <el-menu-item index="/employee">
        <i class="menu-icon bi bi-person" aria-hidden="true"></i>
        <span class="menu-label">Quản lý người dùng</span>
      </el-menu-item>

      <el-menu-item index="/customer">
        <i class="menu-icon bi bi-person-fill" aria-hidden="true"></i>
        <span class="menu-label">Quản lý khách hàng</span>
      </el-menu-item>

      <el-menu-item index="/invoices">
        <i class="menu-icon bi bi-file-earmark" aria-hidden="true"></i>
        <span class="menu-label">Quản lý hóa đơn</span>
      </el-menu-item>

      <el-menu-item index="/discount-campaigns">
        <i class="menu-icon bi bi-percent" aria-hidden="true"></i>
        <span class="menu-label">Quản lý đợt giảm giá</span>
      </el-menu-item>

      <el-menu-item index="/blacklist-history">
        <i class="menu-icon bi bi-exclamation-octagon" aria-hidden="true"></i>
        <span class="menu-label">Quản lý khách xấu</span>
      </el-menu-item>

      <!-- <el-sub-menu index="statistics">
        <template #title>
          <i class="menu-icon bi bi-graph-up" aria-hidden="true"></i>
          <span class="menu-label">Thống kê</span>
        </template>
        <el-menu-item index="/statistics">
          <i class="menu-icon bi bi-bar-chart" aria-hidden="true"></i>
          <span class="menu-label">Thống kê chi tiết</span>
        </el-menu-item>
      </el-sub-menu> -->
    </el-menu>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'
// Dùng ảnh trong public (Vite):
const logoSrc = `${import.meta.env.BASE_URL}logo.jpg`
const route = useRoute()
</script>

<style scoped>
.sidebar-container {
  background-color: #fff;
  width: 250px;
  min-height: 100vh;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.logo-container {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-img {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 50%;
  margin-right: 10px;
}

.logo-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: #303133;
}

/* El Menu */
.el-menu-vertical {
  border-right: none;
  width: 100%;
  --menu-icon-size: 18px;
}

/* Chuẩn hoá icon Bootstrap cho menu */
.menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--menu-icon-size);
  width: calc(var(--menu-icon-size) + 6px); /* để label thẳng hàng */
  height: var(--menu-icon-size);
  margin-right: 10px;
  line-height: 1;
  color: #909399;
}

.menu-label {
  line-height: 1;
}

/* Active & hover */
.el-menu-item.is-active .menu-label,
.el-menu-item.is-active .menu-icon,
.el-sub-menu.is-active .menu-label,
.el-sub-menu.is-active .menu-icon {
  color: #409EFF; /* màu chủ đạo Element Plus */
}

.el-menu-item:hover .menu-icon,
.el-sub-menu__title:hover .menu-icon {
  color: #409EFF;
}

/* Đồng bộ padding trái các item & tiêu đề submenu */
.el-menu-item,
.el-sub-menu__title {
  padding-left: 20px !important;
}

/* Item con trong submenu có thêm indent nhẹ */
.el-menu-item-group .el-menu-item {
  padding-left: 40px !important;
}
</style>
