<template>
  <el-card class="box-card">
    <template #header>
      <span>Quản lý vật liệu</span>
    </template>

    <!-- Form Thêm / Sửa -->
    <div class="form-section">
      <el-form :model="form" ref="formRef" label-width="120px">
        <el-form-item
          label="Tên vật liệu"
          :rules="[{ required: true, message: 'Tên không được để trống', trigger: 'blur' }]"
        >
          <el-input v-model="form.name" placeholder="Nhập tên vật liệu..." />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit">{{
            isEditing ? 'Cập nhật' : 'Thêm mới'
          }}</el-button>
          <el-button @click="resetForm">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Bảng hiển thị vật liệu -->
    <el-table :data="materials" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="materialCode" label="Mã vật liệu" />
      <el-table-column prop="materialName" label="Tên vật liệu" />
      <el-table-column prop="status" label="Trạng thái">
        <template #default="scope">
          <span :style="{ color: scope.row.status === 1 ? 'green' : 'red' }">
            {{ scope.row.status === 1 ? 'Hoạt động' : 'Không hoạt động' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="Ngày tạo">
        <template #default="scope">
          {{ formatDateTime(scope.row.createdDate) }}
        </template>
      </el-table-column>

      <el-table-column label="Hành động" width="200">
        <template #default="scope">
          <el-button size="small" @click="editMaterial(scope.row)">Sửa</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(scope.row.id)">Xóa</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// Import your pre-configured API client
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, RefreshRight, CirclePlus, Brush } from '@element-plus/icons-vue' // Ensure all used icons are imported

const materials = ref([])
const form = ref({ id: null, name: '' }) // 'name' for the form input
const isEditing = ref(false)
const formRef = ref(null)
const loading = ref(false) // Added loading state for better UX

// You might want to add validation rules for 'name' here, similar to other components
// const rules = {
//   name: [
//     { required: true, message: 'Tên vật liệu không được để trống', trigger: 'blur' },
//     { min: 2, message: 'Tên vật liệu tối thiểu 2 ký tự', trigger: 'blur' },
//     { max: 50, message: 'Tên vật liệu tối đa 50 ký tự', trigger: 'blur' },
//     // Add custom validator for special characters if needed
//   ]
// }

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  // Check if date is valid before formatting
  if (isNaN(date.getTime())) {
    return dateStr;
  }
  return date.toLocaleString('vi-VN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

const fetchMaterials = async () => {
  loading.value = true // Set loading state to true
  try {
    // Use apiClient for the GET request
    const response = await apiClient.get('/admin/material/hien-thi')
    materials.value = response.data
    ElMessage.success('Tải danh sách vật liệu thành công.')
  } catch (error) {
    console.error('Lỗi khi tải danh sách vật liệu:', error)
    ElMessage.error('Lỗi khi tải danh sách vật liệu.')
    materials.value = [] // Clear data on error
  } finally {
    loading.value = false // Set loading state to false
  }
}

const resetForm = () => {
  form.value = { id: null, name: '' }
  isEditing.value = false
  // formRef.value?.clearValidate() // Uncomment if you add rules and formRef to <el-form>
  ElMessage.info('Form đã được đặt lại.');
}

const handleSubmit = async () => {
  // If you add rules and use formRef, uncomment the validation block below
  // if (formRef.value) {
  //   const valid = await formRef.value.validate();
  //   if (!valid) {
  //     ElMessage.error('Vui lòng kiểm tra lại thông tin form.');
  //     return;
  //   }
  // }

  if (!form.value.name || form.value.name.trim() === '') {
    ElMessage.warning('Vui lòng nhập tên vật liệu')
    return
  }

  // Optional: Check for duplicate names (case-insensitive, exclude current item if editing)
  const nameTrimmed = form.value.name.trim().toLowerCase();
  const existed = materials.value.some(
    (m) => m.materialName?.trim().toLowerCase() === nameTrimmed && m.id !== form.value.id
  );
  if (existed) {
    ElMessage.warning('Tên vật liệu đã tồn tại');
    return;
  }

  loading.value = true // Set loading state for submission
  try {
    if (isEditing.value) {
      // Use apiClient for the PUT request
      await apiClient.put(`/admin/material/${form.value.id}`, null, {
        params: { name: form.value.name },
      })
      ElMessage.success('Cập nhật thành công')
    } else {
      // Use ElMessageBox.confirm for new entry confirmation
      await ElMessageBox.confirm('Bạn có chắc chắn muốn thêm mới vật liệu?', 'Xác nhận', {
        confirmButtonText: 'Thêm',
        cancelButtonText: 'Hủy',
        type: 'info',
      });
      // Use apiClient for the POST request
      await apiClient.post('/admin/material', null, {
        params: { name: form.value.name },
      })
      ElMessage.success('Thêm mới thành công')
    }
    await fetchMaterials() // Refresh the list after successful operation
    resetForm() // Clear the form
  } catch (error) {
    console.error('Lỗi khi lưu dữ liệu vật liệu:', error)
    // Handle user cancellation (from ElMessageBox.confirm) or API errors
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác.')
    } else if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(`Lỗi: ${error.response.data.message}`);
    } else {
      ElMessage.error('Có lỗi xảy ra khi lưu dữ liệu.')
    }
  } finally {
    loading.value = false // Reset loading state
  }
}

const editMaterial = (material) => {
  // Map fetched 'materialName' to form's 'name'
  form.value = { id: material.id, name: material.materialName }
  isEditing.value = true
  ElMessage.info(`Đang chỉnh sửa: ${material.materialName}`);
}

const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa vật liệu này?', 'Cảnh báo', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning',
    })
    // Use apiClient for the DELETE request
    await apiClient.delete(`/admin/material/${id}`)
    ElMessage.success('Xóa thành công')
    await fetchMaterials() // Refresh the list
    // If the deleted item was the one being edited, reset the form
    if (form.value.id === id) {
      resetForm();
    }
  } catch (error) {
    console.error('Lỗi khi xóa vật liệu:', error)
    // Handle user cancellation or API errors
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.')
    } else if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(`Lỗi: ${error.response.data.message}`);
    } else {
      ElMessage.error('Lỗi khi xóa vật liệu.')
    }
  }
}

onMounted(fetchMaterials)
</script>

<style scoped>
.box-card {
  max-width: 800px;
  margin: 30px auto;
}
.form-section {
  margin-bottom: 20px;
}
</style>
