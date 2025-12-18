<template>
  <div class="customer-form-container">
    <el-card class="box-card">
      <div class="card-header">
        <el-button type="info" :icon="ArrowLeft" @click="goBack" class="back-button">
          Quay lại
        </el-button>
        <h2 class="form-title">Thêm khách hàng mới</h2>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="submitForm" class="customer-form" ref="customerFormRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Họ tên" prop="customerName">
              <el-input v-model="form.customerName" placeholder="Nhập họ tên" :prefix-icon="User" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tên đăng nhập" prop="username">
              <el-input v-model="form.username" placeholder="Tên đăng nhập" :prefix-icon="UserFilled" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Mật khẩu" prop="password">
              <el-input v-model="form.password" type="password" placeholder="Mật khẩu" :prefix-icon="Lock" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Số điện thoại" prop="phone">
              <el-input v-model="form.phone" placeholder="Số điện thoại" :prefix-icon="Phone" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Email" prop="email">
              <el-input v-model="form.email" type="email" placeholder="Email" :prefix-icon="Message" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngày sinh" prop="dateOfBirth">
              <el-date-picker
                v-model="form.dateOfBirth"
                type="date"
                placeholder="Chọn ngày sinh"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
                :prefix-icon="Calendar"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Giới tính" prop="gender">
              <el-select v-model="form.gender" placeholder="Chọn giới tính" style="width: 100%;">
                <el-option label="Nam" :value="1" />
                <el-option label="Nữ" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tỉnh/Thành phố" prop="provinceCode">
              <el-select v-model="form.provinceCode" placeholder="Chọn Tỉnh/Thành" @change="handleProvinceChange" filterable style="width: 100%;">
                <el-option
                  v-for="item in provinces"
                  :key="item.ProvinceID"
                  :label="item.ProvinceName"
                  :value="item.ProvinceID"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Quận/Huyện" prop="districtCode">
              <el-select
                v-model="form.districtCode"
                placeholder="Chọn Quận/Huyện"
                :disabled="!districts.length"
                @change="handleDistrictChange"
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="item in districts"
                  :key="item.DistrictID"
                  :label="item.DistrictName"
                  :value="item.DistrictID"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Phường/Xã" prop="wardCode">
              <el-select
                v-model="form.wardCode"
                placeholder="Chọn Phường/Xã"
                :disabled="!wards.length"
                @change="handleWardChange"
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="item in wards"
                  :key="item.WardCode"
                  :label="item.WardName"
                  :value="item.WardCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Số nhà, tên đường" prop="houseName">
          <el-input v-model="form.houseName" placeholder="Số nhà, tên đường" :prefix-icon="HomeFilled" />
        </el-form-item>

        <el-form-item class="submit-button-container">
          <el-button type="success" native-type="submit" :icon="CirclePlus" size="large">
            Thêm khách hàng
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios'; // Keep axios for external GHN API calls
import apiClient from '@/utils/axiosInstance'; // Import your pre-configured API client
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
// Import icons from Element Plus Icons Vue
import { ArrowLeft, User, UserFilled, Lock, Phone, Message, Calendar, HomeFilled, CirclePlus } from '@element-plus/icons-vue';

const router = useRouter();
const customerFormRef = ref(null); // Add ref for the form

const goBack = () => {
  router.push({ name: 'CustomerList' }); // Ensure 'CustomerList' is the correct route name for your customer list page
};

const form = ref({
  customerName: '',
  username: '',
  password: '',
  phone: '',
  dateOfBirth: null,
  country: 'Việt Nam', // Default to Vietnam
  email: '',
  gender: 1, // Default to Male (assuming 1 for male, 0 for female or other)
  provinceCode: null, // Changed to null to keep placeholder
  provinceName: '',
  districtCode: null,
  districtName: '',
  wardCode: null,
  wardName: '',
  houseName: '',
});

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);

const GHN_TOKEN = '847c9bb7-6e42-11ee-a59f-a260851ba65c'; // Your GHN Token

