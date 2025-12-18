<template>
  <el-container class="min-h-screen bg-gray-100">
    <el-main class="p-6">
      <el-card class="max-w-4xl mx-auto" shadow="always">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-4">
              <el-button
                type="info"
                circle
                @click="goBack"
                class="hover:bg-gray-600 hover:text-white transition-colors"
                title="Quay lại"
              >
                <el-icon><ArrowLeft /></el-icon>
              </el-button>
              <h2 class="text-2xl font-bold text-gray-800">
                <el-icon class="mr-2 text-blue-500"><Ticket /></el-icon> Thêm Voucher
              </h2>
            </div>
          </div>
        </template>

        <el-form
          ref="voucherForm"
          :model="voucher"
          :rules="rules"
          label-position="top"
          @submit.prevent="submitForm"
        >
          <!-- Tên -->
          <el-form-item label="Tên voucher" prop="voucherName">
            <el-input v-model="voucher.voucherName" placeholder="Nhập tên voucher" clearable>
              <template #prefix><el-icon><PriceTag /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- Giảm giá -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Phần trăm giảm (%)" prop="discountPercentage">
                <el-input-number
                  v-model="voucher.discountPercentage"
                  :min="0"
                  :max="100"
                  :step="0.1"
                  :disabled="isDiscountPercentageDisabled"
                  :formatter="formatPercentage"
                  :parser="parsePercentage"
                  placeholder="Nhập phần trăm giảm (VD: 9.3)"
                  class="w-full"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Số tiền giảm (VNĐ)" prop="discountAmount">
                <el-input-number
                  v-model="voucher.discountAmount"
                  :min="0"
                  :disabled="isDiscountAmountDisabled"
                  :formatter="formatCurrency"
                  :parser="parseCurrency"
                  placeholder="Nhập số tiền giảm"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Điều kiện đơn & giảm tối đa -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Giá trị đơn tối thiểu (VNĐ)" prop="minOrderValue">
                <el-input-number
                  v-model="voucher.minOrderValue"
                  :min="0"
                  :formatter="formatCurrency"
                  :parser="parseCurrency"
                  placeholder="Nhập giá trị tối thiểu"
                  class="w-full"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Giảm tối đa (VNĐ)" prop="maxDiscountValue">
                <el-input-number
                  v-model="voucher.maxDiscountValue"
                  :min="0"
                  :disabled="isMaxDiscountDisabled"
                  :formatter="formatCurrency"
                  :parser="parseCurrency"
                  placeholder="Nhập số tiền giảm tối đa"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Giá trị đơn tối thiểu để NHẬN voucher -->
          <el-form-item
            label="Giá trị đơn tối thiểu để NHẬN voucher (VNĐ) - có thể để trống"
            prop="minOrderToReceiveText"
          >
            <el-input
              v-model.trim="voucher.minOrderToReceiveText"
              placeholder="Nhập số tiền (vd: 200000) hoặc bỏ trống"
              clearable
            >
              <template #prefix><el-icon><InfoFilled /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- Ngày hiệu lực -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Ngày bắt đầu" prop="startDate">
                <el-date-picker
                  v-model="voucher.startDate"
                  type="datetime"
                  placeholder="Chọn ngày bắt đầu"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="updateStatus"
                  class="w-full"
                >
                  <template #prefix><el-icon><Calendar /></el-icon></template>
                </el-date-picker>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Ngày kết thúc" prop="endDate">
                <el-date-picker
                  v-model="voucher.endDate"
                  type="datetime"
                  placeholder="Chọn ngày kết thúc"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                >
                  <template #prefix><el-icon><Calendar /></el-icon></template>
                </el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Mô tả -->
          <el-form-item label="Mô tả" prop="description">
            <el-input v-model="voucher.description" type="textarea" :rows="3" placeholder="Nhập mô tả voucher">
              <template #prefix><el-icon><InfoFilled /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- Loại đơn & khách hàng riêng tư -->
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="Loại đơn hàng" prop="orderType">
                <el-select v-model="voucher.orderType" placeholder="Chọn loại đơn hàng" class="w-full">
                  <el-option :value="0" label="Bán tại quầy" />
                  <el-option :value="1" label="Bán online" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8" v-if="voucher.voucherType === 2">
              <el-form-item label="Khách hàng áp dụng" prop="customerId">
                <el-select
                  v-model="voucher.customerId"
                  placeholder="Chọn hoặc tìm khách hàng"
                  class="w-full"
                  clearable
                  filterable
                  :filter-method="filterCustomers"
                >
                  <template #prefix><el-icon><User /></el-icon></template>
                  <el-option
                    v-for="customer in customers"
                    :key="customer.id"
                    :value="customer.id"
                    :label="customer.customerName || `ID: ${customer.id}`"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Số lượng -->
          <el-form-item label="Số lượng" prop="quantity">
            <el-input-number
              v-model="voucher.quantity"
              :min="1"
              placeholder="Nhập số lượng voucher"
              class="w-full"
            />
          </el-form-item>

          <!-- Actions -->
          <div class="flex gap-4 mt-4">
            <el-button type="primary" @click="submitForm">
              <el-icon class="mr-2"><Check /></el-icon> Thêm Voucher
            </el-button>
            <el-button type="info" @click="resetForm">
              <el-icon class="mr-2"><Refresh /></el-icon> Reset Form
            </el-button>
          </div>
        </el-form>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh, ArrowLeft, Ticket, PriceTag, InfoFilled, Calendar, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

