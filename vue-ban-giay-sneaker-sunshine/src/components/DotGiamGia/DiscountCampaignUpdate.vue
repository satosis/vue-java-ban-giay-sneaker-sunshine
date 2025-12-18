<template>
  <el-container class="page-container">
    <el-main>
      <el-card class="wizard-card" shadow="never">
        <!-- HEADER -->
        <template #header>
          <div class="card-header">
            <el-page-header @back="goBack">
              <h1>Cập nhật Đợt Giảm Giá</h1>
            </el-page-header>
          </div>
        </template>

        <!-- STEPS -->
        <el-steps :active="activeStep" finish-status="success" align-center class="wizard-steps">
          <el-step title="Thiết lập thông tin" :icon="Edit" />
          <el-step title="Chọn sản phẩm & SPCT" :icon="Goods" />
          <el-step title="Xem lại & Hoàn tất" :icon="CircleCheck" />
        </el-steps>

        <el-form :model="form" label-position="top" @submit.prevent>
          <!-- STEP 1 -->
          <section v-show="activeStep === 0" class="step-content">
            <el-row :gutter="24" justify="center">
              <el-col :span="24" :md="16">
                <h3 class="step-title">1) Thông tin chung</h3>
                <el-skeleton v-if="loadingCampaign" :rows="6" animated />
                <template v-else>
                  <el-form-item label="Tên đợt giảm giá" required>
                    <el-input v-model="form.name" placeholder="VD: Summer Sale 2025" size="large" />
                  </el-form-item>

                  <el-row :gutter="16">
                    <el-col :span="24" :md="12">
                      <el-form-item label="Giá trị giảm (%)" required>
                        <el-input-number v-model="form.discountPercentage" :min="1" :max="99" :step="1" size="large" style="width:100%" />
                      </el-form-item>
                    </el-col>
                    <!-- <el-col :span="24" :md="12">
                      <el-form-item label="Trạng thái">
                        <el-segmented v-model="form.status" :options="[{label:'Hoạt động',value:1},{label:'Tạm ngưng',value:0}]" size="large" />
                      </el-form-item>
                    </el-col> -->
                  </el-row>

                  <el-form-item label="Thời gian diễn ra" required>
                    <el-date-picker
                      v-model="dateRange"
                      type="datetimerange"
                      range-separator="Đến"
                      start-placeholder="Bắt đầu"
                      end-placeholder="Kết thúc"
                      format="YYYY-MM-DD HH:mm:ss"
                      value-format="YYYY-MM-DDTHH:mm:ss"
                      size="large"
                      style="width:100%"
                    />
                  </el-form-item>

                  <el-form-item label="Mô tả / Ghi chú">
                    <el-input v-model="form.description" type="textarea" :rows="4" placeholder="Ghi chú quản trị..." />
                  </el-form-item>
                </template>
              </el-col>
            </el-row>
          </section>

          <!-- STEP 2 -->
          <section v-show="activeStep === 1" class="step-content">
            <h3 class="step-title">2) Chọn sản phẩm & SPCT</h3>
            <el-row :gutter="20">
              <!-- LEFT: PRODUCTS -->
              <el-col :span="10">
                <el-card class="table-card" shadow="none">
                  <template #header>
                    <div class="table-card-header">
                      <div class="title">Danh sách sản phẩm</div>
                      <el-input
                        v-model="productSearch"
                        placeholder="Tìm tên hoặc mã SP..."
                        :prefix-icon="Search"
                        clearable
                        @input="debouncedFetchProducts"
                        style="max-width:260px"
                      />
                    </div>
                  </template>

                  <el-table
                    ref="productTableRef"
                    v-loading="loadingProducts"
                    :data="products"
                    border
                    stripe
                    height="500"
                    highlight-current-row
                    :row-key="row => String(row.id)"
                    :reserve-selection="true"
                    @row-click="onProductRowClick"
                    @selection-change="onProductSelectionChange"
                  >
                    <el-table-column type="selection" width="50" align="center" />
                    <el-table-column label="Sản phẩm" min-width="250">
                      <template #default="{ row }">
                        <div class="product-cell">
                          <div class="product-name">{{ row.productText }}</div>
                          <div class="product-code">Mã: {{ row.productCode || '-' }}</div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="quantity" label="Tổng tồn" width="90" align="center" />
                  </el-table>

                  <el-pagination
                    v-if="totalItems > 0"
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :total="totalItems"
                    :page-sizes="[10,20,50,100]"
                    layout="total, sizes, prev, pager, next"
                    @current-change="fetchProducts"
                    @size-change="onSizeChangeProducts"
                    class="table-pagination"
                  />
                </el-card>
              </el-col>

              <!-- RIGHT: PRODUCT DETAILS -->
              <el-col :span="14">
                <el-card class="table-card" shadow="none">
                  <template #header>
                    <div class="table-card-header">
                      <div class="title">
                        Sản phẩm chi tiết
                        <el-tag v-if="activeProductId && spctScope === 'single'" size="small" type="info" class="ml-2">
                          Đang xem: {{ activeProductName || '#' + activeProductId }}
                        </el-tag>
                      </div>
                      <div class="scope-switch">
                        <el-segmented v-model="spctScope" :options="scopeOptions" size="small" />
                      </div>
                    </div>
                  </template>

                  <div v-if="currentProductIds.length === 0" class="empty-state">
                    <el-empty description="Chọn sản phẩm ở bảng bên trái để xem SPCT" />
                  </div>

                  <div v-else>
                    <div class="spct-filters">
                      <el-select v-model="selectedColorId" clearable filterable placeholder="Lọc theo màu sắc" @change="onSpctFilterChanged" style="min-width:180px">
                        <el-option v-for="c in allColors" :key="c.id" :label="c.name" :value="c.id" />
                      </el-select>

                      <el-select v-model="selectedBrandId" clearable filterable placeholder="Lọc theo thương hiệu" @change="onSpctFilterChanged" style="min-width:200px">
                        <el-option v-for="b in allBrands" :key="b.id" :label="b.name" :value="b.id" />
                      </el-select>

                      <el-button text @click="clearSpctFilters" :icon="Delete">Xóa lọc</el-button>
                    </div>

                    <el-table
                      ref="spctTableRef"
                      v-loading="loadingDetails"
                      :data="details"
                      border
                      stripe
                      height="420"
                      :row-key="r => String(r.id)"
                      :reserve-selection="true"
                      @selection-change="onDetailSelectionChange"
                      class="mt-4"
                    >
                      <el-table-column
                        type="selection"
                        width="50"
                        align="center"
                        :selectable="r => Number(r.quantity) > 0"
                      />
                      <el-table-column prop="productName" label="Tên sản phẩm" width="180" show-overflow-tooltip />
                      <el-table-column label="Thuộc tính" width="170">
                        <template #default="{ row }">
                          <el-tag size="small" effect="plain" class="attribute-tag">Màu: {{ row.colorText }}</el-tag>
                          <el-tag size="small" effect="plain" class="attribute-tag">Size: {{ row.sizeText }}</el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="Giá bán" width="100" align="right">
                        <template #default="{ row }">{{ formatCurrency(row.sellPrice) }}</template>
                      </el-table-column>
                      <el-table-column prop="quantity" label="Tồn" width="80" align="center" />
                      <el-table-column label="% riêng" width="140" header-align="center">
                        <template #default="{ row }">
                          <el-input-number
                            v-model="spctExtraPercent[row.id]"
                            :min="1" :max="99" :step="1"
                            placeholder="Mặc định"
                            controls-position="right"
                            style="width:100%"
                          />
                        </template>
                      </el-table-column>
                    </el-table>

                    <el-pagination
                      v-if="detailsTotal > 0"
                      v-model:current-page="detailsPage"
                      v-model:page-size="detailsSize"
                      :total="detailsTotal"
                      :page-sizes="[10,20,50,100]"
                      layout="total, sizes, prev, pager, next"
                      @current-change="fetchDetails"
                      @size-change="onSizeChangeDetails"
                      class="table-pagination"
                    />
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </section>

          <!-- STEP 3 -->
          <section v-show="activeStep === 2" class="step-content">
            <h3 class="step-title">3) Xem lại & Hoàn tất</h3>

            <el-card class="table-card" shadow="none" style="margin-bottom:16px">
              <template #header>
                <div class="table-card-header"><div class="title">Chỉnh sửa nhanh thời gian diễn ra</div></div>
              </template>

              <el-row :gutter="16">
                <el-col :span="24" :md="16">
                  <el-form-item label="Thời gian diễn ra" required>
                    <el-date-picker
                      v-model="dateRangeStep3"
                      type="datetimerange"
                      range-separator="Đến"
                      start-placeholder="Bắt đầu"
                      end-placeholder="Kết thúc"
                      format="YYYY-MM-DD HH:mm:ss"
                      value-format="YYYY-MM-DDTHH:mm:ss"
                      size="large"
                      style="width:100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-alert v-if="dateError" :title="dateError" type="error" show-icon :closable="false" />
            </el-card>

            <el-descriptions :column="2" border class="review-descriptions">
              <el-descriptions-item label="Tên chiến dịch">{{ form.name }}</el-descriptions-item>
              <el-descriptions-item label="Trạng thái">
                <el-tag :type="form.status === 1 ? 'success' : 'warning'">{{ form.status === 1 ? 'Đang hoạt động' : 'Tạm ngưng' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Giảm mặc định"><el-tag type="danger">{{ form.discountPercentage }}%</el-tag></el-descriptions-item>
              <el-descriptions-item label="Mô tả">{{ form.description || 'Không có' }}</el-descriptions-item>
              <el-descriptions-item label="Bắt đầu">{{ form.startDate }}</el-descriptions-item>
              <el-descriptions-item label="Kết thúc">{{ form.endDate }}</el-descriptions-item>
            </el-descriptions>

            <el-divider />

            <div class="selection-summary">
              <h4>Đối tượng áp dụng</h4>
              <div class="summary-items">
                <div class="summary-item"><div class="count">{{ totalSelectedProducts }}</div><div class="label">Sản phẩm</div></div>
                <div class="summary-item"><div class="count">{{ totalSelectedDetails }}</div><div class="label">SPCT</div></div>
              </div>
              <el-alert :title="`SPCT không đặt % riêng sẽ tự áp dụng ${form.discountPercentage}%`" type="info" :closable="false" show-icon class="mt-4" />
            </div>
          </section>
        </el-form>

        <!-- FOOTER -->
        <div class="footer-actions">
          <div><el-button v-if="activeStep > 0" @click="prevStep" size="large">Quay lại</el-button></div>
          <div>
            <el-button v-if="activeStep < 2" type="primary" @click="nextStep" size="large">Tiếp theo</el-button>
            <el-button v-if="activeStep === 2" type="success" :icon="CircleCheck" @click="updateCampaign" size="large">Lưu cập nhật</el-button>
          </div>
        </div>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { reactive, ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Goods, CircleCheck, Search, Delete } from '@element-plus/icons-vue'
import apiClient from '@/utils/axiosInstance'
import { debounce } from 'lodash-es'

/* Router */
const router = useRouter()
const route = useRoute()
const campaignId = route.params.id

/* Steps */
const activeStep = ref(0)
const nextStep = () => {
  if (activeStep.value === 0) {
    if (!form.name?.trim() || form.discountPercentage === '' || form.discountPercentage === null || !form.startDate || !form.endDate) {
      ElMessage.warning('Vui lòng điền đủ thông tin ở bước 1.')
      return
    }
  }
  if (activeStep.value === 1 && selectedProductIds.value.length === 0) {
    ElMessage.warning('Bạn phải chọn ít nhất 1 sản phẩm.')
    return
  }
  activeStep.value++
}
const prevStep = () => { if (activeStep.value > 0) activeStep.value-- }

/* Form */
const form = reactive({ name:'', discountPercentage:'', description:'', status:1, startDate:'', endDate:'' })

/* Date binding */
const dateRange = computed({
  get(){ return form.startDate && form.endDate ? [form.startDate, form.endDate] : [] },
  set(v){ if(Array.isArray(v) && v.length===2){ ;[form.startDate,form.endDate]=v } else { form.startDate=''; form.endDate='' } },
})
const dateRangeStep3 = computed({ get:()=>dateRange.value, set:v => (dateRange.value = v) })
const dateError = ref('')

/* Helpers */
const goBack = () => router.back()
const textOf = (obj, keys)=>{ for(const k of keys){ const v = k.split('.').reduce((a,kk)=>a && a[kk]!==undefined ? a[kk] : undefined, obj); if(v!==undefined && v!==null && String(v).trim()!=='') return String(v)} return '' }
const fallbackId = id => (id!==undefined && id!==null ? `#${id}` : '')
const normalizeProduct = r => ({ ...r,
  productText: textOf(r,['productText']) || textOf(r,['productName','name']) || textOf(r,['product.productName','product.name']) || fallbackId(r.id),
  brandText:   textOf(r,['brandText'])   || textOf(r,['brandName'])         || textOf(r,['brand.brandName','brand.name']) || textOf(r,['product.brand.brandName','product.brand.name']) || fallbackId(r.brandId),
})
const normalizeDetail = r => ({ ...r,
  productText: textOf(r,['productText']) || textOf(r,['productName','product.productName','product.name']) || fallbackId(r.productId),
  brandText:   textOf(r,['brandText'])   || textOf(r,['brandName','brand.brandName','brand.name']) || textOf(r,['product.brand.brandName','product.brand.name']) || fallbackId(r.brandId),
  colorText:   textOf(r,['colorText'])   || textOf(r,['colorName','color.colorName','color.name']) || fallbackId(r.colorId),
  sizeText:    textOf(r,['sizeText'])    || textOf(r,['sizeName','size.sizeName','size.name','sizeValue']) || fallbackId(r.sizeId),
})
const totalFrom = (data, contentLen) => data?.page?.totalElements ?? data?.totalElements ?? contentLen ?? 0
const formatCurrency = v => (v===null || v===undefined ? '' : Number(v).toLocaleString('vi-VN'))
const toKey = x => String(x)

/* PRODUCTS (left) */
const productTableRef = ref()
const products = ref([])
const loadingProducts = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const selectedProductIds = ref([])         // chỉ chứa SP **có SPCT**
const productSearch = ref('')

/* Cache SPCT theo SP để khỏi gọi lặp lại */
const productDetailCache = reactive(new Map())   // pid -> { ids: number[], has: boolean }

const fetchProducts = async ()=>{
  loadingProducts.value = true
  try{
    const params = { page: currentPage.value - 1, size: pageSize.value, keyword: productSearch.value?.trim() || undefined }
    const { data } = await apiClient.get('/admin/products/search/tanh', { params })
    const content = data?.content || []
    products.value = content.map(normalizeProduct)
    totalItems.value = totalFrom(data, content.length)

    await nextTick()
    productTableRef.value?.clearSelection?.()
    // chỉ toggle những SP đã thực sự nằm trong selectedProductIds (tức là có SPCT)
    products.value.forEach(row => {
      if (selectedProductIds.value.includes(row.id)) productTableRef.value?.toggleRowSelection?.(row, true)
    })

    if (spctScope.value === 'single') {
      const current = products.value.find(p => selectedProductIds.value.includes(p.id))
      if (current) setActiveProduct(current)
    }
  }catch(e){
    console.error(e); ElMessage.error('Không thể tải danh sách sản phẩm.')
    products.value=[]; totalItems.value=0
  }finally{ loadingProducts.value=false }
}
const debouncedFetchProducts = debounce(()=>{ currentPage.value=1; fetchProducts() }, 400)
const onSizeChangeProducts = ()=>{ currentPage.value=1; fetchProducts() }

/* Lấy toàn bộ SPCT của 1 SP (không lọc) & cache */
const fetchAllDetailIdsByProduct = async (productId) => {
  if (productDetailCache.has(productId)) return productDetailCache.get(productId)
  const { data } = await apiClient.get(`/admin/products/${productId}/details`, { params: { page:0, size:1000 } })
  const ids = (data?.content || []).map(d => d.id)
  const has = ids.length > 0
  const entry = { ids, has }
  productDetailCache.set(productId, entry)
  return entry
}

/* Khi click 1 hàng SP: chỉ cho toggle nếu có SPCT */
const activeProductId = ref(null)
const activeProductName = ref('')
const setActiveProduct = row => { activeProductId.value = row?.id || null; activeProductName.value = row?.productText || ''; detailsPage.value = 1; fetchDetails() }
const onProductRowClick = async (row) => {
  try{
    const { has } = await fetchAllDetailIdsByProduct(row.id)
    if (!has) {
      ElMessage.info('Sản phẩm này chưa có SPCT, không thể chọn.')
      return
    }
    productTableRef.value?.toggleRowSelection?.(row)
    setActiveProduct(row)
  }catch{
    ElMessage.error('Không kiểm tra được SPCT của sản phẩm.')
  }
}

/* PRODUCT DETAILS (right) */
const spctTableRef = ref()
const details = ref([])
const loadingDetails = ref(false)
const detailsPage = ref(1)
const detailsSize = ref(10)
const detailsTotal = ref(0)
const selectedDetailIds = ref([])
const spctExtraPercent = reactive({})

const allColors = ref([]), allBrands = ref([])
const selectedColorId = ref(null), selectedBrandId = ref(null)

const fetchAllColorsBrands = async ()=>{
  try{
    const [cRes,bRes] = await Promise.all([apiClient.get('/admin/color/hien-thi'), apiClient.get('/admin/brand/hien-thi')])
    allColors.value = (cRes.data || []).map(x=>({id:x.id, name:x.name || x.colorName || x.code || `Màu #${x.id}`}))
    allBrands.value = (bRes.data || []).map(x=>({id:x.id, name:x.name || x.brandName || x.code || `Brand #${x.id}`}))
  }catch(e){
    console.error(e); ElMessage.error('Không thể tải danh mục màu sắc/brand.')
    allColors.value = []; allBrands.value = []
  }
}

/* Scope */
const spctScope = ref('single')
const currentProductIds = computed(()=> spctScope.value==='all' ? selectedProductIds.value.slice() : activeProductId.value ? [activeProductId.value] : [])
const clearSpctFilters = ()=>{ selectedColorId.value=null; selectedBrandId.value=null; detailsPage.value=1; fetchDetails() }
const onSpctFilterChanged = ()=>{ detailsPage.value=1; fetchDetails() }

/* Fetch details (single/all) */
const fetchDetailsSingle = async (productId) => {
  const params = { page: detailsPage.value - 1, size: detailsSize.value, colorId: selectedColorId.value || undefined, brandId: selectedBrandId.value || undefined }
  const { data } = await apiClient.get(`/admin/products/${productId}/details`, { params })
  const content = data?.content || []
  details.value = content.map(normalizeDetail)
  detailsTotal.value = totalFrom(data, content.length)
}
const fetchDetailsAll = async (productIds) => {
  const params = { page:0, size:1000, colorId: selectedColorId.value || undefined, brandId: selectedBrandId.value || undefined }
  const results = await Promise.all(productIds.map(pid => apiClient.get(`/admin/products/${pid}/details`, { params }).then(res=>res.data?.content || []).catch(()=>[])))
  const merged = results.flat().map(normalizeDetail)
  detailsTotal.value = merged.length
  const start = (detailsPage.value - 1) * detailsSize.value
  details.value = merged.slice(start, start + detailsSize.value)
}
const fetchDetails = async ()=>{
  const pids = currentProductIds.value
  if (!pids.length){ details.value=[]; detailsTotal.value=0; return }
  loadingDetails.value = true
  try{
    if (spctScope.value==='single') await fetchDetailsSingle(pids[0]); else await fetchDetailsAll(pids)

    await nextTick()
    spctTableRef.value?.clearSelection?.()
    const picked = new Set(selectedDetailIds.value.map(toKey))
    details.value.forEach(row=>{
      if (picked.has(toKey(row.id)) && Number(row.quantity) > 0) spctTableRef.value?.toggleRowSelection?.(row, true)
      if (spctExtraPercent[row.id] !== undefined && spctExtraPercent[row.id] !== null) spctExtraPercent[row.id] = Number(spctExtraPercent[row.id])
    })
  }catch(e){
    console.error(e); ElMessage.error('Không thể tải SPCT.'); details.value=[]; detailsTotal.value=0
  }finally{ loadingDetails.value=false }
}
const onSizeChangeDetails = ()=>{ detailsPage.value=1; fetchDetails() }
const onDetailSelectionChange = rows => { selectedDetailIds.value = rows.map(r=>r.id) }
const totalSelectedProducts = computed(()=> selectedProductIds.value.length )
const totalSelectedDetails = computed(()=> selectedDetailIds.value.length )

/* LOAD CAMPAIGN */
const loadingCampaign = ref(true)
const normalizeIso = s => (!s ? '' : typeof s === 'string' ? s.replace(' ', 'T') : s)

const fetchCampaign = async ()=>{
  loadingCampaign.value = true
  try{
    const { data } = await apiClient.get(`/admin/campaigns/${campaignId}`)
    form.name = data?.name ?? ''
    form.discountPercentage = data?.discountPercentage ?? ''
    form.description = data?.description ?? ''
    form.status = data?.status ?? 1
    form.startDate = normalizeIso(data?.startDate) || ''
    form.endDate = normalizeIso(data?.endDate) || ''

    // 1) Preselect SP: CHỈ chọn SP có SPCT
    selectedProductIds.value = []
    const prodLinks = data?.products || data?.productLinks || []
    for (const link of (prodLinks || [])) {
      const pid = link?.product?.id ?? link?.productId
      if (pid == null) continue
      const { has, ids } = await fetchAllDetailIdsByProduct(pid)
      if (has) {
        selectedProductIds.value.push(pid)
        // gom luôn SPCT để tick đủ
        ids.forEach(id => addDetailId(id))
      }
    }

    // 2) Preselect SPCT riêng + % riêng (nếu backend có link chi tiết)
    const pdLinks = data?.productDetails || data?.productDetailLinks || []
    for (const l of (pdLinks || [])) {
      const pdId = l?.productDetail?.id ?? l?.productDetailId
      if (pdId != null) addDetailId(pdId)
      const pct = l?.discountPercentage
      if (pdId != null && pct !== null && pct !== undefined && pct !== '') spctExtraPercent[pdId] = Number(pct)
    }
  }catch(e){
    console.error(e); ElMessage.error(e?.response?.data?.message || 'Không thể tải thông tin đợt giảm giá.'); router.back()
  }finally{ loadingCampaign.value=false }
}

/* Helpers add/remove detail ids (unique) */
const addDetailId = (id)=>{
  const set = new Set(selectedDetailIds.value.map(toKey))
  set.add(toKey(id))
  selectedDetailIds.value = Array.from(set)
}
const removeDetailIds = (idsToRemove)=>{
  const rm = new Set(idsToRemove.map(toKey))
  selectedDetailIds.value = selectedDetailIds.value.filter(id => !rm.has(toKey(id)))
}

/* SUBMIT */
const validateFinal = ()=>{
  dateError.value = ''
  if (!form.name?.trim() || form.discountPercentage === '' || form.discountPercentage === null) { ElMessage.error('Thiếu tên chiến dịch hoặc % giảm.'); if (activeStep.value===0) return false }
  if (!form.startDate || !form.endDate) { dateError.value='Vui lòng chọn đầy đủ thời gian bắt đầu và kết thúc.'; return false }
  const now=new Date(), start=new Date(form.startDate), end=new Date(form.endDate), today=new Date(now.getFullYear(),now.getMonth(),now.getDate())
  if (start < today) { dateError.value='Ngày bắt đầu không được ở quá khứ.'; return false }
  if (start >= end) { dateError.value='Ngày kết thúc phải sau ngày bắt đầu.'; return false }
  if (selectedProductIds.value.length===0 && selectedDetailIds.value.length===0) { ElMessage.warning('Chọn ít nhất 1 SP hoặc 1 SPCT.'); if (activeStep.value===1) return false }
  return true
}
const buildPayload = ()=>{
  const productDetailsPayload = selectedDetailIds.value.map(id=>{
    const percent = spctExtraPercent[id]
    return { productDetailId: id, ...(percent !== undefined && percent !== null && percent !== '' && { discountPercentage: Number(percent) }) }
  })
  return {
    name: form.name.trim(),
    discountPercentage: Number(form.discountPercentage),
    description: form.description?.trim() || null,
    status: form.status,
    startDate: form.startDate,
    endDate: form.endDate,
    products: selectedProductIds.value.map(id=>({ productId:id })),
    productDetails: productDetailsPayload,
  }
}
const updateCampaign = async ()=>{
  if (!validateFinal()) return
  const payload = buildPayload()
  try{
    await apiClient.put(`/admin/campaigns/${campaignId}`, payload, { headers:{ 'Content-Type':'application/json' } })
    ElMessage.success('Cập nhật đợt giảm giá thành công!')
    router.push('/discount-campaigns')
  }catch(e){
    console.error('Update campaign error:', { status:e?.response?.status, data:e?.response?.data, payload })
    ElMessage.error(e?.response?.data?.message || `Không thể cập nhật đợt giảm giá (HTTP ${e?.response?.status || 'ERR'}).`)
  }
}

/* SYNC: thêm/bỏ SP ⇒ thêm/bỏ SPCT, chỉ cho chọn SP có SPCT */
const prevSelectedProductIds = ref([])
const onProductSelectionChange = async (rows)=>{
  // rows là những SP đang được tick sau thao tác
  const newIds = rows.map(r=>r.id)

  // Tách phần "muốn thêm" và "muốn bỏ"
  const added = newIds.filter(id => !prevSelectedProductIds.value.includes(id))
  const removed = prevSelectedProductIds.value.filter(id => !newIds.includes(id))

  // CHỈ giữ các id added có SPCT
  const validAdded = []
  for (const pid of added) {
    const { has, ids } = await fetchAllDetailIdsByProduct(pid)
    if (has) {
      validAdded.push(pid)
      ids.forEach(id => addDetailId(id))
    } else {
      // rollback tích trên UI (do Element Plus đã tick sẵn)
      const row = products.value.find(p => p.id === pid)
      if (row) productTableRef.value?.toggleRowSelection?.(row, false)
      ElMessage.info(`"${textOf(row||{},['productText','productName']) || 'Sản phẩm'}" chưa có SPCT, không thể chọn.`)
    }
  }

  // BỎ SPCT khi bỏ SP
  if (removed.length) {
    const idsToRemove = []
    for (const pid of removed) {
      const { ids } = await fetchAllDetailIdsByProduct(pid)
      idsToRemove.push(...ids)
    }
    removeDetailIds(idsToRemove)
  }

  // Cập nhật danh sách SP đã chọn (chỉ bao gồm SP có SPCT)
  selectedProductIds.value = [
    ...prevSelectedProductIds.value.filter(id => !removed.includes(id)),
    ...validAdded
  ]

  prevSelectedProductIds.value = [...selectedProductIds.value]

  // Refresh bảng phải
  if (spctScope.value === 'single') {
    if (!activeProductId.value || !selectedProductIds.value.includes(activeProductId.value)) {
      const first = products.value.find(p => selectedProductIds.value.includes(p.id))
      if (first) setActiveProduct(first)
      else { activeProductId.value=null; activeProductName.value=''; details.value=[]; detailsTotal.value=0 }
    } else { detailsPage.value=1; fetchDetails() }
  } else { detailsPage.value=1; fetchDetails() }
}

/* Watch & Init */
watch(spctScope, ()=>{ detailsPage.value=1; fetchDetails() })
watch(selectedProductIds, v => { if (v.length > 1 && spctScope.value === 'single') spctScope.value = 'all' })

onMounted(async ()=>{
  await fetchCampaign()
  await fetchAllColorsBrands()
  await fetchProducts()

  // Set active product nếu đang ở single
  if (selectedProductIds.value.length && spctScope.value === 'single') {
    const first = products.value.find(p => selectedProductIds.value.includes(p.id))
    if (first) setActiveProduct(first)
  }

  prevSelectedProductIds.value = [...selectedProductIds.value]
  await fetchDetails()
})
</script>

<style scoped>
.page-container { background:#f5f7fb; min-height:100vh; padding:20px; }
.wizard-card { width:100%; max-width:1440px; margin:0 auto; border-radius:12px; }
:deep(.el-card__header){ background:#fff; }
.card-header h1{ margin:0; font-size:20px; font-weight:700; }

.wizard-steps{ padding:16px 0; margin:0 16px 12px; border-bottom:1px solid var(--el-border-color-lighter); }
:deep(.el-step__title){ font-size:14px; }

.step-content{ padding:10px 16px; }
.step-title{ font-size:18px; font-weight:600; margin-bottom:6px; color:var(--el-text-color-primary); }

.table-card{ border:1px solid var(--el-border-color-lighter); border-radius:12px; }
:deep(.table-card .el-card__header){ background:#fcfcfd; padding:12px 16px; }
:deep(.table-card .el-card__body){ padding:8px; }
.table-card-header{ display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
.table-card-header .title{ font-weight:600; font-size:16px; }

.product-cell{ display:flex; flex-direction:column; }
.product-name{ font-weight:500; color:var(--el-text-color-primary); }
.product-code{ font-size:12px; color:var(--el-text-color-secondary); }

.attribute-tag{ margin-right:6px; margin-top:4px; }

.table-pagination{ padding:10px 0 0; display:flex; justify-content:center; margin-top:16px; }

.empty-state{ display:flex; justify-content:center; align-items:center; height:500px; }

.spct-filters{ display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.scope-switch{ display:flex; align-items:center; gap:8px; }
.ml-2{ margin-left:8px; }
.mt-4{ margin-top:16px; }

.review-descriptions{ border-radius:8px; }
.selection-summary{ padding:16px; background:#f9fafb; border-radius:12px; }
.selection-summary h4{ margin:0 0 12px; font-size:16px; font-weight:600; }
.summary-items{ display:flex; gap:24px; text-align:center; }
.summary-item .count{ font-size:26px; font-weight:700; color:var(--el-color-primary); }
.summary-item .label{ font-size:13px; color:var(--el-text-color-secondary); }

.footer-actions{ display:flex; justify-content:space-between; align-items:center; padding:14px 16px; border-top:1px solid var(--el-border-color-lighter); background:rgba(255,255,255,.9); position:sticky; bottom:0; z-index:10; }
</style>
