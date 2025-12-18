<template>
  <div>
    <div class="search-bar-container">
      <el-input
        v-model="keyword"
        placeholder="Tìm mã hóa đơn, tên/SĐT khách hàng..."
        @input="emitSearch"
        clearable
        :prefix-icon="Search"
      />

      <!-- Trạng thái TẠI QUẦY (orderType = 0) -->
      <el-select
        v-model="counterStatusKey"
        @change="emitSearch"
        placeholder="Trạng thái tại quầy"
        clearable
      >
        <el-option
          v-for="opt in COUNTER_STATUS_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>

      <!-- Trạng thái ONLINE (orderType = 1) -->
      <el-select
        v-model="onlineStatusKey"
        @change="emitSearch"
        placeholder="Trạng thái online"
        clearable
      >
        <el-option
          v-for="opt in ONLINE_STATUS_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>

      <el-date-picker
        v-model="createdDate"
        type="date"
        placeholder="Chọn ngày tạo"
        @change="emitSearch"
        clearable
        format="DD/MM/YYYY"
        value-format="YYYY-MM-DD"
      />

      <el-button
        type="primary"
        @click="startScan"
        :disabled="scanning"
        :loading="scanning"
        :icon="Camera"
      >
        Quét QR
      </el-button>

      <el-button class="ms-2" @click="onClearClick">
        Xoá lọc
      </el-button>
    </div>

    <!-- QR scanner -->
    <el-card v-if="scanning" class="qr-scanner-container mt-3" shadow="never">
      <template #header>
        <div class="d-flex justify-content-between align-items-center">
          <span>Đưa mã QR vào vùng camera</span>
          <el-button type="danger" @click="stopScan" :icon="CircleClose" circle />
        </div>
      </template>
      <div id="qr-reader" style="width: 100%;"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Camera, CircleClose } from '@element-plus/icons-vue'
import { Html5Qrcode } from 'html5-qrcode'
import apiClient from '@/utils/axiosInstance.js'

const emit = defineEmits(['search', 'clear'])

const keyword = ref('')
const counterStatusKey = ref('')   // trạng thái tại quầy (TrangThaiTong)
const onlineStatusKey  = ref('')   // trạng thái online (TrangThaiChiTiet)
const createdDate = ref('')        // 'YYYY-MM-DD'

// TẠI QUẦY: chỉ 3 trạng thái
const COUNTER_STATUS_OPTIONS = [
  { label: 'Tất cả (tại quầy)', value: '' },
  { label: 'Chờ xử lý',         value: 'DANG_XU_LY' },
  { label: 'Thành công',        value: 'THANH_CONG' },
  { label: 'Đã hủy',            value: 'DA_HUY' },
]

// ONLINE: đúng như ảnh bạn gửi
const ONLINE_STATUS_OPTIONS = [
  { label: 'Tất cả (online)',     value: '' },
  { label: 'Chờ xử lý',           value: 'CHO_XU_LY' },
  { label: 'Đã xử lý',            value: 'DA_XU_LY' },
  { label: 'Chờ giao hàng',       value: 'CHO_GIAO_HANG' },
  { label: 'Đang giao hàng',      value: 'DANG_GIAO_HANG' },
  { label: 'Giao hàng thành công', value: 'GIAO_THANH_CONG' },
  { label: 'Giao hàng thất bại',  value: 'GIAO_THAT_BAI' },
  { label: 'Hủy đơn',             value: 'HUY_DON' },
]

let debounceTimer = null
function emitSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    emit('search', {
      keyword: (keyword.value || '').trim() || undefined,
      counterStatusKey: counterStatusKey.value || undefined,
      onlineStatusKey:  onlineStatusKey.value  || undefined,
      createdDate: createdDate.value || undefined,
    })
  }, 250)
}

function onClearClick() {
  keyword.value = ''
  counterStatusKey.value = ''
  onlineStatusKey.value = ''
  createdDate.value = ''
  emit('clear')
}

/* ===== QR Scan (tuỳ chọn) ===== */
const scanning = ref(false)
let html5QrCode = null

