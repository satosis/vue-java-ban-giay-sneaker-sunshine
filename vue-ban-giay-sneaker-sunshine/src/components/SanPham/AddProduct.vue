<template>
  <div class="page">
    <!-- Header -->
    <div class="page-header">
      <el-button @click="goBack" round>
        <el-icon><ArrowLeft /></el-icon>
        Quay lại
      </el-button>
      <h2>Thêm sản phẩm mới</h2>
    </div>

    <!-- Form -->
    <el-form label-position="top" class="card" :model="newProduct" :inline="false">
      <!-- Danh mục -->
      <el-form-item label="Chọn danh mục sản phẩm" :error="errors.categoryIds">
        <el-select
          v-model="newProduct.categoryIds"
          multiple
          filterable
          collapse-tags
          placeholder="Chọn danh mục"
          value-key="id"
          style="width: 100%"
        >
          <el-option
            v-for="c in categoryList"
            :key="c.id"
            :label="c.categoryName"
            :value="c"
          />
        </el-select>
        <div class="hint">
          <span>Đã chọn: </span>
          <template v-if="newProduct.categoryIds.length === 0">
            <el-text type="info">Chưa chọn</el-text>
          </template>
          <template v-else>
            <el-space wrap>
              <el-tag
                v-for="c in newProduct.categoryIds"
                :key="c.id"
                size="small"
                type="success"
              >{{ c.categoryName }}</el-tag>
            </el-space>
          </template>
        </div>
      </el-form-item>

      <!-- Tên sp -->
      <el-form-item label="Tên sản phẩm" :error="errors.productName">
        <el-input
          v-model="newProduct.productName"
          placeholder="Nhập tên sản phẩm"
          clearable
        />
      </el-form-item>

      <!-- 2 cột thông tin chọn lựa -->
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="Chất liệu" :error="errors.materialId">
            <el-select v-model="newProduct.materialId" placeholder="Chọn chất liệu" style="width:100%">
              <el-option v-for="m in materialList" :key="m.id" :label="m.materialName" :value="m.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="Nhà cung cấp" :error="errors.supplierId">
            <el-select v-model="newProduct.supplierId" placeholder="Chọn nhà cung cấp" style="width:100%">
              <el-option v-for="s in supplierList" :key="s.id" :label="s.supplierName" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="Đế giày" :error="errors.soleId">
            <el-select v-model="newProduct.soleId" placeholder="Chọn loại đế" style="width:100%">
              <el-option v-for="s in soleList" :key="s.id" :label="s.soleName" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="Cổ giày" :error="errors.styleId">
            <el-select v-model="newProduct.styleId" placeholder="Chọn cổ giày" style="width:100%">
              <el-option v-for="s in styleList" :key="s.id" :label="s.styleName" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="Thương hiệu" :error="errors.brandId">
            <el-select v-model="newProduct.brandId" placeholder="Chọn thương hiệu" style="width:100%">
              <el-option v-for="b in brandList" :key="b.id" :label="b.brandName" :value="b.id" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :xs="24" :sm="12">
          <el-form-item label="Dành cho" :error="errors.genderId">
            <el-radio-group v-model="newProduct.genderId">
              <el-radio :label="1">Nam</el-radio>
              <el-radio :label="2">Nữ</el-radio>
              <el-radio :label="3">Unisex</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Trọng lượng + Giá bán chung -->
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="Cân nặng (gram)" :error="errors.weight">
            <el-input-number
              v-model="newProduct.weight"
              :min="1"
              :step="1"
              :precision="0"
              style="width:100%"
              @change="onWeightChange"
              @blur="onWeightBlur"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="Giá bán (áp dụng cho tất cả biến thể)" :error="errors.sellPrice">
            <div class="price-row">
              <el-input-number
                v-model="newProduct.sellPrice"
                :min="1"
                :step="1000"
                style="width:100%"
                @change="onSellPriceChange"
                @blur="onSellPriceBlur"
              />
              <!-- Hiển thị dạng đã format -->
              <div class="formatted-price" v-if="formattedSellPrice">
                {{ formattedSellPrice }}
              </div>
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- Ghi chú -->
      <el-form-item label="Ghi chú sản phẩm">
        <el-input
          v-model="newProduct.description"
          type="textarea"
          :rows="3"
          placeholder="Ghi chú sản phẩm"
        />
      </el-form-item>

      <!-- Kích thước: có nút thêm nhanh -->
      <el-form-item :error="errors.selectedSizes">
        <template #label>
          <div class="label-with-btn">
            <span>Kích thước</span>
            <el-button size="small" type="primary" plain icon>
              <el-icon @click.stop="openAddSizeDialog"><Plus /></el-icon>
            </el-button>
          </div>
        </template>
        <el-checkbox-group v-model="selectedSizes" @change="generateProductDetails">
          <el-space wrap>
            <el-checkbox
              v-for="sz in sizeList"
              :key="sz.id"
              :label="sz.id"
            >{{ sz.sizeName }}</el-checkbox>
          </el-space>
        </el-checkbox-group>
      </el-form-item>

      <!-- Màu sắc: có nút thêm nhanh -->
      <el-form-item :error="errors.selectedColors">
        <template #label>
          <div class="label-with-btn">
            <span>Màu sắc</span>
            <el-button size="small" type="primary" plain icon>
              <el-icon @click.stop="openAddColorDialog"><Plus /></el-icon>
            </el-button>
          </div>
        </template>
        <el-checkbox-group v-model="selectedColors" @change="generateProductDetails">
          <el-space wrap>
            <el-checkbox
              v-for="cl in colorList"
              :key="cl.id"
              :label="cl.id"
            >{{ cl.colorName }}</el-checkbox>
          </el-space>
        </el-checkbox-group>
      </el-form-item>

      <!-- Upload ảnh theo màu -->
      <div v-if="selectedColors.length > 0" class="block">
        <el-alert
          title="Tải ảnh theo từng màu. Mỗi màu nên có ít nhất 1 ảnh."
          type="info"
          :closable="false"
          show-icon
          class="mb-12"
        />
        <el-row :gutter="16">
          <el-col v-for="cid in selectedColors" :key="cid" :xs="24" :md="12">
            <el-card shadow="never" class="color-card">
              <template #header>
                <div class="card-header">
                  <span>Ảnh cho màu: <b>{{ getColorName(cid) }}</b></span>
                </div>
              </template>

              <el-upload
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                multiple
                :limit="10"
                accept="image/*"
                :file-list="colorImages[cid] || []"
                :on-change="(file, list) => handleFileChange(file, list, cid)"
                :on-remove="(file, list) => handleFileRemove(file, list, cid)"
                :on-preview="handlePreview"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>

              <div v-if="errors[`colorImage_${cid}`]" class="el-form-item__error mt-6">
                {{ errors[`colorImage_${cid}`] }}
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div v-if="productDetails.length > 0" class="block">
        <div v-if="errors.productDetails" class="el-form-item__error mb-8">{{ errors.productDetails }}</div>

        <el-table :data="productDetails" border style="width: 100%">
          <el-table-column prop="sizeName" label="Kích thước" width="140" />
          <el-table-column prop="colorName" label="Màu sắc" width="180" />
          <el-table-column label="Số lượng" width="200">
            <template #default="{ row, $index }">
              <el-input-number
                v-model="row.quantity"
                :min="0"
                :step="1"
                :precision="0"
                style="width: 100%"
                @change="(val) => onQuantityChange(val, row, $index)"
              />
              <div v-if="errors[`productDetail_${$index}_quantity`]" class="el-form-item__error">
                {{ errors[`productDetail_${$index}_quantity`] }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Actions -->
      <div class="actions">
        <el-button size="large" @click="goBack">Huỷ</el-button>
        <el-button size="large" type="primary" @click="openConfirmDialog">Thêm sản phẩm</el-button>
      </div>
    </el-form>

    <!-- Confirm Dialog -->
    <el-dialog v-model="isModalVisible" title="Xác nhận" width="420px">
      <span>Bạn có chắc chắn muốn lưu sản phẩm này không?</span>
      <template #footer>
        <el-button @click="closeModal">Hủy</el-button>
        <el-button type="primary" @click="saveProduct">Xác nhận</el-button>
      </template>
    </el-dialog>

    <!-- Dialog thêm Kích thước nhanh -->
    <el-dialog v-model="isAddSizeDialog" title="Thêm kích thước" width="420px" @close="resetAddSize">
      <el-form>
        <el-form-item label="Tên kích thước">
          <el-input v-model="newSizeName" placeholder="Ví dụ: 39,40,41" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddSizeDialog">Hủy</el-button>
        <el-button type="primary" @click="addSize" :loading="addingSize">Thêm</el-button>
      </template>
    </el-dialog>

    <!-- Dialog thêm Màu sắc nhanh -->
    <el-dialog v-model="isAddColorDialog" title="Thêm màu sắc" width="420px" @close="resetAddColor">
      <el-form>
        <el-form-item label="Tên màu">
          <el-input v-model="newColorName" placeholder="Ví dụ: Đen, Trắng" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddColorDialog">Hủy</el-button>
        <el-button type="primary" @click="addColor" :loading="addingColor">Thêm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import apiClient from '@/utils/axiosInstance'

const router = useRouter()
const ArrowLeftIcon = ArrowLeft
const PlusIcon = Plus

// ---------- CONSTANTS ----------
const MAX_SELL_PRICE = 999_999_999      // 9 chữ số
const MAX_WEIGHT = 99_999                // 5 chữ số
const MAX_QTY = 999_999                  // tối đa 6 chữ số cho số lượng chi tiết

// Data lists
const brandList = ref([])
const materialList = ref([])
const categoryList = ref([])
const sizeList = ref([])
const colorList = ref([])
const soleList = ref([])
const supplierList = ref([])
const styleList = ref([])

// UI states
const isModalVisible = ref(false)

// Quick-add dialogs
const isAddSizeDialog = ref(false)
const isAddColorDialog = ref(false)
const newSizeName = ref('')
const newColorName = ref('')
const addingSize = ref(false)
const addingColor = ref(false)

const colorImages = ref({}) // { [colorId]: [{name,url,file,uid}] }

const selectedSizes = ref([])   // array of sizeId
const selectedColors = ref([])  // array of colorId

// generated details
const productDetails = ref([])

const newProduct = ref({
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
  sellPrice: null, // giá bán chung cho tất cả biến thể (số)
  description: '',
})

// errors
const errors = ref({})

// Helpers
const notify = (message, type = 'success') => {
  ElNotification({ title: type === 'success' ? 'Thành công' : 'Lỗi', message, type, duration: 2500 })
}
const getColorName = (colorId) => colorList.value.find(c => c.id === colorId)?.colorName || 'Không xác định'

// ---------- Price formatting helper ----------
const formatVND = (value) => {
  const n = Number(value)
  if (!Number.isFinite(n)) return ''
  return `${new Intl.NumberFormat('vi-VN').format(Math.trunc(n))} VND`
}
const formattedSellPrice = computed(() => {
  return newProduct.value.sellPrice ? formatVND(newProduct.value.sellPrice) : ''
})

const isPositiveNumber = (v) => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0
}
const isNonNegativeInteger = (v) => {
  if (v === null || v === undefined || v === '') return false
  const n = Number(v)
  return Number.isFinite(n) && Number.isInteger(n) && n >= 0
}

