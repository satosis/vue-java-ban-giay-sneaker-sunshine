<template>
  <div class="p-6 md:p-8 bg-gray-50 min-h-screen">
    <div class="max-w-7xl mx-auto">
      <h2 class="text-3xl font-bold mb-8 text-gray-800">Tổng Quan Thống Kê</h2>

<div class="filter-container">
    <label for="typeSelect" class="form-label">Chọn loại thống kê:</label>
    <select
      id="typeSelect"
      v-model="type"
      @change="loadRevenue"
      class="form-select"
    >
      <option value="day">Ngày</option>
      <option value="week">Tuần</option>
      <option value="quarter">Quý</option>
      <option value="year">Năm</option>
    </select>

    <div class="result-box mt-3">
      <h5>Kết quả API:</h5>
      <pre>{{ revenueData }}</pre>
    </div>
  </div>

      <div v-if="isLoading">
        <el-row :gutter="24">
          <el-col :span="12" v-for="n in 4" :key="n" class="mb-6">
            <el-card shadow="never" class="border border-gray-200 rounded-xl">
              <el-skeleton :rows="5" animated />
            </el-card>
          </el-col>
          <el-col :span="24" class="mb-6">
            <el-card shadow="never" class="border border-gray-200 rounded-xl">
              <el-skeleton :rows="6" animated />
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-alert
        v-else-if="error"
        title="Không thể tải dữ liệu"
        :description="error"
        type="error"
        show-icon
        :closable="false"
      />



      <el-row :gutter="24" v-else>
        <el-col :span="24" class="mb-6">
          <div class="flex justify-end">
            <el-button
              type="primary"
              size="medium"
              icon="el-icon-s-data"
              @click="goToOrderStaff"
            >
              Thống kê đơn hàng theo nhân viên
            </el-button>
          </div>
        </el-col>

        <el-col :xs="24" :sm="12" :md="12" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><Calendar /></el-icon>
              <span>Doanh thu theo tháng ({{ currentYear }})</span>
            </h3>
            <BarChart :chart-data="monthlyRevenueChart" />
          </div>
        </el-col>

        <el-col :xs="24" :sm="12" :md="12" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><DataLine /></el-icon>
              <span>Doanh thu theo năm</span>
            </h3>
            <LineChart :chart-data="yearlyRevenueChart" />
          </div>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><ShoppingCart /></el-icon>
              <span>Doanh thu theo loại đơn</span>
            </h3>
            <PieChart :chart-data="orderTypeChart" />
          </div>
        </el-col>

        <el-col :xs="24" :sm="12" :md="8" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><MessageBox /></el-icon>
              <span>Trạng thái hóa đơn</span>
            </h3>
            <DoughnutChart :chart-data="invoiceStatusChart" />
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="8" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><DataAnalysis /></el-icon>
              <span>Số liệu quan trọng</span>
            </h3>
            <div class="kpi-grid">
              <div class="kpi-item">
                <span class="kpi-value">{{ totalRevenueThisYear.toLocaleString() }}</span>
                <span class="kpi-label">Doanh thu {{ currentYear }} (VND)</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-value">{{ paidInvoicesCount }}</span>
                <span class="kpi-label">Đơn hàng thành công</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-value">{{ todayRevenue.toLocaleString() }}</span>
                <span class="kpi-label">Doanh thu hôm nay (VND)</span>
              </div>
            </div>
          </div>
        </el-col>

        <el-col :span="24" class="mb-4">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="đến"
            start-placeholder="Ngày bắt đầu"
            end-placeholder="Ngày kết thúc"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="mr-4"
          />
          <el-button type="primary" @click="fetchTopProducts">Tìm kiếm</el-button>
        </el-col>

        <el-col :span="24" class="mb-6">
          <div class="chart-card">
            <h3 class="chart-title">
              <el-icon><TrophyBase /></el-icon>
              <span>Top sản phẩm bán chạy</span>
            </h3>
            <BarChart :chart-data="topProductsChart" />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/utils/axiosInstance'
import {
  Calendar,
  DataLine,
  ShoppingCart,
  MessageBox,
  TrophyBase,
  DataAnalysis,
} from '@element-plus/icons-vue'
import BarChart from '@/components/charts/BarChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import DoughnutChart from '@/components/charts/DoughnutChart.vue'

const router = useRouter()

const isLoading = ref(true)
const error = ref(null)

const currentYear = new Date().getFullYear()
const monthlyRevenue = ref([])
const yearlyRevenue = ref([])
const orderTypeRevenue = ref([])
const topProducts = ref([])
const invoiceStatusStats = ref([])
const todayRevenue = ref(0)
const dateRange = ref([])

// 📌 lấy top sản phẩm
const fetchTopProducts = async () => {
  try {
    const params = {}
    if (dateRange.value.length === 2) {
      params.start = `${dateRange.value[0]}T00:00:00`
      params.end = `${dateRange.value[1]}T23:59:59`
    }
    const res = await apiClient.get('/admin/statistics/top-products', { params })
    topProducts.value = res.data
  } catch (e) {
    console.error('Lỗi khi tải top sản phẩm:', e)
    if (e.response?.status === 403) {
      router.push('/error')
    } else {
      error.value = e.message
    }
  }
}

