<template>
  <div class="container">
    <!-- Card: Form -->
    <el-card shadow="hover" class="mb-4">
      <template #header>
        <div class="card-header">
          <el-icon><Brush /></el-icon>
          <span>Quản lý Cổ Giày</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        label-width="100%"
        @keyup.enter.native="confirmSubmit"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="18" :md="12" :lg="10">
            <el-form-item label="Tên cổ giày" prop="styleName">
              <el-input
                v-model="form.styleName"
                placeholder="Nhập tên cổ giày"
                maxlength="50"
                show-word-limit
                clearable
                :disabled="submitting"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button
            type="primary"
            :icon="isEditing ? Edit : CirclePlus"
            @click="confirmSubmit"
            :loading="submitting"
            :disabled="submitting"
          >
            {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
          </el-button>

          <el-button
            type="warning"
            :icon="RefreshRight"
            @click="resetForm"
            :disabled="submitting"
          >
            Làm mới
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- Toolbar: Search (client-side) -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="Tìm theo tên kiểu dáng..."
        clearable
        class="w-72"
        @input="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="ml-2" @click="fetchStyles" :loading="loading">
        Tải lại
      </el-button>
    </div>

    <el-divider content-position="left">Danh sách kiểu dáng</el-divider>

    <!-- Table -->
    <el-table :data="filteredStyles" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="100" sortable />
      <el-table-column prop="styleName" label="Tên kiểu dáng" min-width="220" />
      <el-table-column label="Hành động" width="220" align="center">
        <template #default="{ row }">
          <el-button
            type="primary"
            :icon="Edit"
            size="small"
            @click="editStyle(row)"
            :disabled="submitting"
          >
            Sửa
          </el-button>
          <el-button
            type="danger"
            :icon="Delete"
            size="small"
            @click="confirmDelete(row.id)"
            :disabled="submitting"
          >
            Xóa
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && filteredStyles.length === 0" class="empty">Không có dữ liệu phù hợp.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Brush, Edit, CirclePlus, RefreshRight, Delete, Search } from '@element-plus/icons-vue'

// ===== State =====
const styles = ref([])
const loading = ref(false)
const submitting = ref(false)

const formRef = ref()
const form = ref({ id: null, styleName: '' })
const isEditing = ref(false)

const keyword = ref('')

// ===== Computed =====
const normalized = (s) => (s ?? '').toString().trim().toLowerCase()

const filteredStyles = computed(() => {
  const k = normalized(keyword.value)
  if (!k) return styles.value
  return styles.value.filter((s) => normalized(s.styleName).includes(k))
})

// ===== Validation Rules =====
const NAME_REGEX = /^[0-9A-Za-zÀ-ỹ\s'()\-]+$/u // Cho phép chữ/số, dấu tiếng Việt, khoảng trắng, ' ( ) -

const rules = {
  styleName: [
    { required: true, message: 'Tên kiểu dáng không được để trống', trigger: 'blur' },
    { min: 2, message: 'Tối thiểu 2 ký tự', trigger: 'blur' },
    { max: 50, message: 'Tối đa 50 ký tự', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        const v = (value ?? '').replace(/\s+/g, ' ').trim()
        if (!v) return cb(new Error('Tên kiểu dáng không được để trống'))
        if (!NAME_REGEX.test(v)) return cb(new Error("Chỉ cho phép chữ/số, khoảng trắng và ',()-"))
        // Trùng tên (không phân biệt hoa thường, bỏ khoảng trắng dư)
        const vNorm = v.toLowerCase()
        const dup = styles.value.some((s) =>
          s.styleName && s.styleName.trim().toLowerCase() === vNorm && (!isEditing.value || s.id !== form.value.id)
        )
        if (dup) return cb(new Error('Tên kiểu dáng đã tồn tại'))
        cb()
      },
      trigger: ['blur', 'change']
    }
  ]
}

// ===== API =====
const fetchStyles = async () => {
  loading.value = true
  try {
    const res = await apiClient.get('/admin/style/hien-thi')
    styles.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    ElMessage.error('Không thể tải danh sách kiểu dáng')
  } finally {
    loading.value = false
  }
}

const createStyle = (name) => apiClient.post('/admin/style', null, { params: { name } })
const updateStyle = (id, name) => apiClient.put(`/admin/style/${id}`, null, { params: { name } })
const deleteStyle = (id) => apiClient.delete(`/admin/style/${id}`)

// ===== Handlers =====
const onSearch = () => {
  // Client-side filter only; nothing else to do
}

const resetForm = () => {
  form.value = { id: null, styleName: '' }
  isEditing.value = false
  formRef.value?.resetFields()
}

const confirmSubmit = () => {
  const action = isEditing.value ? 'Cập nhật' : 'Thêm mới'
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn ${action.toLowerCase()} kiểu dáng này?`,
    'Xác nhận',
    { confirmButtonText: action, cancelButtonText: 'Hủy', type: 'info' }
  )
    .then(() => handleSubmit())
    .catch(() => {})
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return

    const name = (form.value.styleName ?? '').replace(/\s+/g, ' ').trim()
    submitting.value = true
    try {
      if (isEditing.value) {
        await updateStyle(form.value.id, name)
        ElMessage.success('Cập nhật thành công')
      } else {
        await createStyle(name)
        ElMessage.success('Thêm mới thành công')
      }
      await fetchStyles()
      resetForm()
    } catch (e) {
      ElMessage.error('Lỗi khi lưu dữ liệu')
    } finally {
      submitting.value = false
    }
  })
}

const editStyle = (style) => {
  form.value = { id: style.id, styleName: style.styleName }
  isEditing.value = true
  ElMessage.info(`Đang chỉnh sửa: ${style.styleName}`)
}

const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc muốn xóa kiểu dáng này?', 'Cảnh báo', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning'
    })
    submitting.value = true
    await deleteStyle(id)
    ElMessage.success('Xóa thành công')
    await fetchStyles()
  } catch (e) {
    if (e?.message !== 'cancel') ElMessage.error('Xóa thất bại')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchStyles)
</script>

<style scoped>
.container { max-width: 960px; margin: 0 auto; padding: 20px; }
.card-header { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 18px; }
.form-actions { margin-top: 10px; display: flex; gap: 8px; }
.toolbar { display: flex; align-items: center; margin: 8px 0 12px; }
.w-72 { width: 18rem; }
.ml-2 { margin-left: .5rem; }
.mb-4 { margin-bottom: 1rem; }
.empty { padding: 16px; color: #666; font-style: italic; }
</style>
