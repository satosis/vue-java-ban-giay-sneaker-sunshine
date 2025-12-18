<template>
  <el-container class="min-h-screen bg-gray-100">
    <el-main class="p-6">
      <el-card class="max-w-4xl mx-auto" shadow="always">
        <template #header>
          <div class="flex items-center gap-4">
            <el-button type="info" circle @click="goBack" title="Quay lại">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <h2 class="text-2xl font-bold text-gray-800">
              <el-icon class="mr-2 text-orange-500"><Ticket /></el-icon>
              Cập nhật Voucher
            </h2>
          </div>
        </template>

        <el-form
          ref="voucherForm"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="onSubmit"
        >
          <!-- Tên -->
          <el-form-item label="Tên voucher" prop="voucherName">
            <el-input v-model="form.voucherName" placeholder="Nhập tên voucher" clearable>
              <template #prefix><el-icon><PriceTag /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- Giảm giá -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Phần trăm giảm (%)" prop="discountPercentage">
                <el-input-number
                  v-model="form.discountPercentage"
                  :min="0"
                  :max="100"
                  :step="0.1"
                  :disabled="!!form.discountAmount"
                  :formatter="formatPercent"
                  :parser="parsePercent"
                  class="w-full"
                  placeholder="VD: 9.5"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Số tiền giảm (VNĐ)" prop="discountAmount">
                <el-input-number
                  v-model="form.discountAmount"
                  :min="0"
                  :disabled="!!form.discountPercentage"
                  :formatter="formatMoney"
                  :parser="parseMoney"
                  class="w-full"
                  placeholder="VD: 50000"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Điều kiện đơn & giảm tối đa -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Giá trị đơn tối thiểu (VNĐ)" prop="minOrderValue">
                <el-input-number
                  v-model="form.minOrderValue"
                  :min="0"
                  :formatter="formatMoney"
                  :parser="parseMoney"
                  class="w-full"
                  placeholder="VD: 200000"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Giảm tối đa (VNĐ)" prop="maxDiscountValue">
                <el-input-number
                  v-model="form.maxDiscountValue"
                  :min="0"
                  :disabled="isMaxDiscountDisabled"
                  :formatter="formatMoney"
                  :parser="parseMoney"
                  class="w-full"
                  :placeholder="isMaxDiscountDisabled ? 'Tự khóa khi giảm theo tiền' : 'Bắt buộc khi giảm theo %'"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Giá trị đơn tối thiểu để NHẬN voucher -->
          <el-form-item
            label="Giá trị đơn tối thiểu để NHẬN voucher (VNĐ) - có thể để trống"
            prop="minOrderToReceive"
          >
            <el-input-number
              v-model="form.minOrderToReceive"
              :min="0"
              :formatter="formatMoney"
              :parser="parseMoney"
              class="w-full"
              placeholder="Bỏ trống nếu không áp dụng"
            />
          </el-form-item>

          <!-- Ngày hiệu lực -->
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Ngày bắt đầu" prop="startDate">
                <el-date-picker
                  v-model="form.startDate"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  placeholder="Chọn ngày bắt đầu"
                >
                  <template #prefix><el-icon><Calendar /></el-icon></template>
                </el-date-picker>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="Ngày kết thúc" prop="endDate">
                <el-date-picker
                  v-model="form.endDate"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  placeholder="Chọn ngày kết thúc"
                >
                  <template #prefix><el-icon><Calendar /></el-icon></template>
                </el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Mô tả -->
          <el-form-item label="Mô tả" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="Nhập mô tả">
              <template #prefix><el-icon><InfoFilled /></el-icon></template>
            </el-input>
          </el-form-item>

          <!-- Loại đơn & khách hàng riêng tư -->
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="Loại đơn hàng" prop="orderType">
                <el-select v-model="form.orderType" placeholder="Chọn loại" class="w-full">
                  <el-option :value="0" label="Bán tại quầy" />
                  <el-option :value="1" label="Bán online" />
                </el-select>
              </el-form-item>
            </el-col>

            <!-- Giữ nguyên voucherType để tương thích BE -->
            <!-- <el-col :span="8">
              <el-form-item label="Loại voucher" prop="voucherType">
                <el-select v-model="form.voucherType" placeholder="Chọn loại" class="w-full">
                  <el-option :value="1" label="Công khai" />
                  <el-option :value="2" label="Riêng tư" />
                </el-select>
              </el-form-item>
            </el-col> -->

            <el-col :span="8" v-if="form.voucherType === 2">
              <el-form-item label="Khách hàng áp dụng" prop="customerId">
                <el-select
                  v-model="form.customerId"
                  placeholder="Chọn khách hàng"
                  class="w-full"
                  clearable
                  filterable
                  :filter-method="filterCustomers"
                >
                  <template #prefix><el-icon><User /></el-icon></template>
                  <el-option
                    v-for="c in customers"
                    :key="c.id"
                    :label="c.customerName || `ID: ${c.id}`"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Số lượng -->
          <el-form-item label="Số lượng" prop="quantity">
            <el-input-number v-model="form.quantity" :min="1" class="w-full" />
          </el-form-item>

          <!-- Actions -->
          <div class="flex gap-4 mt-6">
            <el-button type="primary" @click="onSubmit">
              <el-icon class="mr-2"><Check /></el-icon> Cập nhật Voucher
            </el-button>
            <el-button @click="onReset">
              <el-icon class="mr-2"><Refresh /></el-icon> Đặt lại
            </el-button>
          </div>
        </el-form>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '@/utils/axiosInstance'