// 📌 chuyển trang
const goToOrderStaff = () => {
  router.push('/nhan-vien-xu-ly')
}

// 📌 lấy doanh thu hôm nay
const fetchTodayRevenue = async () => {
  try {
    const res = await apiClient.get('/admin/statistics/revenue/today')
    todayRevenue.value = res.data
  } catch (e) {
    console.error('Lỗi khi tải doanh thu hôm nay:', e)
    if (e.response?.status === 403) {
      router.push('/error')
    }
  }
}

// load revenue động theo type
const type = ref('week')
const revenueData = ref(null)

const loadRevenue = async () => {
  isLoading.value = true
  try {
    const { data } = await apiClient.get('/admin/online-sales/get-revenue', {
      params: { type: type.value }
    })
    revenueData.value = data
    console.log('data: ', revenueData.value)
  } catch (e) {
    console.error('Lỗi khi load revenue:', e)
    if (e.response?.status === 403) {
      router.push('/error')
    } else {
      error.value = e.message
    }
  } finally {
    isLoading.value = false
  }
}

// 📌 load thống kê tổng hợp
const loadStatistics = async () => {
  isLoading.value = true
  error.value = null
  try {
    const [m, y, o, s] = await Promise.all([
      apiClient.get('/admin/statistics/monthly', { params: { year: currentYear } }),
      apiClient.get('/admin/statistics/yearly'),
      apiClient.get('/admin/statistics/order-type'),
      apiClient.get('/admin/statistics/status'),
    ])

    monthlyRevenue.value = m.data
    yearlyRevenue.value = y.data
    orderTypeRevenue.value = o.data
    invoiceStatusStats.value = s.data

    await fetchTopProducts()
    await fetchTodayRevenue()
  } catch (e) {
    console.error('Lỗi khi load thống kê:', e)
    if (e.response?.status === 403) {
      router.push('/error')
    } else {
      error.value = e.message
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadStatistics()
  loadRevenue()
})

// 🎨 chart configs
const chartColors = {
  blue: '#3b82f6',
  green: '#22c55e',
  purple: '#8b5cf6',
  pink: '#ec4899',
  orange: '#f97316',
  red: '#ef4444',
  teal: '#14b8a6',
  blue_bg: 'rgba(59, 130, 246, 0.2)',
}

const monthlyRevenueChart = computed(() => ({
  labels: monthlyRevenue.value.map((i) => `T${i.month}`),
  datasets: [
    {
      label: 'Doanh thu (VND)',
      backgroundColor: chartColors.green,
      borderRadius: 4,
      data: monthlyRevenue.value.map((i) => i.totalRevenue),
    },
  ],
}))

const yearlyRevenueChart = computed(() => ({
  labels: yearlyRevenue.value.map((i) => `Năm ${i.year}`),
  datasets: [
    {
      label: 'Doanh thu (VND)',
      borderColor: chartColors.blue,
      backgroundColor: chartColors.blue_bg,
      fill: true,
      tension: 0.3,
      data: yearlyRevenue.value.map((i) => i.totalRevenue),
    },
  ],
}))

const orderTypeChart = computed(() => ({
  labels: orderTypeRevenue.value.map((i) => (i.orderType === 1 ? 'Tại quầy' : 'Online')),
  datasets: [
    {
      label: 'Doanh thu theo loại',
      backgroundColor: [chartColors.pink, chartColors.blue],
      data: orderTypeRevenue.value.map((i) => i.totalRevenue),
    },
  ],
}))

const topProductsChart = computed(() => ({
  labels: topProducts.value.map((i) => i.productName),
  datasets: [
    {
      label: 'Số lượng đã bán',
      backgroundColor: chartColors.purple,
      borderRadius: 4,
      data: topProducts.value.map((i) => i.totalQuantitySold),
    },
  ],
}))

const invoiceStatusChart = computed(() => ({
  labels: invoiceStatusStats.value.map((i) => i.status),
  datasets: [
    {
      label: 'Số lượng đơn',
      backgroundColor: [chartColors.orange, chartColors.green, chartColors.red],
      data: invoiceStatusStats.value.map((i) => i.totalInvoices),
    },
  ],
}))

const totalRevenueThisYear = computed(() =>
  monthlyRevenue.value.reduce((sum, item) => sum + item.totalRevenue, 0),
)

const paidInvoicesCount = computed(
  () => invoiceStatusStats.value.find((s) => s.statusCode === 1)?.totalInvoices || 0,
)
</script>

<style scoped>
.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.05), 0 2px 4px -2px rgb(0 0 0 / 0.05);
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chart-title {
  font-weight: 600;
  margin-bottom: 20px;
  font-size: 1rem;
  color: #374151;
  display: flex;
  align-items: center;
}
.chart-title .el-icon {
  margin-right: 8px;
  font-size: 1.2rem;
  color: #4b5563;
}
.kpi-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: 100%;
  justify-content: center;
}
.kpi-item {
  display: flex;
  flex-direction: column;
}
.kpi-value {
  font-size: 2.25rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}
.kpi-label {
  font-size: 0.875rem;
  color: #6b7280;
}
</style>