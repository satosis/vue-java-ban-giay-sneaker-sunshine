<template>
  <div class="ssn-wrap">
    <!-- Topbar -->
    <header class="ssn-topbar" role="banner">
      <div class="left">
        <el-button link type="primary" @click="goBack" aria-label="Quay lại trang danh sách sản phẩm">
          <el-icon><ArrowLeft /></el-icon>
          <span class="ml-1">Quay lại</span>
        </el-button>
        <span class="divider" aria-hidden="true"></span>
        <h1 class="title">Cập nhật sản phẩm</h1>
      </div>

      <div class="right">
        <el-button @click="openConfirmDialog" type="success" round size="large" aria-label="Cập nhật sản phẩm">
          Cập nhật
        </el-button>
      </div>
    </header>

    <!-- Form container -->
    <main class="ssn-container" role="main">
      <el-form
        ref="productForm"
        :model="updateProduct"
        :rules="rules"
        label-position="top"
        class="ssn-form"
        autocomplete="off"
      >
        <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
          <!-- LEFT (main) -->
          <section class="xl:col-span-2 space-y-6" aria-label="Thông tin sản phẩm và biến thể">
            <!-- General info card -->
            <el-card shadow="never" class="ssn-card" aria-labelledby="general-info-heading">
              <template #header>
                <div class="card-header">
                  <h2 id="general-info-heading">Thông tin chung</h2>
                </div>
              </template>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <el-form-item label="Tên sản phẩm" prop="productName">
                  <el-input v-model="updateProduct.productName" placeholder="Nhập tên sản phẩm" clearable />
                </el-form-item>

                <el-form-item label="Thương hiệu" prop="brandId">
                  <el-select v-model="updateProduct.brandId" placeholder="Chọn thương hiệu" filterable clearable>
                    <el-option v-for="br in brandList" :key="br.id" :label="br.brandName" :value="br.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Nhà cung cấp" prop="supplierId">
                  <el-select v-model="updateProduct.supplierId" placeholder="Chọn nhà cung cấp" filterable clearable>
                    <el-option v-for="sp in supplierList" :key="sp.id" :label="sp.supplierName" :value="sp.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Chất liệu" prop="materialId">
                  <el-select v-model="updateProduct.materialId" placeholder="Chọn chất liệu" filterable clearable>
                    <el-option v-for="mt in materialList" :key="mt.id" :label="mt.materialName" :value="mt.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Loại đế" prop="soleId">
                  <el-select v-model="updateProduct.soleId" placeholder="Chọn loại đế" filterable clearable>
                    <el-option v-for="s in soleList" :key="s.id" :label="s.soleName" :value="s.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Cổ giày" prop="styleId">
                  <el-select v-model="updateProduct.styleId" placeholder="Chọn cổ giày" filterable clearable>
                    <el-option v-for="st in styleList" :key="st.id" :label="st.styleName" :value="st.id" />
                  </el-select>
                </el-form-item>

                <el-form-item label="Dành cho" prop="genderId">
                  <el-radio-group v-model="updateProduct.genderId" class="radio-inline">
                    <el-radio :label="'1'">Nam</el-radio>
                    <el-radio :label="'2'">Nữ</el-radio>
                    <el-radio :label="'3'">Unisex</el-radio>
                  </el-radio-group>
                </el-form-item>

                <!-- Weight + Price row -->
                <div class="grid grid-cols-2 gap-4">
                  <el-form-item label="Cân nặng (gram)" prop="weight">
                    <el-input
                      type="number"
                      v-model.number="updateProduct.weight"
                      placeholder="0"
                      clearable
                      :min="0"
                      @blur="onWeightBlur"
                      inputmode="numeric"
                      aria-describedby="weight-hint"
                    />
                  </el-form-item>

                  <el-form-item label="Giá bán mặc định" prop="sellPrice">
                    <div class="price-row" role="group" aria-label="Giá bán">
                      <el-input
                        type="number"
                        v-model.number="updateProduct.sellPrice"
                        placeholder="0"
                        clearable
                        :min="0"
                        :max="MAX_SELL_PRICE"
                        style="max-width: 220px;"
                        @blur="onSellPriceBlur"
                        inputmode="numeric"
                        aria-describedby="sellprice-hint"
                      />
                      <div class="formatted-price" v-if="formattedSellPrice" aria-hidden="true">
                        {{ formattedSellPrice }}
                      </div>
                    </div>
                    <p id="sellprice-hint" class="note">Số nguyên, min 0, max 9 chữ số — hiển thị dạng VND.</p>
                  </el-form-item>
                </div>
              </div>

              <el-form-item label="Danh mục" prop="categoryIds" class="mt-2">
                <Multiselect
                  v-model="updateProduct.categoryIds"
                  :options="categoryList"
                  :multiple="true"
                  :close-on-select="false"
                  placeholder="Chọn danh mục"
                  label="categoryName"
                  track-by="id"
                  class="w-full"
                />
                <div class="chips" v-if="updateProduct.categoryIds?.length">
                  <span class="chip" v-for="c in updateProduct.categoryIds" :key="typeof c === 'object' ? c.id : c">
                    {{ typeof c === 'object' ? c.categoryName : getCategoryName(c) }}
                  </span>
                </div>
              </el-form-item>

              <el-form-item label="Mô tả" prop="description">
                <el-input
                  v-model="updateProduct.description"
                  type="textarea"
                  :rows="4"
                  placeholder="Mô tả ngắn về sản phẩm..."
                  maxlength="1000"
                  show-word-limit
                />
              </el-form-item>
            </el-card>

            <!-- Variants card -->
            <el-card shadow="never" class="ssn-card" aria-labelledby="variants-heading">
              <template #header>
                <div class="card-header">
                  <h2 id="variants-heading">Biến thể (Size & Màu)</h2>
                </div>
              </template>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <el-form-item label="Kích thước" prop="selectedSizes">
                  <el-checkbox-group v-model="updateProduct.selectedSizes" @change="generateProductDetails">
                    <div class="check-grid" role="list" aria-label="Danh sách kích thước">
                      <div role="listitem" v-for="size in sizeList" :key="size.id" class="check-item">
                        <el-checkbox :label="size.id">{{ size.sizeName }}</el-checkbox>
                      </div>
                    </div>
                  </el-checkbox-group>
                </el-form-item>

                <el-form-item label="Màu sắc" prop="selectedColors">
                  <el-checkbox-group v-model="updateProduct.selectedColors" @change="generateProductDetails">
                    <div class="check-grid" role="list" aria-label="Danh sách màu sắc">
                      <div role="listitem" v-for="color in colorList" :key="color.id" class="check-item">
                        <el-checkbox :label="color.id">{{ color.colorName }}</el-checkbox>
                      </div>
                    </div>
                  </el-checkbox-group>
                </el-form-item>
              </div>

              <el-divider />

              <div class="variants">
                <el-empty v-if="!updateProduct.productDetails.length" description="Chọn Size và Màu để tạo biến thể" />
                <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <article
                    v-for="(detail, index) in updateProduct.productDetails"
                    :key="detail.sizeId + '-' + detail.colorId"
                    class="variant-card"
                    :aria-labelledby="`variant-${index}-label`"
                  >
                    <header class="variant-head">
                      <div :id="`variant-${index}-label`" class="variant-meta">
                        <span class="badge">{{ detail.sizeName }}</span>
                        <span class="sep">×</span>
                        <span class="badge badge-blue">{{ detail.colorName }}</span>
                      </div>
                      <div class="variant-price">
                        <span class="price-badge">{{ formatCurrency(updateProduct.sellPrice) }}</span>
                      </div>
                    </header>

                    <div class="variant-body">
                      <el-form-item
                        :prop="'productDetails.' + index + '.quantity'"
                        :rules="[rules.productDetailQuantity[0]]"
                      >
                        <label class="form-label">Số lượng</label>
                        <el-input
                          type="number"
                          v-model.number="detail.quantity"
                          :min="0"
                          clearable
                          @blur="onDetailQuantityBlur(detail, index)"
                          inputmode="numeric"
                          aria-label="Số lượng biến thể"
                        />
                        <p class="note">Số nguyên, tối đa 6 chữ số.</p>
                      </el-form-item>
                    </div>
                  </article>
                </div>
              </div>
            </el-card>

            <!-- Confirmation summary -->
            <el-card shadow="never" class="ssn-card">
              <template #header>
                <div class="card-header">
                  <h2>Xác nhận cập nhật</h2>
                </div>
              </template>

              <div class="confirm-row flex items-center justify-between">
                <div class="meta">
                  <div class="meta-line">Tổng biến thể: <b>{{ updateProduct.productDetails.length || 0 }}</b></div>
                  <div class="meta-line">Tổng số lượng: <b>{{ totalQuantity }}</b></div>
                </div>

                <div class="actions">
                  <el-button type="success" size="large" @click="openConfirmDialog">Cập nhật sản phẩm</el-button>
                </div>
              </div>
            </el-card>
          </section>

          <!-- RIGHT (images) -->
          <aside class="xl:col-span-1 space-y-6" aria-label="Hình ảnh theo màu">
            <el-card shadow="never" class="ssn-card" aria-labelledby="images-heading">
              <template #header>
                <div class="card-header">
                  <h2 id="images-heading">Hình ảnh theo màu</h2>
                </div>
              </template>

              <el-alert
                v-if="!updateProduct.selectedColors?.length"
                type="info"
                :closable="false"
                show-icon
                class="mb-3"
                description="Chọn màu để tải ảnh tương ứng."
              />

              <section v-for="colorId in updateProduct.selectedColors" :key="colorId" class="color-block" :aria-label="`Ảnh màu ${getColorName(colorId)}`">
                <div class="color-heading">
                  <div class="dot" />
                  <div class="name">{{ getColorName(colorId) }}</div>
                  <div class="count">({{ (colorImages[colorId] || []).length }}/10)</div>
                </div>

                <el-upload
                  action="#"
                  list-type="picture-card"
                  :limit="10"
                  multiple
                  accept="image/*"
                  :file-list="colorImages[colorId] || []"
                  :auto-upload="false"
                  :on-preview="handlePreview"
                  :on-change="(file, fileList) => handleFileChange(file, fileList, colorId)"
                  :on-remove="(file, fileList) => handleFileRemove(file, fileList, colorId)"
                  aria-label="Tải ảnh màu"
                >
                  <template #default>
                    <div class="upload-slot">
                      <el-icon><Plus /></el-icon>
                      <span>Thêm ảnh</span>
                    </div>
                  </template>
                </el-upload>
              </section>

              <!-- Hidden form item used to run images validator -->
              <el-form-item v-if="updateProduct.selectedColors.length > 0" prop="images" style="display:none;">
                <div />
              </el-form-item>
            </el-card>
          </aside>
        </div>
      </el-form>
    </main>

    <!-- Confirm dialog -->
    <el-dialog
      title="Xác nhận"
      v-model="isModalVisible"
      width="420px"
      center
      :before-close="closeModal"
      class="ssn-dialog"
    >
      <p>Bạn có chắc chắn muốn cập nhật sản phẩm này không?</p>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeModal">Hủy</el-button>
          <el-button type="success" @click="saveProduct">Xác nhận</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/*
  Toàn bộ file được viết cho Vue 3 + <script setup>, Element Plus, vue-multiselect.
  Những điểm chính:
  - productDetails nằm trong updateProduct để el-form validate đúng.
  - Toàn bộ quantity được convert về Number khi fetch (preserve 0).
  - Validator productDetailQuantity nhận string hoặc number, convert, và chấp nhận >=0 (tổng vẫn phải >0).
  - Sau fetch gọi nextTick + clearValidate để tránh validate cũ.
*/