import { Check, Refresh, ArrowLeft, Ticket, PriceTag, InfoFilled, Calendar, User } from '@element-plus/icons-vue'

/** ===== Router & Params ===== */
const route = useRoute()
const router = useRouter()
const voucherId = route.params.id

/** ===== Refs & Stores ===== */
const voucherForm = ref(null)
const original = ref({})
const customers = ref([])
const allCustomers = ref([]) // giữ danh sách gốc để filter
const DISCOUNT_BUFFER = 1000

/** ===== Form state ===== */
const form = reactive({
  voucherName: '',
  discountPercentage: null, // BigDecimal
  discountAmount: null,     // Integer
  minOrderValue: null,      // BigDecimal
  maxDiscountValue: null,   // BigDecimal (bắt buộc khi giảm theo %)
  minOrderToReceive: null,  // BigDecimal
  startDate: null,          // 'YYYY-MM-DD HH:mm:ss'
  endDate: null,
  description: '',
  orderType: 1,
  voucherType: 1,           // 1: công khai, 2: riêng tư
  customerId: null,
  quantity: null,
  status: 1,
})

/** ===== Computed ===== */
const isMaxDiscountDisabled = computed(() => !!form.discountAmount)

/** ===== Format/Parser helpers ===== */
const nf = new Intl.NumberFormat('vi-VN', { style: 'decimal' })
const formatMoney = (v) => (v == null || v === '' ? '' : `${nf.format(v)} VNĐ`)
const parseMoney  = (v) => v ? Number(String(v).replace(/[^\d]/g, '')) : 0
const formatPercent = (v) => (v == null || v === '' ? '' : `${Number(v).toFixed(1).replace(/\.0$/, '')} %`)
const parsePercent  = (v) => {
  if (!v) return 0
  const num = parseFloat(String(v).replace(',', '.').replace(/[^\d.]/g, ''))
  return Number.isNaN(num) ? 0 : Math.min(Math.max(num, 0), 100)
}
const formatVndCompact = (n) => nf.format(n) + ' đ'

