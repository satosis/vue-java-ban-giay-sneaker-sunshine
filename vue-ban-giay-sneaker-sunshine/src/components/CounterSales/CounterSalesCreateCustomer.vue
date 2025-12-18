<template>
  <div>
    <el-dialog
      v-model="dialogVisible"
      title="Thông tin Khách hàng Mới"
      width="500px"
      :before-close="handleClose"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="customerForm"
        :rules="formRules"
        label-position="top"
        @submit.prevent="submitForm"
        novalidate
      >
        <el-alert v-if="apiError" :title="apiError" type="error" show-icon class="mb-4" :closable="false" />

        <el-form-item label="Số điện thoại" prop="phone">
          <el-input
            v-model="customerForm.phone"
            type="tel"
            placeholder="Nhập số điện thoại"
            size="large"
            autocomplete="tel"
            clearable
          />
        </el-form-item>

        <el-form-item label="Tên khách hàng" prop="name">
          <el-input
            v-model="customerForm.name"
            placeholder="Nhập tên khách hàng"
            size="large"
            autocomplete="name"
            clearable
          />
        </el-form-item>

        <el-form-item label="Email" prop="email">
          <el-input
            v-model="customerForm.email"
            type="email"
            placeholder="Nhập email (tuỳ chọn)"
            size="large"
            autocomplete="email"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog" :disabled="loading">Hủy</el-button>
          <el-button type="primary" @click="submitForm" :loading="loading">
            {{ loading ? 'Đang xử lý...' : 'Lưu Khách Hàng' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="confirmationDialogVisible"
      title="Khách hàng đã được tạo"
      width="400px"
      center
      destroy-on-close
    >
      <div class="text-success d-flex align-items-center mb-3">
        <el-icon class="me-2"><SuccessFilled /></el-icon>
        <span class="fw-semibold">Tạo khách hàng thành công!</span>
      </div>
      <p class="mb-1"><strong>Số điện thoại:</strong> {{ createdCustomer?.phone }}</p>
      <p class="mb-1"><strong>Tên khách hàng:</strong> {{ createdCustomer?.customerName || '(Chưa cung cấp tên)' }}</p>
      <p class="mb-3"><strong>Email:</strong> {{ createdCustomer?.email || '(Chưa cung cấp email)' }}</p>
      <el-button type="success" class="w-100" @click="selectCreatedCustomer">
        <el-icon class="me-2"><User /></el-icon>
        Sử dụng khách hàng này
      </el-button>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, defineExpose, defineEmits } from 'vue';
import { ElMessage } from 'element-plus';
import { SuccessFilled, User } from '@element-plus/icons-vue';
import apiClient from '../../utils/axiosInstance.js';

const dialogVisible = ref(false);
const confirmationDialogVisible = ref(false);
const loading = ref(false);
const apiError = ref('');
const formRef = ref(null);
const createdCustomer = ref(null);

const customerForm = reactive({
  phone: '',
  name: '',
  email: '',
});

// --- VALIDATION ---
const validatePhoneNumber = (rule, value, callback) => {
  if (!value) return callback(new Error('Số điện thoại không được để trống.'));
  const phoneRegex = /^(0[35789])\d{8}$/;
  if (!phoneRegex.test(value)) {
    return callback(new Error('SĐT không hợp lệ (10 số, bắt đầu bằng 03, 05, 07, 08, 09)'));
  }
  callback();
};

const validateName = (rule, value, callback) => {
  if (!value || value.trim().length === 0) {
    return callback(new Error('Tên khách hàng không được để trống.'));
  }
  callback();
};

const formRules = reactive({
  phone: [{ validator: validatePhoneNumber, trigger: 'blur' }],
  name: [{ validator: validateName, trigger: 'blur' }],
  email: [
    {
      type: 'email',
      message: 'Email không hợp lệ.',
      trigger: 'blur',
    },
  ],
});

const emit = defineEmits(['created', 'select-customer']);

function openDialog() {
  apiError.value = '';
  createdCustomer.value = null;
  confirmationDialogVisible.value = false;
  if (formRef.value) formRef.value.resetFields();
  dialogVisible.value = true;
}

function closeDialog() {
  dialogVisible.value = false;
}

function handleClose(done) {
  if (!loading.value) done();
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    loading.value = true;
    apiError.value = '';

    try {
      const params = new URLSearchParams();
      params.append('phone', customerForm.phone);
      params.append('name', customerForm.name.trim());
      params.append('email', customerForm.email?.trim() || '');

      const res = await apiClient.post(`/admin/counter-sales/quick-create-customer?${params.toString()}`);
      createdCustomer.value = res.data;
      emit('created', res.data);
      dialogVisible.value = false;
      confirmationDialogVisible.value = true;
      ElMessage.success('Tạo khách hàng thành công!');
    } catch (e) {
      apiError.value = e.response?.data?.message || e.message || 'Đã xảy ra lỗi.';
    } finally {
      loading.value = false;
    }
  });
}

function selectCreatedCustomer() {
  emit('select-customer', createdCustomer.value);
  confirmationDialogVisible.value = false;
  customerForm.phone = '';
  customerForm.name = '';
  customerForm.email = '';
}

defineExpose({ openDialog });
</script>

<style scoped>
.mb-4 {
  margin-bottom: 1.5rem;
}
.mb-1 {
  margin-bottom: 0.25rem;
}
.mb-3 {
  margin-bottom: 1rem;
}
.me-2 {
  margin-right: 0.5rem;
}
.w-100 {
  width: 100%;
}
.d-flex {
  display: flex;
}
.align-items-center {
  align-items: center;
}
.text-success {
  color: var(--el-color-success);
}
.fw-semibold {
  font-weight: 600;
}
</style>