// ---------- Dynamic field validators ----------
const validateSellPriceField = () => {
  delete errors.value.sellPrice
  const v = newProduct.value.sellPrice
  if (v === null || v === undefined || v === '') {
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
    return false
  }
  if (!isPositiveNumber(v)) {
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
    return false
  }
  if (Number(v) > MAX_SELL_PRICE) {
    errors.value.sellPrice = `Giá bán không được lớn hơn ${MAX_SELL_PRICE.toLocaleString('vi-VN')} (tối đa 9 chữ số).`
    return false
  }
  // success
  delete errors.value.sellPrice
  return true
}

const validateWeightField = () => {
  delete errors.value.weight
  const v = newProduct.value.weight
  if (v === null || v === undefined || v === '') {
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
    return false
  }
  if (!isPositiveNumber(v) || !Number.isInteger(Number(v))) {
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
    return false
  }
  if (Number(v) > MAX_WEIGHT) {
    errors.value.weight = `Cân nặng không được lớn hơn ${MAX_WEIGHT.toLocaleString('vi-VN')} (tối đa 5 chữ số).`
    return false
  }
  delete errors.value.weight
  return true
}

const validateCategorySelection = () => {
  if (!Array.isArray(newProduct.value.categoryIds) || newProduct.value.categoryIds.length === 0) {
    errors.value.categoryIds = 'Vui lòng chọn ít nhất một danh mục.'
    return false
  } else {
    delete errors.value.categoryIds
    return true
  }
}