/* ===================== Const & Router ===================== */
const router = useRouter()
const voucherForm = ref(null)
const customers = ref([])
const allCustomers = ref([]) // để giữ danh sách gốc khi filter
const DISCOUNT_BUFFER = 1000

/* ===================== State ===================== */
const voucher = reactive({
  customerId: null,
  employeeId: null,
  voucherName: '',
  discountPercentage: null, // giảm theo %
  discountAmount: null,     // giảm theo số tiền
  minOrderValue: null,
  maxDiscountValue: null,   // bắt buộc khi giảm %
  minOrderToReceiveText: '', // text, có thể để trống (map ra BigDecimal ở BE)
  startDate: null,
  endDate: null,
  status: 1,
  description: '',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  createdBy: 'admin',
  updatedBy: 'admin',
  orderType: 1,
  voucherType: 1, // 1: công khai, 2: riêng tư
  productId: null,
  categoryId: null,
  quantity: null,
})

/* ===================== Formatters / Parsers ===================== */
const nfDecimal = new Intl.NumberFormat('vi-VN', { style: 'decimal' })
const formatCurrency = (value) => {
  if (value === null || value === undefined || value === '') return ''
  return nfDecimal.format(value) + ' VNĐ'
}
const parseCurrency = (value) => {
  if (!value) return 0
  return parseInt(String(value).replace(/[^\d]/g, ''), 10) || 0
}
const formatPercentage = (value) => {
  if (value === null || value === undefined || value === '') return ''
  return `${Number(value).toFixed(1).replace(/\.0$/, '')} %`
}
const parsePercentage = (value) => {
  if (!value) return 0
  const cleaned = String(value).replace(',', '.').replace(/[^\d.]/g, '')
  const parsed = parseFloat(cleaned)
  return isNaN(parsed) ? 0 : Math.min(parsed, 100)
}
// Parse TEXT tiền tệ -> number (BE BigDecimal map OK)
function parseCurrencyText(text) {
  if (text == null || text === '') return null
  const cleaned = String(text).replace(/\s/g, '').replace(/[,VNĐ₫]/gi, '')
  const onlyNumDot = cleaned.replace(/[^\d.]/g, '')
  if (onlyNumDot === '') return null
  const n = Number(onlyNumDot)
  return isNaN(n) ? null : n
}
const formatVndCompact = (n) => nfDecimal.format(n) + ' đ'

/* ===================== Computed (khóa chéo) ===================== */
const isDiscountAmountDisabled = computed(() => !!voucher.discountPercentage)
const isDiscountPercentageDisabled = computed(() => !!voucher.discountAmount)
const isMaxDiscountDisabled = computed(() => !!voucher.discountAmount)

/* ===================== Watchers (dọn dẹp dữ liệu/validate) ===================== */
watch(() => voucher.discountAmount, (val) => {
  if (val != null && val !== '') {
    if (voucher.discountPercentage != null && voucher.discountPercentage !== '') {
      voucher.discountPercentage = null
      voucherForm.value?.clearValidate('discountPercentage')
    }
    voucher.maxDiscountValue = null
    voucherForm.value?.clearValidate('maxDiscountValue')
  }
})
watch(() => voucher.discountPercentage, (val) => {
  if (val != null && val !== '') {
    if (voucher.discountAmount != null && voucher.discountAmount !== '') {
      voucher.discountAmount = null
      voucherForm.value?.clearValidate('discountAmount')
    }
  }
})

