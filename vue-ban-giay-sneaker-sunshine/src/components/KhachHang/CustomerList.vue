<template>
  <div class="customer-list-container">
    <el-card class="box-card">
      <div class="controls-container">
        <el-row :gutter="20" justify="space-between" align="middle">
          <el-col :xs="24" :sm="12" :md="14">
            <div class="header-left">
              <h2 class="title">Quản Lý Khách Hàng</h2>
              <el-input
                v-model="searchKeyword"
                placeholder="Tìm kiếm theo tên, mã, email, SĐT..."
                :prefix-icon="Search"
                clearable
                @clear="fetchCustomers"
                @keyup.enter="handleSearch"
                class="search-input"
              />
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="10">
            <div class="actions">
              <el-space wrap>
                <el-button :icon="Refresh" @click="resetSearch">Làm mới</el-button>
                <el-button type="primary" :icon="Download" :loading="exporting" @click="exportExcel">
                  Xuất Excel
                </el-button>
                <el-button type="success" :icon="Plus" @click="goToAddCustomer">
                  Thêm mới
                </el-button>
              </el-space>
            </div>
          </el-col>
        </el-row>
      </div>

      <el-divider />

      <el-table
        :data="customers"
        border
        stripe
        v-loading="loading"
        element-loading-text="Đang tải dữ liệu..."
        class="customer-table"
        :row-class-name="tableRowClassName"
      >
        <template #empty>
          <el-empty description="Không tìm thấy khách hàng nào" />
        </template>

        <el-table-column type="index" label="#" width="60" :index="indexMethod" align="center" />
        <el-table-column prop="customerName" label="Tên khách hàng" sortable min-width="180" />
        <el-table-column prop="email" label="Email" min-width="200" />
        <el-table-column prop="phone" label="Số điện thoại" width="140" />
        <el-table-column label="Ngày tạo" width="150" sortable prop="createdDate">
          <template #default="scope">
            {{ formatDate(scope.row.createdDate) }}
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="180" align="center">
          <template #default="scope">
            <div v-if="scope.row.isBlacklisted" class="status-cell">
              <el-tag type="danger" effect="dark" size="small">
                Đang bị cấm
              </el-tag>
              <el-tooltip effect="dark" placement="top">
                <template #content>
                  <div class="blacklist-tooltip">
                    <p><strong>Lý do:</strong> {{ scope.row.blacklistReason || 'Không có' }}</p>
                    <p v-if="scope.row.blacklistEndDate">
                      <strong>Hết hạn:</strong> {{ formatDate(scope.row.blacklistEndDate) }}
                    </p>
                     <p v-else><strong>Hết hạn:</strong> Vĩnh viễn</p>
                  </div>
                </template>
                <el-icon class="info-icon"><WarningFilled /></el-icon>
              </el-tooltip>
            </div>
            <el-tag v-else type="success" effect="light" size="small">
              Hoạt động
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Hành động" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-space>
              <el-tooltip content="Sửa thông tin" placement="top">
                <el-button type="primary" :icon="Edit" size="small" circle @click="goToEditCustomer(scope.row.id)" />
              </el-tooltip>
              <el-tooltip content="Xóa khách hàng" placement="top">
                <el-button type="danger" :icon="Delete" size="small" circle @click="confirmDeleteCustomer(scope.row.id)" />
              </el-tooltip>
              <el-tooltip v-if="!scope.row.isBlacklisted" content="Cấm khách hàng" placement="top">
                <el-button type="warning" :icon="CircleClose" size="small" circle @click="confirmBlacklistCustomer(scope.row.id)" />
              </el-tooltip>
              <el-tooltip v-else content="Bỏ cấm khách hàng" placement="top">
                <el-button type="success" :icon="CircleCheck" size="small" circle @click="confirmUnblacklistCustomer(scope.row.id)" />
              </el-tooltip>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          v-model:current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="size"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalElements"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/utils/axiosInstance' // Đảm bảo đường dẫn này đúng trong dự án của bạn
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Edit,
  Delete,
  Search,
  Refresh,
  CircleClose,
  CircleCheck,
  Download
} from '@element-plus/icons-vue'

const router = useRouter()

// --- State Variables ---
const exporting = ref(false)
const customers = ref([])
const currentPage = ref(1)
const size = ref(10)
const totalElements = ref(0)
const loading = ref(false)
const searchKeyword = ref('')