const validateSizesColorsSelection = () => {
  let ok = true
  if (!selectedSizes.value || selectedSizes.value.length === 0) {
    errors.value.selectedSizes = 'Vui lòng chọn ít nhất một kích thước.'
    ok = false
  } else {
    delete errors.value.selectedSizes
  }
  if (!selectedColors.value || selectedColors.value.length === 0) {
    errors.value.selectedColors = 'Vui lòng chọn ít nhất một màu sắc.'
    ok = false
  } else {
    delete errors.value.selectedColors
  }
  return ok
}

const validateProductDetailsQuantities = () => {
  // validate each detail quantity
  let ok = true
  if (!productDetails.value || productDetails.value.length === 0) {
    errors.value.productDetails = 'Không có chi tiết sản phẩm nào được tạo.'
    return false
  } else {
    delete errors.value.productDetails
  }

  productDetails.value.forEach((d, i) => {
    const key = `productDetail_${i}_quantity`
    if (d.quantity === null || d.quantity === undefined || d.quantity === '') {
      errors.value[key] = 'Số lượng phải là số nguyên không âm.'
      ok = false
    } else {
      const n = Number(d.quantity)
      if (!Number.isFinite(n) || !Number.isInteger(n) || n < 0) {
        errors.value[key] = 'Số lượng phải là số nguyên không âm.'
        ok = false
      } else if (n > MAX_QTY) {
        errors.value[key] = `Số lượng không được vượt quá ${MAX_QTY.toLocaleString('vi-VN')}.`
        ok = false
      } else {
        // valid: remove previous error
        if (errors.value[key]) delete errors.value[key]
      }
    }
  })
  return ok
}