/** ===== Validate rules ===== */
const rules = {
  voucherName: [{ required: true, message: 'Vui lòng nhập tên voucher', trigger: 'blur' }],

  discountPercentage: [{
    validator: (r, val, cb) => {
      const hasAmount = form.discountAmount != null && form.discountAmount !== ''
      if ((val == null || val === '') && !hasAmount) {
        return cb(new Error('Vui lòng nhập phần trăm giảm hoặc số tiền giảm'))
      }
      if (val != null && val !== '' && (Number(val) < 0 || Number(val) > 100)) {
        return cb(new Error('Phần trăm giảm phải từ 0 đến 100'))
      }
      return cb()
    },
    trigger: 'change',
  }],

  discountAmount: [{
    validator: (r, val, cb) => {
      const hasPercent = form.discountPercentage != null && form.discountPercentage !== ''
      const hasAmount  = val != null && val !== ''

      // phải nhập 1 trong 2
      if (!hasAmount && !hasPercent) {
        return cb(new Error('Vui lòng nhập số tiền giảm hoặc phần trăm giảm'))
      }
      // không âm
      if (hasAmount && Number(val) < 0) {
        return cb(new Error('Số tiền giảm không thể âm'))
      }

      // ✅ không vượt quá (minOrderValue - 1.000)
      const mov = form.minOrderValue
      if (hasAmount && mov != null && mov !== '') {
        const allowedMax = Math.max(0, Number(mov) - DISCOUNT_BUFFER)
        if (Number(val) > allowedMax) {
          return cb(new Error(
            `Số tiền giảm không thể lớn hơn ${formatVndCompact(allowedMax)} (tức Giá trị đơn tối thiểu trừ 1.000đ)`
          ))
        }
      }
      return cb()
    },
    trigger: 'change',
  }],

  minOrderValue: [
    { required: true, message: 'Vui lòng nhập giá trị đơn tối thiểu', trigger: 'change' },
    {
      validator: (r, val, cb) => {
        if (val != null && Number(val) < 0) return cb(new Error('Giá trị đơn tối thiểu không thể âm'))
        return cb()
      },
      trigger: 'change',
    },
  ],

  // Bắt buộc khi giảm theo %, không bắt buộc khi giảm theo tiền
  maxDiscountValue: [{
    validator: (r, val, cb) => {
      const isPercentMode = form.discountPercentage != null && form.discountPercentage !== ''
      if (!isPercentMode) {
        if (val != null && Number(val) < 0) return cb(new Error('Số tiền giảm tối đa không thể âm'))
        return cb()
      }
      if (val == null || val === '') return cb(new Error('Vui lòng nhập Giảm tối đa khi dùng phần trăm'))
      if (Number(val) < 0) return cb(new Error('Số tiền giảm tối đa không thể âm'))
      return cb()
    },
    trigger: 'change',
  }],

  minOrderToReceive: [{
    validator: (r, val, cb) => {
      if (val == null || val === '') return cb()
      if (Number(val) < 0) return cb(new Error('Giá trị không thể âm'))
      return cb()
    }, trigger: 'change',
  }],

  startDate: [{ required: true, message: 'Vui lòng chọn ngày bắt đầu', trigger: 'change' }],
  endDate: [{
    validator: (r, val, cb) => {
      if (!val) return cb(new Error('Vui lòng chọn ngày kết thúc'))
      if (form.startDate && new Date(val) <= new Date(form.startDate)) {
        return cb(new Error('Ngày kết thúc phải sau ngày bắt đầu'))
      }
      return cb()
    }, trigger: 'change',
  }],

  quantity: [
    { required: true, message: 'Vui lòng nhập số lượng voucher', trigger: 'change' },
    {
      validator: (r, val, cb) => {
        if (val != null && Number(val) < 1) return cb(new Error('Số lượng phải ≥ 1'))
        return cb()
      }, trigger: 'change',
    },
  ],
}

/** ===== Mutual exclusivity bằng watch ===== */
watch(() => form.discountPercentage, (val) => {
  if (val != null && val !== '') {
    if (form.discountAmount != null && form.discountAmount !== '') {
      form.discountAmount = null
      voucherForm.value?.clearValidate('discountAmount')
    }
  }
})
watch(() => form.discountAmount, (val) => {
  if (val != null && val !== '') {
    if (form.discountPercentage != null && form.discountPercentage !== '') {
      form.discountPercentage = null
      voucherForm.value?.clearValidate('discountPercentage')
    }
    // khi chuyển sang "giảm theo tiền": clear maxDiscountValue
    form.maxDiscountValue = null
    voucherForm.value?.clearValidate('maxDiscountValue')
  }
})

/** ===== Load detail ===== */
const fetchVoucher = async () => {
  const { data } = await apiClient.get(`/admin/vouchers/${voucherId}`)
  original.value = { ...(data || {}) }

  form.voucherName        = data?.voucherName ?? ''
  form.discountPercentage = data?.discountPercentage ?? null
  form.discountAmount     = data?.discountAmount ?? null
  form.minOrderValue      = data?.minOrderValue ?? null
  form.maxDiscountValue   = data?.maxDiscountValue ?? null
  form.minOrderToReceive  = data?.minOrderToReceive ?? null

  // chuẩn hóa định dạng hiển thị
  form.startDate = data?.startDate ? String(data.startDate).replace('T',' ').slice(0,19) : null
  form.endDate   = data?.endDate   ? String(data.endDate).replace('T',' ').slice(0,19)   : null

  form.description = data?.description ?? ''
  form.orderType   = data?.orderType ?? 1
  form.voucherType = data?.voucherType ?? 1
  form.customerId  = data?.customerId ? Number(data.customerId) : null
  form.quantity    = data?.quantity ?? null
  form.status      = data?.status ?? 1
}

