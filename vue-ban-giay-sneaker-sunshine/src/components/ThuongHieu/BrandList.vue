<template>
  <div class="container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Brush /></el-icon>
          <span>Quản lý thương hiệu</span>
        </div>
      </template>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-position="top"
        label-width="100%"
      >
        <el-row :gutter="20">
          <el-col :xs="24" :sm="18" :md="12" :lg="10">
            <el-form-item label="Tên thương hiệu" prop="brandName">
              <el-input
                v-model="form.brandName"
                placeholder="Nhập tên thương hiệu"
                maxlength="50"
                show-word-limit
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item class="form-actions">
          <el-button
            type="primary"
            :icon="isEditing ? Edit : CirclePlus"
            @click="handleSubmit"
            :loading="loading"
          >
            {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
          </el-button>
          <el-button type="warning" icon="RefreshRight" @click="resetForm">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-divider content-position="left">Danh sách thương hiệu</el-divider>

    <el-table :data="brands" style="margin-top: 10px" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="brandName" label="Tên thương hiệu" />
      <el-table-column label="Hành động" width="200">
        <template #default="scope">
          <el-button
            type="primary"
            icon="Edit"
            size="small"
            @click="editBrand(scope.row)"
          >Sửa</el-button>
          <el-button
            type="danger"
            icon="Delete"
            size="small"
            @click="deleteBrand(scope.row.id)"
          >Xóa</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// Import your pre-configured API client
import apiClient from '@/utils/axiosInstance' 
import { ElMessage, ElMessageBox } from 'element-plus'
import { Brush, Edit, CirclePlus, RefreshRight, Delete } from '@element-plus/icons-vue' // Added Delete icon

const brands = ref([])
const form = ref({ id: null, brandName: '' })
const isEditing = ref(false)
const formRef = ref()
const loading = ref(false)

const rules = {
  brandName: [
    { required: true, message: 'Tên thương hiệu không được để trống', trigger: 'blur' },
    { min: 2, message: 'Tên thương hiệu tối thiểu 2 ký tự', trigger: 'blur' },
    { max: 50, message: 'Tên thương hiệu tối đa 50 ký tự', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        // Unicode property escape \p{L} matches any kind of letter from any language.
        // \d matches digits, \s matches whitespace.
        const pattern = /^[\p{L}\d\s]+$/u
        if (!pattern.test(value)) {
          callback(new Error('Tên thương hiệu không chứa ký tự đặc biệt'))
          return
        }
        const nameTrimmed = value.trim().toLowerCase()
        const duplicate = brands.value.some(
          (b) => b.brandName.trim().toLowerCase() === nameTrimmed && b.id !== form.value.id
        )
        if (duplicate) {
          callback(new Error('Tên thương hiệu đã tồn tại'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const fetchBrands = async () => {
  loading.value = true; // Set loading state
  try {
    // Use apiClient for the GET request
    const res = await apiClient.get('/admin/brand/hien-thi')
    brands.value = res.data
  } catch (err) {
    console.error('Lỗi khi tải danh sách thương hiệu:', err);
    ElMessage.error('Không thể tải danh sách thương hiệu');
  } finally {
    loading.value = false; // Reset loading state
  }
}

const resetForm = () => {
  form.value = { id: null, brandName: '' }
  isEditing.value = false
  formRef.value?.resetFields() // Safely reset form fields and validation status
  ElMessage.info('Form đã được đặt lại.');
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('Vui lòng kiểm tra lại thông tin form.');
      return;
    }

    const confirmMessage = isEditing.value
      ? 'Bạn có chắc muốn cập nhật thương hiệu này?'
      : 'Bạn có chắc muốn thêm mới thương hiệu này?'

    try {
      await ElMessageBox.confirm(confirmMessage, 'Xác nhận', {
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        type: 'info'
      })

      loading.value = true

      if (isEditing.value) {
        // Use apiClient for the PUT request
        await apiClient.put(`/admin/brand/${form.value.id}`, null, {
          params: { name: form.value.brandName }
        })
        ElMessage.success('Cập nhật thành công')
      } else {
        // Use apiClient for the POST request
        await apiClient.post('/admin/brand', null, {
          params: { name: form.value.brandName }
        })
        ElMessage.success('Thêm mới thành công')
      }

      await fetchBrands()
      resetForm()
    } catch (err) {
      if (err !== 'cancel') {
        ElMessage.error('Lỗi khi lưu dữ liệu')
      }
    } finally {
      loading.value = false
    }
  })
}

const editBrand = (brand) => {
  form.value = { ...brand } // Populate form with existing brand data
  isEditing.value = true // Set editing mode
  ElMessage.info(`Đang chỉnh sửa: ${brand.brandName}`);
}

const deleteBrand = async (id) => {
  try {
    // Show confirmation dialog before deleting
    await ElMessageBox.confirm('Bạn có chắc muốn xóa thương hiệu này?', 'Xác nhận', {
      type: 'warning'
    })
    // Use apiClient for the DELETE request
    await apiClient.delete(`/admin/brand/${id}`)
    ElMessage.success('Xóa thành công')
    await fetchBrands() // Refresh the list after deletion
    // If the deleted item was the one being edited, reset the form
    if (form.value.id === id) {
      resetForm();
    }
  } catch (err) {
    console.error('Lỗi khi xóa thương hiệu:', err);
    // Handle user cancellation or API errors
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.');
    } else if (err.response && err.response.data && err.response.data.message) {
      ElMessage.error(`Không thể xóa: ${err.response.data.message}`);
    } else {
      ElMessage.error('Không thể xóa');
    }
  }
}

// Fetch brands when the component is mounted
onMounted(() => {
  fetchBrands()
})
</script>

<style scoped>
.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 18px;
}
.form-actions {
  margin-top: 10px;
}
</style>