/* ===================== Rules ===================== */
const rules = reactive({
  voucherName: [{ required: true, message: 'Vui lòng nhập tên voucher', trigger: 'blur' }],

  discountPercentage: [{
    validator: (rule, value, callback) => {
      const hasAmount = voucher.discountAmount != null && voucher.discountAmount !== ''
      if ((value == null || value === '') && !hasAmount) {
        return callback(new Error('Vui lòng nhập phần trăm giảm hoặc số tiền giảm'))
      }
      if (value != null && value !== '' && (Number(value) < 0 || Number(value) > 100)) {
        return callback(new Error('Phần trăm giảm phải từ 0 đến 100'))
      }
      return callback()
    },
    trigger: 'change',
  }],

  discountAmount: [{
    validator: (rule, value, callback) => {
      const hasPercent = voucher.discountPercentage != null && voucher.discountPercentage !== ''
      const hasAmount  = value != null && value !== ''

      // phải nhập 1 trong 2
      if (!hasAmount && !hasPercent) {
        return callback(new Error('Vui lòng nhập số tiền giảm hoặc phần trăm giảm'))
      }
      // không âm
      if (hasAmount && Number(value) < 0) {
        return callback(new Error('Số tiền giảm không thể âm'))
      }

      // ✅ không vượt quá (minOrderValue - 1.000)
      const mov = voucher.minOrderValue
      if (hasAmount && mov != null && mov !== '') {
        const allowedMax = Math.max(0, Number(mov) - DISCOUNT_BUFFER)
        if (Number(value) > allowedMax) {
          return callback(new Error(
            `Số tiền giảm không thể lớn hơn ${formatVndCompact(allowedMax)} (tức Giá trị đơn tối thiểu trừ 1.000đ)`
          ))
        }
      }
      return callback()
    },
    trigger: 'change',
  }],

  minOrderValue: [
    { required: true, message: 'Vui lòng nhập giá trị đơn tối thiểu', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value != null && Number(value) < 0) return callback(new Error('Giá trị đơn tối thiểu không thể âm'))
        return callback()
      },
      trigger: 'change',
    },
  ],

  // Bắt buộc khi giảm theo %, không bắt buộc khi giảm theo tiền
  maxDiscountValue: [{
    validator: (rule, value, callback) => {
      const isPercentMode = voucher.discountPercentage != null && voucher.discountPercentage !== ''
      if (!isPercentMode) {
        if (value != null && Number(value) < 0) return callback(new Error('Số tiền giảm tối đa không thể âm'))
        return callback()
      }
      if (value == null || value === '') return callback(new Error('Vui lòng nhập Giảm tối đa khi dùng phần trăm'))
      if (Number(value) < 0) return callback(new Error('Số tiền giảm tối đa không thể âm'))
      return callback()
    },
    trigger: 'change',
  }],

  minOrderToReceiveText: [{
    validator: (rule, value, callback) => {
      if (!value || String(value).trim() === '') return callback()
      const parsed = parseCurrencyText(value)
      if (parsed == null || isNaN(parsed)) return callback(new Error('Giá trị không hợp lệ. Chỉ nhập số, dấu chấm/phẩy ngăn cách.'))
      if (parsed < 0) return callback(new Error('Giá trị không thể âm'))
      return callback()
    },
    trigger: 'blur',
  }],

  startDate: [{
    validator: (rule, value, callback) => {
      if (!value) return callback(new Error('Vui lòng chọn ngày bắt đầu'))
      if (new Date(value) < new Date()) return callback(new Error('Ngày bắt đầu phải lớn hơn hoặc bằng hiện tại'))
      return callback()
    },
    trigger: 'change',
  }],

  endDate: [{
    validator: (rule, value, callback) => {
      if (!value) return callback(new Error('Vui lòng chọn ngày kết thúc'))
      if (new Date(value) < new Date()) return callback(new Error('Ngày kết thúc phải lớn hơn hoặc bằng hiện tại'))
      if (voucher.startDate && new Date(value) <= new Date(voucher.startDate)) {
        return callback(new Error('Ngày kết thúc phải sau ngày bắt đầu'))
      }
      return callback()
    },
    trigger: 'change',
  }],

  quantity: [
    { required: true, message: 'Vui lòng nhập số lượng voucher', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value != null && Number(value) < 1) return callback(new Error('Số lượng phải ≥ 1'))
        return callback()
      },
      trigger: 'change',
    },
  ],
})

/* ===================== Helpers ===================== */
const updateStatus = () => {
  if (!voucher.startDate) {
    voucher.status = 1
    return
  }
  const now = new Date()
  const start = new Date(voucher.startDate)
  voucher.status = start > now ? 2 : 1 // 2: CHỜ BẮT ĐẦU, 1: ĐANG CHẠY
}

const handleVoucherTypeChange = () => {
  if (voucher.voucherType === 1) {
    voucher.customerId = null
    voucherForm.value?.clearValidate('customerId')
  } else if (voucher.voucherType === 2 && !voucher.customerId) {
    ElMessage.warning('Vui lòng chọn khách hàng cho voucher riêng tư.')
  }
}

