<template>
  <div class="p-4 bg-light">
    <el-card class="mb-4 shadow-sm card-filter">
      <el-form
        :inline="true"
        :model="filters"
        class="form-bar flex flex-wrap items-end gap-4"
        label-position="top"
      >
        <el-form-item label="Tìm kiếm (Mã hóa đơn)" class="w-64">
          <el-input
            v-model="filters.code"
            placeholder="Nhập mã hóa đơn"
            clearable
            prefix-icon="el-icon-search"
          />
        </el-form-item>

        <el-form-item label="Tìm kiếm (Số điện thoại khách hàng)" class="w-64">
          <el-input
            v-model="filters.phone"
            placeholder="Nhập số điện thoại khách hàng"
            clearable
            prefix-icon="el-icon-search"
          />
        </el-form-item>

        <el-form-item label="Tìm kiếm (Trạng thái hóa đơn)" class="w-64">
          <el-select
            v-model="filters.isPaid"
            placeholder="Chọn trạng thái hóa đơn"
            clearable
          >
            <el-option label="Đã thanh toán" :value="true" />
            <el-option label="Chưa thanh toán" :value="false" />
          </el-select>
        </el-form-item>

        <el-form-item label="Khoảng thời gian tạo" class="w-96">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="→"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="DD/MM/YYYY HH:mm:ss"
            clearable
            class="w-full"
            unlink-panels
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="search">Tìm kiếm</el-button>
          <el-button icon="el-icon-refresh" @click="resetFilters">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs
      v-model="currentTab"
      @tab-change="handleTabChange"
      type="border-card"
      class="mb-4 shadow-sm"
    >
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.key"
        :label="`${tab.label} (${tab.count})`"
        :name="tab.key"
      />
    </el-tabs>

    <el-table
      :data="invoices"
      v-loading="loading"
      border
      stripe
      class="shadow-sm"
      row-class-name="table-row"
      height="auto"
    >
      <el-table-column label="#" type="index" width="60" align="center" />
      <el-table-column prop="invoiceCode" label="Mã hóa đơn" width="160" />
      <el-table-column prop="customerName" label="Khách hàng" />
      <el-table-column prop="phoneSender" label="SĐT" width="130" />
      <el-table-column label="Ngày tạo" width="220">
        <template #default="scope">
          <el-tooltip
            effect="dark"
            :content="formatDateTime(scope.row.createdDate)"
            placement="top"
          >
            <span>{{ formatDateTime(scope.row.createdDate) }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="finalAmount" label="Tổng tiền" width="150" align="right">
        <template #default="scope">
          {{ scope.row.finalAmount.toLocaleString() }} ₫
        </template>
      </el-table-column>
      <el-table-column label="Hành động" width="160" align="center">
        <template #default="scope">
          <el-button
            size="small"
            type="primary"
            icon="el-icon-view"
            @click="goToStatusPage(scope.row.id)"
          >
            Chi tiết
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import apiClient from '@/utils/axiosInstance'
import { ElMessage } from 'element-plus'

const router = useRouter()

const filters = ref({
  code: '',
  phone: '',
  isPaid: null,
  dateRange: [],
})

const currentTab = ref('CHO_XU_LY')
const invoices = ref([])
const loading = ref(false)

const tabs = ref([
  { key: 'CHO_XU_LY', label: 'Chờ xử lý', count: 0 },
  { key: 'DA_XU_LY', label: 'Đã xử lý', count: 0 },
  { key: 'CHO_GIAO_HANG', label: 'Chờ giao hàng', count: 0 },
  { key: 'DANG_GIAO_HANG', label: 'Đang giao hàng', count: 0 },
  { key: 'GIAO_THANH_CONG', label: 'Giao hàng thành công', count: 0 },
  { key: 'GIAO_THAT_BAI', label: 'Giao hàng thất bại', count: 0 },
  { key: 'HUY_DON', label: 'Đơn hàng hủy', count: 0 },
])

const statusLabelToCode = (label) => {
  const map = {
    'DANG_GIAO_DICH': -3,
    'HUY_DON': -2,
    'HUY_GIAO_DICH': -1,
    'CHO_XU_LY': 0,
    'DA_XU_LY': 1,
    'CHO_GIAO_HANG': 2,
    'DANG_GIAO_HANG': 3,
    'GIAO_THANH_CONG': 4,
    'GIAO_THAT_BAI': 5,
    'MAT_HANG': 6,
    'DA_HOAN_TIEN': 7,
  }
  return map[label] ?? null
}

const search = async () => {
  loading.value = true
  try {
    const requestBody = {
      statusDetail: statusLabelToCode(currentTab.value),
      phone: filters.value.phone || null,
      code: filters.value.code || null,
      createdFrom: filters.value.dateRange[0] || null,
      createdTo: filters.value.dateRange[1] || null,
      isPaid: filters.value?.isPaid ?? null
    }

    const res = await apiClient.post('/admin/online-sales/search', requestBody)
    invoices.value = res.data ?? []

    if (invoices.value.length > 0) {
      const f = invoices.value[0]

      const counters = {
        CHO_XU_LY:       f.totalChoXacNhan ?? 0,
        DA_XU_LY:        f.totalDaXuLy    ?? 0,
        CHO_GIAO_HANG:   f.totalChoGiao   ?? 0,
        DANG_GIAO_HANG:  f.totalDangGiao  ?? 0,
        GIAO_THANH_CONG: f.totalThanhCong ?? 0,
        GIAO_THAT_BAI:   f.totalThatBai   ?? 0,
        HUY_DON:         f.totalHuyDon    ?? 0,
      }

      tabs.value = tabs.value.map(tab => ({
        ...tab,
        count: counters[tab.key] ?? 0
      }))
    } else {

    }
  } catch (err) {
    console.error('Lỗi tìm kiếm:', err)
    ElMessage.error('Đã xảy ra lỗi khi tìm kiếm hóa đơn.')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.value = {
    code: '',
    phone: '',
    isPaid: null,
    dateRange: [],
  }
  search()
}

const handleTabChange = () => {
  search()
}

const goToStatusPage = (invoiceId) => {
  router.push({ name: 'InvoiceStatus', params: { invoiceId } })
}

const formatDateTime = (dateStr) => {
  return dayjs(dateStr).format('DD/MM/YYYY HH:mm:ss')
}

onMounted(() => {
  search()
})
</script>

<style scoped>
.form-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.card-filter {
  border-radius: 12px;
  background-color: #fff;
  padding: 16px;
}

.el-table .table-row:hover {
  background-color: #f5f7fa;
}

.justify-end {
  display: flex;
  justify-content: flex-end;
}

.bg-light {
  background-color: #fafafa;
  min-height: 100vh;
}

.w-64 {
  width: 256px;
}

.w-96 {
  width: 384px;
}

.w-full {
  width: 100%;
}
</style>
