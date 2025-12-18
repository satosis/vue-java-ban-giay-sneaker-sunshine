<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- Tiêu đề và nút thêm -->
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-semibold">Quản lý danh mục</h2>
        <el-button type="primary" @click="openDialog">
          <el-icon><Plus /></el-icon> Thêm danh mục
        </el-button>
      </div>

      <!-- Bảng danh sách -->
      <el-table :data="categories" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="categoryName" label="Tên danh mục" />
        <el-table-column prop="categoryCode" label="Mã danh mục" />
        <el-table-column prop="description" label="Mô tả" />
        <el-table-column prop="status" label="Trạng thái" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? 'Hoạt động' : 'Đã xóa' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="editCategory(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button size="small" type="danger" @click="confirmDelete(row.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Form thêm/sửa -->
      <el-dialog :title="isEdit ? 'Cập nhật danh mục' : 'Thêm danh mục'" v-model="dialogVisible" width="500px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
          <el-form-item label="Tên danh mục" prop="categoryName">
            <el-input v-model="form.categoryName" />
          </el-form-item>
          <el-form-item label="Mô tả">
            <el-input type="textarea" v-model="form.description" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">Hủy</el-button>
          <el-button type="primary" @click="submitForm">Lưu</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// Import your pre-configured API client
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue' // Ensure all used icons are imported

// State variables for the component
const categories = ref([])
const dialogVisible = ref(false) // Controls the visibility of the add/edit dialog
const isEdit = ref(false) // Flag to determine if we are editing or adding
const editingId = ref(null) // Stores the ID of the category being edited
const formRef = ref() // Reference to the Element Plus form component for validation

// Form data for adding/editing a category
const form = ref({
  categoryName: '',
  description: ''
})

// Validation rules for the category form
const rules = {
  categoryName: [
    { required: true, message: 'Vui lòng nhập tên danh mục', trigger: 'blur' },
    { min: 3, message: 'Tên danh mục phải có ít nhất 3 ký tự', trigger: 'blur' },
    {
      // Custom validator to check for duplicate category names (case-insensitive, excluding current item if editing)
      validator: (_, value, callback) => {
        const trimmed = value?.trim().toLowerCase()
        const exists = categories.value.some(c =>
          c.categoryName?.trim().toLowerCase() === trimmed &&
          (!isEdit.value || c.id !== editingId.value) // If editing, exclude the current item from duplication check
        )
        if (exists) {
          callback(new Error('Tên danh mục đã tồn tại'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  description: [
    { max: 255, message: 'Mô tả không được vượt quá 255 ký tự', trigger: 'blur' }
  ]
}

// --- API Interactions ---

// Fetches the list of categories from the backend
const fetchCategories = async () => {
  try {
    // Use apiClient for GET request
    const res = await apiClient.get('/admin/categories/hien-thi')
    categories.value = res.data
    ElMessage.success('Tải danh sách danh mục thành công')
  } catch (err) {
    console.error('Lỗi khi tải danh sách danh mục:', err);
    ElMessage.error('Không thể tải danh sách danh mục');
  }
}

// Opens the dialog for adding a new category, resetting the form
const openDialog = () => {
  isEdit.value = false
  form.value = {
    categoryName: '',
    description: ''
  }
  dialogVisible.value = true
  // Reset validation errors if any from previous interactions
  formRef.value?.resetFields(); 
}

// Opens the dialog for editing an existing category, populating the form with data
const editCategory = (category) => {
  isEdit.value = true
  editingId.value = category.id
  form.value = {
    categoryName: category.categoryName,
    description: category.description
  }
  dialogVisible.value = true
  // Reset validation errors if any from previous interactions
  formRef.value?.clearValidate();
}

// Helper function to format a Date object to ISO string
const formatDateToISOString = (date) => date.toISOString()

// Handles form submission for both adding and updating categories
const submitForm = async () => {
  // Validate the form
  const valid = await formRef.value.validate()
  if (!valid) {
    ElMessage.error('Vui lòng kiểm tra lại thông tin form.');
    return;
  }

  // Get current date for createdDate/updatedDate
  const now = formatDateToISOString(new Date())
  const payload = {
    ...form.value,
    updatedDate: now
  }
  // Only set createdDate if adding a new category
  if (!isEdit.value) {
    payload.createdDate = now
  }

  try {
    // Show confirmation dialog before proceeding with API call
    await ElMessageBox.confirm(
      isEdit.value
        ? 'Bạn có chắc chắn muốn cập nhật danh mục này?'
        : 'Bạn có chắc chắn muốn thêm mới danh mục này?',
      'Xác nhận',
      {
        confirmButtonText: isEdit.value ? 'Cập nhật' : 'Thêm',
        cancelButtonText: 'Hủy',
        type: 'info'
      }
    );

    // Perform API call based on whether it's an edit or add operation
    if (isEdit.value) {
      // Use apiClient for PUT request
      await apiClient.put(`/admin/categories/${editingId.value}`, payload)
      ElMessage.success('Cập nhật thành công')
    } else {
      // Use apiClient for POST request
      await apiClient.post('/admin/categories', payload)
      ElMessage.success('Thêm mới thành công')
    }

    // Close dialog and refresh category list
    dialogVisible.value = false
    fetchCategories()
  } catch (err) {
    // Handle user cancellation (from ElMessageBox) or API errors
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Đã hủy thao tác.');
    } else {
      console.error('Lỗi khi lưu dữ liệu danh mục:', err);
      // Display backend error message if available
      if (err.response && err.response.data && err.response.data.message) {
        ElMessage.error(`Lưu thất bại: ${err.response.data.message}`);
      } else {
        ElMessage.error('Lưu thất bại');
      }
    }
  }
}

// Confirms and deletes a category
const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa danh mục này?', 'Cảnh báo', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning'
    });

    // Use apiClient for DELETE request
    await apiClient.delete(`/admin/categories/${id}`)
    ElMessage.success('Đã xóa thành công')
    fetchCategories() // Refresh the list
  } catch (err) {
    // Handle user cancellation or API errors
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.');
    } else {
      console.error('Lỗi khi xóa danh mục:', err);
      if (err.response && err.response.data && err.response.data.message) {
        ElMessage.error(`Xóa thất bại: ${err.response.data.message}`);
      } else {
        ElMessage.error('Xóa thất bại');
      }
    }
  }
}

// Fetch categories when the component is mounted
onMounted(fetchCategories)
</script>

<style scoped>
.text-xl {
  font-size: 1.25rem;
}
</style>
