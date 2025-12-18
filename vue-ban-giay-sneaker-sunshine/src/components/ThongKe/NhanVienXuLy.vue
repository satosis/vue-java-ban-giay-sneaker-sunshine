<template>
  <div class="employee-sales-report-container">
    <h2>Báo Cáo Doanh Số Nhân Viên</h2>

    <!-- Nút quay lại -->
    <div class="back-btn">
      <el-button type="default" @click="goBack">Quay lại</el-button>
    </div>

    <!-- Form lọc -->
    <el-form :inline="true" :model="filters" class="filters-form" label-width="120px">
      <el-form-item label="Tên nhân viên">
        <el-select
          v-model="filters.employeeId"
          placeholder="Chọn nhân viên"
          filterable
          clearable
        >
          <el-option
            v-for="option in employeeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="Ngày bắt đầu">
        <el-date-picker
          v-model="filters.startDate"
          type="date"
          placeholder="Ngày bắt đầu"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>

      <el-form-item label="Ngày kết thúc">
        <el-date-picker
          v-model="filters.endDate"
          type="date"
          placeholder="Ngày kết thúc"
          value-format="YYYY-MM-DD"
          clearable
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="fetchData">Lọc</el-button>
        <el-button @click="resetFilters">Reset</el-button>
      </el-form-item>
    </el-form>

    <!-- Bảng báo cáo -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="reports"
        border
        class="sales-table"
        empty-text="Không có dữ liệu phù hợp."
      >
        <el-table-column prop="employeeName" label="Nhân viên" width="180"></el-table-column>

        <el-table-column label="Tổng đơn" align="center">
          <el-table-column prop="totalInvoices" label="Đơn" width="100" />
          <el-table-column prop="totalProducts" label="SLSP" width="100" />
          <el-table-column prop="totalRevenue" label="Doanh thu" width="160">
            <template #default="scope">
              <span>{{ formatCurrency(scope.row.totalRevenue) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="Thành công" align="center">
          <el-table-column prop="successInvoices" label="Đơn" width="100" />
          <el-table-column prop="successProducts" label="SLSP" width="100" />
          <el-table-column prop="successRevenue" label="Doanh thu" width="160">
            <template #default="scope">
              <span>{{ formatCurrency(scope.row.successRevenue) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="Hủy" align="center">
          <el-table-column prop="cancelledInvoices" label="Đơn" width="150" />
          <el-table-column prop="cancelledProducts" label="SLSP" width="150" />
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// Dữ liệu và trạng thái
const reports = ref([])
const loading = ref(true)
const error = ref('')
const filters = ref({
  employeeId: '',
  startDate: null,
  endDate: null,
})

const router = useRouter()

const goBack = () => {
  router.back() // Quay lại trang trước
}

// Dữ liệu nhân viên
const employeeData = ref([])

// Computed list cho select
const employeeOptions = computed(() =>
  employeeData.value.map(emp => ({
    label: `${emp.employeeName} (${emp.employeeCode})`,
    value: emp.id,
  }))
)

// Hàm gọi API báo cáo
const fetchData = async () => {
  loading.value = true
  error.value = ''

  const requestBody = {
    employeeId: filters.value.employeeId || null,
    startDate: filters.value.startDate,
    endDate: filters.value.endDate,
  }

  try {
    const response = await apiClient.post('/admin/statistics/employee-sales', requestBody)
    reports.value = response.data
  } catch (err) {
    console.error('Lỗi khi tải dữ liệu:', err)
    error.value = err.response?.data?.message || 'Không thể tải dữ liệu báo cáo.'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

// Hàm load dữ liệu nhân viên
const getEmployeeData = async () => {
  try {
    const response = await apiClient.get('/admin/employees/get-data')
    employeeData.value = response.data
  } catch (err) {
    console.error('Lỗi khi lấy dữ liệu nhân viên:', err)
  }
}

// Hàm reset filter
const resetFilters = () => {
  filters.value.employeeId = ''
  filters.value.startDate = null
  filters.value.endDate = null
  fetchData()
}

// Hàm định dạng tiền tệ
const formatCurrency = value => {
  if (value === null || value === undefined) return '0 VND'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

// Gọi khi mount
onMounted(() => {
  getEmployeeData()
  fetchData()
})
</script>

<style scoped>
.employee-sales-report-container {
  font-family: Arial, sans-serif;
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

h2 {
  text-align: center;
  margin-bottom: 20px;
}

.filters-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  align-items: flex-end;
  margin-bottom: 20px;
  justify-content: flex-start;
}

.filters-form .el-form-item {
  flex: 1 1 200px;
  min-width: 200px;
}

.filters-form .el-form-item:last-child {
  flex: 0 0 auto;
}

.table-container {
  min-height: 300px;
  position: relative;
}

.sales-table {
  width: 100%;
}
</style>
