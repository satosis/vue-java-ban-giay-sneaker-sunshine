<template>
  <div class="container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Brush /></el-icon>
          <span>Quản lý màu sắc</span>
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
            <el-form-item label="Tên màu sắc" prop="colorName">
              <el-input
                v-model="form.colorName"
                placeholder="Nhập tên màu sắc"
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

    <el-divider content-position="left">Danh sách màu</el-divider>

    <el-table :data="colors" style="margin-top: 10px" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="colorName" label="Tên màu sắc" />
      <el-table-column label="Hành động" width="200">
        <template #default="scope">
          <el-button
            type="primary"
            icon="Edit"
            size="small"
            @click="editColor(scope.row)"
          >Sửa</el-button>
          <el-button
            type="danger"
            icon="Delete"
            size="small"
            @click="deleteColor(scope.row.id)"
          >Xóa</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import apiClient from '@/utils/axiosInstance'; // Import the configured axios instance
import { ElMessage, ElMessageBox } from 'element-plus';
import { Brush, Edit, CirclePlus, RefreshRight } from '@element-plus/icons-vue';

const colors = ref([]);
const form = ref({ id: null, colorName: '' });
const isEditing = ref(false);
const formRef = ref();
const loading = ref(false);

const rules = {
  colorName: [
    { required: true, message: 'Tên màu không được để trống', trigger: 'blur' },
    { min: 2, message: 'Tên màu tối thiểu 2 ký tự', trigger: 'blur' },
    { max: 50, message: 'Tên màu tối đa 50 ký tự', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        const pattern = /^[\p{L}\d\s]+$/u;
        if (!pattern.test(value)) {
          callback(new Error('Tên màu không chứa ký tự đặc biệt'))
          return
        }
        const nameTrimmed = value.trim().toLowerCase()
        const duplicate = colors.value.some(
          (c) => c.colorName.trim().toLowerCase() === nameTrimmed && c.id !== form.value.id
        )
        if (duplicate) {
          callback(new Error('Tên màu đã tồn tại'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const fetchColors = async () => {
  try {
    loading.value = true;
    const res = await apiClient.get('/admin/color/hien-thi');
    colors.value = res.data;
  } catch (err) {
    console.error("Failed to fetch colors:", err);
    ElMessage.error('Không thể tải danh sách màu');
  } finally {
    loading.value = false;
  }
};


const resetForm = () => {
  form.value = { id: null, colorName: '' };
  isEditing.value = false;
  formRef.value?.resetFields();
};


const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;

    const confirmMessage = isEditing.value
      ? 'Bạn có chắc muốn cập nhật màu này?'
      : 'Bạn có chắc muốn thêm mới màu này?'

    try {
      await ElMessageBox.confirm(confirmMessage, 'Xác nhận', {
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        type: 'info'
      })

      loading.value = true

      if (isEditing.value) {
        await apiClient.put(`/admin/color/${form.value.id}`, null, {
          params: { name: form.value.colorName }
        })
        ElMessage.success('Cập nhật thành công')
      } else {
        await apiClient.post('/admin/color', null, {
          params: { name: form.value.colorName }
        })
        ElMessage.success('Thêm mới thành công')
      }

      await fetchColors()
      resetForm()
    } catch (err) {
      if (err !== 'cancel') {
        ElMessage.error('Lỗi khi lưu dữ liệu')
      }
    } finally {
      loading.value = false;
    }
  });
};

/**
 * Sets the form to edit mode with the selected color's data.
 * @param {object} color - The color object to be edited.
 */
const editColor = (color) => {
  form.value = { ...color };
  isEditing.value = true;
};

/**
 * Deletes a color after user confirmation.
 * @param {number} id - The ID of the color to delete.
 */
const deleteColor = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc muốn xóa màu này?', 'Xác nhận', {
      type: 'warning',
    });
    loading.value = true;
    // DELETE request from apiClient
    await apiClient.delete(`/admin/color/${id}`);
    ElMessage.success('Xóa thành công');
    await fetchColors();
  } catch (err) {
    if (err !== 'cancel') {
      console.error("Failed to delete color:", err);
      ElMessage.error('Không thể xóa');
    }
  } finally {
    loading.value = false;
  }
};

// Fetch colors when the component is mounted
onMounted(() => {
  fetchColors();
});
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