const validateColorImages = () => {
  let ok = true
  for (const cid of selectedColors.value) {
    if (!colorImages.value[cid] || colorImages.value[cid].length === 0) {
      errors.value[`colorImage_${cid}`] = `Vui lòng tải lên ít nhất một ảnh cho màu ${getColorName(cid)}.`
      ok = false
    } else {
      if (errors.value[`colorImage_${cid}`]) delete errors.value[`colorImage_${cid}`]
    }
  }
  return ok
}

// Full validation used on submit
const validateAll = () => {
  errors.value = {}

  // required & basic fields
  if (!newProduct.value.productName || !String(newProduct.value.productName).trim()) {
    errors.value.productName = 'Tên sản phẩm không được để trống.'
  }

  if (!newProduct.value.materialId) errors.value.materialId = 'Vui lòng chọn chất liệu.'
  if (!newProduct.value.genderId) errors.value.genderId = 'Vui lòng chọn đối tượng dành cho.'
  if (!newProduct.value.supplierId) errors.value.supplierId = 'Vui lòng chọn nhà cung cấp.'
  if (!newProduct.value.soleId) errors.value.soleId = 'Vui lòng chọn loại đế.'
  if (!newProduct.value.styleId) errors.value.styleId = 'Vui lòng chọn cổ giày.'
  if (!newProduct.value.brandId) errors.value.brandId = 'Vui lòng chọn thương hiệu.'

  // category
  validateCategorySelection()

  // weight & sellPrice
  validateWeightField()
  validateSellPriceField()

  // sizes/colors
  validateSizesColorsSelection()

  // product details
  if (selectedSizes.value.length > 0 && selectedColors.value.length > 0) {
    validateProductDetailsQuantities()
  }

  // images
  validateColorImages()

  return Object.keys(errors.value).length === 0
}