async function startScan () {
  scanning.value = true
  await nextTick()
  html5QrCode = new Html5Qrcode('qr-reader')
  try {
    await html5QrCode.start(
      { facingMode: 'environment' },
      { fps: 60, qrbox: { width: 250, height: 250 } },
      msg => { stopScan(); sendQRCodeToServer(msg) },
      _ => {}
    )
  } catch (err) {
    ElMessage.error('Không thể truy cập camera: ' + err)
    scanning.value = false
  }
}
async function stopScan () {
  if (html5QrCode && scanning.value) {
    try { await html5QrCode.stop() } catch (e) { console.warn(e) }
  }
  scanning.value = false
}
// === GIỮ nguyên hàm statusText bạn đã đưa ===
const statusText = (status, statusDetail, orderType) => {
  if (orderType === 0) {
    switch (status) {
      case 'DANG_XU_LY': return 'Chờ xử lý'
      case 'THANH_CONG': return 'Thành công'
      case 'DA_HUY':     return 'Đã hủy'
      case 'HUY_GIAO_DICH': return 'Hủy giao dịch'
      case 'TRA_HANG':      return 'Trả hàng'
      case 'KHIEU_NAI':     return 'Khiếu nại'
      default:              return 'Không xác định'
    }
  } else if (orderType === 1) {
    switch (statusDetail) {
      case 'CHO_XU_LY':       return 'Chờ xử lý'
      case 'DA_XU_LY':        return 'Đã xử lý'
      case 'CHO_GIAO_HANG':   return 'Chờ giao hàng'
      case 'DANG_GIAO_HANG':  return 'Đang giao hàng'
      case 'GIAO_THANH_CONG': return 'Giao hàng thành công'
      case 'GIAO_THAT_BAI':   return 'Giao hàng thất bại'
      case 'HUY_DON':         return 'Hủy đơn'
      case 'HUY_GIAO_DICH':   return 'Hủy giao dịch'
      case 'DANG_GIAO_DICH':  return 'Đang giao dịch'
      case 'MAT_HANG':        return 'Mất hàng'
      case 'DA_HOAN_TIEN':    return 'Đã hoàn tiền'
      case 'DA_HOAN_THANH':   return 'Đã hoàn thành'
      default:                return 'Không xác định'
    }
  }
  return 'Không xác định'
}

// Helper: map text trạng thái -> màu badge
function statusBadgeMeta(status, statusDetail, orderType) {
  const text = statusText(status, statusDetail, orderType)

  // gom nhóm gần giống statusClass bạn dùng ở list
  const isSuccess = ['Thành công', 'Giao hàng thành công', 'Đã hoàn thành'].includes(text)
  const isWarning = ['Chờ xử lý', 'Chờ giao hàng'].includes(text)
  const isDanger  = [
    'Đã hủy', 'Hủy giao dịch', 'Giao hàng thất bại', 'Mất hàng', 'Hủy đơn'
  ].includes(text)

  if (isSuccess) return { text, color: '#52c41a', bg: 'rgba(82,196,26,.12)' }
  if (isWarning) return { text, color: '#faad14', bg: 'rgba(250,173,20,.12)' }
  if (isDanger)  return { text, color: '#ff4d4f', bg: 'rgba(255,77,79,.12)' }
  return { text, color: '#409EFF', bg: 'rgba(64,158,255,.12)' } // info mặc định
}

