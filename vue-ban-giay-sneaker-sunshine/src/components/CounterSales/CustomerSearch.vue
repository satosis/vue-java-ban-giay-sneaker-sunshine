<template>
  <div class="cs-search">
    <h4 class="mb-3">Tìm kiếm khách hàng</h4>

    <el-autocomplete
      v-model="searchPhone"
      :fetch-suggestions="searchCustomers"
      placeholder="Nhập SĐT khách hàng để tìm..."
      :trigger-on-focus="false"
      @select="handleSelectCustomer"
      @input="onInputSanitize"
      @keypress="allowOnlyDigits"
      @paste="blockNonDigits"
      class="w-100"
      size="large"
      clearable
      :maxlength="maxLength"
      :value-key="valueKey"
      :hide-loading="isLoading === false"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>

      <!-- Hiển thị từng item gợi ý -->
      <template #default="{ item }">
        <div class="item">
          <div class="fw-bold">{{ item.customerName || 'Khách lẻ' }}</div>
          <span class="text-muted small">{{ item.phone }}</span>
        </div>
      </template>

      <!-- Trạng thái đang tải -->
      <template #loading>
        <div class="p-3 text-muted">Đang tìm...</div>
      </template>

      <!-- Không có kết quả -->
      <template #empty>
        <div class="p-3 text-muted">Không tìm thấy khách hàng nào.</div>
      </template>
    </el-autocomplete>
  </div>
</template>

<script setup>
import { ref, defineEmits, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

// DÙNG instance axios đã cấu hình sẵn
import apiClient from '../../utils/axiosInstance.js'

// ===== Emits =====
const emit = defineEmits(['select-customer'])

// ===== State =====
const searchPhone = ref('')
const isLoading = ref(false)

// Giới hạn độ dài SĐT (Việt Nam: thường 10 số; để 11 cho an toàn nếu hệ thống có KH cũ)
// Bạn có thể đổi lại 10 nếu muốn.
const maxLength = 11

// Khi Element Plus cần "value" để phân biệt item, ta dùng khóa 'phone'
const valueKey = 'phone'

// ===== Utils =====
/** Debounce thuần */
const debounce = (fn, delay = 300) => {
  let t = null
  return (...args) => {
    clearTimeout(t)
    t = setTimeout(() => fn(...args), delay)
  }
}

/** Chỉ cho phép gõ phím số (0–9) */
const allowOnlyDigits = (event) => {
  const code = event.which || event.keyCode
  // Cho phép phím điều khiển (Backspace, Delete, Arrow keys, Tab, Enter)
  if ([8, 9, 13, 37, 39, 46].includes(code)) return
  const char = String.fromCharCode(code)
  if (!/[0-9]/.test(char)) {
    event.preventDefault()
  }
}

/** Chặn dán nếu có ký tự không phải số */
const blockNonDigits = (event) => {
  const text = (event.clipboardData || window.clipboardData).getData('text')
  if (!/^\d+$/.test(text)) {
    event.preventDefault()
    ElMessage.warning('Chỉ được nhập số điện thoại')
  }
}

/** Tự động làm sạch input: bỏ ký tự không phải số & cắt độ dài */
const onInputSanitize = (val) => {
  const digitsOnly = (val || '').replace(/\D+/g, '').slice(0, maxLength)
  if (digitsOnly !== val) {
    searchPhone.value = digitsOnly
  }
}

/** Gọi API lấy gợi ý KH theo tiền tố SĐT (dùng cho el-autocomplete) */
const _searchCustomers = async (queryString, cb) => {
  const q = (queryString || '').trim()
  if (!q) return cb([])
  if (q.length < 1) return cb([])

  isLoading.value = true
  try {
    const { data } = await apiClient.get(
      `/admin/counter-sales/search-by-phone-prefix`,
      { params: { phone: q } }
    )
    cb(Array.isArray(data) ? data : [])
  } catch (err) {
    console.error('Error fetching customers:', err)
    ElMessage.error('Không thể tải danh sách khách hàng.')
    cb([])
  } finally {
    isLoading.value = false
  }
}

// Bọc debounce để giảm số lần gọi API
const searchCustomers = debounce(_searchCustomers, 300)

/** Xử lý khi chọn KH */
const handleSelectCustomer = (customer) => {
  if (!customer) return
  ElMessage.success(`Đã chọn khách hàng: ${customer.customerName || customer.phone}`)
  emit('select-customer', customer)
  // Tuỳ luồng UX: giữ lại số vừa chọn hay xoá input
  searchPhone.value = ''
}
</script>

<style scoped>
.cs-search { }
.w-100 { width: 100%; }
.mb-3 { margin-bottom: 1rem; }

.p-3 { padding: 1rem; }
.fw-bold { font-weight: 600; }
.text-muted { color: var(--el-text-color-secondary); }
.small { font-size: 0.875em; }

.item { display: flex; flex-direction: column; }
</style>