// ---------- Input handlers with limits ----------
const onWeightChange = (val) => {
  if (val === null || val === undefined || val === '') {
    newProduct.value.weight = null
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
    return
  }
  let n = Number(val)
  if (!Number.isFinite(n)) {
    newProduct.value.weight = null
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
    return
  }
  n = Math.trunc(n)
  if (n <= 0) {
    newProduct.value.weight = null
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
    return
  }
  if (n > MAX_WEIGHT) {
    newProduct.value.weight = MAX_WEIGHT
    errors.value.weight = `Cân nặng không được lớn hơn ${MAX_WEIGHT.toLocaleString('vi-VN')}.`
    return
  }
  newProduct.value.weight = n
  delete errors.value.weight
}

const onWeightBlur = () => {
  validateWeightField()
}

const onSellPriceChange = (val) => {
  if (val === null || val === undefined || val === '') {
    newProduct.value.sellPrice = null
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
    return
  }
  let n = Number(val)
  if (!Number.isFinite(n)) {
    newProduct.value.sellPrice = null
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
    return
  }
  n = Math.trunc(n)
  if (n <= 0) {
    newProduct.value.sellPrice = null
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
    return
  }
  if (n > MAX_SELL_PRICE) {
    newProduct.value.sellPrice = MAX_SELL_PRICE
    errors.value.sellPrice = `Giá bán không được lớn hơn ${MAX_SELL_PRICE.toLocaleString('vi-VN')}.`
    return
  }
  newProduct.value.sellPrice = n
  delete errors.value.sellPrice
}

const onSellPriceBlur = () => {
  validateSellPriceField()
}

const onQuantityChange = (val, row, index) => {
  // normalize / enforce integer and max
  if (val === null || val === undefined || val === '') {
    row.quantity = 0
    errors.value[`productDetail_${index}_quantity`] = 'Số lượng phải là số nguyên không âm.'
    return
  }
  let n = Number(val)
  if (!Number.isFinite(n)) {
    row.quantity = 0
    errors.value[`productDetail_${index}_quantity`] = 'Số lượng phải là số nguyên không âm.'
    return
  }
  // integer required
  n = Math.trunc(n)
  if (n < 0) {
    row.quantity = 0
    errors.value[`productDetail_${index}_quantity`] = 'Số lượng không được âm.'
    return
  }
  if (n > MAX_QTY) {
    row.quantity = MAX_QTY
    errors.value[`productDetail_${index}_quantity`] = `Số lượng không được vượt quá ${MAX_QTY.toLocaleString('vi-VN')}.`
    return
  }
  row.quantity = n
  // pass validation for this row
  if (errors.value[`productDetail_${index}_quantity`]) delete errors.value[`productDetail_${index}_quantity`]
}

// ---------- Product details generation ----------
const generateProductDetails = () => {
  const next = []
  const sIds = selectedSizes.value.map(x => Number(x))
  const cIds = selectedColors.value.map(x => Number(x))

  for (const sizeId of sIds) {
    for (const colorId of cIds) {
      const size = sizeList.value.find(s => Number(s.id) === Number(sizeId))
      const color = colorList.value.find(c => Number(c.id) === Number(colorId))
      const existed = productDetails.value.find(d => Number(d.sizeId) === Number(sizeId) && Number(d.colorId) === Number(colorId))
      if (existed) {
        next.push({ ...existed })
      } else {
        next.push({
          sizeId,
          colorId,
          sizeName: size?.sizeName || '',
          colorName: color?.colorName || '',
          quantity: 0,
        })
      }
    }
  }
  next.sort((a, b) => (Number(a.sizeId) - Number(b.sizeId)) || (Number(a.colorId) - Number(b.colorId)))
  productDetails.value = next

  // after generate, validate the productDetails to show feedback immediately
  validateProductDetailsQuantities()
}

// ---------- File uploads ----------
const handleFileChange = (file, fileList, colorId) => {
  const max = 5 * 1024 * 1024
  const size = file.raw?.size ?? file.size ?? 0
  if (size > max) {
    notify(`Ảnh ${file.name} vượt quá 5MB!`, 'error')
    fileList.splice(fileList.indexOf(file), 1)
    return
  }
  const dup = (colorImages.value[colorId] || []).some(f => f.name === file.name && (f.file?.size === size))
  if (dup) {
    notify(`Ảnh ${file.name} đã được chọn cho màu này!`, 'error')
    fileList.splice(fileList.indexOf(file), 1)
    return
  }

  const f = {
    name: file.name,
    url: file.url || (file.raw ? URL.createObjectURL(file.raw) : ''),
    file: file.raw || null,
    uid: file.uid || file.name + '_' + Date.now(),
  }
  if (!colorImages.value[colorId]) colorImages.value[colorId] = []
  colorImages.value[colorId].push(f)
  colorImages.value = { ...colorImages.value }

  if (errors.value[`colorImage_${colorId}`]) delete errors.value[`colorImage_${colorId}`]
}

