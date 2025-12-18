<template>
  <div class="page">
    <!-- Back -->
    <el-button type="primary" plain @click="goBack" class="mb-3">
      <el-icon><ArrowLeft /></el-icon>
      Quay lại
    </el-button>

    <el-card shadow="never" class="card">
      <!-- Header -->
      <div class="header">
        <h3 class="title">
          <el-icon class="title-icon"><List /></el-icon>
          Tiến trình đơn hàng
        </h3>

        <div class="actions">
          <el-button
            v-if="canAdvance"
            type="success"
            @click="confirmAdvance"
            :loading="loadingAdvance"
          >
            <el-icon><ArrowRightBold /></el-icon>
            Trạng thái tiếp theo
          </el-button>

          <el-tooltip v-if="!canCancel" content="Đơn đã đến giai đoạn không thể hủy" placement="top">
            <span><el-button type="danger" disabled>Hủy đơn</el-button></span>
          </el-tooltip>
          <el-button v-else type="danger" @click="showCancelDialog" :loading="loadingCancel">
            <el-icon><Delete /></el-icon>
            Hủy đơn
          </el-button>

          <el-button
            v-if="invoice.statusDetail === 'DANG_GIAO_HANG'"
            type="danger"
            plain
            @click="showFailDialog"
            :loading="loadingFail"
          >
            <el-icon><CloseBold /></el-icon>
            Giao thất bại
          </el-button>

          <el-button
            v-if="invoice.statusDetail === 'YEU_CAU_HOAN'"
            type="danger"
            plain
            @click="showApproveCancelDialog"
            :loading="loadingApproveCancel"
          >
            <el-icon><Delete /></el-icon>
            Xác nhận yêu cầu hủy
          </el-button>

          <el-button @click="showActionHistoryDialog" plain>
            <el-icon><Clock /></el-icon>
            Lịch sử tác động
          </el-button>
        </div>
      </div>

      <!-- Steps -->
      <div class="mt-2">
        <!-- Nhánh lỗi/huỷ -->
        <el-steps
          v-if="['HUY_DON','GIAO_THAT_BAI'].includes(invoice.statusDetail)"
          :active="0"
          align-center
          finish-status="error"
        >
          <el-step :title="invoice.statusDetail === 'HUY_DON' ? 'Đã hủy' : 'Giao hàng thất bại'" status="error" />
        </el-steps>

        <!-- Quy trình chính -->
        <el-steps
          v-else
          :active="getActiveStep(invoice.statusDetail)"
          finish-status="success"
          align-center
        >
          <el-step v-for="s in MAIN_STEPS" :key="s.key" :title="s.label" />
        </el-steps>
      </div>

      <el-divider />

      <!-- Thông tin đơn -->
      <h3 class="title mb-2">
        <el-icon class="title-icon"><Tickets /></el-icon>
        Thông tin đơn hàng
      </h3>

      <el-skeleton :loading="loadingInvoice" animated>
        <template #template>
          <el-skeleton-item variant="text" style="width: 60%" />
          <el-skeleton-item variant="text" style="width: 40%" />
          <el-skeleton-item variant="text" style="width: 80%" />
        </template>

        <template #default>
          <el-descriptions :column="1" border class="desc">
            <el-descriptions-item label="Mã hóa đơn">{{ invoice.invoiceCode || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Khách hàng">{{ invoice.customerName || '—' }}</el-descriptions-item>

            <el-descriptions-item label="Số điện thoại người nhận">
              <div class="row-inline">
                <span>{{ invoice.phone || '—' }}</span>
                <el-tooltip
                  v-if="!canEditContactAddress"
                  content="Không thể sửa sau khi đã sang giai đoạn giao hàng / đã hủy / thất bại"
                  placement="top"
                >
                  <span><el-button size="small" text disabled>Sửa</el-button></span>
                </el-tooltip>
                <el-button
                  v-else
                  size="small"
                  @click="openPhoneDialog"
                  type="primary"
                >
                  <el-icon><EditPen /></el-icon>
                  Sửa
                </el-button>
              </div>
            </el-descriptions-item>

            <el-descriptions-item label="Số điện thoại người gửi">{{ invoice.phoneSender || '—' }}</el-descriptions-item>
            <el-descriptions-item label="Ngày tạo">{{ formatDate(invoice.createdDate) }}</el-descriptions-item>
            <el-descriptions-item label="Tổng tiền">{{ formatCurrency(invoice.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="Giảm giá">{{ formatCurrency(invoice.discountAmount) }}</el-descriptions-item>

            <el-descriptions-item label="Địa chỉ giao hàng">
              <div class="row-inline">
                <span>{{ invoice.deliveryAddress || '—' }}</span>
                <el-tooltip
                  v-if="!canEditContactAddress"
                  content="Không thể sửa sau khi đã sang giai đoạn giao hàng / đã hủy / thất bại"
                  placement="top"
                >
                  <span><el-button size="small" text disabled>Sửa</el-button></span>
                </el-tooltip>
                <el-button
                  v-else
                  size="small"
                  @click="openAddressDialog"
                  type="primary"
                >
                  <el-icon><EditPen /></el-icon>
                  Sửa
                </el-button>
              </div>
            </el-descriptions-item>

            <el-descriptions-item label="Phí vận chuyển">{{ formatCurrency(invoice.shippingFee) }}</el-descriptions-item>
            <el-descriptions-item label="Thành tiền">
              <el-tag type="success" effect="light">{{ formatCurrency(invoice.finalAmount) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </el-skeleton>

      <!-- Dialog: Xác nhận yêu cầu hủy -->
      <el-dialog
        title="Xác nhận yêu cầu hủy"
        v-model="approveCancelDialogVisible"
        width="520px"
        :close-on-click-modal="false"
      >
        <el-form label-position="top">
          <el-form-item label="Ghi chú (tùy chọn)">
            <el-input type="textarea" v-model="approveCancelNote" placeholder="Ghi chú cho hành động xác nhận..." rows="2" maxlength="500" />
          </el-form-item>

          <el-form-item label="Phương thức hoàn tiền" required>
            <el-select v-model="approvePaymentMethod" placeholder="Chọn phương thức hoàn tiền">
              <el-option label="ZaloPay" value="ZALO_PAY" />
              <el-option label="Ngân hàng khác" value="NGAN_HANG_KHAC" />
            </el-select>
          </el-form-item>

          <el-form-item label="Mã giao dịch" required>
            <el-input v-model.trim="approveTransactionCode" placeholder="Nhập mã giao dịch / mã hoàn tiền" clearable />
          </el-form-item>

          <el-form-item v-if="approvePaymentMethod === 'NGAN_HANG_KHAC'" label="Tên ngân hàng" required>
            <el-input v-model.trim="approveBankName" placeholder="Nhập tên ngân hàng nhận hoàn tiền" clearable />
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="approveCancelDialogVisible = false">Hủy</el-button>
          <el-button type="danger" @click="submitApproveCancel" :loading="loadingApproveCancel">Xác nhận</el-button>
        </template>
      </el-dialog>

      <el-divider />

      <!-- Lịch sử thanh toán -->
      <h3 class="title mb-2">
        <el-icon class="title-icon"><CreditCard /></el-icon>
        Lịch sử thanh toán
      </h3>
      <el-table
        :data="invoice.invoiceTransactionResponses || []"
        border
        stripe
        empty-text="Không có giao dịch"
      >
        <el-table-column label="STT" width="70" type="index" />
        <el-table-column prop="transactionCode" label="Mã giao dịch" min-width="160" />
        <el-table-column prop="transactionType" label="Loại giao dịch" min-width="140" />
        <el-table-column prop="paymentMethod" label="PT Thanh toán" min-width="140" />
        <el-table-column prop="paymentTime" label="Thời gian" width="180">
          <template #default="{ row }">{{ formatDate(row?.paymentTime) }}</template>
        </el-table-column>
        <el-table-column prop="transactionNote" label="Ghi chú" min-width="180" />
      </el-table>

      <el-divider />

      <!-- Sản phẩm đã mua -->
      <h3 class="title mb-2">
        <el-icon class="title-icon"><ShoppingCart /></el-icon>
        Sản phẩm đã mua
      </h3>
      <el-table
        :data="invoice.invoiceDetailResponses || []"
        border
        stripe
        empty-text="Không có sản phẩm"
      >
        <el-table-column label="STT" width="70" type="index" />
        <el-table-column prop="productDetailName" label="Tên sản phẩm" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sizeName" label="Kích thước" width="120" />
        <el-table-column prop="colorName" label="Màu sắc" width="120" />
        <el-table-column prop="quantity" label="Số lượng" width="120" align="center" />
        <el-table-column label="Giá" min-width="180" align="right">
          <template #default="{ row }">
            <div class="price">
              <span v-if="row.sellPrice !== row.discountedPrice" class="old">
                {{ formatCurrency(row.sellPrice) }}
              </span>
              <span class="new" :class="{ danger: row.discountedPrice < row.sellPrice }">
                {{ formatCurrency(row.discountedPrice) }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Dialog: cập nhật SĐT -->
    <el-dialog
      v-model="phoneDialogVisible"
      title="Cập nhật số điện thoại"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="Số điện thoại mới">
          <el-input v-model.trim="phoneForm.phone" placeholder="Nhập số điện thoại" maxlength="20" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">Hủy</el-button>
        <el-button type="primary" @click="submitPhoneUpdate" :loading="savingPhone">
          Cập nhật
        </el-button>
      </template>
    </el-dialog>

    <!-- Dialog: cập nhật địa chỉ -->
    <el-dialog
      v-model="addressDialogVisible"
      title="Cập nhật địa chỉ giao hàng"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form :model="addressForm" label-position="top" :disabled="loadingRegions">
        <el-row :gutter="10" class="mb-2">
          <el-col :xs="24" :md="8">
            <el-form-item label="Tỉnh/Thành">
              <el-select
                v-model="addressForm.provinceCode"
                placeholder="Chọn tỉnh/thành"
                filterable
                class="w-full"
                clearable
                :loading="loadingRegions"
                @change="handleProvinceChangeForAddress"
              >
                <el-option
                  v-for="p in provinces"
                  :key="p.ProvinceID"
                  :label="p.ProvinceName"
                  :value="p.ProvinceID"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="8">
            <el-form-item label="Quận/Huyện">
              <el-select
                v-model="addressForm.districtCode"
                placeholder="Chọn quận/huyện"
                filterable
                class="w-full"
                clearable
                :disabled="!addressForm.provinceCode"
                :loading="loadingRegions"
                @change="handleDistrictChangeForAddress"
              >
                <el-option
                  v-for="d in districts"
                  :key="d.DistrictID"
                  :label="d.DistrictName"
                  :value="d.DistrictID"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :md="8">
            <el-form-item label="Phường/Xã">
              <el-select
                v-model="addressForm.wardCode"
                placeholder="Chọn phường/xã"
                filterable
                class="w-full"
                clearable
                :disabled="!addressForm.districtCode"
                :loading="loadingRegions"
                @change="handleWardChangeForAddress"
              >
                <el-option
                  v-for="w in wards"
                  :key="w.WardCode"
                  :label="w.WardName"
                  :value="w.WardCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Số nhà, tên đường">
          <el-input v-model="addressForm.houseNumber" placeholder="Nhập số nhà, tên đường" clearable />
        </el-form-item>

        <el-alert
          v-if="shippingFee !== null"
          :title="`Phí vận chuyển dự kiến: ${formatCurrency(shippingFee)}`"
          type="success"
          show-icon
          class="mt-2"
        />
      </el-form>

      <template #footer>
        <el-button @click="addressDialogVisible = false">Đóng</el-button>
        <el-button type="primary" @click="submitAddressUpdate" :loading="savingAddress">
          Cập nhật
        </el-button>
      </template>
    </el-dialog>

    <!-- Dialog: hủy đơn -->
    <el-dialog title="Hủy đơn hàng" v-model="cancelDialogVisible" width="480px">
      <el-form label-position="top">
        <el-form-item label="Lý do hủy đơn">
          <el-input
            type="textarea"
            v-model="cancelNote"
            placeholder="Nhập lý do hủy đơn..."
            rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <template v-if="isPaid">
          <el-form-item label="Phương thức hoàn tiền">
            <el-select v-model="selectedPaymentMethod" placeholder="Chọn phương thức hoàn tiền" class="w-full">
              <el-option label="ZaloPay" value="ZALO_PAY" />
              <el-option label="Ngân hàng khác" value="NGAN_HANG_KHAC" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="selectedPaymentMethod === 'NGAN_HANG_KHAC'" label="Tên ngân hàng">
            <el-input v-model="bankName" placeholder="Nhập tên ngân hàng" />
          </el-form-item>

          <el-form-item label="Mã giao dịch">
            <el-input v-model="transactionCode" placeholder="Nhập mã giao dịch" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="cancelDialogVisible = false">Hủy</el-button>
        <el-button type="danger" @click="cancelOrder" :loading="loadingCancel">Xác nhận hủy đơn</el-button>
      </template>
    </el-dialog>

    <!-- Dialog: giao hàng thất bại -->
    <el-dialog title="Giao hàng thất bại" v-model="failDialogVisible" width="480px">
      <el-form label-position="top">
        <el-form-item label="Lý do giao hàng thất bại">
          <el-input
            type="textarea"
            v-model="failNote"
            placeholder="Nhập lý do giao hàng thất bại..."
            rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item v-if="isPaid" label="Phương thức thanh toán">
          <el-select v-model="selectedPaymentMethod" placeholder="Chọn phương thức thanh toán" class="w-full">
            <el-option label="Tiền mặt" value="TIEN_MAT" />
            <el-option label="ZaloPay" value="ZALO_PAY" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="failDialogVisible = false">Hủy</el-button>
        <el-button type="danger" @click="markAsFailedDelivery" :loading="loadingFail">Xác nhận</el-button>
      </template>
    </el-dialog>

    <!-- Dialog: lịch sử tác động -->
    <el-dialog title="Lịch sử tác động đơn hàng" v-model="actionHistoryDialogVisible" width="820px">
      <el-table :data="actionHistory" border stripe empty-text="Chưa có lịch sử">
        <el-table-column label="STT" width="70" type="index" />
        <el-table-column prop="oldStatus" label="Trạng thái cũ" min-width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.oldStatus)" effect="light">
              {{ getStatusLabelFromInt(row.oldStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="newStatus" label="Trạng thái mới" min-width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.newStatus)" effect="light">
              {{ getStatusLabelFromInt(row.newStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changedAt" label="Thời gian" width="180">
          <template #default="{ row }">{{ formatDate(row.changedAt) }}</template>
        </el-table-column>
        <el-table-column prop="employeeName" label="Người thực hiện" min-width="160" />
      </el-table>
      <template #footer>
        <el-button @click="actionHistoryDialogVisible = false">Đóng</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '@/utils/axiosInstance'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, List, Tickets, CreditCard, ShoppingCart, EditPen, Delete,
  ArrowRightBold, CloseBold, Clock
} from '@element-plus/icons-vue'

/* ===== Router & base ===== */
const route = useRoute()
const router = useRouter()
const invoiceId = route.params.invoiceId

/* ===== UI loading states ===== */
const loadingInvoice = ref(false)
const loadingAdvance = ref(false)
const loadingRevert = ref(false)
const loadingCancel = ref(false)
const loadingFail = ref(false)
const savingPhone = ref(false)
const savingAddress = ref(false)
const loadingRegions = ref(false)
const loadingApproveCancel = ref(false)

/* ===== Data ===== */
const invoice = ref({})
const isPaid = ref(false)

/* ===== Steps & labels ===== */
const MAIN_STEPS = [
  { key: 'CHO_XU_LY', label: 'Chờ xác nhận' },
  { key: 'DA_XU_LY', label: 'Đã xác nhận' },
  { key: 'CHO_GIAO_HANG', label: 'Chờ giao hàng' },
  { key: 'DANG_GIAO_HANG', label: 'Đang giao hàng' },
  { key: 'GIAO_THANH_CONG', label: 'Giao thành công' },
]
const STEP_KEYS = MAIN_STEPS.map(s => s.key)
const DELIVERY_PHASE = 'DANG_GIAO_HANG'

// Map code int từ BE → nhãn hiển thị & tag color cho Lịch sử tác động
const STATUS_LABELS = {
  [-3]: 'Đang giao dịch',
  [-2]: 'Đã hủy',
  [-1]: 'Hủy giao dịch',
  0 : 'Chờ xác nhận',
  1 : 'Đã xác nhận',
  2 : 'Chờ giao hàng',
  3 : 'Đang giao hàng',
  4 : 'Giao thành công',
  5 : 'Giao thất bại',
  6 : 'Mất hàng',
  7 : 'Đã hoàn tiền',
}
const STATUS_TAG = {
  [-2]: 'danger',
  [-1]: 'danger',
  0 : 'info',
  1 : 'info',
  2 : 'warning',
  3 : 'warning',
  4 : 'success',
  5 : 'danger',
  6 : 'danger',
  7 : 'success',
}

const getActiveStep = (statusKey) => {
  const idx = STEP_KEYS.indexOf(statusKey)
  return idx >= 0 ? idx : 0
}

const isCanceledOrFailed = computed(() =>
  ['HUY_DON', 'GIAO_THAT_BAI'].includes(invoice.value?.statusDetail)
)
const isCancelRequested = computed(() => invoice.value?.statusDetail === 'YEU_CAU_HOAN')
const isBeforeDeliveryPhase = computed(() => {
  const s = invoice.value?.statusDetail
  if (!s || !STEP_KEYS.includes(s)) return false
  return STEP_KEYS.indexOf(s) < STEP_KEYS.indexOf(DELIVERY_PHASE)
})
const canEditContactAddress = computed(() => isBeforeDeliveryPhase.value && !isCanceledOrFailed.value && !isCancelRequested.value)
const canAdvance = computed(() => {
  if (isCancelRequested.value) return false
  const s = invoice.value?.statusDetail
  return STEP_KEYS.includes(s) && s !== 'GIAO_THANH_CONG' && !isCanceledOrFailed.value
})
const canCancel = computed(() => {
  if (isCancelRequested.value) return false
  const s = invoice.value?.statusDetail
  return STEP_KEYS.includes(s) && STEP_KEYS.indexOf(s) < STEP_KEYS.indexOf(DELIVERY_PHASE)
})

/* ===== API: load invoice ===== */
const fetchInvoice = async () => {
  loadingInvoice.value = true
  try {
    const res = await apiClient.get('/admin/online-sales/get-order', { params: { invoiceId } })
    invoice.value = res.data || {}
    isPaid.value = Boolean(res.data?.isPaid)
  } catch (err) {
    ElMessage.error('Lỗi tải đơn hàng')
    console.error(err)
  } finally {
    loadingInvoice.value = false
  }
}

const goBack = () => router.back()

/* ===== Chuyển trạng thái ===== */
const confirmAdvance = async () => {
  try {
    await ElMessageBox.confirm('Bạn có chắc muốn chuyển sang trạng thái tiếp theo?', 'Xác nhận', { type: 'warning' })
    await advanceStatus()
  } catch { /* cancel */ }
}
const advanceStatus = async () => {
  const currentKey = invoice.value?.statusDetail
  const currentIndex = STEP_KEYS.indexOf(currentKey)
  const nextKey = STEP_KEYS[currentIndex + 1]
  if (!nextKey) return ElMessage.info('Không có trạng thái tiếp theo để chuyển.')
  try {
    loadingAdvance.value = true
    await apiClient.put('/admin/online-sales/chuyen-trang-thai', null, {
      params: { invoiceId, statusDetail: nextKey }
    })
    ElMessage.success('Chuyển trạng thái thành công!')
    fetchInvoice()
  } catch (err) {
    ElMessage.error('Lỗi chuyển trạng thái')
    console.error(err)
  } finally {
    loadingAdvance.value = false
  }
}

/* ===== Hủy đơn ===== */
const cancelDialogVisible = ref(false)
const cancelNote = ref('')
const selectedPaymentMethod = ref('')
const transactionCode = ref('')
const bankName = ref('')

const showCancelDialog = () => {
  cancelNote.value = ''
  selectedPaymentMethod.value = ''
  transactionCode.value = ''
  bankName.value = ''
  cancelDialogVisible.value = true
}

const cancelOrder = async () => {
  try {
    if (!cancelNote.value.trim()) return ElMessage.warning('Vui lòng nhập lý do hủy đơn!')
    if (isPaid.value && !selectedPaymentMethod.value) return ElMessage.warning('Vui lòng chọn phương thức hoàn tiền!')
    loadingCancel.value = true

    await apiClient.put('/admin/online-sales/huy-don-va-hoan-tien', null, {
      params: {
        invoiceId,
        statusDetail: 'HUY_DON',
        note: cancelNote.value,
        paymentMethod: selectedPaymentMethod.value || '',
        tradeCode: transactionCode.value || '',
        bankName: bankName.value || '',
        isPaid: isPaid.value
      }
    })
    ElMessage.success('Hủy đơn hàng thành công!')
    cancelDialogVisible.value = false
    fetchInvoice()
  } catch (err) {
    ElMessage.error('Lỗi hủy đơn hàng')
    console.error(err)
  } finally {
    loadingCancel.value = false
  }
}

/* ===== Xác nhận yêu cầu hủy (admin) ===== */
const approveCancelDialogVisible = ref(false)
const approveCancelNote = ref('')
const approvePaymentMethod = ref('')
const approveTransactionCode = ref('')
const approveBankName = ref('')

const showApproveCancelDialog = () => {
  approveCancelNote.value = ''
  approvePaymentMethod.value = ''
  approveTransactionCode.value = ''
  approveBankName.value = ''
  approveCancelDialogVisible.value = true
}

const submitApproveCancel = async () => {
  if (!approvePaymentMethod.value) return ElMessage.warning('Vui lòng chọn phương thức hoàn tiền.')
  if (!approveTransactionCode.value || !approveTransactionCode.value.trim()) return ElMessage.warning('Vui lòng nhập mã giao dịch.')
  if (approvePaymentMethod.value === 'NGAN_HANG_KHAC' && !approveBankName.value.trim()) {
    return ElMessage.warning('Vui lòng nhập tên ngân hàng.')
  }

  try {
    await ElMessageBox.confirm(
      'Xác nhận: hành động này sẽ hủy đơn và thực hiện hoàn tiền theo thông tin bạn nhập. Tiếp tục?',
      'Xác nhận hành động',
      { type: 'warning' }
    )
    loadingApproveCancel.value = true
    await apiClient.put('/admin/online-sales/huy-don-va-hoan-tien', null, {
      params: {
        invoiceId,
        statusDetail: 'HUY_DON',
        note: approveCancelNote.value || 'Xác nhận yêu cầu hủy',
        paymentMethod: approvePaymentMethod.value,
        tradeCode: approveTransactionCode.value,
        bankName: approveBankName.value || '',
        isPaid: Boolean(invoice.value?.isPaid)
      }
    })
    ElMessage.success('Đã xác nhận yêu cầu hủy và hoàn tiền.')
    approveCancelDialogVisible.value = false
    fetchInvoice()
  } catch (err) {
    if (err && err !== 'cancel') {
      ElMessage.error('Lỗi khi xác nhận yêu cầu hủy.')
      console.error(err)
    }
  } finally {
    loadingApproveCancel.value = false
  }
}

/* ===== Giao hàng thất bại ===== */
const failDialogVisible = ref(false)
const failNote = ref('')

const showFailDialog = () => {
  failNote.value = ''
  selectedPaymentMethod.value = ''
  failDialogVisible.value = true
}

const markAsFailedDelivery = async () => {
  try {
    if (!failNote.value.trim()) return ElMessage.warning('Vui lòng nhập lý do giao hàng thất bại!')
    if (isPaid.value && !selectedPaymentMethod.value) return ElMessage.warning('Vui lòng chọn phương thức thanh toán!')
    loadingFail.value = true

    await apiClient.put('/admin/online-sales/failed-shipping', null, {
      params: {
        invoiceId,
        statusDetail: 'GIAO_THAT_BAI',
        note: failNote.value,
        paymentMethod: isPaid.value ? selectedPaymentMethod.value : 'UNKNOWN',
        isPaid: isPaid.value,
      }
    })
    ElMessage.success('Cập nhật trạng thái giao hàng thất bại thành công!')
    failDialogVisible.value = false
    fetchInvoice()
  } catch (err) {
    ElMessage.error('Lỗi cập nhật trạng thái thất bại')
    console.error(err)
  } finally {
    loadingFail.value = false
  }
}

/* ===== Lịch sử tác động ===== */
const actionHistoryDialogVisible = ref(false)
const actionHistory = ref([])

const showActionHistoryDialog = async () => {
  try {
    const res = await apiClient.get('/admin/online-sales/get-order-history', { params: { invoiceId } })
    actionHistory.value = res.data || []
    actionHistoryDialogVisible.value = true
  } catch (err) {
    ElMessage.error('Lỗi tải lịch sử tác động')
    console.error(err)
  }
}

const getStatusLabelFromInt = (code) => STATUS_LABELS?.[code] ?? `Không xác định (${code})`
const getStatusTagType = (code) => STATUS_TAG?.[code] ?? 'info'

/* ===== SĐT ===== */
const phoneDialogVisible = ref(false)
const phoneForm = ref({ phone: '' })
const VN_PHONE_REGEX = /^(0|\+84)\d{9,10}$/

const openPhoneDialog = () => {
  if (!canEditContactAddress.value)
    return ElMessage.warning('Không thể sửa thông tin sau khi đã sang giai đoạn giao hàng.')
  phoneForm.value.phone = String(invoice.value?.phone || '')
  phoneDialogVisible.value = true
}

const submitPhoneUpdate = async () => {
  if (!canEditContactAddress.value) {
    phoneDialogVisible.value = false
    return ElMessage.warning('Không thể sửa thông tin sau khi đã sang giai đoạn giao hàng.')
  }
  const phone = (phoneForm.value.phone || '').replace(/\s+/g, '')
  if (!phone) return ElMessage.warning('Vui lòng nhập số điện thoại.')
  if (!VN_PHONE_REGEX.test(phone)) return ElMessage.warning('Số điện thoại không hợp lệ.')

  try {
    savingPhone.value = true
    await apiClient.put('/admin/online-sales/update-phone', null, {
      params: { invoiceId, phone }
    })
    ElMessage.success('Cập nhật số điện thoại thành công!')
    phoneDialogVisible.value = false
    fetchInvoice()
  } catch (err) {
    ElMessage.error('Lỗi cập nhật số điện thoại!')
    console.error(err)
  } finally {
    savingPhone.value = false
  }
}

/* ===== Địa chỉ & GHN ===== */
const addressDialogVisible = ref(false)
const provinces = ref([])
const districts = ref([])
const wards = ref([])
const addressForm = ref({
  houseNumber: '',
  provinceCode: null, provinceName: '',
  districtCode: null, districtName: '',
  wardCode: null, wardName: '',
})
const shippingFee = ref(null)

// GHN (giữ nguyên token/const như bạn đang dùng)
const GHN_TOKEN = '847c9bb7-6e42-11ee-a59f-a260851ba65c'
const GHN_TOKEN_FEE = '741f1c91-4f42-11f0-8cf5-d2552bfd31d8'
const FROM_DISTRICT_ID = 1483
const FROM_WARD_CODE = '21108'
const SHOP_ID = 5851480

const loadProvincesForAddress = async () => {
  loadingRegions.value = true
  try {
    const res = await axios.post(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/province',
      {},
      { headers: { Token: GHN_TOKEN } }
    )
    provinces.value = res.data.data || []
  } catch {
    ElMessage.error('Không thể tải danh sách tỉnh/thành phố.')
  } finally {
    loadingRegions.value = false
  }
}

const loadDistrictsForAddress = async () => {
  addressForm.value.districtCode = null
  addressForm.value.districtName = ''
  addressForm.value.wardCode = null
  addressForm.value.wardName = ''
  districts.value = []
  wards.value = []
  if (!addressForm.value.provinceCode) return

  loadingRegions.value = true
  try {
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/district',
      { headers: { Token: GHN_TOKEN }, params: { province_id: addressForm.value.provinceCode } }
    )
    districts.value = res.data.data || []
  } catch {
    ElMessage.error('Không thể tải danh sách quận/huyện.')
  } finally {
    loadingRegions.value = false
  }
}

const loadWardsForAddress = async () => {
  addressForm.value.wardCode = null
  addressForm.value.wardName = ''
  wards.value = []
  if (!addressForm.value.districtCode) return

  loadingRegions.value = true
  try {
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/ward',
      { headers: { Token: GHN_TOKEN }, params: { district_id: addressForm.value.districtCode } }
    )
    wards.value = res.data.data || []
  } catch {
    ElMessage.error('Không thể tải danh sách phường/xã.')
  } finally {
    loadingRegions.value = false
  }
}

const handleProvinceChangeForAddress = () => {
  const sel = provinces.value.find(p => p.ProvinceID === addressForm.value.provinceCode)
  addressForm.value.provinceName = sel?.ProvinceName || ''
  loadDistrictsForAddress()
}

const handleDistrictChangeForAddress = () => {
  const sel = districts.value.find(d => d.DistrictID === addressForm.value.districtCode)
  addressForm.value.districtName = sel?.DistrictName || ''
  loadWardsForAddress()
}

const handleWardChangeForAddress = async () => {
  const sel = wards.value.find(w => w.WardCode === addressForm.value.wardCode)
  addressForm.value.wardName = sel?.WardName || ''
  const fee = await calculateShippingFee()
  shippingFee.value = fee
}

const calculateShippingFee = async () => {
  if (!addressForm.value.districtCode || !addressForm.value.wardCode) {
    ElMessage.warning('Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện, Phường/Xã')
    return 0
  }
  try {
    const details = Array.isArray(invoice.value.invoiceDetailResponses) ? invoice.value.invoiceDetailResponses : []
    const totalWeight = details.reduce((sum, it) => {
      const w = Number(it?.weight ?? 200)
      const q = Number(it?.quantity ?? 1)
      return sum + w * q
    }, 0)

    const res = await axios.post(
      'https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee',
      {
        from_district_id: FROM_DISTRICT_ID,
        from_ward_code: FROM_WARD_CODE,
        to_district_id: addressForm.value.districtCode,
        to_ward_code: addressForm.value.wardCode,
        weight: Math.max(totalWeight, 100),
        insurance_value: Number(invoice.value.totalAmount || 0),
        service_type_id: 2,
      },
      { headers: { 'Content-Type': 'application/json', Token: GHN_TOKEN_FEE, ShopId: SHOP_ID } }
    )
    return Number(res?.data?.data?.total ?? 0)
  } catch (err) {
    console.error('❌ Lỗi tính phí ship:', err)
    ElMessage.error('Không thể tính phí vận chuyển. Vui lòng thử lại.')
    return 0
  }
}

const openAddressDialog = async () => {
  if (!canEditContactAddress.value)
    return ElMessage.warning('Không thể sửa địa chỉ sau khi đã sang giai đoạn giao hàng.')
  addressDialogVisible.value = true
  shippingFee.value = null

  addressForm.value = {
    houseNumber: '',
    provinceCode: null, provinceName: '',
    districtCode: null, districtName: '',
    wardCode: null, wardName: '',
  }
  districts.value = []
  wards.value = []

  await loadProvincesForAddress()

  // Parse địa chỉ hiện tại
  const current = invoice.value?.deliveryAddress || ''
  const parts = current.split(' - ').map(s => s.trim())
  if (parts.length >= 4) {
    addressForm.value.houseNumber = parts[0]
    const [wardName, districtName, provinceName] = [parts[1], parts[2], parts[3]]

    const p = provinces.value.find(x => x.ProvinceName === provinceName)
    if (p) {
      addressForm.value.provinceCode = p.ProvinceID
      addressForm.value.provinceName = p.ProvinceName
      await loadDistrictsForAddress()

      const d = districts.value.find(x => x.DistrictName === districtName)
      if (d) {
        addressForm.value.districtCode = d.DistrictID
        addressForm.value.districtName = d.DistrictName
        await loadWardsForAddress()

        const w = wards.value.find(x => x.WardName === wardName)
        if (w) {
          addressForm.value.wardCode = w. WardCode
          addressForm.value.wardName = w. WardName
        }
      }
    }
  }
}

const submitAddressUpdate = async () => {
  if (!canEditContactAddress.value) {
    addressDialogVisible.value = false
    return ElMessage.warning('Không thể sửa địa chỉ sau khi đã sang giai đoạn giao hàng.')
  }
  const { houseNumber, provinceName, districtName, wardName } = addressForm.value
  if (!houseNumber || !provinceName || !districtName || !wardName) {
    return ElMessage.warning('Vui lòng nhập đầy đủ thông tin địa chỉ.')
  }

  try {
    savingAddress.value = true
    const fee = await calculateShippingFee()

    const fullAddress = `${houseNumber} - ${wardName} - ${districtName} - ${provinceName}`
    const totalAmount = Number(invoice.value.totalAmount) || 0
    const discountAmount = Number(invoice.value.discountAmount) || 0
    const finalAmount = totalAmount + fee - discountAmount

    await apiClient.put('/admin/online-sales/update-address', {
      invoiceId,
      newAddress: fullAddress,
      shippingFee: fee,
      finalAmount
    })
    ElMessage.success('Cập nhật địa chỉ thành công!')
    addressDialogVisible.value = false
    fetchInvoice()
  } catch (err) {
    ElMessage.error('Lỗi cập nhật địa chỉ!')
    console.error(err)
  } finally {
    savingAddress.value = false
  }
}

/* ===== Helpers ===== */
const formatDate = (v) => (v ? new Date(v).toLocaleString('vi-VN') : '—')
const formatCurrency = (n) => `${Number(n || 0).toLocaleString('vi-VN')} ₫`

/* ===== Lifecycle ===== */
onMounted(fetchInvoice)
</script>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
}
.card { border-radius: 14px; }
.header {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
}
.title { display: flex; align-items: center; gap: 8px; margin: 0; font-weight: 700; }
.title-icon { color: var(--el-color-primary); }
.actions { display: flex; align-items: center; gap: 8px; }

.desc :deep(.el-descriptions__label) {
  width: 220px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.desc :deep(.el-descriptions__cell) { padding: 10px 12px; }

.row-inline { display: flex; align-items: center; gap: 10px; }
.price { display: flex; gap: 8px; justify-content: flex-end; align-items: baseline; }
.price .old { text-decoration: line-through; color: var(--el-text-color-placeholder); }
.price .new.danger { color: var(--el-color-danger); font-weight: 700; }
</style>