const toNull = (v) => (v && v.trim() !== '' ? v.trim() : null)
const isPhoneLike = (v) => !!v && /^[0-9+\s-]{8,15}$/.test(v.trim())

// Từ searchKeyword suy ra tham số gọi /search
const normalizeDigits = (v) => (v || '').replace(/\D/g, '')
const deriveAdvancedFromKeyword = (kw) => {
  const k = (kw || '').trim()
  if (!k) return { name: null, phone: null, phoneSuffix: null }

  // Nếu đúng 5 chữ số -> coi là phoneSuffix
  if (/^\d{5}$/.test(k)) {
    return { name: null, phone: null, phoneSuffix: k }
  }

  // Nếu có ít nhất 3 chữ số -> coi là phone (contains)
  if (isPhoneLike(k)) {
    return { name: null, phone: k, phoneSuffix: null }
  }

  // còn lại coi là tên
  return { name: k, phone: null, phoneSuffix: null }
}


// --- Data Fetching ---
const fetchCustomers = async () => {
  loading.value = true
  try {
    const hasKeyword = (searchKeyword.value || '').trim() !== ''
    let res

    if (hasKeyword) {
      // Gọi endpoint tìm kiếm nâng cao
      const { name, phone, phoneSuffix } = deriveAdvancedFromKeyword(searchKeyword.value)
      res = await apiClient.get(`/admin/customers/search`, {
        params: {
          name: toNull(name),
          phone: toNull(phone),
          phoneSuffix: toNull(phoneSuffix),
          page: currentPage.value - 1,
          size: size.value,
          sort: 'createdDate,desc'
        }
      })
    } else {
      // Gọi endpoint phân trang mặc định
      res = await apiClient.get(`/admin/customers/phan-trang`, {
        params: {
          page: currentPage.value - 1,
          size: size.value
          // KHÔNG GỬI keyword vì BE không xử lý ở endpoint này
        },
      })
    }

    // Chuẩn hoá đọc dữ liệu
    customers.value = res.data?.content || []
    totalElements.value = res.data?.totalElements ?? res.data?.page?.totalElements ?? 0

    // Giữ hành vi chỉnh trang như cũ
    if (customers.value.length === 0 && currentPage.value > 1 && totalElements.value > 0) {
      currentPage.value = Math.max(1, Math.ceil(totalElements.value / size.value))
      await fetchCustomers()
    } else if (totalElements.value === 0) {
      currentPage.value = 1
    }
  } catch (err) {
    console.error('Lỗi tải danh sách khách hàng:', err)
    if (err.response && err.response.status === 403) {
      router.push('/error')
      return
    }
    ElMessage.error('Không thể tải dữ liệu khách hàng. Vui lòng thử lại sau.')
    customers.value = []
    totalElements.value = 0
    currentPage.value = 1
  } finally {
    loading.value = false
  }
}
// Lấy filename từ header Content-Disposition (nếu có)
const parseFilename = (cd) => {
  if (!cd) return 'customers.xlsx'
  const m = /filename\*=UTF-8''([^;]+)|filename="?([^\";]+)"?/i.exec(cd)
  return decodeURIComponent(m?.[1] || m?.[2] || 'customers.xlsx')
}

const exportExcel = async () => {
  exporting.value = true
  try {
    // Dùng đúng logic suy diễn name/phone theo keyword hiện tại
    const { name, phone } = deriveAdvancedFromKeyword(searchKeyword.value)
    const res = await apiClient.get(`/admin/customers/export`, {
      params: {
        name: toNull(name),
        phone: toNull(phone),
      },
      responseType: 'blob', // Bắt buộc để tải file nhị phân
    })

    const fileName = parseFilename(res.headers['content-disposition'])
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName || 'customers.xlsx'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('Đang tải file Excel…')
  } catch (err) {
    console.error('Export Excel error:', err)
    ElMessage.error('Xuất Excel thất bại. Vui lòng thử lại.')
  } finally {
    exporting.value = false
  }
}



// --- Pagination Handlers ---
const handleSizeChange = (newSize) => {
  size.value = newSize
  currentPage.value = 1 // Reset về trang đầu tiên khi thay đổi kích thước trang
  fetchCustomers()
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  fetchCustomers()
}

