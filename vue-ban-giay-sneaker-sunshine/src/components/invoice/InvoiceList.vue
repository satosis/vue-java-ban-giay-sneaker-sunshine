<template>
  <div class="p-4">
    <el-row justify="space-between" align="middle" class="mb-4">
      <el-col :span="12"><h3 class="mb-0">Danh sách hóa đơn</h3></el-col>
      <el-col :span="12" class="text-end">
        <el-dropdown @command="handleExportCommand">
          <el-button type="primary">
            Xuất Excel <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="selected" :disabled="selectedRows.length === 0"
                >Xuất các hóa đơn đã chọn</el-dropdown-item
              >
              <el-dropdown-item command="currentPage" :disabled="invoices.length === 0"
                >Xuất hóa đơn trang này</el-dropdown-item
              >
              <el-dropdown-item command="all">Xuất tất cả hóa đơn</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-col>
    </el-row>

    <div class="mb-4">
      <InvoiceSearch @search="onSearch" @clear="onClear" />
    </div>

    <el-table
      :data="invoices"
      style="width: 100%"
      v-loading="loading"
      border
      ref="invoiceTable"
      @selection-change="handleRowSelection"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column type="index" label="#" width="60" align="center" :index="tableIndex" />

      <el-table-column prop="invoiceCode" label="Mã hóa đơn" width="160">
        <template #default="scope">{{ getField(scope.row, 'invoiceCode') }}</template>
      </el-table-column>

      <el-table-column prop="customerName" label="Khách hàng" min-width="160">
        <template #default="scope">{{
          getField(scope.row, 'customerName') || 'Khách lẻ'
        }}</template>
      </el-table-column>

      <el-table-column prop="phone" label="Số điện thoại" align="center" width="180">
        <template #default="scope">{{ getField(scope.row, 'phone') || 'Khách lẻ' }}</template>
      </el-table-column>

      <el-table-column prop="finalAmount" label="Thành tiền" align="right" width="150">
        <template #default="scope">{{
          formatCurrency(getField(scope.row, 'finalAmount'))
        }}</template>
      </el-table-column>

      <el-table-column prop="orderType" label="Loại đơn hàng" align="center" width="140">
        <template #default="scope">
          <el-tag type="info" disable-transitions>
            {{ getField(scope.row, 'orderType') === 0 ? 'Tại quầy' : 'Online' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="Trạng thái" align="center" width="180">
        <template #default="scope">
          <el-tag
            :type="
              statusClass(
                getField(scope.row, 'status'),
                getField(scope.row, 'statusDetail'),
                getField(scope.row, 'orderType'),
              )
            "
            disable-transitions
          >
            {{
              statusText(
                getField(scope.row, 'status'),
                getField(scope.row, 'statusDetail'),
                getField(scope.row, 'orderType'),
              )
            }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createdDate" label="Ngày tạo" align="center" width="200">
        <template #default="scope">{{ formatDate(getField(scope.row, 'createdDate')) }}</template>
      </el-table-column>

      <el-table-column label="Thao tác" width="120" align="center" fixed="right">
        <template #default="scope">
          <el-button-group>
            <el-button
              :icon="View"
              type="primary"
              size="small"
              @click="viewInvoiceDetails(getField(scope.row, 'id'))"
              title="Xem chi tiết"
            />
            <!-- Sửa ở đây: truyền scope.row -->
            <el-button
              :icon="Printer"
              type="success"
              size="small"
              @click="printInvoice(scope.row)"
              title="In hóa đơn"
            />
          </el-button-group>
        </template>
      </el-table-column>

      <template #empty
        ><p class="text-center text-muted m-4">Không có hóa đơn nào để hiển thị.</p></template
      >
    </el-table>

    <el-pagination
      v-if="totalPages > 0"
      class="mt-4 justify-content-end"
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

    <el-dialog
  v-model="dialogVisible"
  title="Chi tiết hóa đơn"
  width="60%"
  destroy-on-close
>
  <!-- Header tùy biến -->
  <template #header="{ titleId, titleClass }">
    <div class="my-header">
      <h4 :id="titleId" :class="titleClass">
        Chi tiết hóa đơn #{{ selectedInvoice?.invoiceCode || selectedInvoice?.id || '---' }}
      </h4>
    </div>
  </template>

  <div v-if="selectedInvoice">
    <!-- Dòng 1: Khách hàng / Nhân viên -->
    <el-row :gutter="20" class="mb-2">
      <el-col :span="12">
        <strong>Khách hàng:</strong> {{ selectedInvoice.customerName || 'Khách lẻ' }}
      </el-col>
      <el-col :span="12">
        <strong>Nhân viên:</strong> {{ selectedInvoice.employeeName || '---' }}
      </el-col>
    </el-row>

    <!-- Dòng 2: SĐT / Ngày tạo -->
    <el-row :gutter="20" class="mb-2">
      <el-col :span="12">
        <strong>SĐT:</strong> {{ selectedInvoice.phone || '---' }}
      </el-col>
      <el-col :span="12">
        <strong>Ngày tạo:</strong> {{ formatDate(selectedInvoice.createdDate) }}
      </el-col>
    </el-row>

    <!-- Dòng 3: Địa chỉ giao hàng -->
    <el-row :gutter="20" class="mb-2">
      <el-col :span="24">
        <strong>Địa chỉ giao hàng:</strong> {{ selectedInvoice.deliveryAddress || '---' }}
      </el-col>
    </el-row>

    <!-- Dòng 4: Ghi chú -->
    <el-row :gutter="20" class="mb-3">
      <el-col :span="24">
        <strong>Ghi chú:</strong> {{ selectedInvoice.description || '---' }}
      </el-col>
    </el-row>

    <!-- Bảng chi tiết -->
<el-table :data="invoiceDetails" border size="small">
  <el-table-column prop="productName" label="Sản phẩm" />

  <el-table-column prop="quantity" label="Số lượng" align="right" width="120" />

  <el-table-column label="Màu sắc" align="center" width="150">
    <template #default="scope">
      {{ scope.row.color?.colorName || '-' }}
    </template>
  </el-table-column>

  <el-table-column label="Kích thước" align="center" width="150">
    <template #default="scope">
      {{ scope.row.size?.sizeName || '-' }}
    </template>
  </el-table-column>

<el-table-column label="Giá bán" align="right" width="180">
  <template #default="scope">
    <template v-if="hasDiscount(scope.row)">
      <del class="text-gray-500 me-1">{{ formatCurrency(scope.row.sellPrice) }}</del>
      <span class="text-danger">{{ formatCurrency(scope.row.discountedPrice) }}</span>
    </template>
    <template v-else>
      {{ formatCurrency(scope.row.sellPrice) }}
    </template>
  </template>
</el-table-column>


<el-table-column label="Thành tiền" align="right" width="180">
  <template #default="scope">
    {{ formatCurrency(unitPrice(scope.row) * (scope.row.quantity ?? 0)) }}
  </template>
</el-table-column>

</el-table>


    <!-- Tổng kết -->
    <div class="mt-4 text-end">
      <p><strong>Tổng tiền hàng:</strong> {{ formatCurrency(selectedInvoice.totalAmount) }}</p>
      <p><strong>Giảm giá:</strong> {{ formatCurrency(selectedInvoice.discountAmount) }}</p>
      <p><strong>Phí vận chuyển:</strong> {{ formatCurrency(selectedInvoice.shippingFee) }}</p>
      <h4 class="mt-2">
        <strong>Thành tiền:</strong> {{ formatCurrency(selectedInvoice.finalAmount) }}
      </h4>
    </div>
  </div>

  <!-- Footer -->
  <template #footer>
    <span class="dialog-footer">
      <el-button @click="dialogVisible = false">Đóng</el-button>
    </span>
  </template>
</el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '../../utils/axiosInstance.js'
import { View, Printer, ArrowDown } from '@element-plus/icons-vue'
import InvoiceSearch from './InvoiceSearch.vue'
import Swal from 'sweetalert2'
import { ElMessageBox, ElMessage } from 'element-plus'

const invoices = ref([])
const selectedInvoice = ref(null)
const invoiceDetails = ref([])
const page = ref(0)
const size = ref(5)
const totalPages = ref(0)
const totalItems = ref(0)
const isSearching = ref(false)
const loading = ref(true)
const selectedRows = ref([])
const dialogVisible = ref(false)

// điều kiện hiện tại
let currentKeyword = ''
let currentCounterStatusKey = null // tại quầy
let currentOnlineStatusKey = null // online
let currentCreatedDate = null

const EPS = 0.0001 // hoặc 1 nếu giá VND là số nguyên
const toNum = (v) => (v === null || v === undefined ? NaN : Number(v))

const hasDiscount = (row) => {
  const sell = toNum(row?.sellPrice)
  const disc = toNum(row?.discountedPrice)
  // chỉ coi là giảm khi discountedPrice hợp lệ và nhỏ hơn sellPrice
  return !isNaN(sell) && !isNaN(disc) && disc > 0 && disc + EPS < sell
}

const unitPrice = (row) => hasDiscount(row) ? toNum(row.discountedPrice) : toNum(row.sellPrice)


const handleRowSelection = (selection) => {
  selectedRows.value = selection
}
const getField = (inv, field) => inv?.[field] ?? inv?.invoice?.[field]
const tableIndex = (index) => page.value * size.value + index + 1

const formatCurrency = (val) =>
  val == null
    ? '---'
    : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
const formatDate = (val) => {
  if (!val) return '---'
  const d = new Date(val)
  return isNaN(d.getTime())
    ? '---'
    : d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN')
}
const normalizeDateParam = (val) => {
  if (val == null) return undefined
  const s = String(val).trim()
  if (!s || s === 'null' || s === 'undefined') return undefined
  if (val instanceof Date && !isNaN(val.getTime())) {
    const y = val.getFullYear(),
      m = String(val.getMonth() + 1).padStart(2, '0'),
      d = String(val.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  const m = s.match(/^(\d{4}-\d{2}-\d{2})/)
  return m ? m[1] : /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : undefined
}

const buildParams = () => {
  const params = { page: Number(page.value) ?? 0, size: Number(size.value) ?? 10 }
  if (isSearching.value) {
    const kw = (currentKeyword ?? '').trim()
    if (kw) params.keyword = kw
    if (currentCounterStatusKey) params.counterStatusKey = currentCounterStatusKey
    if (currentOnlineStatusKey) params.onlineStatusKey = currentOnlineStatusKey
    const cd = normalizeDateParam(currentCreatedDate)
    if (cd) params.createdDate = cd
  }
  return params
}

const fetchOrSearch = async () => {
  loading.value = true
  const endpoint = isSearching.value ? '/admin/invoices/search' : '/admin/invoices'
  const params = buildParams()
  try {
    const res = await apiClient.get(endpoint, { params })
    const items = res?.data?.content ?? []
    invoices.value = items.map((it) => it?.invoice ?? it)
    const meta = res?.data?.page ?? res?.data ?? {}
    totalPages.value = meta?.totalPages ?? 0
    totalItems.value = meta?.totalElements ?? 0
    page.value = meta?.number ?? params.page
    size.value = meta?.size ?? params.size
  } catch (err) {
    console.error('Lỗi khi tải hóa đơn:', err?.response?.data || err)
    Swal.fire('Lỗi', err?.response?.data?.message || 'Không thể tải danh sách hóa đơn.', 'error')
  } finally {
    loading.value = false
  }
}

const viewInvoiceDetails = async (invoiceId) => {
  if (!invoiceId) return
  try {
    const res = await apiClient.get(`/admin/invoices/${invoiceId}/detail`)
    selectedInvoice.value = res?.data?.invoice ?? res?.data ?? null
    invoiceDetails.value =
      res?.data?.invoice?.invoiceDetails ??
      res?.data?.invoiceDetails ??
      selectedInvoice.value?.invoiceDetails ??
      []
    dialogVisible.value = true
  } catch (err) {
    console.error('Lỗi khi xem chi tiết hóa đơn:', err?.response?.data || err)
    Swal.fire('Lỗi', 'Không thể tải chi tiết hóa đơn.', 'error')
  }
}

const printInvoice = (row) => {
  const invoiceId = getField(row, 'id')
  const orderType = getField(row, 'orderType') // 0: tại quầy, 1: online
  const finalAmount = getField(row, 'finalAmount') || 0

  if (!invoiceId) return

  // Chặn in nếu hóa đơn <= 0đ
  if (finalAmount <= 0) {
    ElMessage.warning('Hóa đơn có tổng tiền bằng 0đ, không thể in.')
    return
  }

  ElMessageBox.confirm('Bạn có chắc muốn in hóa đơn?', 'Xác nhận', {
    confirmButtonText: 'Có, In ngay!',
    cancelButtonText: 'Hủy',
    type: 'warning',
  })
    .then(() => {
      const path =
        orderType === 1
          ? `/admin/invoices/${invoiceId}/export-online`
          : `/admin/invoices/${invoiceId}/export-id`

      return apiClient.get(path, { responseType: 'blob' })
    })
    .then((response) => {
      if (!response) return
      const file = new Blob([response.data], { type: 'application/pdf' })
      const url = URL.createObjectURL(file)
      window.open(url)

      // Nếu muốn auto download:
      const a = document.createElement('a')
      a.href = url
      a.download = `HoaDon-${invoiceId}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    })
    .catch((err) => {
      if (err === 'cancel') return
      console.error('Lỗi khi in hóa đơn:', err?.response?.data || err)
      ElMessage.error('Không thể in hóa đơn. Vui lòng thử lại.')
    })
}

const handleExportCommand = (command) => {
  const url = '/admin/invoices/export-excel'
  const params = {}
  let exportFileName = `hoa_don_${new Date().toLocaleDateString('vi-VN').replace(/\//g, '-')}.xlsx`

  switch (command) {
    case 'selected': {
      if (!selectedRows.value.length)
        return ElMessage.warning('Vui lòng chọn ít nhất một hóa đơn để xuất.')
      const ids = selectedRows.value.map((r) => getField(r, 'id')).filter(Boolean)
      if (!ids.length) return ElMessage.warning('Dòng đã chọn không hợp lệ.')
      params.invoiceIds = ids.join(',')
      ElMessage.info(`Đang xuất ${ids.length} hóa đơn đã chọn...`)
      break
    }
    case 'currentPage': {
      if (!invoices.value.length)
        return ElMessage.warning('Không có hóa đơn nào ở trang hiện tại để xuất.')
      const ids = invoices.value.map((r) => getField(r, 'id')).filter(Boolean)
      params.invoiceIds = ids.join(',')
      ElMessage.info('Đang xuất các hóa đơn trên trang hiện tại...')
      break
    }
    case 'all': {
      exportFileName = `tat_ca_hoa_don_${new Date().toLocaleDateString('vi-VN').replace(/\//g, '-')}.xlsx`
      ElMessage.info('Đang xuất tất cả hóa đơn...')
      break
    }
    default:
      return ElMessage.error('Hành động xuất không hợp lệ.')
  }

  apiClient
    .get(url, { params, responseType: 'blob' })
    .then((response) => {
      const file = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
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

const handlePageChange = (newPage) => {
  page.value = newPage - 1
  fetchOrSearch()
}
const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 0
  fetchOrSearch()
}

// nhận từ component con: keyword, counterStatusKey (quầy), onlineStatusKey (online), createdDate
const onSearch = ({ keyword, counterStatusKey, onlineStatusKey, createdDate }) => {
  page.value = 0
  currentKeyword = (keyword ?? '').trim()
  currentCounterStatusKey = counterStatusKey || null
  currentOnlineStatusKey = onlineStatusKey || null
  currentCreatedDate = normalizeDateParam(createdDate) || null
  isSearching.value = !!(
    currentKeyword ||
    currentCounterStatusKey ||
    currentOnlineStatusKey ||
    currentCreatedDate
  )
  fetchOrSearch()
}

const onClear = () => {
  isSearching.value = false
  currentKeyword = ''
  currentCounterStatusKey = null
  currentOnlineStatusKey = null
  currentCreatedDate = null
  page.value = 0
  fetchOrSearch()
}

/* ===== Hiển thị label & màu trạng thái ===== */
const statusText = (status, statusDetail, orderType) => {
  if (orderType === 0) {
    switch (status) {
      case 'DANG_XU_LY':
        return 'Chờ xử lý'
      case 'THANH_CONG':
        return 'Thành công'
      case 'DA_HUY':
        return 'Đã hủy'
      case 'HUY_GIAO_DICH':
        return 'Hủy giao dịch'
      case 'TRA_HANG':
        return 'Trả hàng'
      case 'KHIEU_NAI':
        return 'Khiếu nại'
      default:
        return 'Không xác định'
    }
  } else if (orderType === 1) {
    switch (statusDetail) {
      case 'CHO_XU_LY':
        return 'Chờ xử lý'
      case 'DA_XU_LY':
        return 'Đã xử lý'
      case 'CHO_GIAO_HANG':
        return 'Chờ giao hàng'
      case 'DANG_GIAO_HANG':
        return 'Đang giao hàng'
      case 'GIAO_THANH_CONG':
        return 'Giao hàng thành công'
      case 'GIAO_THAT_BAI':
        return 'Giao hàng thất bại'
      case 'HUY_DON':
        return 'Hủy đơn'
      case 'HUY_GIAO_DICH':
        return 'Hủy giao dịch'
      case 'DANG_GIAO_DICH':
        return 'Đang giao dịch'
      case 'MAT_HANG':
        return 'Mất hàng'
      case 'DA_HOAN_TIEN':
        return 'Đã hoàn tiền'
      case 'DA_HOAN_THANH':
        return 'Đã hoàn thành'
      default:
        return 'Không xác định'
    }
  }
  return 'Không xác định'
}

const statusClass = (status, statusDetail, orderType) => {
  if (orderType === 0) {
    switch (status) {
      case 'DANG_XU_LY':
        return 'warning'
      case 'THANH_CONG':
        return 'success'
      case 'DA_HUY':
        return 'danger'
      case 'HUY_GIAO_DICH':
        return 'danger'
      case 'TRA_HANG':
        return 'info'
      case 'KHIEU_NAI':
        return 'info'
      default:
        return 'info'
    }
  } else if (orderType === 1) {
    switch (statusDetail) {
      case 'CHO_XU_LY':
        return 'warning'
      case 'DA_XU_LY':
        return 'info'
      case 'CHO_GIAO_HANG':
        return 'info'
      case 'DANG_GIAO_HANG':
        return 'info'
      case 'GIAO_THANH_CONG':
        return 'success'
      case 'GIAO_THAT_BAI':
        return 'danger'
      case 'HUY_DON':
        return 'danger'
      case 'HUY_GIAO_DICH':
        return 'danger'
      case 'DANG_GIAO_DICH':
        return 'info'
      case 'MAT_HANG':
        return 'danger'
      case 'DA_HOAN_TIEN':
        return 'info'
      case 'DA_HOAN_THANH':
        return 'success'
      default:
        return 'info'
    }
  }
  return 'info'
}

onMounted(fetchOrSearch)
</script>

<style scoped>
.mb-4 {
  margin-bottom: 20px;
}
.mt-4 {
  margin-top: 20px;
}
.p-4 {
  padding: 20px;
}
.text-end {
  text-align: right;
}
.justify-content-end {
  justify-content: flex-end;
}
.my-header {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}
.el-icon--right {
  margin-left: 8px;
}
</style>
