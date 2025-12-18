<template>
  <div class="add-employee p-4">
    <el-card shadow="hover">
      <!-- Nút quay lại -->
      <el-button type="info" icon="ArrowLeft" @click="goBack" class="mb-4"> Quay lại </el-button>

      <h2 class="mb-4">
        <el-icon><User /></el-icon> Thêm nhân viên mới
      </h2>

      <el-form :model="form" :rules="rules" ref="employeeForm" label-width="150px">
        <el-form-item label="Họ tên" prop="employeeName">
          <el-input v-model="form.employeeName" />
        </el-form-item>

        <el-form-item label="Tên đăng nhập" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>

        <el-form-item label="Mật khẩu" prop="password">
  <el-input
    v-model="form.password"
    type="password"
    show-password
  />
</el-form-item>


        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>

        <el-form-item label="Điện thoại" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>

        <el-form-item label="Giới tính" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">Nam</el-radio>
            <el-radio :label="0">Nữ</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Ngày sinh" prop="dateOfBirth">
          <el-date-picker
            v-model="form.dateOfBirth"
            type="date"
            placeholder="Chọn ngày sinh"
            format="DD/MM/YYYY"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="Ngày tuyển dụng" prop="hireDate">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            placeholder="Chọn ngày tuyển dụng"
            format="DD/MM/YYYY"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="Lương" prop="salary">
          <el-input
            v-model="form.salary"
            type="text"
            placeholder="Nhập lương"
            :formatter="formatSalary"
            :parser="parseSalary"
          />
        </el-form-item>

        <el-form-item label="Quốc gia" prop="country">
          <el-input v-model="form.country" />
        </el-form-item>

        <el-form-item label="Tỉnh/Thành phố" prop="province">
          <el-input v-model="form.province" />
        </el-form-item>

        <el-form-item label="Quận/Huyện" prop="district">
          <el-input v-model="form.district" />
        </el-form-item>

        <el-form-item label="Phường/Xã" prop="ward">
          <el-input v-model="form.ward" />
        </el-form-item>

        <el-form-item label="Số nhà" prop="houseName">
          <el-input v-model="form.houseName" />
        </el-form-item>

        <el-form-item label="Vai trò" prop="role">
          <el-select v-model="form.role" placeholder="Chọn vai trò">
            <el-option :label="'Quản trị'" :value="1" />
            <el-option :label="'Nhân viên'" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm">Thêm nhân viên</el-button>
          <el-button @click="resetForm">Reset</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
// Import your pre-configured API client
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ArrowLeft, Calendar, Money, Phone, Message, Lock, Male, Female, Location, CirclePlus } from '@element-plus/icons-vue' // Added more relevant icons
import { useRouter } from 'vue-router'

const router = useRouter()

const form = ref({
  employeeName: '',
  username: '',
  password: '',
  email: '',
  phone: '',
  gender: 1,
  dateOfBirth: '',
  country: 'Việt Nam', // Assuming default for new employee is Vietnam
  province: '',
  district: '',
  ward: '',
  houseName: '',
  role: 0, // Default role for a new employee might be 'Nhân viên' (0)
  hireDate: new Date().toISOString().split('T')[0], // Default to today's date
  salary: 0,
})

const employeeForm = ref(null) // Ref for the Element Plus form component

const goBack = () => {
  router.push('/employee')
}

// Formats salary value to Vietnamese currency format
const formatSalary = (value) => {
  // Ensure value is treated as a string before manipulation for safety
  const stringValue = String(value || '');
  // Parse out only digits for formatting
  const numberValue = parseInt(stringValue.replace(/[^\d]/g, ''), 10);
  if (isNaN(numberValue)) return '';
  return numberValue.toLocaleString('vi-VN') + ' VND'; // Added currency symbol
}

// Parses formatted salary back to a plain number
const parseSalary = (value) => {
  if (!value) return null;
  // Ensure value is treated as a string before using replace
  const stringValue = String(value);
  const parsed = parseInt(stringValue.replace(/[^\d]/g, ''), 10);
  return isNaN(parsed) ? null : parsed;
}

