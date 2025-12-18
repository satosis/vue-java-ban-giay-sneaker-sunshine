<template>
  <div class="p-4">
    <el-card shadow="hover" class="card-container">
      <div class="d-flex justify-between align-center mb-3">
        <h2 class="title">Danh sách sản phẩm đã xóa</h2>
        <el-button type="info" @click="goBack" round>
          <el-icon><Back /></el-icon> Quay lại
        </el-button>
      </div>

      <el-table
        :data="products"
        style="width: 100%"
        border
        stripe
        v-loading="loading"
        empty-text="Không có sản phẩm đã xóa nào."
      >
        <el-table-column type="index" label="STT" width="60" :index="indexMethod" />
        <el-table-column prop="productCode" label="Mã SP" width="120" sortable />
        <el-table-column prop="productName" label="Tên SP" sortable />
        <el-table-column prop="brandName" label="Thương hiệu" width="150" />
        <el-table-column prop="materialName" label="Chất liệu" width="150" />
        <el-table-column label="Giá bán" width="120">
          <template #default="scope">
            {{ formatPrice(scope.row.sellPrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="Số lượng" width="100" />
        <el-table-column label="Ngày tạo" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdDate) }}
          </template>
        </el-table-column>
        <el-table-column label="Danh mục" width="180">
          <template #default="scope">
            <div class="category-tags-container">
              <el-tag
                v-for="(cat, idx) in scope.row.categoryNames"
                :key="idx"
                size="small"
                class="mr-1 mb-1"
                type="info"
              >
                {{ cat }}
              </el-tag>
              <span v-if="!scope.row.categoryNames || scope.row.categoryNames.length === 0"
                >N/A</span
              >
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="180">
          <template #default="scope">
            <div class="actions-cell">
              <el-button
                type="primary"
                size="small"
                @click="openRestoreDialog(scope.row)"
                class="mr-2"
              >
                Khôi phục
              </el-button>

              <el-button
                type="danger"
                size="small"
                @click="openDeleteDialog(scope.row)"
              >
                Xóa
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="d-flex justify-end mt-4">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page + 1"
          :page-sizes="[5, 8, 10, 20, 50]"
          :page-size="size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalElements"
          background
        />
      </div>
    </el-card>

    <!-- Restore Dialog -->
    <el-dialog
      title="Xác nhận khôi phục sản phẩm"
      v-model="isRestoreDialogVisible"
      width="30%"
      center
      class="confirm-dialog"
    >
      <p>Bạn có chắc chắn muốn khôi phục sản phẩm này không?</p>
      <template #footer>
        <el-button type="success" @click="saveProduct" round>Xác nhận</el-button>
        <el-button type="danger" @click="isRestoreDialogVisible = false" round>Hủy</el-button>
      </template>
    </el-dialog>

    <!-- Delete Dialog -->
    <el-dialog
      title="Xác nhận xóa sản phẩm"
      v-model="isDeleteDialogVisible"
      width="30%"
      center
      class="confirm-dialog"
    >
      <p class="text-danger">Hành động này sẽ xóa sản phẩm vĩnh viễn. Bạn có chắc chắn?</p>
      <template #footer>
        <el-button type="danger" @click="deleteProduct" round :loading="deleteLoading">Xóa vĩnh viễn</el-button>
        <el-button @click="isDeleteDialogVisible = false" round>Hủy</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  ElMessage,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElButton,
  ElCard,
  ElTag,
  ElIcon,
  ElDialog,
} from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import apiClient from '../../utils/axiosInstance.js'

const router = useRouter()

// --- Helper Functions ---
const formatPrice = (price) => {
  if (price === null || price === undefined) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(price)
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}/${year} ${hours}:${minutes}`
}

const indexMethod = (index) => {
  return page.value * size.value + index + 1
}

// --- State Variables ---
const products = ref([])
const page = ref(0)
const size = ref(8)
const totalElements = ref(0)
const loading = ref(false)

// Restore dialog state
const isRestoreDialogVisible = ref(false)
const currentProductId = ref(null)

// Delete dialog state
const isDeleteDialogVisible = ref(false)
const currentDeleteProductId = ref(null)
const deleteLoading = ref(false)

// --- Navigation Function ---
const goBack = () => {
  router.push('/product')
}

// --- API Data Fetching Functions ---
const fetchData = async () => {
  loading.value = true
  try {
    const requestBody = {
      page: page.value,
      size: size.value,
    }
    const response = await apiClient.post(
      '/admin/products/inactive',
      requestBody,
      {
        headers: { 'Content-Type': 'application/json' },
      }
    )
    products.value = response.data.data
    totalElements.value = response.data.pagination.totalElements
  } catch (error) {
    console.error('Lỗi khi tải danh sách sản phẩm đã xóa:', error)
    ElMessage.error('Không thể tải danh sách sản phẩm đã xóa. Vui lòng thử lại.')
  } finally {
    loading.value = false
  }
}

// --- Dialog Management ---
const openRestoreDialog = (product) => {
  currentProductId.value = product.id
  isRestoreDialogVisible.value = true
}

const openDeleteDialog = (product) => {
  currentDeleteProductId.value = product.id
  isDeleteDialogVisible.value = true
}

// --- Save (Restore) Product Logic ---
const saveProduct = async () => {
  try {
    await apiClient.put(
      `/admin/products/restore/${currentProductId.value}`,
      null,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    )
    ElMessage.success('Sản phẩm đã được khôi phục thành công!')
    isRestoreDialogVisible.value = false
    currentProductId.value = null
    fetchData() // Reload product list
  } catch (error) {
    console.error('Lỗi khi khôi phục sản phẩm:', error)
    ElMessage.error('Không thể khôi phục sản phẩm. Vui lòng thử lại.')
  }
}

const deleteProduct = async () => {
  if (!currentDeleteProductId.value) return
  deleteLoading.value = true
  try {

    await apiClient.delete(`/admin/products/delete-forever/${currentDeleteProductId.value}`, {
      headers: {
        'Content-Type': 'application/json',
      },
    })
    ElMessage.success('Sản phẩm đã được xóa vĩnh viễn.')
    isDeleteDialogVisible.value = false
    currentDeleteProductId.value = null
    fetchData()
  } catch (error) {
    console.error('Lỗi khi xóa sản phẩm:', error)
    // Hiển thị message chi tiết nếu có lỗi từ server
    const errMsg = error?.response?.data?.message || 'Không thể xóa sản phẩm. Vui lòng thử lại.'
    ElMessage.error(errMsg)
  } finally {
    deleteLoading.value = false
  }
}

// --- Pagination Handlers ---
const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 0 // Reset to first page
  fetchData()
}

const handleCurrentChange = (newPage) => {
  page.value = newPage - 1 // Convert to 0-indexed
  fetchData()
}

// Initial data fetch on component mount
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.p-4 {
  padding: 1rem;
}

.card-container {
  border-radius: 8px;
  overflow: hidden;
}

.title {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
}

.d-flex {
  display: flex;
}

.justify-between {
  justify-content: space-between;
}

.align-center {
  align-items: center;
}

.mb-3 {
  margin-bottom: 1rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

.confirm-dialog :deep(.el-dialog__body) {
  text-align: center;
  font-size: 1.1rem;
}

.confirm-dialog :deep(.el-dialog__footer) {
  text-align: center;
}

.category-tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* Style cho vùng button hành động */
.actions-cell {
  display: flex;
  align-items: center;
}

/* Khoảng cách giữa nút */
.actions-cell .mr-2 {
  margin-right: 8px;
}

.text-danger {
  color: #e53e3e;
  font-weight: 500;
}
</style>
