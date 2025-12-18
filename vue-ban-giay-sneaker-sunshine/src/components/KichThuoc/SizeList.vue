<template>
  <el-card class="box-card">
    <template #header>
      <span>Quản lý kích thước</span>
    </template>

    <!-- Form Thêm / Sửa -->
    <div class="form-section">
      <el-form :model="form" ref="formRef" label-width="120px">
        <el-form-item
          label="Tên kích thước"
          :rules="[{ required: true, message: 'Tên không được để trống', trigger: 'blur' }]"
        >
          <el-input v-model="form.name" placeholder="Nhập tên kích thước..." />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit">{{
              isEditing ? 'Cập nhật' : 'Thêm mới'
            }}</el-button>
          <el-button @click="resetForm">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Bảng hiển thị kích thước -->
    <el-table :data="sizes" style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="sizeCode" label="Mã kích thước" />
      <el-table-column prop="sizeName" label="Tên kích thước" />
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
          <el-button size="small" @click="editSize(scope.row)">Sửa</el-button>
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

const sizes = ref([])
const form = ref({ id: null, name: '' }) // 'name' for the form input
const isEditing = ref(false)
const formRef = ref(null)
const loading = ref(false)

const rules = {
  name: [
    { required: true, message: 'Tên kích thước không được để trống', trigger: 'blur' },
    { min: 1, message: 'Tên kích thước tối thiểu 1 ký tự', trigger: 'blur' },
    { max: 20, message: 'Tên kích thước tối đa 20 ký tự', trigger: 'blur' }, // Common size limits
    {
      validator: (_, value, callback) => {
        const pattern = /^[\p{L}\d\s.+-]+$/u
        if (!pattern.test(value)) {
          callback(new Error('Tên kích thước không chứa ký tự đặc biệt hoặc định dạng không hợp lệ'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

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

const fetchSizes = async () => {
  loading.value = true // Set loading state to true
  try {
    // Use apiClient for the GET request
    const response = await apiClient.get('/admin/size/hien-thi')
    sizes.value = response.data
    ElMessage.success('Tải dữ liệu kích thước thành công.')
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu kích thước:', error)
    ElMessage.error('Lỗi khi tải dữ liệu kích thước.')
    sizes.value = [] // Clear data on error
  } finally {
    loading.value = false // Set loading state to false
  }
}

const resetForm = () => {
  form.value = { id: null, name: '' }
  isEditing.value = false
  formRef.value?.resetFields() // Safely reset form fields and validation status
  ElMessage.info('Form đã được đặt lại.');
}

const handleSubmit = async () => {
  // Validate the form using Element Plus's built-in validation
  const valid = await formRef.value.validate()
  if (!valid) {
    ElMessage.error('Vui lòng kiểm tra lại thông tin form.');
    return;
  }

  const nameTrimmed = form.value.name.trim().toLowerCase()
  // Check for existing size name, excluding the current size if editing
  const existed = sizes.value.some(
    (s) => s.sizeName.trim().toLowerCase() === nameTrimmed && s.id !== form.value.id
  )
  if (existed) {
    ElMessage.warning('Tên kích thước đã tồn tại');
    return;
  }

  loading.value = true // Set loading state for submission
  try {
    if (isEditing.value) {
      await ElMessageBox.confirm('Bạn có chắc chắn muốn cập nhật kích thước này?', 'Xác nhận', {
        confirmButtonText: 'Cập nhật',
        cancelButtonText: 'Hủy',
        type: 'info',
      })

      await axios.put(`http://localhost:8080/api/admin/size/${form.value.id}`, null, {
        params: { name: form.value.name },
      })
      ElMessage.success('Cập nhật thành công')
    } else {
      await ElMessageBox.confirm('Bạn có chắc chắn muốn thêm mới kích thước?', 'Xác nhận', {
        confirmButtonText: 'Thêm',
        cancelButtonText: 'Hủy',
        type: 'info',
      });
      // Use apiClient for the POST request
      await apiClient.post('/admin/size', null, {
        params: { name: form.value.name },
      });
      ElMessage.success('Thêm mới thành công');
    }
    await fetchSizes(); // Refresh the list after successful operation
    resetForm(); // Clear the form
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('Có lỗi xảy ra')
    } else {
      ElMessage.info('Đã hủy thao tác')
    }
  }
}


const editSize = (size) => {
  // Map fetched 'sizeName' to form's 'name'
  form.value = { id: size.id, name: size.sizeName }
  isEditing.value = true
  ElMessage.info(`Đang chỉnh sửa: ${size.sizeName}`);
}

const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa kích thước này?', 'Xác nhận', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning',
    });
    // Use apiClient for the DELETE request
    await apiClient.delete(`/admin/size/${id}`);
    ElMessage.success('Đã xóa thành công');
    await fetchSizes(); // Refresh the list
    // If the deleted item was the one being edited, reset the form
    if (form.value.id === id) {
      resetForm();
    }
  } catch (error) {
    console.error('Lỗi khi xóa kích thước:', error);
    // Handle user cancellation or API errors
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.');
    } else if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(`Lỗi: ${error.response.data.message}`);
    } else {
      ElMessage.error('Lỗi khi xóa kích thước.');
    }
  }
}

onMounted(fetchSizes)
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