const filterCustomers = (query) => {
  if (!query) {
    customers.value = [...allCustomers.value]
    return
  }
  const lower = query.toLowerCase()
  customers.value = allCustomers.value.filter((c) =>
    (c.customerName || '').toLowerCase().includes(lower) || String(c.id).includes(query)
  )
}

const resetForm = () => {
  voucherForm.value?.resetFields()
  Object.assign(voucher, {
    customerId: null,
    employeeId: null,
    voucherName: '',
    discountPercentage: null,
    discountAmount: null,
    minOrderValue: null,
    maxDiscountValue: null,
    minOrderToReceiveText: '',
    startDate: null,
    endDate: null,
    status: 1,
    description: '',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    createdBy: 'admin',
    updatedBy: 'admin',
    orderType: 1,
    voucherType: 1,
    productId: null,
    categoryId: null,
    quantity: null,
  })
}

/* ===================== Submit ===================== */
const submitForm = async () => {
  try {
    await voucherForm.value.validate()

    // ✅ Guard: discountAmount không vượt quá (minOrderValue - 1.000)
    if (voucher.discountAmount != null && voucher.discountAmount !== '' &&
        voucher.minOrderValue != null && voucher.minOrderValue !== '') {
      const allowedMax = Math.max(0, Number(voucher.minOrderValue) - DISCOUNT_BUFFER)
      if (Number(voucher.discountAmount) > allowedMax) {
        ElMessage.error(`Số tiền giảm không thể lớn hơn ${formatVndCompact(allowedMax)} (tức Giá trị đơn tối thiểu trừ 1.000đ)`)
        return
      }
    }

    await ElMessageBox.confirm('Bạn có chắc chắn muốn thêm voucher này?', 'Xác nhận', {
      confirmButtonText: 'Thêm',
      cancelButtonText: 'Hủy',
      type: 'warning',
    })

    updateStatus()

    // Chuẩn hoá TEXT -> số hoặc null cho minOrderToReceive
    const minOrderToReceiveParsed = voucher.minOrderToReceiveText?.trim()
      ? parseCurrencyText(voucher.minOrderToReceiveText)
      : null

    // chỉ giữ MỘT loại giảm
    const discountPercentage = voucher.discountAmount ? null : voucher.discountPercentage
    const discountAmount     = voucher.discountPercentage ? null : voucher.discountAmount

    const payload = {
      customerId: voucher.customerId,
      employeeId: voucher.employeeId,
      voucherName: voucher.voucherName,
      discountPercentage: discountPercentage ?? null,
      discountAmount: discountAmount ?? null,
      minOrderValue: voucher.minOrderValue ?? null,
      maxDiscountValue: voucher.maxDiscountValue ?? null,
      minOrderToReceive: minOrderToReceiveParsed, // BigDecimal trên BE, có thể null
      startDate: voucher.startDate,
      endDate: voucher.endDate,
      status: voucher.status,
      description: voucher.description,
      createdDate: voucher.createdAt,  // nếu BE dùng createdDate
      updatedDate: voucher.updatedAt,  // nếu BE dùng updatedDate
      createdBy: voucher.createdBy,
      updatedBy: voucher.updatedBy,
      orderType: voucher.orderType,
      voucherType: voucher.voucherType,
      productId: voucher.productId,
      categoryId: voucher.categoryId,
      quantity: voucher.quantity,
    }

    await apiClient.post('/admin/vouchers/create', payload)
    ElMessage.success('Thêm voucher thành công!')
    router.push('/voucher')
    resetForm()
  } catch (err) {
    if (err && err.message === 'cancel') {
      ElMessage.info('Hủy thêm voucher.')
      return
    }
    console.error('Lỗi khi thêm voucher:', err)
    ElMessage.error('Thêm voucher thất bại! Vui lòng kiểm tra lại.')
  }
}

/* ===================== API ===================== */
const fetchCustomers = async () => {
  try {
    const res = await apiClient.get('/admin/customers')
    allCustomers.value = Array.isArray(res.data) ? res.data : []
    customers.value = [...allCustomers.value]
  } catch (e) {
    console.error('Lỗi khi tải danh sách khách hàng:', e)
    ElMessage.error('Không thể tải danh sách khách hàng!')
  }
}

const goBack = () => {
  router.push('/voucher')
  ElMessage.info('Đã quay lại trang trước.')
}

onMounted(() => {
  fetchCustomers()
})
</script>

<style scoped>
:deep(.el-form-item__label){ @apply font-semibold text-gray-700; }
:deep(.el-input-number){ @apply w-full; }
:deep(.el-select){ @apply w-full; }
:deep(.el-date-picker){ @apply w-full; }
</style>
