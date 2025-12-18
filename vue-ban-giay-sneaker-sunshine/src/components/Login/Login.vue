<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>Đăng Nhập Hệ Thống</span>
        </div>
      </template>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="100px"
        @submit.prevent="handleLogin"
        v-loading="loading"
        element-loading-text="Đang xử lý..."
      >
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
          class="error-alert"
        />
        
        <el-form-item label="Tên đăng nhập" prop="username">
          <el-input v-model="loginForm.username" placeholder="Nhập tên đăng nhập" />
        </el-form-item>
        
        <el-form-item label="Mật khẩu" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            show-password
            placeholder="Nhập mật khẩu"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" native-type="submit" :disabled="loading">
            Đăng Nhập
          </el-button>
          <el-button @click="resetForm">Làm mới</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();

const loginFormRef = ref(null);
const loading = ref(false);
const errorMessage = ref('');

const loginForm = reactive({
  username: '',
  password: '',
});

const loginRules = reactive({
  username: [
    { required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
  ],
});

const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      errorMessage.value = '';

      try {
        const response = await axios.post('http://localhost:8080/api/auth/login', {
          username: loginForm.username,
          password: loginForm.password,
        });

        const { token, employeeName } = response.data;

        localStorage.setItem('token', token);
        localStorage.setItem('employeeName', employeeName);

        console.log('Token đã lưu:', localStorage.getItem('token'));
        console.log('Tên nhân viên:', localStorage.getItem('employeeName'));

        ElMessage({
          message: 'Đăng nhập thành công!',
          type: 'success',
        });

        router.push({ name: 'Home' });

      } catch (error) {
        const message = error.response?.data?.message || 'Đã xảy ra lỗi trong quá trình đăng nhập.';
        errorMessage.value = message;

        ElMessage({
          message,
          type: 'error',
        });
      } finally {
        loading.value = false;
      }
    }
  });
};

const resetForm = () => {
  if (!loginFormRef.value) return;
  loginFormRef.value.resetFields();
  errorMessage.value = '';
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f2f6fc;
}

.login-card {
  width: 500px;
}

.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}

.error-alert {
  margin-bottom: 20px;
}
</style>