// --- Table Utilities ---
const indexMethod = (index) => {
  return (currentPage.value - 1) * size.value + index + 1
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    return dateStr // Trả về chuỗi gốc nếu không phải định dạng ngày hợp lệ
  }
  // Định dạng ngày theo chuẩn Việt Nam (DD/MM/YYYY)
  return date.toLocaleDateString('vi-VN')
}

const getTrustScoreTagType = (score) => {
  if (score >= 80) return 'success' // Điểm cao
  if (score >= 50) return 'warning' // Điểm trung bình
  return 'danger' // Điểm thấp (nguy hiểm)
}

// Hàm này quyết định lớp CSS cho mỗi hàng trong bảng
const tableRowClassName = ({ row }) => {
  // Tô đỏ hàng nếu khách hàng bị đánh dấu là "isBlacklisted: true"
  // Điều này yêu cầu backend phải cung cấp trường 'isBlacklisted'
  if (row.isBlacklisted) {
    return 'danger-row' // Áp dụng lớp CSS 'danger-row' để tô đỏ
  }
  return '' // Không áp dụng lớp nào khác
}

// --- Navigation ---
const goToAddCustomer = () => {
  router.push({ name: 'AddCustomer' })
}

const goToEditCustomer = (id) => {
  router.push({ name: 'UpdateCustomer', params: { id: id } })
}

// --- Search Functionality ---
const handleSearch = () => {
  currentPage.value = 1 // Luôn tìm kiếm từ trang đầu tiên
  fetchCustomers()
}

const resetSearch = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchCustomers()
}

// --- Customer Actions (Delete, Blacklist, Unblacklist) ---

// Xác nhận xóa khách hàng
const confirmDeleteCustomer = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa khách hàng này?', 'Cảnh báo', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning',
    })
    await deleteCustomer(id)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.')
    } else {
      console.error('Lỗi xác nhận xóa:', error)
      ElMessage.error('Có lỗi xảy ra khi xác nhận xóa.')
    }
  }
}

// Gửi yêu cầu xóa khách hàng đến API
const deleteCustomer = async (id) => {
  try {
    await apiClient.delete(`/admin/customers/${id}`)
    ElMessage.success('Xóa khách hàng thành công!')
    // Điều chỉnh trang nếu item cuối cùng của trang bị xóa
    if (customers.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    await fetchCustomers() // Tải lại danh sách để cập nhật
  } catch (err) {
    console.error('Lỗi khi xóa khách hàng:', err)
    ElMessage.error('Không thể xóa khách hàng. Vui lòng thử lại.')
  }
}

// Xác nhận cấm khách hàng (hiển thị 2 hộp thoại: lý do và thời gian)
const confirmBlacklistCustomer = async (id) => {
  try {
    // Bước 1: Nhập lý do cấm
    const { value: reason } = await ElMessageBox.prompt(
      'Vui lòng nhập lý do cấm khách hàng:',
      'Cấm khách hàng',
      {
        confirmButtonText: 'Cấm',
        cancelButtonText: 'Hủy',
        inputType: 'textarea', // Cho phép nhập nhiều dòng
        inputPlaceholder: 'Lý do cấm (ví dụ: Vi phạm chính sách, hành vi không phù hợp)',
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return 'Lý do cấm không được để trống.'
          }
          return true
        },
        inputErrorMessage: 'Lý do không hợp lệ.',
        showClose: false, // Không cho phép đóng bằng nút X
      },
    )

    if (reason) {
      // Nếu người dùng đã nhập lý do
      // Bước 2: Nhập số ngày cấm
      const { value: duration } = await ElMessageBox.prompt(
        'Nhập **số ngày cấm** khách hàng (để trống hoặc 0 nếu cấm vĩnh viễn):',
        'Thời gian cấm',
        {
          confirmButtonText: 'Cấm',
          cancelButtonText: 'Hủy',
          inputType: 'number', // Cho phép nhập số
          inputPlaceholder: 'Ví dụ: 30 (ngày)',
          inputValidator: (value) => {
            // Cho phép để trống (null) hoặc chuỗi rỗng để cấm vĩnh viễn
            if (value === null || value.trim() === '') {
              return true
            }
            const num = parseInt(value, 10)
            // Kiểm tra phải là số và không âm
            if (isNaN(num) || num < 0) {
              return 'Số ngày không hợp lệ. Vui lòng nhập số dương hoặc để trống.'
            }
            return true
          },
          inputErrorMessage: 'Số ngày không hợp lệ.',
          showClose: false,
        },
      )

      // Chuyển đổi duration sang số nguyên. Nếu là null/rỗng, giữ nguyên null để backend xử lý là vĩnh viễn.
      const durationInDays =
        duration === null || duration.trim() === '' ? null : parseInt(duration, 10)

      await blacklistCustomer(id, reason, durationInDays)
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác cấm khách hàng.')
    } else {
      console.error('Lỗi xác nhận cấm khách hàng:', error)
      ElMessage.error('Có lỗi xảy ra khi xác nhận cấm khách hàng.')
    }
  }
}

