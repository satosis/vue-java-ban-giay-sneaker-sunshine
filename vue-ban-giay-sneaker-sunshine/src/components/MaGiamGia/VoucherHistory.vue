<template>
  <div class="voucher-status-page">
    <el-card shadow="hover" class="header-card">
      <div class="header-content">
        <div>
          <h2 class="title">
            <template v-if="voucher.voucherCode">
              {{ voucher.voucherName || 'Voucher' }} — <small>{{ voucher.voucherCode }}</small>
            </template>
            <template v-else>Thông tin Voucher</template>
          </h2>
          <p class="subtitle">ID: <strong>{{ voucherIdDisplay }}</strong></p>
        </div>

        <div class="actions">
          <el-button
            @click="goBack"
            size="small"
            plain
            style="margin-right: 8px"
          >
            <i class="el-icon-arrow-left" /> Quay lại
          </el-button>

          <el-button
            :loading="loading"
            @click="fetchVoucher"
            icon="el-icon-refresh"
            size="small"
            plain
          >
            Làm mới
          </el-button>
        </div>
      </div>
    </el-card>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="h1" style="width:40%"></el-skeleton-item>
        <el-row :gutter="20" class="kpi-row" style="margin-top: 16px">
          <el-col :xs="24" :sm="12" :md="6" v-for="n in 4" :key="n">
            <el-skeleton-item variant="button" style="height: 90px"></el-skeleton-item>
          </el-col>
        </el-row>
      </template>

      <template #default>
        <el-row :gutter="20" class="kpi-row" style="margin-top: 16px">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="kpi-card">
              <div class="kpi-label">Tổng tiền đã giảm</div>
              <div class="kpi-value">{{ formatCurrency(voucher.totalDiscountGiven) }}</div>
              <div class="kpi-sub">Tích lũy từ khi tạo</div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="kpi-card">
              <div class="kpi-label">Tiền giảm trong ngày</div>
              <div class="kpi-value">{{ formatCurrency(voucher.discountToday) }}</div>
              <div class="kpi-sub">Trong ngày hôm nay</div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="kpi-card">
              <div class="kpi-label">Tổng số lần đã dùng</div>
              <div class="kpi-value">{{ formatNumber(voucher.totalUses) }}</div>
              <div class="kpi-sub">Lượt sử dụng tích lũy</div>
            </el-card>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-card class="kpi-card">
              <div class="kpi-label">Lần dùng hôm nay</div>
              <div class="kpi-value">{{ formatNumber(voucher.usesToday) }}</div>
              <div class="kpi-sub">Số lượt trong ngày</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="detail-card" shadow="never" style="margin-top: 20px">
          <el-descriptions title="Chi tiết voucher" :column="2" border>
            <el-descriptions-item label="Mã voucher">{{ voucher.voucherCode || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Tên voucher">{{ voucher.voucherName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Voucher ID">{{ voucher.voucherId ?? voucherIdDisplay }}</el-descriptions-item>
            <el-descriptions-item label="Cập nhật lần cuối">{{ lastUpdatedLabel }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <div v-if="error" class="error-block">
          <el-alert title="Lỗi khi tải dữ liệu" type="error" :description="error" show-icon />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '@/utils/axiosInstance' // giữ nguyên đường dẫn của bạn

// Lấy route + router
const route = useRoute()
const router = useRouter()

// voucherId là ref và lấy ưu tiên từ params.id / params.voucherId / query.voucherId
const voucherId = ref(
  route.params.id ?? route.params.voucherId ?? route.query.voucherId ?? null
)

const loading = ref(false)
const error = ref(null)
const lastFetchedAt = ref(null)

const voucher = ref({
  voucherId: null,
  voucherCode: null,
  voucherName: null,
  totalDiscountGiven: 0,
  discountToday: 0,
  totalUses: 0,
  usesToday: 0
})

// Hiển thị id (ưu tiên voucherId ref, nếu không thì voucher trả về)
const voucherIdDisplay = computed(() => voucherId.value ?? voucher.value.voucherId ?? 'Không có')
const lastUpdatedLabel = computed(() => (lastFetchedAt.value ? new Date(lastFetchedAt.value).toLocaleString('vi-VN') : '—'))

function formatCurrency(value) {
  const v = Number(value ?? 0)
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)
}
function formatNumber(value) {
  const v = Number(value ?? 0)
  return v.toLocaleString('vi-VN')
}

async function fetchVoucher() {
  error.value = null
  if (!voucherId.value) {
    error.value = 'Không tìm thấy voucherId trong route hoặc query.'
    return
  }

  loading.value = true
  try {
    const id = voucherId.value

    const res = await apiClient.get(`/admin/vouchers/get-voucher/${id}`, {
      params: { voucherId: id }
    })

    const payload = res.data ?? {}
    const dataObj = payload.data ?? payload

    voucher.value.voucherId = dataObj.voucherId ?? dataObj.id ?? Number(id)
    voucher.value.voucherCode = dataObj.voucherCode ?? dataObj.voucher_code ?? dataObj.code ?? null
    voucher.value.voucherName = dataObj.voucherName ?? dataObj.voucher_name ?? dataObj.name ?? null

    voucher.value.totalDiscountGiven = Number(dataObj.totalDiscountGiven ?? dataObj.total_discount_given ?? 0)
    voucher.value.discountToday = Number(dataObj.discountToday ?? dataObj.discount_today ?? 0)
    voucher.value.totalUses = Number(dataObj.totalUses ?? dataObj.total_uses ?? dataObj.uses ?? 0)
    voucher.value.usesToday = Number(dataObj.usesToday ?? dataObj.uses_today ?? 0)

    lastFetchedAt.value = Date.now()
  } catch (err) {
    console.error(err)
    if (err?.response?.data) {
      // backend có thể trả { message: '...' } hoặc object khác
      error.value = err.response.data.message || JSON.stringify(err.response.data)
    } else if (err?.message) {
      error.value = err.message
    } else {
      error.value = 'Có lỗi khi tải dữ liệu voucher.'
    }
  } finally {
    loading.value = false
  }
}

// Hàm quay lại
function goBack() {
  // Nếu có lịch sử thì quay lại, nếu không thì đi về route mặc định (nếu cần)
  if (window.history.length > 1) {
    router.back()
  } else {
    // thay '/voucher-list' bằng route an toàn bạn muốn fallback
    router.push('/')
  }
}

// Khi route params/query thay đổi => cập nhật voucherId và fetch lại
watch(
  () => [route.params.id, route.params.voucherId, route.query.voucherId],
  ([pId, pVoucherId, qVoucherId]) => {
    voucherId.value = pId ?? pVoucherId ?? qVoucherId ?? null
    if (voucherId.value) fetchVoucher()
  }
)

onMounted(() => {
  fetchVoucher()
})
</script>

<style scoped>
.voucher-status-page {
  max-width: 1100px;
  margin: 16px auto;
  padding: 0 12px;
}
.header-card {
  padding: 12px;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}
.subtitle {
  margin: 0;
  color: var(--el-text-color-warning);
  font-size: 13px;
}
.kpi-row {
  margin-top: 12px;
}
.kpi-card {
  text-align: left;
  padding: 14px;
}
.kpi-label {
  color: #737373;
  font-size: 13px;
}
.kpi-value {
  font-size: 20px;
  font-weight: 700;
  margin-top: 6px;
}
.kpi-sub {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
.detail-card {
  margin-top: 16px;
}
.error-block {
  margin-top: 16px;
}
</style>
