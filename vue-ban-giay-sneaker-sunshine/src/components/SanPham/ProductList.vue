<template>
  <div class="page">
    <!-- Header actions -->
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="goToAdd">Thêm sản phẩm</el-button>

      <div class="toolbar__right">
        <el-button type="success" :icon="Document" @click="downloadExcel">Xuất Excel</el-button>
        <el-button
          v-if="selectedIds.size"
          type="danger"
          plain
          :icon="CircleClose"
          @click="clearAllSelections"
        >
          Bỏ chọn ({{ selectedIds.size }})
        </el-button>
        <el-button :icon="Clock" @click="goToHistory">Lịch sử sản phẩm</el-button>
      </div>
    </div>

    <!-- Compact filter (sticky) -->
    <el-card shadow="never" class="filter-card" body-class="filter-card__body">
      <template #header>
        <div class="filter-card__head">
          <div class="filter-title">Tìm kiếm</div>
          <el-link type="primary" :underline="false" @click="showAdvanced = !showAdvanced">
            {{ showAdvanced ? 'Thu gọn' : 'Mở rộng' }}
          </el-link>
        </div>
      </template>

      <div class="filter-row">
        <div class="filter-left">
          <el-form :model="filters" :inline="true" size="small" class="filter-inline" @submit.prevent>
            <el-form-item label="Tên/Mã" class="w-64">
              <el-input
                v-model="filters.keyword"
                clearable
                :prefix-icon="Search"
                placeholder="Nhập tên hoặc mã"
                @keyup.enter="onSearch"
              />
            </el-form-item>

            <el-form-item label="Danh mục" class="w-60">
              <el-select
                v-model="filters.categoryIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                filterable
                clearable
                placeholder="Chọn"
              >
                <el-option v-for="c in categoryList" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="Thương hiệu" class="w-56">
              <el-select v-model="filters.brandId" filterable clearable placeholder="Chọn">
                <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="Chất liệu">
              <el-select v-model="filters.materialId" clearable filterable placeholder="Chọn">
                <el-option v-for="m in materialList" :key="m.id" :label="m.materialName" :value="m.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="Loại đế">
              <el-select v-model="filters.soleId" clearable filterable placeholder="Chọn">
                <el-option v-for="s in soleList" :key="s.id" :label="s.soleName" :value="s.id" />
              </el-select>
            </el-form-item>

          </el-form>
        </div>

        <div class="filter-right">
          <el-button type="primary" :icon="Search" @click="onSearch">Tìm kiếm</el-button>
          <el-button :icon="Refresh" @click="onReset">Đặt lại</el-button>
        </div>
      </div>

      <!-- Nâng cao -->
    <el-collapse-transition>
  <div v-show="showAdvanced" class="advanced">
    <el-form :model="filters" label-position="top" size="small" class="advanced-grid">

      <!-- Cổ giày -->
      <el-form-item label="Cổ giày" class="col-span-2">
        <el-select v-model="filters.styleId" clearable filterable placeholder="Chọn">
          <el-option v-for="s in styleList" :key="s.id" :label="s.styleName" :value="s.id" />
        </el-select>
      </el-form-item>

      <!-- Dành cho -->   
      <el-form-item label="Dành cho" class="col-span-2">
        <el-radio-group v-model="filters.genderId" class="full-center">
          <el-radio-button :label="1">Nam</el-radio-button>
          <el-radio-button :label="2">Nữ</el-radio-button>
          <el-radio-button :label="3">Cả hai</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- Ngày tạo -->
      <el-form-item label="Ngày tạo (Từ - Đến)" class="col-span-2">
        <div class="date-range">
          <el-date-picker
            v-model="filters.createdFrom"
            type="datetime"
            placeholder="Từ ngày"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            clearable
          />
          <el-date-picker
            v-model="filters.createdTo"
            type="datetime"
            placeholder="Đến ngày"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            clearable
          />
        </div>
      </el-form-item>

    </el-form>
  </div>