/** ===== Customers ===== */
const fetchCustomers = async () => {
  try {
    const { data } = await apiClient.get('/admin/customers')
    allCustomers.value = Array.isArray(data) ? data : []
    customers.value = [...allCustomers.value]
  } catch (e) {
    console.error('Lỗi khi tải khách hàng:', e)
    ElMessage.error('Không thể tải danh sách khách hàng!')
  }
}
const filterCustomers = (q) => {
  if (!q) {
    customers.value = [...allCustomers.value]
    return
  }
  const lower = q.toLowerCase()
  customers.value = allCustomers.value.filter((c) =>
    (c.customerName || '').toLowerCase().includes(lower) || String(c.id).includes(q)
  )
}

/** ===== Submit ===== */
const onSubmit = async () => {
  try {
    await voucherForm.value.validate()

    if (!form.quantity || Number(form.quantity) < 1) {
      ElMessage.error('Số lượng voucher phải lớn hơn 0')
      return
    }

    // ✅ Guard: discountAmount ≤ (minOrderValue - 1.000)
    if (form.discountAmount != null && form.discountAmount !== '' &&
        form.minOrderValue != null && form.minOrderValue !== '') {
      const allowedMax = Math.max(0, Number(form.minOrderValue) - DISCOUNT_BUFFER)
      if (Number(form.discountAmount) > allowedMax) {
        ElMessage.error(`Số tiền giảm không thể lớn hơn ${formatVndCompact(allowedMax)} (tức Giá trị đơn tối thiểu trừ 1.000đ)`)
        return
      }
    }

    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn cập nhật voucher này không?',
      'Xác nhận',
      { confirmButtonText: 'OK', cancelButtonText: 'Hủy', type: 'warning' }
    )

    // Chỉ giữ 1 loại giảm trong payload
    const discountPercentage = form.discountAmount ? null : form.discountPercentage
    const discountAmount     = form.discountPercentage ? null : form.discountAmount

    const payload = {
      voucherName: form.voucherName,
      discountPercentage: (discountPercentage == null || Number(discountPercentage) === 0) ? null : discountPercentage,
      discountAmount: (discountAmount == null || Number(discountAmount) === 0) ? null : discountAmount,
      minOrderValue: (form.minOrderValue == null || Number(form.minOrderValue) === 0) ? null : form.minOrderValue,
      maxDiscountValue: (form.maxDiscountValue == null || Number(form.maxDiscountValue) === 0) ? null : form.maxDiscountValue,
      minOrderToReceive: (form.minOrderToReceive == null || Number(form.minOrderToReceive) === 0) ? null : form.minOrderToReceive,
      startDate: form.startDate,
      endDate: form.endDate,
      description: form.description,
      orderType: form.orderType,
      voucherType: form.voucherType,
      customerId: form.voucherType === 2 ? form.customerId : null,
      quantity: form.quantity,
      status: form.status,
    }

    const res = await apiClient.put(`/admin/vouchers/update/${voucherId}`, payload)
    if (res.status !== 200) throw new Error(res.data?.message || 'Cập nhật thất bại')
    ElMessage.success('Cập nhật voucher thành công!')
    router.push('/voucher')
  } catch (err) {
    if (err === 'cancel') {
      ElMessage.info('Đã hủy cập nhật')
      return
    }
    console.error('Lỗi cập nhật voucher:', err)
    ElMessage.error(err?.response?.data?.message || err?.message || 'Cập nhật thất bại, vui lòng kiểm tra lại.')
  }
}

/** ===== Reset & Back ===== */
const onReset = () => {
  const o = original.value || {}
  form.voucherName        = o.voucherName ?? ''
  form.discountPercentage = o.discountPercentage ?? null
  form.discountAmount     = o.discountAmount ?? null
  form.minOrderValue      = o.minOrderValue ?? null
  form.maxDiscountValue   = o.maxDiscountValue ?? null
  form.minOrderToReceive  = o.minOrderToReceive ?? null
  form.startDate = o.startDate ? String(o.startDate).replace('T',' ').slice(0,19) : null
  form.endDate   = o.endDate   ? String(o.endDate).replace('T',' ').slice(0,19)   : null
  form.description = o.description ?? ''
  form.orderType   = o.orderType ?? 1
  form.voucherType = o.voucherType ?? 1
  form.customerId  = o.customerId ? Number(o.customerId) : null
  form.quantity    = o.quantity ?? null
  form.status      = o.status ?? 1
}
const goBack = () => router.back()

onMounted(async () => {
  await Promise.all([fetchVoucher(), fetchCustomers()])
})
</script>

<style scoped>
.el-card { border-radius: 12px; }
:deep(.el-form-item__label){ @apply font-semibold text-gray-700; }
:deep(.el-input-number){ @apply w-full; }
:deep(.el-select){ @apply w-full; }
:deep(.el-date-picker){ @apply w-full; }
</style>
