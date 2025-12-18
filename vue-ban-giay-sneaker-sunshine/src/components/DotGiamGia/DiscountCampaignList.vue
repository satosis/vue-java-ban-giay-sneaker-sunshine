<template>
  <div class="p-4">
    <!-- Header + Export -->
    <el-row justify="space-between" align="middle" class="mb-4">
      <el-col :span="12">
        <h3 class="mb-0">Danh sách đợt giảm giá</h3>
      </el-col>
      <el-col :span="12" class="text-end">
        <el-dropdown @command="handleExportCommand">
          <el-button type="primary">
            Xuất Excel
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="selected" :disabled="selectedRows.length === 0">
                Xuất các đợt đã chọn
              </el-dropdown-item>
              <el-dropdown-item command="currentPage" :disabled="campaigns.length === 0">
                Xuất trang hiện tại
              </el-dropdown-item>
              <el-dropdown-item command="all">Xuất tất cả</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </el-row>

    <!-- Search / Filter -->
    <el-card shadow="never" class="mb-4">
      <el-form :inline="true" @submit.prevent>
        <!-- Keyword -->
        <el-form-item label="Từ khóa">
          <el-input
            v-model.trim="search.keyword"
            placeholder="Tìm theo mã, tên, mô tả..."
            clearable
            @keyup.enter="onSearch"
          />
        </el-form-item>

        <!-- Status -->
        <el-form-item label="Trạng thái">
          <el-select
            v-model="search.status"
            placeholder="Tất cả"
            clearable
            style="width: 200px"
          >
            <el-option :value="0" label="Sắp diễn ra" />
            <el-option :value="1" label="Đang hoạt động" />
            <el-option :value="2" label="Đã kết thúc" />
          </el-select>
        </el-form-item>

        <!-- Created Date (single day) -->
        <el-form-item label="Ngày tạo">
          <el-date-picker
            v-model="search.createdDate"
            type="date"
            placeholder="Chọn ngày"
            format="DD/MM/YYYY"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>

        <!-- Actions -->
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSearch">Tìm kiếm</el-button>
          <el-button @click="onClear">Xóa lọc</el-button>
        </el-form-item>

        <el-form-item class="flex-1 text-end">
          <el-button type="success" @click="goToAddPage">Thêm đợt giảm giá</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-table
      :data="campaigns"
      style="width: 100%"
      v-loading="loading"
      border
      stripe
      ref="campaignTable"
      @selection-change="handleRowSelection"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="#" width="60" align="center" :index="tableIndex" />

      <el-table-column prop="campaignCode" label="Mã" width="160">
        <template #default="{ row }">{{ getField(row, 'campaignCode') || '---' }}</template>
      </el-table-column>

      <el-table-column prop="name" label="Tên đợt giảm giá" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-medium">{{ getField(row, 'name') || '---' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="Thời gian" min-width="240">
        <template #default="{ row }">
          <span>
            {{ formatDate(getField(row, 'startDate')) }}
            <strong>→</strong>
            {{ formatDate(getField(row, 'endDate')) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="Giảm giá" width="150" align="right">
        <template #default="{ row }">
          <el-tag type="success" disable-transitions>
            {{ formatDiscount(row) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="Trạng thái" align="center" width="160">
        <template #default="{ row }">
          <el-tag :type="statusTagType(getField(row, 'status'))" round>
            {{ statusText(getField(row, 'status')) }}
          </el-tag>
        </template>
      </el-table-column>

<el-table-column label="Hành động" width="230" align="center" fixed="right">
  <template #default="{ row }">
    <el-button-group>
      <!-- Xem chi tiết -->
      <el-tooltip content="Xem chi tiết" placement="top">
        <el-button
          :icon="View"
          type="primary"
          size="small"
          @click="$router.push(`/discount-campaigns/detail/${getField(row, 'id')}`)"
        />
      </el-tooltip>

      <!-- Cập nhật -->
      <el-tooltip content="Cập nhật" placement="top">
        <el-button
          :icon="Edit"
          type="warning"
          size="small"
          @click="goToUpdate(getField(row, 'id'))"
        />
      </el-tooltip>

      <!-- Chuyển trạng thái -->
      <el-tooltip content="Chuyển trạng thái" placement="top">
        <el-button
          :icon="Refresh"
          type="success"
          size="small"
          @click="changeStatus(row)"
        />
      </el-tooltip>
    </el-button-group>
  </template>
</el-table-column>


      <template #empty>
        <p class="text-center text-muted m-4">Không có đợt giảm giá nào để hiển thị.</p>
      </template>
    </el-table>

    <!-- Pagination -->
    <div class="mt-4 justify-content-end">
      <el-pagination
        v-if="totalPages > 0"
        background
        layout="total, prev, pager, next, sizes"
        :total="totalItems"
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[5, 10, 20, 50, 100]"
        :pager-count="5"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/utils/axiosInstance'
import { View, Edit, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

/* ------------ State ------------ */
const campaigns = ref([])
const page = ref(0)
const size = ref(5)
const totalPages = ref(0)
const totalItems = ref(0)
const loading = ref(true)
const selectedRows = ref([])

/* ------------ Filters (đã đổi sang createdDate) ------------ */
const search = ref({
  keyword: '',
  status: null,
  createdDate: null, // YYYY-MM-DD (string) do value-format của el-date-picker
})

/* ------------ Utils ------------ */
const handleRowSelection = (selection) => { selectedRows.value = selection }
const getField = (obj, field) => obj?.[field] ?? obj?.campaign?.[field] ?? null
const tableIndex = (index) => page.value * size.value + index + 1

const formatDate = (val) => {
  if (!val) return '---'
  const d = new Date(typeof val === 'string' ? val.replace(' ', 'T') : val)
  return Number.isNaN(d.getTime()) ? '---' : d.toLocaleDateString('vi-VN')
}

const statusText = (status) => {
  switch (status) {
    case 0: return 'Sắp diễn ra'
    case 1: return 'Đang hoạt động'
    case 2: return 'Đã kết thúc'
    default: return 'Không xác định'
  }
}
const statusTagType = (status) => {
  switch (status) {
    case 0: return 'warning'
    case 1: return 'success'
    case 2: return 'info'
    default: return 'info'
  }
}

const formatDiscount = (row) => {
  const percent = getField(row, 'discountPercentage')
  const amount  = getField(row, 'discountAmount')
  if (percent != null) return `${percent}%`
  if (amount != null) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount)
  }
  return '---'
}

/* Chuẩn hóa params đúng với API mới */
const buildParams = () => {
  const params = {
    page: Number(page.value) ?? 0,
    size: Number(size.value) ?? 10,
  }
  const kw = (search.value.keyword ?? '').trim()
  if (kw) params.keyword = kw
  if (search.value.status !== null && search.value.status !== undefined) params.status = search.value.status
  if (search.value.createdDate) params.createdDate = search.value.createdDate // đã là YYYY-MM-DD theo value-format
  return params
}

/* ------------ API ------------ */
const fetchOrSearch = async () => {
  loading.value = true
  const params = buildParams()
  const hasFilter = !!(params.keyword || params.status !== undefined || params.createdDate)
  const endpoint = hasFilter ? '/admin/campaigns/search' : '/admin/campaigns'

  try {
    const res = await apiClient.get(endpoint, { params })
    const items = res?.data?.content ?? []
    campaigns.value = items.map(it => it?.campaign ?? it)

    const meta = res?.data?.page ?? res?.data ?? {}
    totalPages.value = meta?.totalPages ?? 0
    totalItems.value = meta?.totalElements ?? 0
    page.value = meta?.number ?? params.page
    size.value = meta?.size ?? params.size
  } catch (err) {
    console.error('Lỗi khi tải đợt giảm giá:', err?.response?.data || err)

    // ===== THÊM NHÁNH 403 giống fetchCustomers =====
    if (err?.response?.status === 403) {
      router.push('/error')
      return
    }

    ElMessage.error('Không thể tải danh sách đợt giảm giá.')
    campaigns.value = []
    totalPages.value = 0
    totalItems.value = 0
  } finally {
    loading.value = false
  }
}


const changeStatus = async (row) => {
  const id = getField(row, 'id')
  if (!id) return
  try {
    await apiClient.post(`/admin/campaigns/${id}/delete`) // theo API cũ bạn đang dùng
    ElMessage.success('Đã chuyển trạng thái thành công!')
    fetchOrSearch()
  } catch (error) {
    console.error('Lỗi khi chuyển trạng thái:', error?.response?.data || error)
    ElMessage.error('Không thể chuyển trạng thái!')
  }
}

/* ------------ Export Excel ------------ */
const handleExportCommand = (command) => {
  const url = '/admin/campaigns/export-excel'
  const params = {}
  let exportFileName = `dot_giam_gia_${new Date().toLocaleDateString('vi-VN').replace(/\//g, '-')}.xlsx`

  switch (command) {
    case 'selected': {
      if (!selectedRows.value.length) return ElMessage.warning('Vui lòng chọn ít nhất một đợt để xuất.')
      const ids = selectedRows.value.map(r => getField(r, 'id')).filter(Boolean)
      if (!ids.length) return ElMessage.warning('Dòng đã chọn không hợp lệ.')
      params.campaignIds = ids.join(',')
      ElMessage.info(`Đang xuất ${ids.length} đợt đã chọn...`)
      break
    }
    case 'currentPage': {
      if (!campaigns.value.length) return ElMessage.warning('Không có dữ liệu ở trang hiện tại để xuất.')
      const ids = campaigns.value.map(r => getField(r, 'id')).filter(Boolean)
      params.campaignIds = ids.join(',')
      ElMessage.info('Đang xuất các đợt trên trang hiện tại...')
      break
    }
    case 'all': {
      exportFileName = `tat_ca_dot_giam_gia_${new Date().toLocaleDateString('vi-VN').replace(/\//g, '-')}.xlsx`
      ElMessage.info('Đang xuất tất cả...')
      break
    }
    default:
      return ElMessage.error('Hành động xuất không hợp lệ.')
  }

  apiClient.get(url, { params, responseType: 'blob' })
    .then((response) => {
      const file = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const fileURL = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = fileURL
      link.download = exportFileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(fileURL)
      ElMessage.success('Xuất Excel thành công!')
    })
    .catch((error) => {
      console.error('Lỗi khi xuất Excel:', error?.response?.data || error)
      ElMessage.error('Không thể xuất Excel. Vui lòng thử lại.')
    })
}

/* ------------ Pagination / Actions ------------ */
const handlePageChange = (newPage) => { page.value = newPage - 1; fetchOrSearch() }
const handleSizeChange = (newSize) => { size.value = newSize; page.value = 0; fetchOrSearch() }

const onSearch = () => { page.value = 0; fetchOrSearch() }
const onClear = () => {
  search.value = { keyword: '', status: null, createdDate: null }
  page.value = 0
  fetchOrSearch()
}

const goToAddPage = () => router.push('/discount-campaigns/add')
const goToUpdate = (id) => router.push(`/discount-campaigns/update/${id}`)

/* ------------ Mount ------------ */
onMounted(fetchOrSearch)
</script>

<style scoped>
/* helpers giống màn Hóa đơn */
.mb-4 { margin-bottom: 20px; }
.mt-4 { margin-top: 20px; }
.p-4  { padding: 20px; }
.text-end { text-align: right; }
.justify-content-end { display: flex; justify-content: flex-end; }
.my-header { display: flex; flex-direction: row; justify-content: space-between; }
.el-icon--right { margin-left: 8px; }


.font-medium { font-weight: 600; }
.text-muted { color: #6b7280; }
</style>