</el-collapse-transition>

    </el-card>

    <div class="summary">
      Đã chọn: <b>{{ selectedIds.size }}</b> / Tổng: <b>{{ totalElements }}</b> sản phẩm
    </div>

    <el-card shadow="never">
      <el-table
        ref="tableRef"
        v-loading="tableLoading"
        :data="productList"
        :row-key="rowKey"
        border
        stripe
        height="540"
        @selection-change="onSelectionChange"
        @sort-change="onSortChange"
      >
        <el-table-column type="selection" width="50" :reserve-selection="true" />

        <el-table-column label="STT" width="80">
          <template #default="{ $index }">
            {{ page * size + $index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="productCode" label="Mã" min-width="120" show-overflow-tooltip sortable />
        <el-table-column prop="productName" label="Tên sản phẩm" min-width="220" show-overflow-tooltip />
        <el-table-column prop="brandName" label="Thương hiệu" min-width="140" />
        <el-table-column prop="styleName" label="Cổ giày" min-width="120" />

        <el-table-column label="Giá bán" min-width="180">
          <template #default="{ row }">
            <template v-if="hasEffectiveDiscountInDetails(row)">
      <span class="line-through mr-2 text-muted">
        {{ formatCurrency(getMinPricesFromDetails(row).minOriginal) }}
      </span>
              <span class="price-sale">
        {{ formatCurrency(getMinPricesFromDetails(row).minDiscounted) }}
      </span>
            </template>
            <template v-else>
      <span class="price">
        {{ formatCurrency(getMinPricesFromDetails(row).minOriginal) }}
      </span>
            </template>
          </template>
        </el-table-column>


        <el-table-column prop="quantity" label="Số lượng" width="110">
          <template #default="{ row }">
            <el-tag :type="row.quantity < 10 ? 'danger' : 'success'" effect="light">
              {{ row.quantity }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Hành động" fixed="right" width="170">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="goToUpdate(row.id)" />
            <el-button size="small" :icon="InfoFilled" @click="goToDetail(row.id)" />
            <el-popconfirm
              title="Chuyển sản phẩm vào thùng rác?"
              confirm-button-text="Xóa"
              cancel-button-text="Hủy"
              confirm-button-type="danger"
              @confirm="deleteProduct(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger" :icon="Delete" />
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalElements"
          :current-page="page + 1"
          :page-size="size"
          :page-sizes="[5, 8, 10, 20, 50]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Document,
  Clock,
  CircleClose,
  Edit,
  InfoFilled,
  Delete,
} from '@element-plus/icons-vue'
import apiClient from '@/utils/axiosInstance'

type ProductRow = {
  id: number
  productCode: string
  productName: string
  brandName: string
  styleName: string
  sellPrice: number
  discountedPrice: number
  discountPercentage: number
  quantity: number
}

const router = useRouter()

/* ===== UI State ===== */
const showAdvanced = ref(false)
const tableLoading = ref(false)

/* ===== Filters ===== */
const filters = ref({
  keyword: '',
  brandId: null as number | null,
  genderId: null as number | null,
  styleId: null as number | null,
  soleId: null as number | null,
  materialId: null as number | null,
  createdFrom: null as string | null,
  createdTo: null as string | null,
  categoryIds: [] as number[],
  priceMin: null as number | null,
  priceMax: null as number | null,
  status: 1,
})

/* ===== Reference Data ===== */
const brandList = ref<any[]>([])
const materialList = ref<any[]>([])
const categoryList = ref<any[]>([])
const soleList = ref<any[]>([])
const styleList = ref<any[]>([])

/* ===== Table & Pagination ===== */
const productList = ref<ProductRow[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(8)

/* ===== Sorting (server-side) ===== */
const sortField = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc' | null>(null)

/* ===== Selection (reserve across pages) ===== */
const selectedIds = ref<Set<number>>(new Set<number>())
const rowKey = (row: ProductRow) => row.id
const tableRef = ref<any>(null)

const onSelectionChange = (rows: ProductRow[]) => {
  const currentPageIds = productList.value.map(r => r.id)
  currentPageIds.forEach(id => selectedIds.value.delete(id))
  rows.forEach(r => selectedIds.value.add(r.id))
}

const clearAllSelections = () => {
  selectedIds.value.clear()
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  ElMessage.info('Đã bỏ chọn tất cả sản phẩm.')
}

/* ===== Fetch reference data ===== */
const fetchCategories = async () => {
  try {
    const res = await apiClient.get('/admin/categories/hien-thi')
    categoryList.value = res.data
  } catch {
    ElMessage.error('Lấy danh mục thất bại!')
  }
}
const fetchMaterial = async () => {
  try {
    const res = await apiClient.get('/admin/material/hien-thi')
    materialList.value = res.data
  } catch {
    ElMessage.error('Lấy chất liệu thất bại!')
  }
}
const fetchBrand = async () => {
  try {
    const res = await apiClient.get('/admin/brand/hien-thi')
    brandList.value = res.data
  } catch {
    ElMessage.error('Lấy thương hiệu thất bại!')
  }
}
const fetchSole = async () => {
  try {
    const res = await apiClient.get('/admin/sole/hien-thi')
    soleList.value = res.data
  } catch {
    ElMessage.error('Lấy đế giày thất bại!')
  }
}
const fetchStyle = async () => {
  try {
    const res = await apiClient.get('/admin/style/hien-thi')
    styleList.value = res.data
  } catch {
    ElMessage.error('Lấy phong cách thất bại!')
  }
}

/* ===== Validation ===== */
const validateRanges = (): boolean => {
  const { priceMin, priceMax, createdFrom, createdTo } = filters.value
  if (priceMin != null && priceMax != null && priceMin > priceMax) {
    ElMessage.warning('Giá Min không được lớn hơn Giá Max.')
    return false
  }
  if (createdFrom && createdTo && createdFrom > createdTo) {
    ElMessage.warning('Ngày bắt đầu không được lớn hơn ngày kết thúc.')
    return false
  }
  return true
}

/* ===== Fetch products ===== */
const fetchProduct = async () => {
  if (!validateRanges()) return
  tableLoading.value = true
  try {
    const payload: any = {
      keyword: filters.value.keyword || null,
      brandId: filters.value.brandId || null,
      genderId: filters.value.genderId || null,
      styleId: filters.value.styleId || null,
      soleId: filters.value.soleId || null,
      materialId: filters.value.materialId || null,
      createdFrom: filters.value.createdFrom || null,
      createdTo: filters.value.createdTo || null,
      categoryIds: filters.value.categoryIds || [],
      priceMin: filters.value.priceMin ?? null,
      priceMax: filters.value.priceMax ?? null,
      page: page.value,
      size: size.value,
      status: 1,
    }

    if (sortField.value) {
      payload.sortField = sortField.value
      payload.sortOrder = sortOrder.value
    }

    const res = await apiClient.post('/admin/products/search', payload)
    productList.value = res.data.data || []
    console.log('data: ',productList.value)
    totalElements.value = res.data.pagination?.totalElements || 0
    totalPages.value = res.data.pagination?.totalPages || 0

    // restore visual selection
    await nextTick()
    if (tableRef.value) {
      tableRef.value.clearSelection()
      productList.value.forEach((row) => {
        if (selectedIds.value.has(row.id)) {
          tableRef.value.toggleRowSelection(row, true)
        }
      })
    }
  } catch (err: any) {
    if (err?.response?.status === 403) {
      router.push('/error')
      return
    }
    ElMessage.error('Tải danh sách sản phẩm thất bại!')
    productList.value = []
    totalElements.value = 0
    totalPages.value = 0
    page.value = 0
  } finally {
    tableLoading.value = false
  }
}

/* ===== Actions ===== */
const onSearch = () => {
  page.value = 0
  fetchProduct()
}
const onReset = () => {
  filters.value = {
    keyword: '',
    brandId: null, genderId: null, styleId: null, soleId: null, materialId: null,
    createdFrom: null, createdTo: null,
    categoryIds: [],
    priceMin: null, priceMax: null,
    status: 1,
  }
  page.value = 0
  size.value = 8
  selectedIds.value.clear()
  if (tableRef.value) tableRef.value.clearSelection()
  fetchProduct()
}

const handleSizeChange = (newSize: number) => { size.value = newSize; page.value = 0; fetchProduct() }
const handleCurrentChange = (newPage: number) => { page.value = newPage - 1; fetchProduct() }

/* ===== Sorting handler (server-side) ===== */
const onSortChange = ({ prop, order }: { prop: string | undefined; order: string | undefined }) => {
  if (!prop || !order) {
    sortField.value = null
    sortOrder.value = null
  } else {
    sortField.value = prop
    sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  }
  page.value = 0
  fetchProduct()
}

const downloadExcel = async () => {
  try {
    const hasIds = selectedIds.value.size > 0
    const msg = hasIds
      ? `Xuất Excel cho ${selectedIds.value.size} sản phẩm đã chọn?`
      : 'Xuất Excel toàn bộ sản phẩm theo bộ lọc hiện tại?'
    await ElMessageBox.confirm(msg, 'Xác nhận', {
      type: 'warning', confirmButtonText: 'Xuất', cancelButtonText: 'Hủy',
    })

    let url = '', data: any, filename = ''
    if (hasIds) {
      url = '/admin/products/export-excel/by-ids'
      data = Array.from(selectedIds.value)
      filename = 'products-by-ids.xlsx'
    } else {
      const { keyword, brandId, genderId, styleId, soleId, materialId, createdFrom, createdTo, categoryIds, priceMin, priceMax } =
        filters.value
      url = '/admin/products/export-excel/by-filter'
      data = {
        keyword: keyword || null, brandId: brandId || null, genderId: genderId || null,
        styleId: styleId || null, soleId: soleId || null, materialId: materialId || null,
        createdFrom: createdFrom || null, createdTo: createdTo || null,
        categoryIds: categoryIds || [], priceMin: priceMin ?? null, priceMax: priceMax ?? null,
        status: 1,
      }
      filename = 'products-by-filter.xlsx'
    }

    const res = await apiClient.post(url, data, { responseType: 'blob' })
    const blobUrl = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a'); a.href = blobUrl; a.download = filename
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    window.URL.revokeObjectURL(blobUrl)
    ElMessage.success('Xuất Excel thành công!')
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error('Xuất Excel thất bại!')
  }
}

const deleteProduct = async (id: number) => {
  try {
    await apiClient.delete(`/admin/products/${id}`)
    ElMessage.success('Xóa sản phẩm thành công!')
    selectedIds.value.delete(id)
    fetchProduct()
  } catch {
    ElMessage.error('Xóa sản phẩm thất bại!')
  }
}

/* ===== Navigation ===== */
const goToAdd = () => router.push('/product/add')
const goToHistory = () => router.push('/product/history')
const goToUpdate = (id: number) => router.push({ name: 'UpdateProduct', params: { id } })
const goToDetail = (id: number) => router.push({ name: 'DetailProduct', params: { id } })

/* ===== Helpers ===== */
const formatCurrency = (val: number) => {
  if (val == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', minimumFractionDigits: 0 }).format(val)
}

// Trả về { minOriginal: Number, minDiscounted: Number|null }
// CHÚ Ý: chỉ lấy từ row.productDetails
function getMinPricesFromDetails(row) {
  const details = Array.isArray(row?.productDetails) ? row.productDetails : []
  if (!details.length) {
    // nếu không có chi tiết, fallback về row.sellPrice / row.discountedPrice nếu muốn
    const orig = Number(row?.sellPrice) || 0
    const disc = (row?.discountedPrice != null && Number(row.discountedPrice) < orig)
      ? Number(row.discountedPrice)
      : null
    return { minOriginal: orig, minDiscounted: disc }
  }

  let minOriginal = Number.POSITIVE_INFINITY
  let minDiscounted = Number.POSITIVE_INFINITY

  for (const d of details) {
    const sp = Number(d?.sellPrice)
    if (!isNaN(sp)) minOriginal = Math.min(minOriginal, sp)

    // 1) ưu tiên discountedPrice nếu có
    let dp = null
    if (d?.discountedPrice != null && d.discountedPrice !== '') {
      dp = Number(d.discountedPrice)
    } else if (d?.discountPercentage != null && !isNaN(Number(d.discountPercentage)) && !isNaN(sp)) {
      // 2) fallback: tính từ discountPercentage
      const pct = Number(d.discountPercentage)
      dp = Math.round(sp * (100 - pct) / 100)
    }

    if (dp != null && !isNaN(dp) && !isNaN(sp) && dp < sp) {
      minDiscounted = Math.min(minDiscounted, dp)
    }
  }

  if (minOriginal === Number.POSITIVE_INFINITY) minOriginal = 0
  if (minDiscounted === Number.POSITIVE_INFINITY) minDiscounted = null

  return { minOriginal, minDiscounted }
}

function hasEffectiveDiscountInDetails(row) {
  const { minOriginal, minDiscounted } = getMinPricesFromDetails(row)
  return minDiscounted != null && minDiscounted < minOriginal
}


/* ===== Init ===== */
onMounted(() => {
  fetchBrand(); fetchCategories(); fetchMaterial(); fetchSole(); fetchStyle(); fetchProduct()
})
</script>

<style scoped>
/* Layout tổng thể */
.page { padding: 16px; }

/* Toolbar */
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.toolbar__right { display: flex; gap: 8px; }

/* Filter card (sticky để luôn thấy bảng) */
.filter-card { position: sticky; top: 0; z-index: 12; background: #fff; margin-bottom: 10px; }
.filter-card__head { display: flex; align-items: center; justify-content: space-between; }
.filter-title { font-weight: 600; }
.filter-card__body { padding-top: 10px; padding-bottom: 8px; }

/* NEW: filter row container: left (inputs) + right (buttons) */
.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

/* left group: inputs; allow wrap for small screens */
.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1 1 auto;
  min-width: 0; /* important for truncation in flex layouts */
}

/* right group: buttons fixed on the right */
.filter-right {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

/* Inline form items inside left group */
.filter-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-inline :deep(.el-form-item) {
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
  margin-bottom: 6px;
  vertical-align: middle;
}

/* cụm price inputs: buộc kích thước hợp lý cho el-input-number */
.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* đặt width/min-width cho el-input-number để không bị co quá nhỏ */
.price-range :deep(.el-input-number),
.price-range :deep(.el-input-number) :deep(input) {
  min-width: 110px;
  width: 110px;
}

/* thêm padding cho form-item chứa price để tránh chồng lấn */
.price-form-item { padding-right: 4px; }

/* Width helpers */
.w-56 { width: 224px; }
.w-60 { width: 240px; }
.w-64 { width: 256px; }

/* Advanced area */
.advanced { margin-top: 8px; }
.advanced-grid { display: grid; grid-template-columns: repeat(1, minmax(0,1fr)); gap: 10px; }
@media (min-width: 768px) { .advanced-grid { grid-template-columns: repeat(3, minmax(0,1fr)); } }
.col-span-3 { grid-column: span 3 / span 3; }
.date-range { display: grid; grid-template-columns: 1fr; gap: 8px; }
@media (min-width: 768px) { .date-range { grid-template-columns: 1fr 1fr; } }

/* Summary */
.summary { color: #6b7280; margin: 6px 0 10px; }

/* Prices */
.text-muted { color: #9ca3af; }
.line-through { text-decoration: line-through; }
.mr-2 { margin-right: .5rem; }
.price { font-weight: 600; }
.price-sale { color: #d03050; font-weight: 600; }

/* Pager */
.pager { display: flex; justify-content: end; margin-top: 12px; }

/* Responsive: nếu màn hình quá nhỏ, left và right sẽ bẻ hàng */
@media (max-width: 720px) {
  .filter-row { flex-direction: column; align-items: stretch; gap: 8px; }
  .filter-right { justify-content: flex-start; }
}
</style>