// Function to load provinces/cities from GHN
const loadProvinces = async () => {
  try {
    // Use direct axios for GHN API, as apiClient has a specific baseURL
    const res = await axios.post(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/province',
      {},
      { headers: { Token: GHN_TOKEN } }
    );
    provinces.value = res.data.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách tỉnh/thành phố:', error);
    ElMessage.error('Không thể tải danh sách tỉnh/thành phố.');
  }
};

// Function to load districts/counties from GHN
const loadDistricts = async () => {
  // Reset district and ward values when province changes
  form.value.districtCode = null;
  form.value.districtName = '';
  form.value.wardCode = null;
  form.value.wardName = '';
  districts.value = [];
  wards.value = [];

  if (!form.value.provinceCode) return; // Do not load if no province is selected

  try {
    // Use direct axios for GHN API
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/district',
      {
        headers: { Token: GHN_TOKEN },
        params: { province_id: form.value.provinceCode },
      }
    );
    districts.value = res.data.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách quận/huyện:', error);
    ElMessage.error('Không thể tải danh sách quận/huyện.');
  }
};

// Function to load wards/communes from GHN
const loadWards = async () => {
  // Reset ward values when district changes
  form.value.wardCode = null;
  form.value.wardName = '';
  wards.value = [];

  if (!form.value.districtCode) return; // Do not load if no district is selected

  try {
    // Use direct axios for GHN API
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/ward',
      {
        headers: { Token: GHN_TOKEN },
        params: { district_id: form.value.districtCode },
      }
    );
    wards.value = res.data.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách phường/xã:', error);
    ElMessage.error('Không thể tải danh sách phường/xã.');
  }
};

// Handle province selection change
const handleProvinceChange = () => {
  const selected = provinces.value.find(p => p.ProvinceID === form.value.provinceCode);
  form.value.provinceName = selected?.ProvinceName || '';
  loadDistricts();
};

// Handle district selection change
const handleDistrictChange = () => {
  const selected = districts.value.find(d => d.DistrictID === form.value.districtCode);
  form.value.districtName = selected?.DistrictName || '';
  loadWards();
};

// Handle ward selection change
const handleWardChange = () => {
  const selected = wards.value.find(w => w.WardCode === form.value.wardCode);
  form.value.wardName = selected?.WardName || '';
};

// Submit form
const submitForm = async () => {
  try {
    // Validate the form first
    await customerFormRef.value.validate();

    // Format dateOfBirth before sending
    let formattedDateOfBirth = null;
    if (form.value.dateOfBirth instanceof Date) {
      const year = form.value.dateOfBirth.getFullYear();
      const month = String(form.value.dateOfBirth.getMonth() + 1).padStart(2, '0');
      const day = String(form.value.dateOfBirth.getDate()).padStart(2, '0');
      formattedDateOfBirth = `${year}-${month}-${day}`;
    } else if (typeof form.value.dateOfBirth === 'string' && form.value.dateOfBirth) {
      // If it's already a string and not empty, assume it's correctly formatted
      formattedDateOfBirth = form.value.dateOfBirth;
    }

    // Ensure address fields are sent correctly according to API structure
    const customerData = {
      customerName: form.value.customerName,
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone,
      email: form.value.email,
      dateOfBirth: formattedDateOfBirth, // Use the formatted value
      gender: form.value.gender,
      provinceCode: form.value.provinceCode || null,
      provinceName: form.value.provinceName || '',
      districtCode: form.value.districtCode || null,
      districtName: form.value.districtName || '',
      wardCode: form.value.wardCode || '',
      wardName: form.value.wardName || '',
      houseName: form.value.houseName || '',
      country: form.value.country || 'Việt Nam',
    };

    console.log("Dữ liệu gửi đi:", customerData);

    // Use apiClient for your backend API calls
    const res = await apiClient.post('/admin/customers', customerData);
    ElMessage.success('Thêm khách hàng thành công!');
    console.log('Khách hàng đã thêm:', res.data);
    router.push({ name: 'CustomerList' }); // Redirect to customer list page
  } catch (error) {
    console.error('Lỗi khi thêm khách hàng:', error);
    if (error.response && error.response.data && error.response.data.message) {
      ElMessage.error(`Lỗi: ${error.response.data.message}`);
    } else if (error.name === 'ValidationError') { // If you use Element Plus validation
      ElMessage.error('Vui lòng điền đầy đủ và đúng thông tin!');
    } else {
      ElMessage.error('Có lỗi xảy ra khi thêm khách hàng. Vui lòng kiểm tra lại thông tin.');
    }
  }
};