async function sendQRCodeToServer(qrCode) {
  try {
    const { data } = await apiClient.post('/admin/invoices/qr-scan', { invoiceCode: qrCode })
    const invoice  = data?.invoice ?? {}
    const details  = Array.isArray(data?.details) ? data.details : []

    const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
    const safe = (s, def = '—') => (s == null || s === '') ? def : s
    const orderType = Number(invoice.orderType ?? 0) // 0 tại quầy, 1 online

    // Lấy text & màu theo định nghĩa statusText
    const { text: statusLabel, color: stColor, bg: stBg } =
      statusBadgeMeta(invoice.status, invoice.statusDetail, orderType)

    // Bảng sản phẩm
    const rowsHtml = details.length
      ? details.map((item, idx) => {
          const qty       = Number(item.quantity ?? 0)
          const unitPrice = Number(item.price ?? item.sellPrice ?? 0)
          const lineTotal = qty * unitPrice
          const sizeName  = safe(item.size?.sizeName)
          const colorName = safe(item.color?.colorName)
          const name      = safe(item.productName, 'Sản phẩm')

          return `
            <tr>
              <td style="padding:8px 10px; text-align:center;">${idx + 1}</td>
              <td style="padding:8px 10px;">
                <div style="font-weight:600; line-height:1.3">${name}</div>
                <div style="color:#909399; font-size:12px; margin-top:2px;">Size: ${sizeName} · Màu: ${colorName}</div>
              </td>
              <td style="padding:8px 10px; text-align:right;">${fmt(unitPrice)}</td>
              <td style="padding:8px 10px; text-align:center;">${qty}</td>
              <td style="padding:8px 10px; text-align:right; font-weight:600;">${fmt(lineTotal)}</td>
            </tr>
          `
        }).join('')
      : `
        <tr>
          <td colspan="5" style="padding:14px 10px; text-align:center; color:#909399;">
            Không có sản phẩm trong hóa đơn.
          </td>
        </tr>
      `

    const total       = Number(invoice.finalAmount ?? 0)
    const totalAmount = Number(invoice.totalAmount ?? 0)
    const discount    = Number(invoice.discountAmount ?? 0)
    const shipping    = Number(invoice.shippingFee ?? 0)

    const headerHtml = `
      <div style="display:flex; align-items:center; justify-content:space-between; gap:12px;">
        <div>
          <div style="font-size:14px; color:#909399;">Mã HĐ</div>
          <div style="font-weight:700; font-size:16px;">${safe(invoice.invoiceCode, '—')}</div>
        </div>
        <div style="display:inline-flex; align-items:center; gap:8px;">
          <span
            style="
              display:inline-block;
              padding:4px 10px;
              border-radius:999px;
              font-weight:600;
              font-size:12px;
              color:${stColor};
              background:${stBg};
            "
          >${statusLabel}</span>
        </div>
      </div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:8px; margin-top:10px;">
        <div>
          <div style="font-size:12px; color:#909399;">Khách hàng</div>
          <div style="font-weight:600;">${safe(invoice.customerName, 'Khách lẻ')}</div>
        </div>
        <div>
          <div style="font-size:12px; color:#909399;">Ngày tạo</div>
          <div style="font-weight:600;">${formatDateTime(invoice.createdDate)}</div>
        </div>
      </div>
    `

    const tableHtml = `
      <table style="width:100%; border-collapse:collapse; margin-top:12px; font-size:13px;">
        <thead>
          <tr style="background:#f5f7fa;">
            <th style="padding:8px 10px; text-align:center; width:44px;">#</th>
            <th style="padding:8px 10px; text-align:left;">Sản phẩm</th>
            <th style="padding:8px 10px; text-align:right; width:110px;">Đơn giá (₫)</th>
            <th style="padding:8px 10px; text-align:center; width:70px;">SL</th>
            <th style="padding:8px 10px; text-align:right; width:130px;">Thành tiền (₫)</th>
          </tr>
        </thead>
        <tbody>
          ${rowsHtml}
        </tbody>
      </table>
    `

    const totalsHtml = `
      <div style="margin-top:12px; display:flex; justify-content:flex-end;">
        <div style="min-width:320px;">
          <div style="display:flex; justify-content:space-between; padding:6px 0; color:#606266;">
            <span>Tổng tiền hàng</span><span>${fmt(totalAmount)}</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:6px 0; color:#606266;">
            <span>Giảm giá</span><span>- ${fmt(discount)}</span>
          </div>
          <div style="display:flex; justify-content:space-between; padding:6px 0; color:#606266;">
            <span>Phí vận chuyển</span><span>${fmt(shipping)}</span>
          </div>
          <div style="height:1px; background:#ebeef5; margin:6px 0;"></div>
          <div style="display:flex; justify-content:space-between; padding:6px 0;">
            <span style="font-weight:700;">TỔNG CỘNG</span>
            <span style="font-weight:800; font-size:16px; color:#f56c6c;">${fmt(total)} ₫</span>
          </div>
        </div>
      </div>
    `

    const htmlContent = `
      <div style="text-align:left; font-size:13px; line-height:1.5; color:#303133;">
        ${headerHtml}
        <div style="height:1px; background:#ebeef5; margin:12px 0;"></div>
        ${tableHtml}
        ${totalsHtml}
      </div>
    `

    ElMessageBox.alert(htmlContent, 'Thông tin hóa đơn', {
      dangerouslyUseHTMLString: true,
      customClass: 'invoice-preview-box',
      showConfirmButton: true,
      confirmButtonText: 'Đóng',
      center: false,
    })
  } catch (error) {
    ElMessage.error(`Lỗi: ${error?.response?.data || error?.message}`)
  }
}

// Helper hiển thị datetime
function formatDateTime(val) {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return '—'
  const dd = d.toLocaleDateString('vi-VN')
  const tt = d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  return `${dd} ${tt}`
}


</script>

<style scoped>
.search-bar-container { display: flex; gap: 10px; align-items: center; }
.qr-scanner-container { max-width: 400px; }
.mt-3 { margin-top: 1rem; }
.ms-2 { margin-left: 0.5rem; }
.d-flex { display: flex; }
.justify-content-between { justify-content: space-between; }
.align-items-center { align-items: center; }
.el-select, .el-date-editor { width: 220px; }
</style>