import { ref, reactive, onMounted, watch, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Multiselect from 'vue-multiselect'
import 'vue-multiselect/dist/vue-multiselect.css'
import { ElNotification } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import apiClient from '@/utils/axiosInstance'

/* Router */
const router = useRouter()
const route = useRoute()

/* Limits */
const MAX_SELL_PRICE = 999_999_999
const MAX_QTY = 999_999

/* Refs & state */
const productForm = ref(null)
const brandList = ref([])
const materialList = ref([])
const categoryList = ref([])
const sizeList = ref([])
const colorList = ref([])
const soleList = ref([])
const supplierList = ref([])
const styleList = ref([])

const isModalVisible = ref(false)
const colorImages = ref({}) // { colorId: [{ name, url, file, isOld, id }] }
const deletedImageIds = ref([])
const oldColorIds = ref([])

/* Reactive product model (productDetails inside model) */
const updateProduct = reactive({
  categoryIds: [],
  productName: '',
  materialId: null,
  styleId: null,
  supplierId: null,
  genderId: null,
  soleId: null,
  brandId: null,
  originPrice: null,
  weight: null,
  sellPrice: null,
  description: '',
  selectedSizes: [],
  selectedColors: [],
  productDetails: [] // important: each item { id, sizeId, colorId, sizeName, colorName, quantity }
})

/* Computed total quantity */
const totalQuantity = computed(() =>
  (updateProduct.productDetails || []).reduce((s, d) => s + Number(d.quantity || 0), 0)
)

/* Notifications */
const showSuccess = (message) => ElNotification({ title: 'Thành công', message, type: 'success', duration: 3000, position: 'top-right' })
const showError = (message) => ElNotification({ title: 'Lỗi', message, type: 'error', duration: 3000, position: 'top-right' })

/* Currency formatting */
const formattedSellPrice = computed(() => {
  if (updateProduct.sellPrice === null || updateProduct.sellPrice === undefined || updateProduct.sellPrice === '') return ''
  const n = Number(updateProduct.sellPrice) || 0
  return n.toLocaleString('vi-VN') + ' VND'
})
const formatCurrency = (v) => {
  if (v == null) return '0 VND'
  const n = Number(v) || 0
  return n.toLocaleString('vi-VN') + ' VND'
}

/* Helpers */
const getColorName = (colorId) => colorList.value.find(c => c.id === colorId)?.colorName || 'Không xác định'
const getCategoryName = (id) => categoryList.value.find(c => c.id === id)?.categoryName || ''

/* Validation rules (productDetailQuantity uses Promise-style validator) */
const rules = {
  categoryIds: [
    {
      required: true,
      message: 'Vui lòng chọn ít nhất một danh mục',
      trigger: 'change'
    }
  ],
  productName: [{ required: true, message: 'Vui lòng nhập tên sản phẩm', trigger: 'blur' }],
  materialId: [{ required: true, message: 'Vui lòng chọn chất liệu', trigger: 'change' }],
  supplierId: [{ required: true, message: 'Vui lòng chọn nhà cung cấp', trigger: 'change' }],
  soleId: [{ required: true, message: 'Vui lòng chọn loại đế', trigger: 'change' }],
  styleId: [{ required: true, message: 'Vui lòng chọn cổ giày', trigger: 'change' }],
  brandId: [{ required: true, message: 'Vui lòng chọn thương hiệu', trigger: 'change' }],
  genderId: [{ required: true, message: 'Vui lòng chọn đối tượng', trigger: 'change' }],
  weight: [
    { required: true, message: 'Vui lòng nhập cân nặng', trigger: 'blur' },
    { type: 'number', min: 0, message: 'Cân nặng phải là số không âm', trigger: 'blur' }
  ],
  sellPrice: [
    {
      validator: (rule, value) => {
        return new Promise((resolve, reject) => {
          if (value === null || value === undefined || value === '') return reject(new Error('Vui lòng nhập giá bán'))
          const n = Number(value)
          if (!Number.isFinite(n) || !Number.isInteger(n)) return reject(new Error('Giá bán phải là số nguyên và không chứa chữ'))
          if (n < 0) return reject(new Error('Giá bán phải lớn hơn hoặc bằng 0'))
          if (n > MAX_SELL_PRICE) return reject(new Error(`Giá bán không được lớn hơn ${MAX_SELL_PRICE.toLocaleString('vi-VN')}`))
          resolve()
        })
      },
      trigger: 'blur'
    }
  ],
  description: [{ required: true, message: 'Vui lòng nhập mô tả sản phẩm', trigger: 'blur' }],
  selectedSizes: [
    { required: true, message: 'Vui lòng chọn ít nhất một kích thước', trigger: 'change' }
  ],
  selectedColors: [
    { required: true, message: 'Vui lòng chọn ít nhất một màu sắc', trigger: 'change' }
  ],
  images: [
    {
      validator: () => {
        return new Promise((resolve, reject) => {
          for (const colorId of updateProduct.selectedColors) {
            const files = colorImages.value[colorId]
            if (!files || files.length === 0) return reject(new Error(`Vui lòng tải lên ít nhất một ảnh cho màu ${getColorName(colorId)}`))
          }
          resolve()
        })
      },
      trigger: 'change'
    }
  ],
  // per-variant quantity validator: accepts >= 0 (0 allowed), integer, <= MAX_QTY
  productDetailQuantity: [{
    validator: (rule, value) => {
      return new Promise((resolve, reject) => {
        if (value === null || value === undefined || value === '') {
          // force user to explicitly set value (can be 0)
          return reject(new Error('Vui lòng nhập số lượng (>= 0)'))
        }
        const n = Number(value)
        if (!Number.isFinite(n) || !Number.isInteger(n)) return reject(new Error('Số lượng phải là số nguyên'))
        if (n < 0) return reject(new Error('Số lượng không được âm'))
        if (n > MAX_QTY) return reject(new Error(`Số lượng không được vượt quá ${MAX_QTY.toLocaleString('vi-VN')}`))
        resolve()
      })
    },
    trigger: 'blur'
  }]
}

/* Fetchers */
const fetchCategories = async () => { try { categoryList.value = (await apiClient.get('/admin/categories/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy danh mục sản phẩm!') } }
const fetchMaterial = async () => { try { materialList.value = (await apiClient.get('/admin/material/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy chất liệu sản phẩm!') } }
const fetchSupplier = async () => { try { supplierList.value = (await apiClient.get('/admin/supplier/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy nhà cung cấp!') } }
const fetchBrand = async () => { try { brandList.value = (await apiClient.get('/admin/brand/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy thương hiệu sản phẩm!') } }
const fetchSole = async () => { try { soleList.value = (await apiClient.get('/admin/sole/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy đế giày sản phẩm!') } }
const fetchStyle = async () => { try { styleList.value = (await apiClient.get('/admin/style/hien-thi')).data } catch (e) { console.error(e); showError('Lỗi lấy phong cách sản phẩm!') } }
const fetchSizesAndColors = async () => {
  try {
    const [sizesRes, colorsRes] = await Promise.all([apiClient.get('/admin/size/hien-thi'), apiClient.get('/admin/color/hien-thi')])
    sizeList.value = sizesRes.data
    colorList.value = colorsRes.data
  } catch (e) {
    console.error(e); showError('Lỗi lấy kích thước hoặc màu sắc!')
  }
}

/* Load product detail
   Important: convert quantity to Number (preserve 0), then clear validation state.
*/
const fetchProduct = async () => {
  const id = route.params.id
  try {
    const product = (await apiClient.get(`/admin/products/${id}`)).data

    updateProduct.categoryIds = product.categories?.map(c => ({ id: c.id, categoryName: c.categoryName })) || []
    updateProduct.productName = product.productName || ''
    updateProduct.materialId = product.materialId || null
    updateProduct.styleId = product.styleId || null
    updateProduct.supplierId = product.supplierId || null
    updateProduct.genderId = product.genderId ? String(product.genderId) : null
    updateProduct.soleId = product.soleId || null
    updateProduct.brandId = product.brandId || null
    updateProduct.originPrice = product.originPrice ?? null
    updateProduct.weight = product.weight ?? null
    updateProduct.sellPrice = product.sellPrice ?? null
    updateProduct.description = product.description || ''
    updateProduct.selectedSizes = [...new Set(product.productDetails?.map(d => d.sizeId) || [])]
    updateProduct.selectedColors = [...new Set(product.productDetails?.map(d => d.colorId) || [])]

    // Convert quantities to Number (preserve 0). If server sends "10" -> becomes 10.
    updateProduct.productDetails = (product.productDetails || []).map(d => ({
      id: d.id ?? null,
      sizeId: d.sizeId,
      colorId: d.colorId,
      sizeName: d.sizeName ?? '',
      colorName: d.colorName ?? '',
      quantity: (d.quantity === null || d.quantity === undefined) ? null : Number(d.quantity)
    }))

    // images
    colorImages.value = {}
    ;(product.productImages || []).forEach(img => {
      if (img.status === '1') {
        const colorId = img.colorId
        if (!colorImages.value[colorId]) colorImages.value[colorId] = []
        colorImages.value[colorId].push({
          name: img.imageName,
          url: img.image?.startsWith('data:image') ? img.image : `data:image/png;base64,${img.image}`,
          isOld: true,
          id: img.id,
          file: null
        })
      }
    })
    colorImages.value = { ...colorImages.value }

    // ensure element-plus form clears old errors (important)
    await nextTick()
    productForm.value?.clearValidate()
  } catch (e) {
    console.error(e); showError('Lỗi lấy dữ liệu sản phẩm!')
  }
}

/* Generate productDetails from selectedSizes x selectedColors while preserving existing entries (including 0) */
const generateProductDetails = () => {
  const existed = [...(updateProduct.productDetails || [])]
  const out = []

  for (const sizeId of updateProduct.selectedSizes) {
    for (const colorId of updateProduct.selectedColors) {
      const size = sizeList.value.find(s => s.id === sizeId)
      const color = colorList.value.find(c => c.id === colorId)
      const old = existed.find(d => d.sizeId === sizeId && d.colorId === colorId)
      const baseQty = (old && old.quantity !== null && old.quantity !== undefined) ? Number(old.quantity) : null
      out.push({
        id: old?.id ?? null,
        sizeId,
        colorId,
        sizeName: size?.sizeName ?? '',
        colorName: color?.colorName ?? '',
        quantity: baseQty
      })
    }
  }

  updateProduct.productDetails = out
  // clear validation for new set
  nextTick(() => productForm.value?.clearValidate())
}

/* Input sanitizers / blur handlers */
const onSellPriceBlur = () => {
  const v = updateProduct.sellPrice
  if (v === null || v === undefined || v === '') {
    updateProduct.sellPrice = null
    productForm.value?.validateField('sellPrice')
    return
  }
  let n = Number(v)
  if (!Number.isFinite(n)) {
    updateProduct.sellPrice = null
    productForm.value?.validateField('sellPrice')
    return
  }
  n = Math.trunc(n)
  if (n < 0) n = 0
  if (n > MAX_SELL_PRICE) n = MAX_SELL_PRICE
  updateProduct.sellPrice = n
  productForm.value?.validateField('sellPrice')
}

const onDetailQuantityBlur = (detail, index) => {
  const v = detail.quantity
  if (v === null || v === undefined || v === '') {
    detail.quantity = null
    productForm.value?.validateField(`productDetails.${index}.quantity`)
    return
  }
  let n = Number(v)
  if (!Number.isFinite(n)) {
    detail.quantity = null
    productForm.value?.validateField(`productDetails.${index}.quantity`)
    return
  }
  n = Math.trunc(n)
  if (n > MAX_QTY) n = MAX_QTY
  detail.quantity = n
  productForm.value?.validateField(`productDetails.${index}.quantity`)
}

const onWeightBlur = () => {
  const v = updateProduct.weight
  if (v === null || v === undefined || v === '') {
    updateProduct.weight = null
    productForm.value?.validateField('weight')
    return
  }
  let n = Number(v)
  if (!Number.isFinite(n)) {
    updateProduct.weight = null
    productForm.value?.validateField('weight')
    return
  }
  n = Math.trunc(n)
  if (n < 0) n = 0
  updateProduct.weight = n
  productForm.value?.validateField('weight')
}

/* Watchers */

// sanitize sellPrice live
watch(() => updateProduct.sellPrice, (val) => {
  if (val === null || val === undefined || val === '') return
  if (typeof val === 'string') {
    const digits = val.replace(/[^\d]/g, '')
    const n = digits === '' ? null : Number(digits)
    updateProduct.sellPrice = n
  } else {
    let n = Number(val)
    if (!Number.isFinite(n)) { updateProduct.sellPrice = null; return }
    n = Math.trunc(n)
    if (n < 0) n = 0
    if (n > MAX_SELL_PRICE) n = MAX_SELL_PRICE
    if (n !== val) updateProduct.sellPrice = n
  }
})

// enforce integer truncation for any typed decimals in productDetails
watch(() => updateProduct.productDetails, (n) => {
  (n || []).forEach(d => {
    if (d && d.quantity !== null && d.quantity !== undefined) {
      const num = Number(d.quantity)
      if (Number.isFinite(num)) {
        const t = Math.trunc(num)
        if (t !== d.quantity) d.quantity = t
      }
    }
  })
}, { deep: true })

// when removing a color, mark its images for deletion and remember old color
watch(() => updateProduct.selectedColors, (newColors, oldColors) => {
  const removed = oldColors?.filter(cid => !newColors.includes(cid)) || []
  removed.forEach(cid => {
    if (!oldColorIds.value.includes(cid)) oldColorIds.value.push(cid)
    if (colorImages.value[cid]) {
      colorImages.value[cid].forEach(img => {
        if (img.isOld && img.id && !deletedImageIds.value.includes(img.id)) deletedImageIds.value.push(img.id)
      })
      delete colorImages.value[cid]
    }
  })
  colorImages.value = { ...colorImages.value }
  productForm.value?.validateField('images')
}, { deep: true })

/* Upload handlers */
const handleFileChange = (file, fileList, colorId) => {
  const max = 5 * 1024 * 1024
  const fsize = file.raw?.size ?? file.size ?? 0
  if (fsize > max) {
    showError(`Ảnh ${file.name} vượt quá 5MB!`)
    colorImages.value[colorId] = fileList.filter(f => (f.raw?.size ?? f.size) <= max).map(item => ({
      name: item.name,
      url: item.url || (item.raw ? URL.createObjectURL(item.raw) : ''),
      file: item.raw || null,
      isOld: item.isOld || false,
      id: item.id || null
    }))
    colorImages.value = { ...colorImages.value }
    return
  }

  const existed = colorImages.value[colorId] || []
  const isDup = existed.some(f => f.name === file.name && (!f.file || f.file.size === fsize))
  if (isDup) {
    showError(`Ảnh ${file.name} đã được chọn cho màu này!`)
    colorImages.value[colorId] = fileList.map(item => ({
      name: item.name,
      url: item.url || (item.raw ? URL.createObjectURL(item.raw) : ''),
      file: item.raw || null,
      isOld: item.isOld || false,
      id: item.id || null
    }))
    colorImages.value = { ...colorImages.value }
    return
  }

  colorImages.value[colorId] = fileList.map(item => ({
    name: item.name,
    url: item.url || (item.raw ? URL.createObjectURL(item.raw) : ''),
    file: item.raw || null,
    isOld: item.isOld || false,
    id: item.id || null
  }))
  colorImages.value = { ...colorImages.value }
  productForm.value?.validateField('images')
}
const handleFileRemove = (file, fileList, colorId) => {
  if (file.isOld && file.id && !deletedImageIds.value.includes(file.id)) deletedImageIds.value.push(file.id)
  colorImages.value[colorId] = fileList.map(item => ({
    name: item.name,
    url: item.url || (item.raw ? URL.createObjectURL(item.raw) : ''),
    file: item.raw || null,
    isOld: item.isOld || false,
    id: item.id || null
  }))
  colorImages.value = { ...colorImages.value }
  productForm.value?.validateField('images')
}
const handlePreview = (file) => window.open(file.url, '_blank')

/* Dialog controls - use async validate to avoid race */
const openConfirmDialog = async () => {
  try {
    await productForm.value.validate()
    // validate each variant has quantity >= 0 (we accept 0 per-variant, but require total > 0)
    for (let i = 0; i < (updateProduct.productDetails || []).length; i++) {
      const d = updateProduct.productDetails[i]
      if (d.quantity === null || d.quantity === undefined || Number(d.quantity) < 0) {
        showError('Vui lòng nhập số lượng hợp lệ (>= 0) cho tất cả biến thể.')
        await productForm.value.validateField(`productDetails.${i}.quantity`).catch(() => {})
        return
      }
    }
    const totalQty = (updateProduct.productDetails || []).reduce((s, d) => s + Number(d.quantity || 0), 0)
    if (totalQty <= 0) {
      showError('Tổng số lượng của các biến thể phải lớn hơn 0!')
      return
    }
    // validate images presence
    for (const colorId of updateProduct.selectedColors) {
      if (!colorImages.value[colorId] || colorImages.value[colorId].length === 0) {
        showError(`Vui lòng tải lên ít nhất 1 ảnh cho màu ${getColorName(colorId)}`)
        return
      }
    }

    isModalVisible.value = true
  } catch (err) {
    // validation failed
    showError('Vui lòng sửa các lỗi trên form trước khi tiếp tục.')
    nextTick(() => {
      const el = document.querySelector('.is-error')
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }
}
const closeModal = () => { isModalVisible.value = false }
const goBack = () => { router.push('/product') }

/* Save handler */
const saveProduct = async () => {
  try {
    const mergedDetails = (updateProduct.productDetails || []).filter(d =>
      updateProduct.selectedSizes.includes(d.sizeId) && updateProduct.selectedColors.includes(d.colorId)
    )

    // final check quantities
    for (let i = 0; i < mergedDetails.length; i++) {
      const q = mergedDetails[i].quantity
      if (q === null || q === undefined || !Number.isInteger(Number(q)) || Number(q) < 0 || Number(q) > MAX_QTY) {
        showError('Số lượng biến thể không hợp lệ. Vui lòng kiểm tra lại.')
        return
      }
    }

    const formData = new FormData()
    formData.append('productName', updateProduct.productName || '')
    formData.append('materialId', updateProduct.materialId || '')
    formData.append('supplierId', updateProduct.supplierId || '')
    formData.append('brandId', updateProduct.brandId || '')
    formData.append('soleId', updateProduct.soleId || '')
    formData.append('styleId', updateProduct.styleId || '')
    formData.append('genderId', updateProduct.genderId || '')
    formData.append('weight', updateProduct.weight ?? 0)
    formData.append('originPrice', updateProduct.originPrice ?? 0)
    formData.append('sellPrice', updateProduct.sellPrice ?? 0)
    formData.append('quantity', mergedDetails.reduce((s, d) => s + Number(d.quantity || 0), 0))
    formData.append('description', updateProduct.description || '')

    // categories
    updateProduct.categoryIds.forEach((cat, idx) => {
      const id = typeof cat === 'object' ? cat.id : cat
      formData.append(`categoryIds[${idx}]`, id)
    })

    // product details
    mergedDetails.forEach((d, idx) => {
      if (d.id) formData.append(`productDetails[${idx}].id`, d.id)
      formData.append(`productDetails[${idx}].sizeId`, d.sizeId)
      formData.append(`productDetails[${idx}].colorId`, d.colorId)
      formData.append(`productDetails[${idx}].sellPrice`, updateProduct.sellPrice ?? 0)
      formData.append(`productDetails[${idx}].quantity`, d.quantity ?? 0)
    })

    if (deletedImageIds.value.length > 0) deletedImageIds.value.forEach((id, idx) => formData.append(`oldImageIds[${idx}]`, id))
    if (oldColorIds.value.length > 0) oldColorIds.value.forEach((id, idx) => formData.append(`removedColorIds[${idx}]`, id))

    let imageIndex = 0
    Object.entries(colorImages.value).forEach(([colorId, files]) => {
      files.filter(f => f.file && !f.isOld).forEach(f => {
        formData.append(`productImages[${imageIndex}].productImages`, f.file)
        formData.append(`productImages[${imageIndex}].colorId`, colorId)
        imageIndex++
      })
    })

    const id = route.params.id
    await apiClient.put(`/admin/products/${id}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })

    showSuccess('Cập nhật sản phẩm thành công!')
    isModalVisible.value = false

    // reset local state then redirect
    updateProduct.productName = ''
    updateProduct.categoryIds = []
    updateProduct.materialId = null
    updateProduct.styleId = null
    updateProduct.supplierId = null
    updateProduct.genderId = null
    updateProduct.soleId = null
    updateProduct.brandId = null
    updateProduct.originPrice = null
    updateProduct.weight = null
    updateProduct.sellPrice = null
    updateProduct.description = ''
    updateProduct.selectedSizes = []
    updateProduct.selectedColors = []
    updateProduct.productDetails = []
    colorImages.value = {}
    deletedImageIds.value = []
    oldColorIds.value = []

    setTimeout(() => router.push('/product'), 900)
  } catch (e) {
    console.error('Error updating product:', e)
    const msg = e.response?.data?.error || e.response?.data?.message || 'Đã xảy ra lỗi khi cập nhật sản phẩm.'
    showError(msg)
    isModalVisible.value = false
  }
}

/* Mount */
onMounted(() => {
  fetchBrand(); fetchMaterial(); fetchSizesAndColors(); fetchSole(); fetchStyle(); fetchCategories(); fetchSupplier(); fetchProduct()
})
</script>

<style scoped>
/* =========================
   Theme variables
   ========================= */
:root{
  --bg: #f6f9ff;
  --card: #ffffff;
  --text: #0f172a;
  --muted: #64748b;
  --accent: #2563eb;
  --accent-2: #1d4ed8;
  --accent-ring: rgba(37,99,235,.12);
  --danger: #ef4444;
  --surface: #f8fafc;
  --border: #e6eef8;
  --shadow: 0 6px 18px rgba(15,23,42,0.06);
  --radius: 12px;
  --input-height: 40px;
}

/* =========================
   Base layout
   ========================= */
.ssn-wrap {
  background: var(--bg);
  min-height: 100vh;
  color: var(--text);
  font-family: Inter, system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  padding-bottom: 48px;
  box-sizing: border-box;
}

.ssn-container {
  max-width: 1200px;
  margin: 18px auto 40px;
  padding: 0 16px;
  box-sizing: border-box;
}

/* =========================
   Topbar
   ========================= */
.ssn-topbar{
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255,255,255,0.95);
  border-bottom: 1px solid #eef4ff;
  backdrop-filter: blur(6px) saturate(140%);
}

.ssn-topbar .left { display:flex; align-items:center; gap:12px; }
.ssn-topbar .divider { width:1px; height:22px; background:#e5e9f2; }
.ssn-topbar .title { margin:0; font-size:18px; font-weight:700; color:var(--text); }

.ssn-card{
  background: var(--card);
  border-radius: 14px;
  border: 1px solid #eef4ff;
  box-shadow: var(--shadow);
  overflow: hidden;
  padding-bottom: 12px;
}
.ssn-card :deep(.el-card__header){
  background: transparent;
  border-bottom: 1px solid #eef4ff;
  padding: 14px 18px;
}
.card-header h2, .card-header h3 { margin:0; font-size:15px; font-weight:700; color:var(--text); }

.ssn-form :deep(.el-form-item__label) {
  font-weight:700;
  color:#1f2937;
  margin-bottom:6px;
  font-size:13px;
}

:deep(.el-input__inner),
:deep(.el-select .el-input__inner),
:deep(.el-input-number__wrapper),
:deep(.el-input--prefix) {
  height: var(--input-height);
  line-height: var(--input-height);
  padding: 8px 12px;
  border-radius: 10px;
  box-sizing: border-box;
  font-size: 14px;
}

:deep(.el-textarea__inner){
  border-radius:10px;
  padding: 10px 12px;
  font-size:14px;
}

.note {
  margin-top:6px;
  color:var(--muted);
  font-size:12px;
}

.ssn-form :deep(.el-form-item__error){
  color: var(--danger);
  font-size:12px;
  margin-top:6px;
}

.price-row {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.formatted-price {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 160px;
  height: var(--input-height);
  padding: 0 12px;
  font-weight:700;
  font-size:14px;
  color: #0f172a;
  background: #f5f7fa;
  border: 1px solid var(--border);
  border-radius: 10px;
  box-sizing: border-box;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .formatted-price { min-width: 120px; height: 36px; font-size: 13px; }
  .price-row { gap:8px; }
}

.chips { display:flex; flex-wrap:wrap; gap:8px; margin-top:8px; }
.chip {
  display:inline-flex;
  align-items:center;
  gap:8px;
  background:#eff6ff;
  border:1px solid #dbeafe;
  color:#1e40af;
  padding:6px 10px;
  border-radius:999px;
  font-size:12px;
  font-weight:600;
}

.check-grid {
  display:grid;
  grid-template-columns: repeat(3, minmax(0,1fr));
  gap:8px 12px;
}
.check-item :deep(.el-checkbox__label) { color:#334155; }

.variant-card{
  border-radius:12px;
  border:1px solid #eef4ff;
  padding:12px;
  background:var(--surface);
  display:flex;
  flex-direction:column;
  justify-content:space-between;
  min-height:108px;
}

.variant-head{
  display:flex;
  align-items:center;
  gap:8px;
  justify-content:space-between;
  flex-wrap:wrap;
}
.variant-meta { display:flex; align-items:center; gap:8px; }
.badge {
  background:#f1f5f9;
  color:#0f172a;
  font-weight:700;
  font-size:12px;
  padding:4px 8px;
  border-radius:8px;
}
.badge-blue { background:#e8f1ff; color:var(--accent-2); }
.sep { color:#94a3b8; font-weight:600; }

.price-badge {
  margin-left:8px;
  background:#fffaf0;
  border:1px solid #ffedd5;
  color:#92400e;
  font-weight:700;
  padding:4px 8px;
  border-radius:8px;
  font-size:12px;
}

.variant-body { margin-top:10px; }
.form-label { display:block; font-weight:600; margin-bottom:6px; color:#334155; }

.color-block { margin-bottom:12px; }
.color-heading {
  display:flex;
  align-items:center;
  gap:10px;
  margin-bottom:8px;
}
.color-heading .dot {
  width:10px; height:10px; border-radius:50%;
  background:var(--accent);
  box-shadow: 0 0 0 4px var(--accent-ring);
}
.color-heading .name { font-weight:700; color:var(--text); font-size:13px; }
.color-heading .count { color:var(--muted); font-size:12px; }

.upload-slot {
  display:flex;
  flex-direction:column;
  align-items:center;
  justify-content:center;
  padding:8px;
  min-width:92px;
}
:deep(.el-upload__tip) { display:none; }
:deep(.el-upload-list__item) { max-width:100px; }

.confirm-row { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:12px; }
.meta { color:#334155; font-size:14px; }
.meta-line { margin-bottom:6px; }

.is-error :deep(.el-input__inner),
.is-error :deep(.el-select .el-input__inner) {
  border-color: var(--danger) !important;
  box-shadow: 0 0 0 4px rgba(239,68,68,0.06);
}

.sr-only { position:absolute !important; width:1px; height:1px; padding:0; margin:-1px; overflow:hidden; clip:rect(0,0,0,0); white-space:nowrap; border:0; }

@media (max-width: 1024px) {
  .check-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .xl\:col-span-2 { grid-column: auto; }
  .xl\:col-span-1 { grid-column: auto; }
}
</style>
