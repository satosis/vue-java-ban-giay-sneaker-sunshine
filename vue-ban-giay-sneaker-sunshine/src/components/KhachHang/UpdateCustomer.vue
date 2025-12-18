<template>
  <div class="update-customer-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <el-button type="text" :icon="ArrowLeft" @click="goBack" class="back-button">
            Quay lại
          </el-button>
          <h2 class="title">Cập nhật khách hàng</h2>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" class="customer-form"
        label-position="left">
        <el-form-item label="Họ tên khách hàng" prop="customerName">
          <el-input v-model="form.customerName" :prefix-icon="Avatar" placeholder="Nhập họ tên" clearable />
        </el-form-item>

        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" :prefix-icon="Message" placeholder="Nhập email" clearable />
        </el-form-item>

        <el-form-item label="Số điện thoại" prop="phone">
          <el-input v-model="form.phone" :prefix-icon="Phone" placeholder="Nhập số điện thoại" clearable />
        </el-form-item>

        <el-form-item label="Giới tính" prop="gender">
          <el-select v-model="form.gender" :prefix-icon="Male" placeholder="Chọn giới tính" style="width: 100%">
            <el-option label="Nam" :value="1" :icon="Male" />
            <el-option label="Nữ" :value="0" :icon="Female" />
          </el-select>
        </el-form-item>

        <el-form-item label="Ngày sinh" prop="dateOfBirth">
          <el-date-picker v-model="form.dateOfBirth" :prefix-icon="Calendar" type="date" placeholder="Chọn ngày sinh"
            style="width: 100%" :disabled-date="disableFutureDates" />
        </el-form-item>

        <el-form-item class="action-buttons">
          <el-button type="primary" :icon="Check" @click="updateCustomer" :loading="submitting" round>
            Cập nhật
          </el-button>
          <el-button type="success" :icon="Location" @click="openAddressDialog" round>
            Quản lý địa chỉ
          </el-button>
          <el-button :icon="Refresh" @click="resetForm" round>Làm mới</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="addressDialogVisible" title="Quản lý địa chỉ khách hàng" width="900px"
      :close-on-click-modal="false" class="address-dialog">
      <div class="address-actions">
        <el-button type="primary" :icon="Plus" @click="openAddAddressForm" round>
          Thêm địa chỉ mới
        </el-button>
      </div>

      <el-table :data="addresses" border stripe v-loading="addressLoading"
        element-loading-text="Đang tải danh sách địa chỉ..." empty-text="Chưa có địa chỉ nào." class="address-table">
        <el-table-column prop="houseName" label="Số nhà, tên đường" min-width="150" />
        <el-table-column prop="wardName" label="Phường/Xã" min-width="120" />
        <el-table-column prop="districtName" label="Quận/Huyện" min-width="120" />
        <el-table-column prop="provinceName" label="Tỉnh/Thành phố" min-width="120" />
        <el-table-column label="Mặc định" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.defaultAddress ? 'success' : 'info'">
              {{ scope.row.defaultAddress ? 'Mặc định' : 'Không' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="200" fixed="right">
          <template #default="scope">
            <el-button type="info" :icon="Star" size="small" circle @click="setDefaultAddress(scope.row.id)"
              title="Đặt làm mặc định" v-if="!scope.row.defaultAddress" />
            <el-button type="primary" :icon="Edit" size="small" circle @click="editAddress(scope.row)" title="Sửa" />
            <el-button type="danger" :icon="Delete" size="small" circle @click="confirmDeleteAddress(scope.row.id)"
              title="Xóa" />
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="addressFormVisible" :title="isEditingAddress ? 'Sửa địa chỉ' : 'Thêm địa chỉ'" width="500px"
        append-to-body :close-on-click-modal="false">
        <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="120px"
          class="address-form">
          <el-form-item label="Tỉnh/Thành phố" prop="provinceCode">
            <el-select v-model="addressForm.provinceCode" placeholder="Chọn Tỉnh/Thành"
              @change="handleProvinceChangeForAddress" filterable style="width: 100%;">
              <el-option v-for="item in provinces" :key="item.ProvinceID" :label="item.ProvinceName"
                :value="item.ProvinceID" />
            </el-select>
          </el-form-item>

          <el-form-item label="Quận/Huyện" prop="districtCode">
            <el-select v-model="addressForm.districtCode" placeholder="Chọn Quận/Huyện" :disabled="!districts.length"
              @change="handleDistrictChangeForAddress" filterable style="width: 100%;">
              <el-option v-for="item in districts" :key="item.DistrictID" :label="item.DistrictName"
                :value="item.DistrictID" />
            </el-select>
          </el-form-item>

          <el-form-item label="Phường/Xã" prop="wardCode">
            <el-select v-model="addressForm.wardCode" placeholder="Chọn Phường/Xã" :disabled="!wards.length"
              @change="handleWardChangeForAddress" filterable style="width: 100%;">
              <el-option v-for="item in wards" :key="item.WardCode" :label="item.WardName" :value="item.WardCode" />
            </el-select>
          </el-form-item>

          <el-form-item label="Số nhà, tên đường" prop="houseNumber">
            <el-input v-model="addressForm.houseNumber" :prefix-icon="Location" placeholder="Nhập số nhà, tên đường"
              clearable />
          </el-form-item>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="addressFormVisible = false" round>Hủy</el-button>
            <el-button type="primary" :icon="Check" @click="saveAddress" :loading="addressSubmitting" round>
              Lưu
            </el-button>
          </span>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios'; // Keep axios for external GHN API calls
import apiClient from '@/utils/axiosInstance'; // Import your pre-configured API client
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  Avatar,
  Message,
  Phone,
  Male,
  Female,
  Calendar,
  Check,
  Refresh,
  Location,
  Plus,
  Edit,
  Delete,
  Star // Import the Star icon
} from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const customerId = route.params.id;
const formRef = ref(null);
const addressFormRef = ref(null);
const submitting = ref(false);
const addressSubmitting = ref(false);
const addressDialogVisible = ref(false);
const addressFormVisible = ref(false);
const isEditingAddress = ref(false);
const addresses = ref([]);
const addressLoading = ref(false);

const form = ref({
  customerName: '',
  email: '',
  phone: '',
  gender: null,
  dateOfBirth: ''
});

const addressForm = ref({
  id: null,
  provinceCode: null,
  provinceName: '',
  districtCode: null,
  districtName: '',
  wardCode: null,
  wardName: '',
  houseNumber: '',
  country: 'Việt Nam' // Assuming default country is Vietnam for addresses
});

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);