const handleFileRemove = (file, fileList, colorId) => {
  colorImages.value[colorId] = fileList.map(item => ({
    name: item.name,
    url: item.url || (item.raw ? URL.createObjectURL(item.raw) : ''),
    file: item.raw || null,
    uid: item.uid || item.name + '_' + Date.now(),
  }))
  colorImages.value = { ...colorImages.value }
  // revalidate this color
  if (!colorImages.value[colorId] || colorImages.value[colorId].length === 0) {
    errors.value[`colorImage_${colorId}`] = `Vui lòng tải lên ít nhất một ảnh cho màu ${getColorName(colorId)}.`
  } else {
    delete errors.value[`colorImage_${colorId}`]
  }
}

const handlePreview = (file) => {
  if (file.url) window.open(file.url, '_blank')
}

// ---------- Actions ----------
const openConfirmDialog = () => {
  if (!validateAll()) {
    notify('Vui lòng điền đầy đủ và chính xác các trường bắt buộc.', 'error')
    requestAnimationFrame(() => {
      const errEl = document.querySelector('.el-form-item__error')
      if (errEl) errEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
    return
  }
  isModalVisible.value = true
}
const closeModal = () => { isModalVisible.value = false }

const saveProduct = async () => {
  try {
    if (!validateAll()) {
      notify('Dữ liệu còn lỗi. Vui lòng kiểm tra các trường.', 'error')
      isModalVisible.value = false
      return
    }

    const totalQty = productDetails.value.reduce((s, d) => s + Number(d.quantity || 0), 0)
    if (totalQty <= 0) {
      notify('Tổng số lượng các biến thể phải lớn hơn 0!', 'error')
      isModalVisible.value = false
      return
    }

    for (const cid of selectedColors.value) {
      if (!colorImages.value[cid] || colorImages.value[cid].length === 0) {
        notify(`Vui lòng chọn ít nhất một ảnh cho màu ${getColorName(cid)}!`, 'error')
        isModalVisible.value = false
        return
      }
    }

    const formData = new FormData()
    formData.append('productName', newProduct.value.productName || '')
    formData.append('materialId', newProduct.value.materialId || '')
    formData.append('supplierId', newProduct.value.supplierId || '')
    formData.append('brandId', newProduct.value.brandId || '')
    formData.append('soleId', newProduct.value.soleId || '')
    formData.append('styleId', newProduct.value.styleId || '')
    formData.append('genderId', newProduct.value.genderId || '')
    formData.append('weight', newProduct.value.weight || 0)
    formData.append('originPrice', newProduct.value.originPrice || 0)

    formData.append('sellPrice', newProduct.value.sellPrice ?? 0)
    formData.append('quantity', totalQty)
    formData.append('description', newProduct.value.description || '')

    const cats = Array.isArray(newProduct.value.categoryIds) ? newProduct.value.categoryIds : []
    cats.forEach((cat, idx) => formData.append(`categoryIds[${idx}]`, cat?.id ?? cat))

    productDetails.value.forEach((d, i) => {
      formData.append(`productDetails[${i}].sizeId`, d.sizeId)
      formData.append(`productDetails[${i}].colorId`, d.colorId)
      formData.append(`productDetails[${i}].quantity`, d.quantity)
      formData.append(`productDetails[${i}].sellPrice`, newProduct.value.sellPrice ?? 0)
    })

    let imgIdx = 0
    Object.entries(colorImages.value).forEach(([cid, files]) => {
      files.filter(f => f.file).forEach(f => {
        formData.append(`productImages[${imgIdx}].productImages`, f.file)
        formData.append(`productImages[${imgIdx}].colorId`, cid)
        imgIdx++
      })
    })

    const res = await apiClient.post('/admin/products', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    notify(`Thêm sản phẩm thành công! Giá: ${formattedSellPrice.value}`, 'success')
    isModalVisible.value = false

    // reset
    newProduct.value = {
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
    }
    colorImages.value = {}
    productDetails.value = []
    selectedSizes.value = []
    selectedColors.value = []
    errors.value = {}

    setTimeout(() => router.push('/product'), 800)
  } catch (e) {
    console.error('Lỗi thêm sản phẩm:', e)
    const m = e.response?.data?.error || e.response?.data?.message || 'Đã xảy ra lỗi khi thêm sản phẩm.'
    notify(m, 'error')
    isModalVisible.value = false
  }
}

// ---------- Quick add size/color ----------
const openAddSizeDialog = () => {
  newSizeName.value = ''
  isAddSizeDialog.value = true
}
const closeAddSizeDialog = () => { isAddSizeDialog.value = false }
const resetAddSize = () => { newSizeName.value = '' }

const openAddColorDialog = () => {
  newColorName.value = ''
  isAddColorDialog.value = true
}
const closeAddColorDialog = () => { isAddColorDialog.value = false }
const resetAddColor = () => { newColorName.value = '' }

const addSize = async () => {
  const name = (newSizeName.value || '').trim()
  if (!name) {
    notify('Vui lòng nhập tên kích thước.', 'error')
    return
  }
  addingSize.value = true
  try {
    const res = await apiClient.post('/admin/size', null, {
      params: { name: name }
    });
    const created = res?.data || { id: `tmp-size-${Date.now()}`, sizeName: name }
    sizeList.value.push(created)
    const newId = created.id
    if (!selectedSizes.value.includes(newId)) selectedSizes.value.push(newId)
    generateProductDetails()
    notify('Thêm kích thước thành công.')
    closeAddSizeDialog()
  } catch (err) {
    console.error('Lỗi tạo kích thước:', err)
    notify('Lỗi khi tạo kích thước. Vui lòng thử lại.', 'error')
  } finally {
    addingSize.value = false
  }
}

const addColor = async () => {
  const name = (newColorName.value || '').trim()
  if (!name) {
    notify('Vui lòng nhập tên màu sắc.', 'error')
    return
  }
  addingColor.value = true
  try {
    const res = await apiClient.post('/admin/color', null, {
      params: { name: name }
    });
    const created = res?.data || { id: `tmp-color-${Date.now()}`, colorName: name }
    colorList.value.push(created)
    const newId = created.id
    if (!selectedColors.value.includes(newId)) selectedColors.value.push(newId)
    generateProductDetails()
    notify('Thêm màu sắc thành công.')
    closeAddColorDialog()
  } catch (err) {
    console.error('Lỗi tạo màu sắc:', err)
    notify('Lỗi khi tạo màu sắc. Vui lòng thử lại.', 'error')
  } finally {
    addingColor.value = false
  }
}

// ---------- Watchers (dynamic validation) ----------
watch(() => newProduct.value.sellPrice, () => {
  // live validate price when changed externally
  if (newProduct.value.sellPrice !== null && newProduct.value.sellPrice !== undefined) {
    // ensure integer and within max
    let n = Number(newProduct.value.sellPrice)
    if (!Number.isFinite(n)) {
      errors.value.sellPrice = 'Giá bán phải là số hợp lệ.'
    } else {
      n = Math.trunc(n)
      if (n <= 0) {
        errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
      } else if (n > MAX_SELL_PRICE) {
        errors.value.sellPrice = `Giá bán không được lớn hơn ${MAX_SELL_PRICE.toLocaleString('vi-VN')}.`
        newProduct.value.sellPrice = MAX_SELL_PRICE
      } else {
        delete errors.value.sellPrice
        newProduct.value.sellPrice = n
      }
    }
  } else {
    errors.value.sellPrice = 'Giá bán phải là số lớn hơn 0.'
  }
})

watch(() => newProduct.value.weight, () => {
  if (newProduct.value.weight !== null && newProduct.value.weight !== undefined) {
    let n = Number(newProduct.value.weight)
    if (!Number.isFinite(n)) {
      errors.value.weight = 'Cân nặng phải là số hợp lệ.'
    } else {
      n = Math.trunc(n)
      if (n <= 0) {
        errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
      } else if (n > MAX_WEIGHT) {
        errors.value.weight = `Cân nặng không được lớn hơn ${MAX_WEIGHT.toLocaleString('vi-VN')}.`
        newProduct.value.weight = MAX_WEIGHT
      } else {
        delete errors.value.weight
        newProduct.value.weight = n
      }
    }
  } else {
    errors.value.weight = 'Cân nặng phải là số nguyên lớn hơn 0 (gram).'
  }
})

// watch productDetails deeply to validate quantities live
watch(productDetails, (n, o) => {
  validateProductDetailsQuantities()
}, { deep: true })

// watch selectedColors to remove images and errors when deselected
watch(selectedColors, (n, o) => {
  const removed = (o || []).filter(id => !n.includes(id))
  removed.forEach(id => {
    delete colorImages.value[id]
    colorImages.value = { ...colorImages.value }
    if (errors.value[`colorImage_${id}`]) delete errors.value[`colorImage_${id}`]
  })
  generateProductDetails()
})

// watch selectedSizes to regenerate details
watch(selectedSizes, () => generateProductDetails())

// ---------- Fetchers ----------
const fetchCategories = async () => {
  try {
    const { data } = await apiClient.get('/admin/categories/hien-thi')
    categoryList.value = data
  } catch (e) { notify('Lỗi lấy danh mục!', 'error') }
}
const fetchMaterial = async () => {
  try {
    const { data } = await apiClient.get('/admin/material/hien-thi')
    materialList.value = data
  } catch (e) { notify('Lỗi lấy chất liệu!', 'error') }
}
const fetchSupplier = async () => {
  try {
    const { data } = await apiClient.get('/admin/supplier/hien-thi')
    supplierList.value = data
  } catch (e) { notify('Lỗi lấy nhà cung cấp!', 'error') }
}
const fetchBrand = async () => {
  try {
    const { data } = await apiClient.get('/admin/brand/hien-thi')
    brandList.value = data
  } catch (e) { notify('Lỗi lấy thương hiệu!', 'error') }
}
const fetchSole = async () => {
  try {
    const { data } = await apiClient.get('/admin/sole/hien-thi')
    soleList.value = data
  } catch (e) { notify('Lỗi lấy đế giày!', 'error') }
}
const fetchStyle = async () => {
  try {
    const { data } = await apiClient.get('/admin/style/hien-thi')
    styleList.value = data
  } catch (e) { notify('Lỗi lấy cổ giày!', 'error') }
}
const fetchSizesAndColors = async () => {
  try {
    const [sRes, cRes] = await Promise.all([
      apiClient.get('/admin/size/hien-thi'),
      apiClient.get('/admin/color/hien-thi'),
    ])
    sizeList.value = sRes.data
    colorList.value = cRes.data
  } catch (e) { notify('Lỗi lấy kích thước hoặc màu sắc!', 'error') }
}

const goBack = () => router.push('/product')

onMounted(() => {
  fetchBrand()
  fetchMaterial()
  fetchSizesAndColors()
  fetchSole()
  fetchStyle()
  fetchCategories()
  fetchSupplier()
})
</script>

<style scoped>
.page {
  max-width: 1100px;
  margin: 32px auto;
  padding: 0 12px;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0 0 0 8px; font-weight: 700; }
.card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.hint { margin-top: 6px; }
.block { margin-top: 16px; }
.mb-8 { margin-bottom: 8px; }
.mb-12 { margin-bottom: 12px; }
.mt-6 { margin-top: 6px; }
.color-card { margin-bottom: 12px; }
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
.note { font-size: 12px; color: #666; margin-top: 6px; }

/* price display */
.price-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.formatted-price {
  min-width: 160px;
  font-weight: 600;
  color: #333;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 8px 10px;
  text-align: center;
}

/* label + add button */
.label-with-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label-with-btn el-button {
  padding: 2px;
}

/* dialogs inputs fullwidth */
.el-dialog .el-input {
  width: 100%;
}

.is-invalid :deep(.el-input__wrapper),
.is-invalid :deep(.el-input-number__decrease),
.is-invalid :deep(.el-input-number__increase) {
  border-color: var(--el-color-danger) !important;
}
</style>
