<template>
  <div class="employee-list px-4 py-6 md:px-8">
    <!-- Header / Actions -->
    <el-card class="rounded-2xl shadow-sm card-header" body-style="padding: 16px 20px;">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div class="flex items-center gap-3">
          <el-icon :size="26" class="text-primary"><Avatar /></el-icon>
          <div>
            <h2 class="text-xl font-semibold leading-none">Danh sách nhân viên</h2>
            <p class="text-sm text-gray-500 mt-1">Tìm kiếm, xuất Excel và quản trị nhân viên</p>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <el-button type="success" plain round @click="exportExcel" :loading="exporting" :disabled="loading">
            <el-icon class="mr-1"><Download /></el-icon>
            Xuất Excel
          </el-button>
          <el-button type="primary" round @click="addEmployee">
            <el-icon class="mr-1"><CirclePlus /></el-icon>
            Thêm nhân viên
          </el-button>
        </div>
      </div>

      <!-- Unified Search -->
      <el-form class="mt-4" :inline="true" @submit.prevent>
        <el-form-item>
          <el-input
            v-model.trim="key"
            placeholder="Tìm theo mã,họ tên,email"
            class="w-80 md:w-96"
            clearable
            :prefix-icon="Search"
            @keyup.enter.native="handleUnifiedSearch"
            @clear="handleUnifiedSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUnifiedSearch" :loading="loading">
            <el-icon class="mr-1"><Search /></el-icon>
            Tìm
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset" :disabled="!key && !isSearching">Xóa lọc</el-button>
        </el-form-item>
        <el-form-item v-if="isSearching && key" class="!ml-2">
          <el-tag type="info" effect="plain" closable @close="handleReset">
            Từ khóa: <strong class="ml-1">{{ key }}</strong>
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card class="rounded-2xl mt-5 shadow-sm" body-style="padding: 0;">
      <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <div class="text-gray-600 text-sm">
          <template v-if="!isSearching">
            Tổng: <strong>{{ totalElements }}</strong> bản ghi
          </template>
          <template v-else>
            Kết quả tìm thấy: <strong>{{ employees.length }}</strong> bản ghi
          </template>
        </div>
        <div class="flex items-center gap-2">
          <el-tooltip content="Làm mới" placement="top">
            <el-button circle :loading="loading" @click="refresh">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <el-table
        :data="tableData"
        :loading="loading"
        border
        stripe
        highlight-current-row
        class="rounded-b-2xl"
        element-loading-text="Đang tải dữ liệu…"
        :empty-text="isSearching ? 'Không có nhân viên phù hợp' : 'Chưa có dữ liệu'"
      >
        <el-table-column label="STT" width="80" align="center" fixed>
          <template #default="{ $index }">
            {{ isSearching ? $index + 1 : pageStartIndex + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="employeeCode" label="Mã NV" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.employeeCode }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="employeeName" label="Họ tên" min-width="180" show-overflow-tooltip />
        <el-table-column prop="email" label="Email" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="Điện thoại" min-width="140" />

        <el-table-column label="Giới tính" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="genderType(row.gender)" effect="plain">{{ genderText(row.gender) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Vai trò" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)" effect="plain">{{ mapRole(row.role) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Hành động" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex gap-2 justify-center">
              <el-tooltip content="Cập nhật" placement="top">
                <el-button type="primary" size="small" @click="updateEmployee(row.id)" circle>
                  <el-icon :size="16"><EditPen /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="Xóa" placement="top">
                <el-button type="danger" size="small" @click="deleteEmployee(row.id)" circle>
                  <el-icon :size="16"><DeleteFilled /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div class="py-12 flex flex-col items-center gap-3 text-gray-500">
            <el-icon :size="40"><User /></el-icon>
            <div class="text-sm">Không có dữ liệu để hiển thị</div>
          </div>
        </template>
      </el-table>

      <!-- Pagination (hidden on search) -->
      <div v-if="!isSearching" class="flex flex-col md:flex-row md:items-center justify-between gap-4 px-4 py-3 border-t border-gray-100">
        <div class="text-gray-600 text-sm">
          Trang <strong>{{ currentPage }}</strong> / {{ Math.max(1, Math.ceil(totalElements / pageSize)) }}
        </div>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50, 100]"
          :total="totalElements"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>

      <!-- Back to paged list when searching -->
      <div v-else class="flex items-center justify-between px-4 py-3 border-t border-gray-100 text-gray-600">
        <div class="text-sm">Đang xem kết quả tìm kiếm.</div>
        <el-button link type="primary" @click="handleReset">Quay lại danh sách phân trang</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Avatar, CirclePlus, EditPen, DeleteFilled, Refresh, Search, Download, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ===== State =====
const key = ref('')
const employees = ref([])
const loading = ref(false)
const exporting = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)

const isSearching = ref(false)
let searchTimer = null

const pageStartIndex = computed(() => (currentPage.value - 1) * pageSize.value)
const tableData = computed(() => employees.value)

// ===== API: danh sách mặc định (phân trang) =====
const fetchEmployees = async () => {
  loading.value = true
  isSearching.value = false
  try {
    const res = await apiClient.get('/admin/employees', {
      params: { page: currentPage.value - 1, size: pageSize.value }
    })
    const data = res?.data || {}
    employees.value = data.content ?? []
    totalElements.value = Number(data.page?.totalElements ?? 0)

    // Nếu trang hiện tại vượt quá tổng sau khi xóa
    if (employees.value.length === 0 && currentPage.value > 1 && totalElements.value > 0) {
      currentPage.value = Math.max(1, currentPage.value - 1)
      await fetchEmployees()
    }
  } catch (e) {
    if (e?.response?.status === 403) return router.push('/error')
    console.error(e)
    ElMessage.error('Không thể tải dữ liệu nhân viên.')
    employees.value = []
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

// ===== API: tìm kiếm bằng 1 key (map sang 3 trường BE) =====
const unifiedSearchCall = async () => {
  const q = key.value?.trim()
  if (!q) {
    currentPage.value = 1
    await fetchEmployees()
    return
  }
  loading.value = true
  try {
    const res = await apiClient.get('/admin/employees/search', {
      params: { employeeCode: q, employeeName: q, email: q }
    })
    employees.value = res?.data ?? []
    isSearching.value = true
  } catch (e) {
    if (e?.response?.status === 403) return router.push('/error')
    console.error(e)
    ElMessage.error('Tìm kiếm thất bại.')
    employees.value = []
    isSearching.value = true
  } finally {
    loading.value = false
  }
}

const handleUnifiedSearch = () => {
  // debounce 300ms để mượt mà hơn nếu người dùng bấm liên tục
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => unifiedSearchCall(), 300)
}

const handleReset = async () => {
  key.value = ''
  currentPage.value = 1
  await fetchEmployees()
}

// ===== Export Excel theo key hiện tại =====
const exportExcel = async () => {
  try {
    exporting.value = true
    const q = key.value?.trim() || ''
    const res = await apiClient.get('/admin/employees/export-excel', {
      params: { employeeCode: q || null, employeeName: q || null, email: q || null },
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    const ts = new Date().toISOString().replace(/[:.]/g, '-')
    a.href = url
    a.download = `employees-${ts}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('Xuất Excel thành công!')
  } catch (e) {
    if (e?.response?.status === 403) return router.push('/error')
    console.error(e)
    ElMessage.error('Xuất Excel thất bại.')
  } finally {
    exporting.value = false
  }
}

// ===== Pagination =====
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchEmployees()
}
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchEmployees()
}

// ===== Utils =====
const genderText = (g) => (g === 1 ? 'Nam' : g === 2 ? 'Nữ' : 'Khác')
const genderType = (g) => (g === 1 ? 'success' : g === 2 ? 'danger' : 'info')
const roleType = (r) => (r === 1 ? 'primary' : r === 2 ? 'warning' : 'info')
const mapRole = (role) => {
  switch (role) {
    case 2: return 'Nhân viên'
    case 1: return 'Quản trị'
    default: return 'Khác'
  }
}

// ===== Actions =====
const refresh = () => (isSearching.value ? handleUnifiedSearch() : fetchEmployees())
const addEmployee = () => router.push('/employee/add')
const updateEmployee = (id) => router.push(`/employee/update/${id}`)
const deleteEmployee = async (id) => {
  try {
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn xóa nhân viên này không?',
      'Xác nhận',
      { confirmButtonText: 'Xóa', cancelButtonText: 'Hủy', type: 'warning' }
    )
    await apiClient.delete(`/admin/employees/${id}`)
    ElMessage.success('Xóa nhân viên thành công.')
    isSearching.value ? await unifiedSearchCall() : await fetchEmployees()
  } catch (e) {
    if (e === 'cancel' || e === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.')
    } else if (e?.response?.status === 403) {
      router.push('/error')
    } else {
      console.error(e)
      ElMessage.error('Xóa nhân viên thất bại.')
    }
  }
}

// ===== Lifecycle =====
onMounted(fetchEmployees)
</script>

<style scoped>
.employee-list { max-width: 1400px; margin: 0 auto; }
.text-primary { color: var(--el-color-primary); }

/***** Card polish *****/
.card-header { border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-blank);
}

/***** Table tweaks *****/
:deep(.el-table) {
  --el-table-border-color: var(--el-border-color-lighter);
}
:deep(.el-table .cell) {
  white-space: nowrap;
}

/***** Responsive *****/
@media (max-width: 768px) {
  .card-header :deep(.el-form-item) { margin-bottom: 8px; }
}
</style>