const GHN_TOKEN = '847c9bb7-6e42-11ee-a59f-a260851ba65c'; // Your GHN Token

// Validation Rules for Main Customer Form
const rules = ref({
  customerName: [
    { required: true, message: 'Vui lòng nhập họ tên', trigger: 'blur' },
    { min: 2, max: 100, message: 'Họ tên phải từ 2 đến 100 ký tự', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Vui lòng nhập email', trigger: 'blur' },
    { type: 'email', message: 'Email không hợp lệ', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: 'blur' },
    { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại phải là 10 hoặc 11 chữ số', trigger: 'blur' }
  ],
  gender: [{ required: true, message: 'Vui lòng chọn giới tính', trigger: 'change' }],
  dateOfBirth: [{ required: true, message: 'Vui lòng chọn ngày sinh', trigger: 'change' }]
});

// Validation Rules for Address Form
const addressRules = ref({
  provinceCode: [{ required: true, message: 'Vui lòng chọn Tỉnh/Thành phố', trigger: 'change' }],
  districtCode: [{ required: true, message: 'Vui lòng chọn Quận/Huyện', trigger: 'change' }],
  wardCode: [{ required: true, message: 'Vui lòng chọn Phường/Xã', trigger: 'change' }],
  houseNumber: [{ required: true, message: 'Vui lòng nhập số nhà, tên đường', trigger: 'blur' }]
});

const disableFutureDates = (time) => {
  return time.getTime() > Date.now();
};

// Fetch customer details using apiClient
const fetchCustomer = async () => {
  try {
    const res = await apiClient.get(`/admin/customers/${customerId}`);
    form.value = {
      ...res.data,
      dateOfBirth: res.data.dateOfBirth ? new Date(res.data.dateOfBirth) : null
    };
  } catch (err) {
    console.error('Lỗi tải thông tin khách hàng:', err);
    ElMessage.error('Không thể tải thông tin khách hàng!');
  }
};

// Fetch customer addresses using apiClient
const fetchAddresses = async () => {
  addressLoading.value = true;
  try {
    const res = await apiClient.get(`/admin/customers/${customerId}/addresses`);
    addresses.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải danh sách địa chỉ:', err);
    ElMessage.error('Không thể tải danh sách địa chỉ!');
    addresses.value = [];
  } finally {
    addressLoading.value = false;
  }
};

// Update customer details using apiClient
const updateCustomer = async () => {
  try {
    await formRef.value.validate();
    submitting.value = true;
    await apiClient.put(`/admin/customers/${customerId}`, {
      ...form.value,
      dateOfBirth: form.value.dateOfBirth ? form.value.dateOfBirth.toISOString() : null
    });
    ElMessage.success('Cập nhật khách hàng thành công!');
    router.push('/customer');
  } catch (err) {
    console.error('Lỗi khi cập nhật khách hàng:', err);
    ElMessage.error('Lỗi khi cập nhật khách hàng!');
  } finally {
    submitting.value = false;
  }
};

const openAddressDialog = () => {
  addressDialogVisible.value = true;
  loadProvincesForAddress(); // Load provinces when dialog opens
  fetchAddresses();
};

const openAddAddressForm = () => {
  isEditingAddress.value = false;
  addressForm.value = {
    id: null,
    provinceCode: null,
    provinceName: '',
    districtCode: null,
    districtName: '',
    wardCode: null,
    wardName: '',
    houseNumber: '',
    country: 'Việt Nam'
  };
  districts.value = [];
  wards.value = [];
  addressFormVisible.value = true;
  // No need to load provinces again here, as they are loaded when addressDialogVisible is true
};

const editAddress = async (address) => {
  isEditingAddress.value = true;
  addressForm.value = { ...address };

  // Set selected province and load districts
  addressForm.value.provinceCode = address.provinceId; // Assuming backend uses provinceId
  addressForm.value.provinceName = address.provinceName;
  await loadDistrictsForAddress(); // Load districts based on selected province

  // Set selected district and load wards
  addressForm.value.districtCode = address.districtId; // Assuming backend uses districtId
  addressForm.value.districtName = address.districtName;
  await loadWardsForAddress(); // Load wards based on selected district

  addressForm.value.wardCode = address.wardCode;
  addressForm.value.wardName = address.wardName;

  addressFormVisible.value = true;
};

// Function to load provinces/cities for addresses using direct axios
const loadProvincesForAddress = async () => {
  try {
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

// Function to load districts/counties for addresses using direct axios
const loadDistrictsForAddress = async () => {
  addressForm.value.districtCode = null;
  addressForm.value.districtName = '';
  addressForm.value.wardCode = null;
  addressForm.value.wardName = '';
  districts.value = [];
  wards.value = [];

  if (!addressForm.value.provinceCode) return;

  try {
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/district',
      {
        headers: { Token: GHN_TOKEN },
        params: { province_id: addressForm.value.provinceCode },
      }
    );
    districts.value = res.data.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách quận/huyện:', error);
    ElMessage.error('Không thể tải danh sách quận/huyện.');
  }
};

// Function to load wards/communes for addresses using direct axios
const loadWardsForAddress = async () => {
  addressForm.value.wardCode = null;
  addressForm.value.wardName = '';
  wards.value = [];

  if (!addressForm.value.districtCode) return;

  try {
    const res = await axios.get(
      'https://online-gateway.ghn.vn/shiip/public-api/master-data/ward',
      {
        headers: { Token: GHN_TOKEN },
        params: { district_id: addressForm.value.districtCode },
      }
    );
    wards.value = res.data.data;
  } catch (error) {
    console.error('Lỗi khi tải danh sách phường/xã:', error);
    ElMessage.error('Không thể tải danh sách phường/xã.');
  }
};

// Handle province selection change in address form
const handleProvinceChangeForAddress = () => {
  const selected = provinces.value.find(p => p.ProvinceID === addressForm.value.provinceCode);
  addressForm.value.provinceName = selected?.ProvinceName || '';
  loadDistrictsForAddress();
};

// Handle district selection change in address form
const handleDistrictChangeForAddress = () => {
  const selected = districts.value.find(d => d.DistrictID === addressForm.value.districtCode);
  addressForm.value.districtName = selected?.DistrictName || '';
  loadWardsForAddress();
};

// Handle ward selection change in address form
const handleWardChangeForAddress = () => {
  const selected = wards.value.find(w => w.WardCode === addressForm.value.wardCode);
  addressForm.value.wardName = selected?.WardName || '';
};

// Save (add/update) address using apiClient
const saveAddress = async () => {
  try {
    await addressFormRef.value.validate();
    addressSubmitting.value = true;

    const addressDataToSend = {
      provinceCode: addressForm.value.provinceCode,
      provinceName: addressForm.value.provinceName,
      districtCode: addressForm.value.districtCode,
      districtName: addressForm.value.districtName,
      wardCode: addressForm.value.wardCode,
      wardName: addressForm.value.wardName,
      houseName: addressForm.value.houseNumber, // Ensure this matches backend field name
      country: addressForm.value.country,
    };

    if (isEditingAddress.value) {
      await apiClient.put(
        `/admin/customers/${customerId}/addresses/${addressForm.value.id}`,
        addressDataToSend
      );
      ElMessage.success('Cập nhật địa chỉ thành công!');
    } else {
      await apiClient.post(
        `/admin/customers/${customerId}/addresses`,
        addressDataToSend
      );
      ElMessage.success('Thêm địa chỉ thành công!');
    }
    addressFormVisible.value = false;
    fetchAddresses();
  } catch (err) {
    console.error('Lỗi khi lưu địa chỉ:', err);
    // More specific error handling based on backend response if available
    if (err.response && err.response.data && err.response.data.message) {
      ElMessage.error(`Lỗi: ${err.response.data.message}`);
    } else {
      ElMessage.error('Lỗi khi lưu địa chỉ! Vui lòng kiểm tra lại thông tin.');
    }
  } finally {
    addressSubmitting.value = false;
  }
};

// Confirm and delete address using apiClient
const confirmDeleteAddress = async (addressId) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa địa chỉ này?', 'Cảnh báo', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning'
    });
    await apiClient.delete(`/admin/customers/${customerId}/addresses/${addressId}`);
    ElMessage.success('Xóa địa chỉ thành công!');
    fetchAddresses();
  } catch (err) {
    if (err === 'cancel' || err === 'close') {
      ElMessage.info('Đã hủy thao tác xóa.');
    } else {
      console.error('Lỗi khi xóa địa chỉ:', err);
      ElMessage.error('Lỗi khi xóa địa chỉ!');
    }
  }
};