// Disable future dates for Date of Birth and Hire Date pickers
const disableFutureDates = (time) => {
  return time.getTime() > Date.now();
};

// Validation Rules for the form
const rules = {
  employeeName: [{ required: true, message: 'Vui lòng nhập họ tên', trigger: 'blur' }],
  username: [
    { required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' },
    { min: 3, max: 50, message: 'Tên đăng nhập phải từ 3 đến 50 ký tự', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
    { min: 6, message: 'Mật khẩu phải có ít nhất 6 ký tự', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: 'blur' },
    { pattern: /^(0|\+84)[3|5|7|8|9][0-9]{8}$/, message: 'Số điện thoại không hợp lệ', trigger: 'blur' }
  ],
  role: [{ required: true, message: 'Vui lòng chọn vai trò', trigger: 'change' }],
  gender: [{ required: true, message: 'Vui lòng chọn giới tính', trigger: 'change' }],
  dateOfBirth: [{ required: true, message: 'Vui lòng chọn ngày sinh', trigger: 'change' }],
  hireDate: [{ required: true, message: 'Vui lòng chọn ngày tuyển dụng', trigger: 'change' }],
  salary: [
    { required: true, message: 'Vui lòng nhập lương', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        const parsed = parseSalary(value); // Use the helper to parse
        if (parsed === null || parsed < 0) {
          callback(new Error('Lương phải là số và lớn hơn hoặc bằng 0'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
}

// Submits the form data to the backend
const submitForm = () => {
  employeeForm.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm('Bạn có chắc chắn muốn thêm nhân viên này?', 'Xác nhận', {
          confirmButtonText: 'Xác nhận',
          cancelButtonText: 'Hủy',
          type: 'warning',
        });

        // Prepare data for sending, ensuring salary is a number
        const dataToSend = {
          ...form.value,
          salary: parseSalary(form.value.salary), // Ensure salary is parsed before sending
          // Convert Date objects to ISO strings if necessary, though Element Plus date-picker with value-format usually handles this
          dateOfBirth: form.value.dateOfBirth ? new Date(form.value.dateOfBirth).toISOString().split('T')[0] : null,
          hireDate: form.value.hireDate ? new Date(form.value.hireDate).toISOString().split('T')[0] : null,
        };

        // --- Debugging 403 Forbidden Error ---
        console.log('Sending data to backend:', dataToSend);
        const token = localStorage.getItem('token');
        console.log('Token from localStorage:', token ? 'Token exists (length: ' + token.length + ')' : 'Token is MISSING or empty');
        // --- End Debugging ---

        // Use apiClient for the POST request
        await apiClient.post('/admin/employees', dataToSend);
        ElMessage.success('Thêm nhân viên thành công!');
        resetForm();
        router.push('/employee'); // Navigate back to employee list after success
      } catch (error) {
        console.error('Lỗi khi thêm nhân viên:', error);
        if (error.response && error.response.data && error.response.data.message) {
          ElMessage.error(`Lỗi: ${error.response.data.message}`);
        } else if (error === 'cancel' || error === 'close') {
          ElMessage.info('Đã hủy thao tác thêm nhân viên.');
        } else {
          ElMessage.error('Có lỗi xảy ra khi thêm nhân viên. Vui lòng kiểm tra lại thông tin.');
        }
      }
    } else {
      ElMessage.error('Vui lòng điền đầy đủ và đúng thông tin!');
    }
  })
}

// Resets the form fields and validation status
const resetForm = () => {
  employeeForm.value.resetFields();
  // Reset other non-form-item bound properties or specific defaults
  form.value.salary = 0;
  form.value.hireDate = new Date().toISOString().split('T')[0];
  form.value.gender = 1;
  form.value.role = 0;
}
</script>

<style scoped>
.add-employee {
  max-width: 800px;
  margin: auto;
}
</style>
