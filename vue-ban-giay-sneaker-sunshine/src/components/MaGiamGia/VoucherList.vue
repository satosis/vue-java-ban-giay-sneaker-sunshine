<template>
  <div class="voucher-page" ref="pageRef">
    <!-- Header -->
    <div class="page-header" ref="headerRef">
      <div class="title-wrap">
        <el-icon class="title-icon"><Ticket /></el-icon>
        <div>
          <h2 class="title">Quản lý Voucher</h2>
          <div class="subtitle">Tìm kiếm, xuất Excel và quản trị voucher</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="success" size="small" @click="exportExcel">Xuất Excel</el-button>
        <el-button type="primary" size="small" :icon="Plus" @click="onAddVoucher">Thêm Voucher</el-button>
      </div>
    </div>

    <!-- Bộ lọc -->
    <el-card shadow="never" class="filter-card" ref="filterRef">
      <el-form :inline="true" :model="searchVoucher" class="filter-form" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="searchVoucher.keyword"
            placeholder="Tìm mã / tên voucher"
            clearable
            class="w-72"
            @keyup.enter="fetchVoucher(0)"
          />
        </el-form-item>

        <el-form-item>
          <el-select v-model="searchVoucher.status" placeholder="Trạng thái" clearable class="w-48" @change="fetchVoucher(0)">
            <el-option label="Tất cả" :value="null" />
            <el-option label="Đang diễn ra" :value="1" />
            <el-option label="Sắp diễn ra" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-select v-model="searchVoucher.orderType" placeholder="Loại đơn hàng" clearable class="w-48" @change="fetchVoucher(0)">
            <el-option label="Tại quầy" :value="0" />
            <el-option label="Online" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-select
            v-model="searchVoucher.categoryIds"
            placeholder="Danh mục"
            clearable
            filterable
            class="w-60"
            @change="fetchVoucher(0)"
          >
            <el-option
              v-for="c in categoryList"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <div class="filter-buttons">
          <el-button type="primary" size="small" @click="fetchVoucher(0)">Tìm</el-button>
          <el-button size="small" @click="resetForm">Xóa bộ lọc</el-button>
        </div>
      </el-form>
    </el-card>

    <!-- Bảng dữ liệu -->
    <el-card shadow="never" class="table-card" ref="tableCardRef">
      <el-table
        :data="vouchers"
        border
        stripe
        size="large"
        style="width: 100%"
        :row-key="row => row.id"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        element-loading-text="Đang tải dữ liệu..."
        empty-text="Chưa có voucher phù hợp."
        :height="tableHeight"
        :show-header="true"
        class="responsive-voucher-table"
      >
        <el-table-column type="selection" width="56" />

        <!-- Mã -->
        <el-table-column
          prop="voucherCode"
          label="Mã"
          :min-width="40"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="mono strong">{{ row.voucherCode || '-' }}</span>
          </template>
        </el-table-column>

        <!-- Tên -->
        <el-table-column
          prop="voucherName"
          label="Tên"
          :min-width="30"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="text">{{ row.voucherName || '-' }}</span>
          </template>
        </el-table-column>

        <!-- Giảm % -->
        <el-table-column
          label="Giảm (%)"
          :min-width="30"
          align="center"
        >
          <template #default="{ row }">
            <el-tag v-if="row.discountPercentage" type="warning" effect="light">
              {{ Number(row.discountPercentage) }}%
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <!-- Tiền giảm -->
        <el-table-column
          label="Tiền giảm"
          :min-width="20"
          align="right"
        >
          <template #default="{ row }">
            <span class="mono">{{ row.discountAmount ? toVND(row.discountAmount) : '-' }}</span>
          </template>
        </el-table-column>

        <!-- Tối thiểu / Tối đa -->
        <el-table-column
          label="Tối thiểu / Tối đa"
          :min-width="40"
          align="right"
          class-name="col-minmax"
        >
          <template #default="{ row }">
            <div class="minmax">
              <div class="mono small-line">{{ row.minOrderValue ? toVND(row.minOrderValue) : '-' }}</div>
              <div class="mono small-line muted">{{ row.maxDiscountValue ? toVND(row.maxDiscountValue) : '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <!-- Loại đơn -->
        <el-table-column
          label="Loại đơn"
          :min-width="20"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.orderType === 0 ? 'success' : row.orderType === 1 ? 'info' : 'default'" effect="light">
              {{ row.orderType === 0 ? 'Tại quầy' : row.orderType === 1 ? 'Online' : '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Trạng thái -->
        <el-table-column
          label="Trạng thái"
          :min-width="35"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- Thao tác (rộng hơn) -->
        <el-table-column
          label="Thao tác"
          :min-width="28"
          align="center"
          fixed="right"
          class-name="col-actions"
        >
          <template #default="{ row }">
            <div class="actions-cell">
              <el-tooltip content="Lịch sử" placement="top">
                <el-button type="primary" :icon="Reading" size="small" circle @click="onHistory(row)" />
              </el-tooltip>

              <el-tooltip content="Sửa" placement="top">
                <el-button type="primary" :icon="Edit" size="small" circle @click="onEditVoucher(row)" />
              </el-tooltip>

              <el-tooltip content="Xóa" placement="top">
                <el-button type="danger" :icon="Delete" size="small" circle @click="onDeleteVoucher(row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" ref="paginationRef">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="page + 1"
          :page-size="size"
          :total="totalElements"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Ticket, Edit, Delete, Plus, Reading } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const vouchers = ref([])
const totalElements = ref(0)
const page = ref(0)
const size = ref(10)
const categoryList = ref([])
const selectedVouchers = ref([])
const loading = ref(false)

const pageRef = ref(null)
const headerRef = ref(null)
const filterRef = ref(null)
const tableCardRef = ref(null)
const paginationRef = ref(null)

const tableHeight = ref(420)

const router = useRouter()

const searchVoucher = ref({
  keyword: null,
  status: null,
  orderType: null,
  voucherType: null,
  categoryIds: null,
  productId: null,
})

const computeTableHeight = async () => {
  await nextTick()
  try {
    const headerH = headerRef.value ? headerRef.value.getBoundingClientRect().height : 0
    const filterH = filterRef.value ? filterRef.value.getBoundingClientRect().height : 0
    const paginationH = paginationRef.value ? paginationRef.value.getBoundingClientRect().height : 0
    const pageTop = pageRef.value ? pageRef.value.getBoundingClientRect().top : 0
    const buffer = 48
    const available = window.innerHeight - pageTop - headerH - filterH - paginationH - buffer
    tableHeight.value = Math.max(320, Math.min(available, window.innerHeight - 160))
  } catch (e) {
    tableHeight.value = Math.max(420, window.innerHeight - 320)
  }
}

/* Fetch / actions */
const resetForm = async () => {
  searchVoucher.value = {
    keyword: null,
    status: 1,
    orderType: null,
    voucherType: null,
    categoryIds: null,
    productId: null,
  }
  page.value = 0
  selectedVouchers.value = []
  await fetchVoucher()
}

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/admin/categories/hien-thi')
    categoryList.value = response.data || []
  } catch (error) {
    console.error('Lỗi lấy danh mục:', error)
    ElMessage.error(`Lấy danh mục thất bại: ${error.message}`)
    categoryList.value = []
  }
}

const fetchVoucher = async (newPage = 0) => {
  try {
    loading.value = true
    const searchParams = {
      keyword: searchVoucher.value.keyword?.trim() || null,
      status: searchVoucher.value.status ?? null,
      orderType: searchVoucher.value.orderType ?? null,
      voucherType: searchVoucher.value.voucherType ?? null,
      categoryId: searchVoucher.value.categoryIds ?? null,
      page: newPage,
      size: size.value,
    }

    const requestBody = Object.keys(searchParams).every(
      (key) => searchParams[key] === null || (Array.isArray(searchParams[key]) && searchParams[key].length === 0)
    ) ? {} : searchParams

    const response = await apiClient.post('/admin/vouchers/search', requestBody)
    vouchers.value = response?.data?.data || []
    totalElements.value = response?.data?.pagination?.totalElements ?? 0
    page.value = newPage
    selectedVouchers.value = []

    if (!vouchers.value.length) {
      ElMessage.warning('Không tìm thấy voucher nào!')
    }

    await nextTick()
    computeTableHeight()
  } catch (error) {
    console.error('Lỗi tải danh sách voucher:', error?.response?.data || error)

    // ===== THÊM NHÁNH 403 giống fetchCustomers =====
    if (error?.response?.status === 403) {
      router.push('/error')
      return
    }

    ElMessage.error(`Tải danh sách voucher thất bại: ${error?.message || 'Vui lòng thử lại sau.'}`)
    vouchers.value = []
    totalElements.value = 0
    // page giữ nguyên theo newPage để đồng bộ UI
  } finally {
    loading.value = false
  }
}


/* selection & pagination */
const handleSelectionChange = (val) => {
  selectedVouchers.value = val.map(voucher => voucher.id)
}
const handlePageChange = (newPage) => { fetchVoucher(newPage - 1) }
const handlePageSizeChange = (newSize) => { size.value = newSize; fetchVoucher(0) }

const exportExcel = async () => {
  const ids = (selectedVouchers.value || [])
    .map(v => (typeof v === 'object' ? (v.id ?? v.value ?? v) : v))
    .map(val => (typeof val === 'string' ? Number(val) : val))
    .filter(n => Number.isFinite(n))

  if (ids.length === 0) {
    ElMessage.warning('Vui lòng chọn ít nhất một voucher để xuất!')
    return
  }

  try {
    const res = await apiClient.post('/admin/vouchers/export-excel/by-ids', ids, { responseType: 'blob' })
    const disposition = res.headers?.['content-disposition'] || ''
    const match = /filename\*?=(?:UTF-8'')?["']?([^;"']+)/i.exec(disposition)
    const filename = match?.[1] ? decodeURIComponent(match[1]) : 'vouchers-by-ids.xlsx'

    const blob = new Blob([res.data], {
      type: res.headers?.['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)

    ElMessage.success('Xuất Excel thành công!')
  } catch (error) {
    let msg = error?.message || 'Không rõ nguyên nhân'
    try {
      if (error?.response?.data instanceof Blob) {
        const text = await error.response.data.text()
        if (text) msg = text
      }
    } catch (_) {}
    console.error('Lỗi khi xuất Excel:', error)
    ElMessage.error(`Xuất Excel thất bại: ${msg}`)
  }
}

/* Navigate */
const onAddVoucher = () => { router.push({ name: 'AddVoucher' }); ElMessage.success('Chuyển đến trang thêm voucher!') }
const onEditVoucher = (row) => { router.push({ name: 'UpdateVoucher', params: { id: row.id } }); ElMessage.success('Chuyển đến trang sửa voucher!') }
const onHistory = (row) => { router.push({ name: 'VoucherHistory', params: { id: row.id } }); ElMessage.success('Chuyển đến trang history voucher!') }
const onDeleteVoucher = (row) => {
  ElMessageBox.confirm(`Xác nhận xóa voucher ${row.voucherCode}?`, 'Xác nhận xóa', {
    confirmButtonText: 'Xóa',
    cancelButtonText: 'Hủy',
    type: 'warning',
  })
    .then(async () => {
      try {
        await apiClient.delete(`/admin/vouchers/${row.id}`)
        ElMessage.success('Xóa voucher thành công!')
        await fetchVoucher(page.value)
      } catch (error) {
        console.error('Lỗi khi xóa:', error)
        ElMessage.error(`Xóa voucher thất bại: ${error.message}`)
      }
    })
    .catch(() => {
      ElMessage.info('Đã hủy xóa voucher')
    })
}

/* utils hiển thị */
const toVND = (v) => Number(v).toLocaleString('vi-VN') + ' ₫'
const formatDate = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return '-'
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}
const statusText = (s) => (s === 1 ? 'Đang diễn ra' : s === 2 ? 'Sắp diễn ra' : s === 0 ? 'Ngừng hoạt động' : '-')
const statusTagType = (s) => (s === 1 ? 'success' : s === 2 ? 'warning' : s === 0 ? 'info' : 'default')

/* lifecycles */
let ro = null
const onWindowResize = () => computeTableHeight()

onMounted(async () => {
  await computeTableHeight()
  await Promise.all([fetchCategories(), fetchVoucher()])

  window.addEventListener('resize', onWindowResize)

  if (window.ResizeObserver) {
    ro = new ResizeObserver(() => computeTableHeight())
    if (headerRef.value) ro.observe(headerRef.value)
    if (filterRef.value) ro.observe(filterRef.value)
    if (tableCardRef.value) ro.observe(tableCardRef.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize)
  if (ro) {
    try { ro.disconnect() } catch (_) {}
    ro = null
  }
})
</script>

<style scoped>
.voucher-page {
  max-width: 96vw;
  margin: 20px auto 40px;
  padding: 0 16px;
  box-sizing: border-box;
}
@media (min-width: 1400px) {
  .voucher-page { max-width: 1680px; }
}

/* Header */
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px;
}
.title-wrap { display: flex; align-items: center; gap: 12px; }
.title-icon { font-size: 26px; }
.title { margin: 0; font-size: 24px; font-weight: 700; color: #1f2937; }
.subtitle { font-size: 12px; color: #6b7280; }
.header-actions { display: flex; gap: 8px; }

/* Cards */
.filter-card, .table-card {
  border-radius: 12px;
  border: 1px solid #ebeef5;
}
.filter-card { margin-bottom: 12px; }

/* Filter form */
.filter-form { display: flex; align-items: center; flex-wrap: wrap; gap: 10px 14px; }
.w-48 { width: 192px; }
.w-60 { width: 240px; }
.w-72 { width: 288px; }
.filter-buttons { display: flex; gap: 8px; margin-left: auto; }

/* Table */
.table-card {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-sizing: border-box;
}

/* ensure ellipsis */
.responsive-voucher-table .cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* minmax small lines */
.minmax .small-line { line-height: 1; font-size: 12px; }
.minmax .muted { color: #6b7280; }

/* actions */
.col-actions { min-width: 240px; padding-right: 8px; box-sizing: border-box; }
.actions-cell {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
}
.actions-cell .el-button { padding: 6px; }

/* pagination */
.pagination-wrap {
  display: flex; justify-content: flex-end; padding: 12px 8px 4px;
  background: transparent;
}

/* typography */
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; }
.strong { font-weight: 600; }
.text { color: #111827; }

/* responsive hide columns progressively */
@media (max-width: 1100px) {
  .col-datetime { display: none; }
}
@media (max-width: 900px) {
  .col-minmax { display: none; }
}
@media (max-width: 700px) {
  /* fallback hide specific columns for very small screens */
  .responsive-voucher-table .el-table__body-wrapper td:nth-child(4),
  .responsive-voucher-table .el-table__header-wrapper th:nth-child(4) {
    display: none;
  }
  .filter-buttons { margin-left: 0; }
}

/* small screens */
@media (max-width: 768px) {
  .voucher-page { padding: 0 12px; }
  .filter-buttons { width: 100%; justify-content: flex-start; margin-left: 0; }
  .page-header { flex-direction: column; align-items: flex-start; gap: 8px; }
}
</style>