// Set default address using apiClient
const setDefaultAddress = async (addressId) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn đặt địa chỉ này làm mặc định?', 'Cảnh báo', {
      confirmButtonText: 'Xác nhận',
      cancelButtonText: 'Hủy',
      type: 'warning'
    });
    await apiClient.put(`/admin/customers/${customerId}/addresses/${addressId}/set-default`);
    ElMessage.success('Đặt địa chỉ mặc định thành công!');
    fetchAddresses(); // Refresh addresses to show the updated default
  } catch (err) {
    console.error('Lỗi khi đặt địa chỉ mặc định:', err);
    ElMessage.error('Không thể đặt địa chỉ mặc định!');
  }
};

const resetForm = () => {
  formRef.value.resetFields();
  fetchCustomer();
};

const goBack = () => {
  router.push('/customer'); // Assuming '/customer' is the correct route for the customer list
};

onMounted(() => {
  fetchCustomer();
});
</script>
<style scoped>
.update-customer-container {
  max-width: 700px;
  margin: 40px auto;
  padding: 0 20px;
}

.box-card {
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  background: linear-gradient(145deg, #ffffff, #f9fafb);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  border-bottom: 1px solid #ebeef5;
}

.back-button {
  font-size: 16px;
  color: #606266;
  padding: 8px 12px;
  transition: color 0.3s;
}

.back-button:hover {
  color: #409eff;
}

.title {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.customer-form {
  padding: 20px;
}

.el-form-item {
  margin-bottom: 22px;
}

.el-form-item__label {
  font-weight: 500;
  color: #606266;
  font-size: 15px;
}

.el-input,
.el-select,
.el-date-picker {
  --el-input-border-radius: 8px;
  --el-input-focus-border: #409eff;
}

.el-input__wrapper {
  background-color: #f9fafb;
  transition: all 0.3s;
}

.el-input__wrapper:hover {
  background-color: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.el-button {
  --el-button-border-radius: 8px;
  font-weight: 500;
  padding: 12px 20px;
  transition: all 0.3s;
}

.el-button--primary {
  --el-button-bg-color: #409eff;
  --el-button-hover-bg-color: #66b1ff;
}

.el-button--success {
  --el-button-bg-color: #67c23a;
  --el-button-hover-bg-color: #85ce61;
}

.el-button--info {
  /* Added for the new default button */
  --el-button-bg-color: #909399;
  --el-button-hover-bg-color: #a6a9ad;
}

.el-button--default {
  --el-button-bg-color: #f9fafb;
  --el-button-border-color: #dcdfe6;
  --el-button-hover-bg-color: #ffffff;
  --el-button-hover-border-color: #c6e2ff;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.address-dialog .el-dialog__body {
  padding: 20px;
}

.address-actions {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.address-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.address-table .el-table__header-wrapper th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: bold;
}

.address-table .el-table__cell {
  padding: 10px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Styles for the default address tag */
.el-tag--success {
  background-color: #e1f3d8;
  border-color: #e1f3d8;
  color: #67c23a;
}

.el-tag--info {
  background-color: #edf2fc;
  border-color: #edf2fc;
  color: #909399;
}


@media (max-width: 768px) {
  .update-customer-container {
    padding: 0 10px;
    margin: 20px auto;
  }

  .box-card {
    border-radius: 8px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .title {
    font-size: 20px;
  }

  .el-form-item {
    --el-form-label-width: 100px;
  }

  .el-form-item__label {
    font-size: 14px;
  }

  .el-button {
    padding: 10px 16px;
    font-size: 14px;
  }

  .address-dialog {
    width: 90%;
  }

  .address-table {
    font-size: 12px;
  }
}
</style>
