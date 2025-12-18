<template>
  <div class="update-employee p-4">
    <el-card shadow="hover">
      <el-button type="info" icon="ArrowLeft" @click="goBack" class="mb-4"> Quay lại </el-button>
      <h2 class="mb-4">
        <el-icon><User /></el-icon> Cập nhật nhân viên
      </h2>

      <el-form :model="employee" label-width="120px">
        <el-form-item label="Họ tên">
          <el-input v-model="employee.employeeName" />
        </el-form-item>

        <el-form-item label="Tên đăng nhập">
          <el-input v-model="employee.username" />
        </el-form-item>

        <el-form-item label="Mật khẩu">
          <el-input v-model="employee.password" type="password" show-password />
        </el-form-item>

        <el-form-item label="Email">
          <el-input v-model="employee.email" />
        </el-form-item>

        <el-form-item label="Số điện thoại">
          <el-input v-model="employee.phone" />
        </el-form-item>

        <el-form-item label="Giới tính">
          <el-select v-model="employee.gender" placeholder="Chọn giới tính">
            <el-option label="Nam" :value="1" />
            <el-option label="Nữ" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="Ngày sinh">
          <el-date-picker
            v-model="employee.dateOfBirth"
            type="date"
            placeholder="Chọn ngày sinh"
            format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="Ngày vào làm">
          <el-date-picker
            v-model="employee.hireDate"
            type="date"
            placeholder="Chọn ngày vào làm"
            format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="Lương">
          <el-input
            v-model="employee.salary"
            placeholder="Nhập lương"
            :formatter="formatSalary"
            :parser="parseSalary"
          />
        </el-form-item>

        <el-form-item label="Quốc gia">
          <el-input v-model="employee.country" />
        </el-form-item>

        <el-form-item label="Tỉnh/TP">
          <el-input v-model="employee.province" />
        </el-form-item>

        <el-form-item label="Quận/Huyện">
          <el-input v-model="employee.district" />
        </el-form-item>

        <el-form-item label="Phường/Xã">
          <el-input v-model="employee.ward" />
        </el-form-item>

        <el-form-item label="Số nhà">
          <el-input v-model="employee.houseName" />
        </el-form-item>

        <el-form-item label="Vai trò">
          <el-select v-model="employee.role" placeholder="Chọn vai trò">
            <el-option label="Quản trị" :value="1" />
            <el-option label="Nhân viên" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="update">Cập nhật</el-button>
          <el-button @click="resetForm" type="warning">Đặt lại</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// Import your pre-configured API client
import apiClient from '@/utils/axiosInstance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ArrowLeft, Calendar, Money, Phone, Message, Lock, Male, Female, Location, Refresh, Check, Edit } from '@element-plus/icons-vue' // Added more relevant icons

const route = useRoute()
const router = useRouter()
const employeeId = route.params.id

const employee = ref({
  employeeName: '',
  username: '',
  password: '', // Note: It's generally not recommended to fetch/display passwords for security. If this is just a placeholder, that's fine.
  email: '',
  phone: '',
  gender: 1, // Defaulting to 1 (Male) if not fetched
  dateOfBirth: '',
  hireDate: '',
  salary: 0, // Initialize as number for consistent parsing
  country: 'Việt Nam', // Assuming default is Vietnam if not fetched
  province: '', // These address fields might need specific handling if they are nested objects
  district: '',
  ward: '',
  houseName: '',
  role: 0, // Defaulting to 0 (Employee) if not fetched
})

// To store the original data for reset functionality
const originalEmployee = ref({})

// Function to fetch employee data from the backend API
const fetchEmployee = async () => {
  try {
    // Use apiClient for the GET request
    const res = await apiClient.get(`/admin/employees/${employeeId}`)
    // Map response data to form fields
    employee.value = {
      ...res.data,
      // Convert date strings to Date objects for Element Plus date-picker
      dateOfBirth: res.data.dateOfBirth ? new Date(res.data.dateOfBirth) : '',
      hireDate: res.data.hireDate ? new Date(res.data.hireDate) : '',
      salary: res.data.salary || 0 // Ensure salary is a number
    }
    // Store a deep copy of the fetched data for reset
    originalEmployee.value = { ...employee.value }
    ElMessage.success('Tải thông tin nhân viên thành công.')
  } catch (error) {
    console.error('Lỗi tải dữ liệu nhân viên:', error)
    ElMessage.error('Không thể tải dữ liệu nhân viên.')
  }
}

// Formats salary value to Vietnamese currency format with dot separators
const formatSalary = (value) => {
  // Ensure value is treated as a string before manipulation for safety
  const stringValue = String(value || '');
  const numberValue = parseInt(stringValue.replace(/[^\d]/g, ''), 10);
  if (isNaN(numberValue)) return '';
  return numberValue.toLocaleString('vi-VN') + ' VND'; // Added currency symbol
}

// Parses formatted salary string back to a plain number
const parseSalary = (value) => {
  if (!value) return null;
  // Ensure value is treated as a string before using replace
  const stringValue = String(value);
  const parsed = parseInt(stringValue.replace(/[^\d]/g, ''), 10); // Removes dots and other non-digits
  return isNaN(parsed) ? null : parsed;
}

// Function to disable future dates in date pickers
const disableFutureDates = (time) => {
  return time.getTime() > Date.now();
};

// Function to handle the update operation
const update = async () => {
  try {
    // Show confirmation dialog before updating
    await ElMessageBox.confirm(
      'Bạn có chắc chắn muốn cập nhật thông tin nhân viên này?',
      'Xác nhận',
      {
        confirmButtonText: 'Xác nhận',
        cancelButtonText: 'Hủy',
        type: 'warning',
      },
    )

    // Prepare data to send, converting Date objects back to ISO strings if needed
    const dataToSend = {
      ...employee.value,
      dateOfBirth: employee.value.dateOfBirth ? new Date(employee.value.dateOfBirth).toISOString().split('T')[0] : null,
      hireDate: employee.value.hireDate ? new Date(employee.value.hireDate).toISOString().split('T')[0] : null,
      salary: parseSalary(employee.value.salary), // Ensure salary is parsed before sending
    };

    // Use apiClient for the PUT request
    await apiClient.put(`/admin/employees/${employeeId}`, dataToSend)
    ElMessage.success('Cập nhật nhân viên thành công!')
    router.push('/employee') // Navigate back to employee list
  } catch (error) {
    // Handle user cancellation or API errors
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('Đã hủy cập nhật.')
    } else {
      console.error('Lỗi khi cập nhật nhân viên:', error)
      // Display a more specific error message if available from the backend
      if (error.response && error.response.data && error.response.data.message) {
        ElMessage.error(`Cập nhật thất bại: ${error.response.data.message}`)
      } else {
        ElMessage.error('Cập nhật thất bại. Vui lòng thử lại.')
      }
    }
  }
}

// Function to reset the form to its original fetched state
const resetForm = () => {
  employee.value = { ...originalEmployee.value }
  ElMessage.success('Form đã được đặt lại.')
}

// Navigate back to the employee list page
const goBack = () => {
  router.push('/employee')
}

// Fetch employee data when the component is mounted
onMounted(fetchEmployee)
</script>
