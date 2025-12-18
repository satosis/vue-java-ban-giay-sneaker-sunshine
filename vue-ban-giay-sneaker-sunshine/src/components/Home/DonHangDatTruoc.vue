<template>
  <div class="pre-order-management-container">
    <h1 class="page-title">Quản lý Đơn hàng Đặt trước</h1>

    <el-card class="table-card">
      <div v-if="isLoading" class="loading-state">
        <el-skeleton :rows="5" animated />
        <p>Đang tải dữ liệu...</p>
      </div>
      <div v-else-if="error" class="error-state">
        <el-alert :title="error" type="error" show-icon closable />
      </div>
      <el-empty v-else-if="preOrders.length === 0" description="Không có đơn hàng đặt trước nào." />

      <el-table
        v-else
        :data="preOrders"
        style="width: 100%"
        stripe
        border
        v-loading="isLoading"
        element-loading-text="Đang tải..."
      >
        <el-table-column type="index" label="STT" width="60" align="center" />
        <el-table-column prop="productName" label="Sản phẩm" width="200" sortable>
          <template #default="scope">
            <strong>{{ scope.row.productName }}</strong> <br>
            <small>Màu: {{ scope.row.colorName }}</small> <br>
            <small>Size: {{ scope.row.sizeName }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="SL" width="80" align="center" sortable />
        <el-table-column prop="customerName" label="Khách hàng" width="180" sortable>
          <template #default="scope">
            {{ scope.row.customerName }} <br>
            <small>{{ scope.row.phone }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Ngày đặt" width="120" sortable>
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="100" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" :icon="View" @click="viewPreOrderDetail(scope.row.id)">Chi tiết</el-button>
          </template>
        </el-table-column>
      </el-table>

    </el-card>

    <el-dialog
      v-model="detailDialogVisible"
      title="Chi tiết Đơn hàng Đặt trước"
      width="600px"
    >
      <div v-if="selectedPreOrderDetail">
        <p><strong>ID Đặt trước:</strong> {{ selectedPreOrderDetail.id }}</p>
        <p><strong>Tên sản phẩm:</strong> {{ selectedPreOrderDetail.productName }}</p>
        <p><strong>Màu sắc:</strong> {{ selectedPreOrderDetail.colorName }}</p>
        <p><strong>Kích thước:</strong> {{ selectedPreOrderDetail.sizeName }}</p>
        <p><strong>Số lượng:</strong> {{ selectedPreOrderDetail.quantity }}</p>
        <p><strong>Tên khách hàng:</strong> {{ selectedPreOrderDetail.customerName }}</p>
        <p><strong>Số điện thoại:</strong> {{ selectedPreOrderDetail.phone }}</p>
        <p><strong>Email:</strong> {{ selectedPreOrderDetail.email }}</p>
        <p><strong>Ngày đặt:</strong> {{ formatDate(selectedPreOrderDetail.createdAt) }}</p>
        </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">Đóng</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import apiClient from '@/utils/axiosInstance'; // Sử dụng apiClient của bạn
import { ElMessage } from 'element-plus'; // Đã bỏ ElMessageBox vì không còn cập nhật trạng thái
import { View } from '@element-plus/icons-vue';

// --- STATE QUẢN LÝ DỮ LIỆU & TRẠNG THÁI ---
const preOrders = ref([]);
const isLoading = ref(false);
const error = ref(null);

// Dialog cập nhật trạng thái đã bị xóa các ref liên quan
// const editStatusDialogVisible = ref(false);
// const currentPreOrder = ref({});
// const newStatus = ref('');

// Dialog chi tiết
const detailDialogVisible = ref(false);
const selectedPreOrderDetail = ref(null);

// --- LIFECYCLE HOOKS ---
onMounted(() => {
  fetchPreOrders();
});

// --- HÀM XỬ LÝ DỮ LIỆU ---
async function fetchPreOrders() {
  isLoading.value = true;
  error.value = null;
  try {
    const response = await apiClient.get('/admin/pre-orders/get-all');

    if (response.data && Array.isArray(response.data)) {
      preOrders.value = response.data;
    } else {
      throw new Error('Định dạng dữ liệu API không đúng hoặc không có dữ liệu.');
    }
  } catch (err) {
    console.error('Lỗi khi tải đơn hàng đặt trước:', err);
    error.value = 'Không thể tải danh sách đơn hàng đặt trước. Vui lòng thử lại sau.';
    preOrders.value = [];
  } finally {
    isLoading.value = false;
  }
}

// --- HÀM HÀNH ĐỘNG TRÊN BẢNG ---
// Hàm openEditStatusDialog và confirmStatusUpdate đã bị xóa

function viewPreOrderDetail(id) {
  const detail = preOrders.value.find(order => order.id === id);
  if (detail) {
    selectedPreOrderDetail.value = { ...detail };
    detailDialogVisible.value = true;
  } else {
    ElMessage.error('Không tìm thấy chi tiết đơn hàng.');
  }
}

// --- HÀM HỖ TRỢ HIỂN THỊ ---
// Hàm getStatusType và getStatusText đã bị xóa

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const options = { year: 'numeric', month: '2-digit', day: '2-digit' };
  return new Date(dateString).toLocaleDateString('vi-VN', options);
};

</script>

<style scoped>
.pre-order-management-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 25px;
  text-align: center;
}

.table-card {
  padding: 20px;
}

.loading-state, .error-state {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #666;
}

.el-table {
  font-size: 14px;
}

/* El-tag styles (nếu còn sử dụng ở đâu đó, thì giữ lại, không thì xóa) */
.el-tag {
  font-weight: 600;
}

/* Custom styles for smaller font in product/customer columns */
.el-table__column .cell small {
  font-size: 12px;
  color: #777;
}

/* Align buttons in actions column */
.el-table__column .el-button {
  margin: 2px;
}
</style>
