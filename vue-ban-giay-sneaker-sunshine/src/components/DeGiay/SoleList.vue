<template>
  <el-card class="box-card" v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>Quản lý loại đế</span>
        <div class="header-actions">
          <el-button :icon="RefreshRight" @click="fetchSoles" :disabled="loading">Tải lại</el-button>
        </div>
      </div>
    </template>

    <!-- ===== Form Thêm / Sửa ===== -->
    <div class="form-section">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        status-icon
      >
        <el-form-item label="Tên loại đế" prop="name">
          <el-input
            v-model="form.name"
            placeholder="Nhập tên loại đế..."
            clearable
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="loading"
            @click="handleSubmit"
          >
            {{ isEditing ? 'Cập nhật' : 'Thêm mới' }}
          </el-button>
          <el-button :disabled="loading" @click="resetForm">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ===== Bảng hiển thị loại đế ===== -->
    <el-table
      :data="soles"
      style="width: 100%; margin-top: 12px"
      :empty-text="loading ? 'Đang tải...' : 'Không có dữ liệu'"
      border
      size="small"
    >
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="soleCode" label="Mã loại đế" min-width="140" />
      <el-table-column prop="soleName" label="Tên loại đế" min-width="200" />
      <el-table-column prop="status" label="Trạng thái" width="140">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? 'Hoạt động' : 'Không hoạt động' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Ngày tạo" min-width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.createdDate) }}
        </template>
      </el-table-column>
      <el-table-column label="Hành động" width="220" fixed="right">
        <template #default="scope">
          <el-button
            size="small"
            :icon="Edit"
            @click="editSole(scope.row)"
            :disabled="loading"
          >
            Sửa
          </el-button>
          <el-button
            size="small"
            type="danger"
            :icon="Delete"
            @click="confirmDelete(scope.row.id)"
            :disabled="loading"
          >
            Xóa
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, RefreshRight } from '@element-plus/icons-vue'

/** ===== State ===== */
const loading = ref(false)
const soles = ref([])

const form = ref({
  id: null,
  name: '',
})
const isEditing = ref(false)
const formRef = ref(null)

/** ===== Validation rules ===== */
const rules = {
  name: [
    { required: true, message: 'Tên không được để trống', trigger: 'blur' },
    { min: 2, message: 'Tên phải có ít nhất 2 ký tự', trigger: 'blur' },
  ],
}

/** ===== Utils ===== */
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return dateStr
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

/** ===== API ===== */
const fetchSoles = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/admin/sole/hien-thi')
    soles.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu loại đế:', error)
    ElMessage.error('Lỗi khi tải dữ liệu loại đế.')
    soles.value = []
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  try {
    // 1) Validate
    await formRef.value.validate()

    // 2) Check duplicate (case-insensitive), bỏ qua bản ghi đang chỉnh sửa
    const nameTrimmed = (form.value.name || '').trim().toLowerCase()
    const existed = soles.value.some(
      (s) => (s.soleName?.trim().toLowerCase() === nameTrimmed) && s.id !== form.value.id
    )
    if (existed) {
      ElMessage.warning('Tên loại đế đã tồn tại')
      return
    }

    // 3) Confirm
    const actionText = isEditing.value ? 'cập nhật' : 'thêm mới'
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn ${actionText} loại đế này?`,
      'Xác nhận',
      {
        confirmButtonText: isEditing.value ? 'Cập nhật' : 'Thêm',
        cancelButtonText: 'Hủy',
        type: isEditing.value ? 'warning' : 'info',
      }
    )
    // Nếu bấm Hủy/đóng, confirm sẽ ném lỗi 'cancel' hoặc 'close' và nhảy xuống catch.

    loading.value = true

    // 4) Call API
    if (isEditing.value) {
      // PUT cập nhật theo id, backend nhận name qua query param
      await apiClient.put(`/admin/sole/${form.value.id}`, null, {
        params: { name: form.value.name },
      })
      ElMessage.success('Cập nhật thành công')
    } else {
      // POST thêm mới
      await apiClient.post('/admin/sole', null, {
        params: { name: form.value.name },
      })
      ElMessage.success('Thêm mới thành công')
    }

    // 5) Refresh & reset
    await fetchSoles()
    resetForm()
  } catch (err) {
    // Người dùng hủy confirm hoặc đóng dialog
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Đã hủy thao tác')
      return
    }
    console.error(err)
    ElMessage.error('Lỗi khi lưu dữ liệu')
  } finally {
    loading.value = false
  }
}

const editSole = (sole) => {
  form.value = {
    id: sole.id,
    name: sole.soleName || '',
  }
  isEditing.value = true
  ElMessage.info(`Đang chỉnh sửa: ${sole.soleName}`)
}

const resetForm = () => {
  form.value = { id: null, name: '' }
  isEditing.value = false
  formRef.value?.clearValidate()
}

const confirmDelete = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa?', 'Xác nhận', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning',
    })
    loading.value = true
    await apiClient.delete(`/admin/sole/${id}`)
    ElMessage.success('Đã xóa thành công')
    await fetchSoles()
  } catch (err) {
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Thao tác đã bị hủy')
      return
    }
    console.error(err)
    ElMessage.error('Lỗi khi xóa')
  } finally {
    loading.value = false
  }
}

/** ===== Lifecycle ===== */
onMounted(fetchSoles)
</script>

<style scoped>
.box-card {
  max-width: 980px;
  margin: 28px auto;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.form-section {
  margin-bottom: 12px;
}
</style>