// Optional: Function to reset form (can be called after successful submission)
const resetForm = () => {
  customerFormRef.value.resetFields(); // Reset fields and validation
  form.value.provinceCode = null;
  form.value.provinceName = '';
  form.value.districtCode = null;
  form.value.districtName = '';
  form.value.wardCode = null;
  form.value.wardName = '';
  form.value.houseName = '';
  districts.value = [];
  wards.value = [];
};

// Form validation rules
const rules = ref({
  customerName: [{ required: true, message: 'Vui lòng nhập họ tên khách hàng', trigger: 'blur' }],
  username: [
    { required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' },
    { min: 3, message: 'Tên đăng nhập phải có ít nhất 3 ký tự', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' },
    { min: 6, message: 'Mật khẩu phải có ít nhất 6 ký tự', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: 'blur' },
    { pattern: /^(0|\+84)[3|5|7|8|9][0-9]{8}$/, message: 'Số điện thoại không hợp lệ', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không đúng định dạng', trigger: ['blur', 'change'] }
  ],
  dateOfBirth: [{ required: true, message: 'Vui lòng chọn ngày sinh', trigger: 'change' }],
  gender: [{ required: true, message: 'Vui lòng chọn giới tính', trigger: 'change' }],
  provinceCode: [{ required: true, message: 'Vui lòng chọn Tỉnh/Thành phố', trigger: 'change' }],
  districtCode: [{ required: true, message: 'Vui lòng chọn Quận/Huyện', trigger: 'change' }],
  wardCode: [{ required: true, message: 'Vui lòng chọn Phường/Xã', trigger: 'change' }],
  houseName: [{ required: true, message: 'Vui lòng nhập số nhà, tên đường', trigger: 'blur' }]
});


onMounted(() => {
  loadProvinces();
});
</script>

<style scoped>
.customer-form-container {
  max-width: 800px; /* Tăng độ rộng tối đa để form 2 cột đẹp hơn */
  margin: 40px auto; /* Căn giữa và tạo khoảng cách */
  padding: 20px;
}

.box-card {
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); /* Thêm đổ bóng nhẹ */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5; /* Đường kẻ dưới tiêu đề */
}

.form-title {
  margin: 0;
  color: #303133; /* Màu chữ đậm hơn */
  font-size: 26px; /* Kích thước chữ lớn hơn */
  font-weight: bold;
  text-align: center;
  flex-grow: 1; /* Để tiêu đề chiếm hết không gian còn lại */
  padding-right: 120px; /* bù lại cho button quay lại */
}

.back-button {
  min-width: 100px; /* Đảm bảo nút có kích thước nhất quán */
  padding: 10px 15px;
}

.customer-form .el-form-item {
  margin-bottom: 22px; /* Khoảng cách giữa các form item */
}

/* Tùy chỉnh input, select, date-picker để đồng bộ hơn */
.customer-form .el-input,
.customer-form .el-select,
.customer-form .el-date-picker {
  width: 100%;
}

.customer-form .el-input__inner,
.customer-form .el-select__wrapper {
  height: 40px; /* Chiều cao tiêu chuẩn cho input */
  line-height: 40px;
}

.customer-form .el-form-item__label {
  font-weight: 600; /* Nhãn đậm hơn */
  color: #606266;
  margin-bottom: 5px; /* Khoảng cách giữa nhãn và input */
}

.submit-button-container {
  text-align: center; /* Căn giữa nút submit */
  margin-top: 30px; /* Khoảng cách trên nút submit */
}

.submit-button-container .el-button {
  width: 200px; /* Độ rộng cố định cho nút submit */
  padding: 12px 20px; /* Kích thước nút lớn hơn */
  font-size: 16px;
  font-weight: bold;
  border-radius: 8px; /* Bo tròn nút */
}

/* Hiệu ứng focus cho input */
.el-input__inner:focus {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.el-select__wrapper.is-focused {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}
</style>