// Gửi yêu cầu cấm khách hàng đến API
const blacklistCustomer = async (id, reason, durationInDays) => {
  try {
    // API call với lý do và số ngày cấm (durationInDays có thể là null)
    await apiClient.put(`/admin/customers/${id}/blacklist`, {
      reason: reason,
      durationInDays: durationInDays,
    })

    ElMessage.success('Đã cấm khách hàng thành công!')
    await fetchCustomers() // Tải lại danh sách để cập nhật trạng thái
  } catch (err) {
    console.error('Lỗi khi cấm khách hàng:', err)
    ElMessage.error('Không thể cấm khách hàng. Vui lòng thử lại.')
  }
}

// Xác nhận bỏ cấm khách hàng
const confirmUnblacklistCustomer = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn bỏ cấm khách hàng này?', 'Xác nhận', {
      confirmButtonText: 'Bỏ cấm',
      cancelButtonText: 'Hủy',
      type: 'info',
    })
    await unblacklistCustomer(id)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác bỏ cấm khách hàng.')
    } else {
      console.error('Lỗi xác nhận bỏ cấm:', error)
      ElMessage.error('Có lỗi xảy ra khi xác nhận bỏ cấm.')
    }
  }
}

// Gửi yêu cầu bỏ cấm khách hàng đến API
const unblacklistCustomer = async (id) => {
  try {
    await apiClient.put(`/admin/customers/${id}/unblacklist`)
    ElMessage.success('Đã bỏ cấm khách hàng thành công!')
    await fetchCustomers() // Tải lại danh sách để cập nhật trạng thái
  } catch (err) {
    console.error('Lỗi khi bỏ cấm khách hàng:', err)
    ElMessage.error('Không thể bỏ cấm khách hàng. Vui lòng thử lại.')
  }
}

// --- Lifecycle Hook ---
onMounted(() => {
  fetchCustomers() // Tải dữ liệu khi component được mount
})
</script>

<style scoped>
/* --- BỐ CỤC CHUNG --- */
.customer-list-container {
  padding: 24px;
  background-color: #f5f7fa; /* Nền xám nhẹ cho toàn trang */
}

.box-card {
  border: none;
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
  padding: 12px;
}

/* --- KHU VỰC ĐIỀU KHIỂN --- */
.controls-container {
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.title {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 24px;
  font-weight: 600;
  white-space: nowrap; /* Không xuống dòng */
}

.search-input {
  max-width: 350px;
  min-width: 250px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

/* --- BẢNG DỮ LIỆU --- */
.customer-table {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}

/* Style cho hàng bị cấm - nhẹ nhàng hơn */
:deep(.el-table .danger-row) {
  background-color: #fff2f2 !important;
  --el-table-tr-bg-color: #fff2f2;
}
:deep(.el-table .danger-row:hover > td) {
  background-color: #ffe8e8 !important;
}

/* Cột trạng thái */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.info-icon {
  cursor: pointer;
  color: var(--el-color-info);
  font-size: 16px;
}
.blacklist-tooltip {
  max-width: 250px;
}
.blacklist-tooltip p {
  margin: 4px 0;
}

/* --- PHÂN TRANG --- */
.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* --- RESPONSIVE --- */
@media (max-width: 992px) {
  .header-left {
    margin-bottom: 16px;
  }
  .actions {
    justify-content: flex-start;
  }
}

@media (max-width: 576px) {
  .title {
    font-size: 20px;
  }
  .search-input {
    width: 100%;
  }
  .actions .el-space {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
