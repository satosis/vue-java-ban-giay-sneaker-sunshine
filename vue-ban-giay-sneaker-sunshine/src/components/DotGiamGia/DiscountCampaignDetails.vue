<template>
  <el-container class="campaign-details-container">
    <el-header class="cd-header">
      <el-page-header @back="goBack">
        <template #title>Quay lại</template>
        <template #content>
          <span class="cd-title">Chi tiết Đợt giảm giá</span>
        </template>
        <template #extra>
        </template>
      </el-page-header>
    </el-header>

    <el-main class="cd-main">
      <el-skeleton v-if="loading" :rows="10" animated />

      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />

      <div v-else-if="campaign">
        <el-row :gutter="20">
          <el-col :lg="15" :md="24">
            <el-card class="mb-4" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>Thông tin chung</span>
                </div>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="Tên đợt giảm giá" label-class-name="desc-label" :span="2">
                  {{ campaign.name || '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="Mã" label-class-name="desc-label">
                  {{ campaign.campaignCode || '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="Mức giảm" label-class-name="desc-label">
                  <el-tag type="success">{{ campaign.discountPercentage ?? 0 }}%</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Thời gian diễn ra" label-class-name="desc-label" :span="2">
                  <el-icon><Calendar /></el-icon>
                  <span class="ml-2">{{ formatDateTime(campaign.startDate) }} → {{ formatDateTime(campaign.endDate) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="Mô tả" label-class-name="desc-label" :span="2">
                  {{ campaign.description || '—' }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>

          <el-col :lg="9" :md="24">
            <el-card class="mb-4" shadow="never">
              <template #header><div class="card-header">Thống kê</div></template>
              <div v-if="stats" class="stats-grid">
                <div class="stat-card">
                  <div class="stat-card-icon bg-blue"><el-icon><Ticket /></el-icon></div>
                  <div class="stat-card-info">
                    <div class="stat-label">Số hóa đơn</div>
                    <div class="stat-value">{{ stats.totalInvoices ?? 0 }}</div>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-card-icon bg-green"><el-icon><Money /></el-icon></div>
                  <div class="stat-card-info">
                    <div class="stat-label">Doanh thu sau giảm</div>
                    <div class="stat-value">{{ formatCurrency(stats.totalAfterDiscount) }}</div>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-card-icon bg-purple"><el-icon><ShoppingBag /></el-icon></div>
                  <div class="stat-card-info">
                    <div class="stat-label">Sản phẩm bán ra</div>
                    <div class="stat-value">{{ stats.totalProductsSold ?? 0 }}</div>
                  </div>
                </div>
                <div class="stat-card">
                   <div class="stat-card-icon bg-orange"><el-icon><Discount /></el-icon></div>
                  <div class="stat-card-info">
                    <div class="stat-label">Tỉ lệ giảm TB</div>
                    <div class="stat-value">{{ stats.averageDiscountRate != null ? stats.averageDiscountRate + '%' : 'N/A' }}</div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="Chưa có dữ liệu thống kê" :image-size="80" />
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="never">
          <template #header><div class="card-header">Dữ liệu áp dụng</div></template>
          <el-tabs v-model="activeTab" class="cd-tabs">
            <el-tab-pane name="products" label="Sản phẩm áp dụng">


              <el-table :data="pagedProducts" border stripe v-loading="loading">
                <template #empty><el-empty description="Không có sản phẩm nào" /></template>
                <el-table-column type="index" label="#" width="60" align="center" />
                <el-table-column prop="productCode" label="Mã sản phẩm" width="120" />
                <el-table-column prop="productName" label="Tên sản phẩm" min-width="250" show-overflow-tooltip />
                <!-- <el-table-column label="Hành động" width="120" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="goToProduct(row.productId)">Xem chi tiết</el-button>
                  </template>
                </el-table-column> -->
              </el-table>

              <div class="table-pagination">
                <el-pagination
                  background layout="total, sizes, prev, pager, next"
                  :total="filteredProducts.length"
                  v-model:current-page="prodPage"
                  v-model:page-size="prodPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane name="productDetails" label="Sản phẩm chi tiết áp dụng">  

              <el-table :data="pagedDetails" border stripe v-loading="loading">
                <template #empty><el-empty description="Không có sản phẩm chi tiết nào" /></template>
                <el-table-column type="index" label="#" width="60" align="center" />
                <el-table-column prop="productName" label="Tên sản phẩm" min-width="220" show-overflow-tooltip />
                <el-table-column prop="productCode" label="Mã sản phẩm" min-width="140" show-overflow-tooltip />
                <el-table-column prop="colorName" label="Màu" width="110" />
                <el-table-column prop="sizeName" label="Size" width="90" align="center" />
                <el-table-column label="Giá bán" width="140" align="right">
                  <template #default="{ row }">
                    {{ formatCurrency(row.sellPrice ?? row.price ?? 0) }}
                  </template>
                </el-table-column>
                <el-table-column label="Giảm (%)" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag type="warning" effect="light">
                      {{ row.discountPercent ?? campaign.discountPercent ?? 0 }}%
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="quantity" label="Tồn kho" width="100" align="center">
                   <template #default="{ row }">{{ row.stock ?? 'N/A' }}</template>
                </el-table-column>
              </el-table>

              <div class="table-pagination">
                <el-pagination
                  background layout="total, sizes, prev, pager, next"
                  :total="filteredDetails.length"
                  v-model:current-page="detailPage"
                  v-model:page-size="detailPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Calendar, Ticket, Money, ShoppingBag, Discount } from '@element-plus/icons-vue'
import apiClient from '@/utils/axiosInstance'

const router = useRouter()
const route = useRoute()

const campaign = ref(null)
const stats = ref(null)
const loading = ref(true)
const error = ref('')
const activeTab = ref('products')

// SP – tìm kiếm & phân trang
const kwProduct = ref('')
const prodPage = ref(1)
const prodPageSize = ref(10)

// SPCT – tìm kiếm & phân trang
const kwDetail = ref('')
const detailPage = ref(1)
const detailPageSize = ref(10)

// Helpers
const formatDateTime = (dateStr) => {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return isNaN(date.getTime()) ? dateStr : date.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}
const formatCurrency = (val) => {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
const statusText = (s) => ({0: 'Sắp diễn ra', 1: 'Đang hoạt động', 2: 'Đã kết thúc'}[s] || 'Không xác định')
const statusTagType = (s) => ({0: 'warning', 1: 'success', 2: 'info'}[s] || '')

// Điều hướng & hành động
const goBack = () => router.back()


// Nguồn dữ liệu
const products = computed(() => campaign.value?.products || [])
const productDetails = computed(() => campaign.value?.productDetails || campaign.value?.details || [])

// Logic lọc và phân trang cho Sản phẩm
const filteredProducts = computed(() => {
  const kw = kwProduct.value.trim().toLowerCase()
  if (!kw) return products.value
  return products.value.filter(p =>
    (p.productName || '').toLowerCase().includes(kw) ||
    (p.productId || '').toString().toLowerCase().includes(kw)
  )
})
const pagedProducts = computed(() => {
  const start = (prodPage.value - 1) * prodPageSize.value
  return filteredProducts.value.slice(start, start + prodPageSize.value)
})

// Logic lọc và phân trang cho SPCT
const filteredDetails = computed(() => {
  const kw = kwDetail.value.trim().toLowerCase()
  if (!kw) return productDetails.value
  return productDetails.value.filter(d =>
    (d.productName || '').toLowerCase().includes(kw) ||
    (d.sku || '').toLowerCase().includes(kw)
  )
})
const pagedDetails = computed(() => {
  const start = (detailPage.value - 1) * detailPageSize.value
  return filteredDetails.value.slice(start, start + detailPageSize.value)
})
const applyDetailFilter = () => { detailPage.value = 1 }


// API calls
const fetchData = async () => {
  loading.value = true
  error.value = ''
  try {
    const id = route.params.id
    // Sử dụng Promise.allSettled để cả hai request chạy song song
    const [campaignRes, statsRes] = await Promise.allSettled([
      apiClient.get(`/admin/campaigns/${id}`),
      apiClient.get(`/admin/campaigns/${id}/statistics`)
    ])

    if (campaignRes.status === 'fulfilled') {
      campaign.value = campaignRes.value.data
    } else {
      console.error("Lỗi tải chiến dịch:", campaignRes.reason)
      throw new Error('Không thể tải chi tiết đợt giảm giá.')
    }

    if (statsRes.status === 'fulfilled') {
      stats.value = statsRes.value.data
    } else {
      console.warn("Lỗi tải thống kê:", statsRes.reason)
      stats.value = null // Vẫn hiển thị trang dù thống kê lỗi
    }

    // reset phân trang
    prodPage.value = 1
    detailPage.value = 1
  } catch (e) {
    error.value = e.message
    campaign.value = null // Xóa dữ liệu cũ nếu có lỗi
  } finally {
    loading.value = false
  }
}

const loadCampaign = () => fetchData(); // Giữ hàm cũ để nút refresh hoạt động
const loadStats = () => { /* Logic đã được gộp vào fetchData */ };

onMounted(fetchData)
watch(() => route.params.id, (newId) => {
  if (newId) fetchData()
})
</script>

<style scoped>
.campaign-details-container {
  background-color: #f7f8fa;
}

/* Header */
.cd-header {
  background-color: #fff;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.cd-title {
  font-size: 1.25rem; /* ~20px */
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.cd-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
:deep(.el-page-header__content) {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

/* Main content */
.cd-main {
  padding: 20px;
}

/* Card header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

/* Descriptions component styling */
.desc-label {
  font-weight: 500 !important;
  min-width: 120px;
}
.el-descriptions__label {
  font-weight: 500;
}

/* Thống kê */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background-color: var(--el-bg-color-page);
  border: 1px solid var(--el-border-color-lighter);
}
.stat-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}
.stat-card-icon .el-icon {
  font-size: 24px;
  color: #fff;
}
.stat-card-info {
  line-height: 1.4;
}
.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
/* Stat Icon Colors */
.bg-blue { background-color: #409eff; }
.bg-green { background-color: #67c23a; }
.bg-purple { background-color: #9b59b6; }
.bg-orange { background-color: #e6a23c; }


/* Tabs & Toolbar */
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.toolbar .el-input {
  max-width: 300px;
}

/* Pagination */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

/* Helpers */
.mb-4 {
  margin-bottom: 20px;
}
.ml-2 {
  margin-left: 0.5rem;
}
</style>