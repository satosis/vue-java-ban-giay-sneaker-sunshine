<template>
  <el-card class="box-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span>Chi tiết sản phẩm: {{ product?.productName || 'Đang tải...' }}</span>
        <el-button type="primary" @click="goBack" size="small" plain>Quay lại</el-button>
      </div>
    </template>

    <el-empty v-if="loading" description="Đang tải dữ liệu..." />
    <el-empty v-else-if="error" description="Lỗi khi tải dữ liệu" />

    <template v-else-if="product">
      <!-- Thông tin chung -->
      <el-descriptions title="Thông tin chung" :column="3" border>
        <el-descriptions-item label="Mã sản phẩm">{{ product.productCode || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Tên sản phẩm">{{ product.productName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Thương hiệu">{{ product.brandName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Chất liệu">{{ product.materialName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Cổ giày">{{ product.styleName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Giới tính">{{ product.genderName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Số lượng tồn kho">{{ product.quantity ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="Trạng thái">{{ product.status === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}</el-descriptions-item>
        <el-descriptions-item label="Mô tả">{{ product.description || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Nhà cung cấp">{{ product.supplierName || 'Không có' }}</el-descriptions-item>
        <el-descriptions-item label="Ngày tạo">{{ formatDate(product.createdDate) }}</el-descriptions-item>
        <el-descriptions-item label="Ngày cập nhật">{{ formatDate(product.updatedDate) }}</el-descriptions-item>
        <el-descriptions-item label="Đánh giá">
          <el-button type="info" size="small" @click="openReviewsDialog()">Xem đánh giá</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="Lịch sử tác động">
          <el-button type="warning" size="small" @click="openHistoryDialog()">Xem lịch sử tác động</el-button>
        </el-descriptions-item>
      </el-descriptions>

      <!-- Chi tiết sản phẩm -->
      <el-table
        v-if="product.productDetails && product.productDetails.length"
        :data="product.productDetails"
        style="margin-top: 30px"
        border
        stripe
      >
        <el-table-column prop="sizeName" label="Size" width="150" align="center" />
        <el-table-column prop="colorName" label="Màu sắc" width="150" align="center" />
        <el-table-column label="Giá bán" min-width="220">
          <template #default="{ row }">
            <div class="price-cell">
              <!-- Trường hợp có giảm giá thực tế (có % và giá giảm nhỏ hơn giá bán) -->
              <template v-if="(row.discountPercentage || 0) > 0 && row.discountedPrice < row.sellPrice">
                <div style="display:flex;align-items:center;gap:8px;">
                  <span class="line-through">{{ formatCurrency(row.sellPrice) }}</span>
                  <span class="price-sale">{{ formatCurrency(row.discountedPrice) }}</span>
                  <span class="discount-badge">-{{ Number(row.discountPercentage).toFixed(0) }}%</span>
                </div>
              </template>

              <!-- Trường hợp không giảm giá -->
              <template v-else>
                <span class="price">{{ formatCurrency(row.sellPrice) }}</span>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="quantity" label="Số lượng" width="120" align="center" />
        <el-table-column label="Trạng thái" width="150" align="center">
          <template #default="{ row }">{{ row.status === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}</template>
        </el-table-column>
        <el-table-column label="QR Code" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openQrDialog(row.productDetailCode)">QR</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Ảnh sản phẩm -->
      <div v-if="product.productImages && product.productImages.length" style="margin-top: 30px">
        <h3>Ảnh sản phẩm</h3>
        <el-carousel height="200px" trigger="click" arrow="always">
          <el-carousel-item v-for="img in product.productImages" :key="img.id">
            <img
              :src="`data:image/png;base64,${img.image}`"
              alt="product image"
              style="max-height: 180px; margin: auto; display: block"
            />
          </el-carousel-item>
        </el-carousel>
      </div>
    </template>

    <!-- Dialog QR Code -->
    <el-dialog v-model="qrDialogVisible" title="QR Code" width="320px" @close="onCloseQrDialog">
      <div style="text-align: center;">
        <el-image
          v-if="qrImageUrl"
          :src="qrImageUrl"
          fit="contain"
          style="width: 220px; height: 220px; margin-bottom: 10px;"
        />
        <div v-else class="text-gray-500" style="height:220px;display:flex;align-items:center;justify-content:center">
          Không tải được QR
        </div>
        <el-button type="primary" plain :loading="downloadingQr" @click="downloadQrCode">Tải về QR</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="reviewsDialogVisible" title="Đánh giá sản phẩm" width="600px">
      <el-empty v-if="productReviews.length === 0" description="Chưa có đánh giá nào." />
      <el-table v-else :data="productReviews" border style="width: 100%">
        <el-table-column label="Người đánh giá" prop="reviewerName" width="180">
          <template #default="{ row }">{{ row.customerName || 'Khách hàng ẩn danh' }}</template>
        </el-table-column>
        <el-table-column label="Số sao" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.rate" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="Nội dung đánh giá" prop="comment" />
        <el-table-column label="Ngày đánh giá" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- Dialog Lịch sử -->
    <el-dialog v-model="historyDialogVisible" title="Lịch sử tác động sản phẩm" width="800px">
      <el-empty v-if="productHistory.length === 0" description="Không có lịch sử tác động." />
      <el-table v-else :data="productHistory" border style="width: 100%">
        <el-table-column label="Người tạo" prop="employeeName" width="180" />
        <el-table-column label="Loại tác động" width="150">
          <template #default="{ row }">{{ getActionType(row.actionType) }}</template>
        </el-table-column>
        <el-table-column label="Trường" prop="fieldName" />
        <el-table-column label="Giá trị cũ" prop="oldValue" />
        <el-table-column label="Giá trị mới" prop="newValue" />
        <el-table-column label="Mô tả" prop="note" />
        <el-table-column label="Thời gian" width="200">
          <template #default="{ row }">{{ formatDateTime(row.createdDate) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import apiClient from '@/utils/axiosInstance'

const route = useRoute()
const router = useRouter()

const product = ref(null)
const loading = ref(false)
const error = ref(false)

/** ---------- QR ---------- **/
const qrDialogVisible = ref(false)
const qrImageUrl = ref('')     // ObjectURL
const currentSpctCode = ref('')
const downloadingQr = ref(false)
let lastObjectUrl = null

function revokeQrUrl() {
  if (lastObjectUrl) {
    URL.revokeObjectURL(lastObjectUrl)
    lastObjectUrl = null
  }
}

async function openQrDialog(productDetailCode) {
  qrDialogVisible.value = true
  if (!productDetailCode) return
  currentSpctCode.value = productDetailCode

  try {
    // Lấy ảnh QR qua axios instance để kèm Authorization → tránh 403
    const res = await apiClient.get(`/admin/products/qrcode/${productDetailCode}`, {
      responseType: 'blob'
    })
    revokeQrUrl()
    const url = URL.createObjectURL(res.data)
    qrImageUrl.value = url
    lastObjectUrl = url
  } catch (e) {
    console.error('Load QR failed:', e?.response || e)
    qrImageUrl.value = ''
    ElNotification({ title: 'Lỗi', message: 'Không thể tải QR code.', type: 'error' })
  }
}

function onCloseQrDialog() {
  qrDialogVisible.value = false
  revokeQrUrl()
  qrImageUrl.value = ''
  currentSpctCode.value = ''
}

async function downloadQrCode() {
  if (!currentSpctCode.value) return
  downloadingQr.value = true
  try {
    const res = await apiClient.get(`/admin/products/qrcode/${currentSpctCode.value}`, {
      responseType: 'blob'
    })
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `qrcode_${currentSpctCode.value}.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Download QR failed:', error?.response || error)
    ElNotification({ title: 'Lỗi', message: 'Không thể tải QR code.', type: 'error' })
  } finally {
    downloadingQr.value = false
  }
}

/** ---------- Reviews ---------- **/
const reviewsDialogVisible = ref(false)
const productReviews = ref([])

async function fetchProductReviews(productId) {
  try {
    const { data } = await apiClient.get(`/admin/products/reviews-product/${productId}`)
    productReviews.value = Array.isArray(data) ? data : []
    return true
  } catch (e) {
    console.error('Lỗi fetch reviews:', e)
    ElNotification({ title: 'Lỗi', message: 'Không thể tải đánh giá sản phẩm.', type: 'error' })
    return false
  }
}
async function openReviewsDialog() {
  if (product.value?.id) {
    const ok = await fetchProductReviews(product.value.id)
    if (ok) reviewsDialogVisible.value = true
  }
}

/** ---------- History ---------- **/
const historyDialogVisible = ref(false)
const productHistory = ref([])

async function fetchProductHistory(productId) {
  try {
    const { data } = await apiClient.get(`/admin/product-histories/get-by-productId`, {
      params: { productId }
    })
    productHistory.value = Array.isArray(data) ? data : []
    return true
  } catch (e) {
    console.error('Lỗi fetch history:', e)
    ElNotification({ title: 'Lỗi', message: 'Không thể tải lịch sử sản phẩm.', type: 'error' })
    return false
  }
}
async function openHistoryDialog() {
  if (product.value?.id) {
    const ok = await fetchProductHistory(product.value.id)
    if (ok) historyDialogVisible.value = true
  }
}

/** ---------- Product ---------- **/
function goBack() { router.push('/product') }
function formatCurrency(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' }) : '0 ₫'
}
function formatDate(s) { return s ? new Date(s).toLocaleDateString('vi-VN') : 'Không có' }
function formatDateTime(s) { return s ? new Date(s).toLocaleString('vi-VN') : 'Không có' }
function getActionType(t) { const map = { CREATE: 'Tạo mới', UPDATE: 'Cập nhật', DELETE: 'Xóa' }; return map[t] || t }

async function fetchProduct(id) {
  loading.value = true
  error.value = false
  try {
    const { data } = await apiClient.get(`/admin/products/${id}`)
    product.value = data
    console.log('data: ',product.value)
  } catch (e) {
    console.error('Lỗi fetch product:', e)
    error.value = true
    ElNotification({ title: 'Lỗi', message: 'Không thể tải thông tin sản phẩm.', type: 'error' })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const id = route.params.id
  if (id) fetchProduct(id)
  else {
    error.value = true
    ElNotification({ title: 'Lỗi', message: 'Không tìm thấy ID sản phẩm trong URL.', type: 'error' })
  }
})

onBeforeUnmount(() => {
  revokeQrUrl()
})
</script>

<style scoped>
.box-card { max-width: 1200px; margin: 20px auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.price-cell { display: flex; align-items: center; }
.line-through { text-decoration: line-through; color: #8a8a8a; margin-right: 6px; }
.price-sale { color: #d03050; font-weight: 700; }
.price { font-weight: 700; }
.discount-badge {
  background: #fff0f2;
  color: #d03050;
  border-radius: 6px;
  padding: 2px 6px;
  font-weight: 700;
  font-size: 12px;
}

</style>
