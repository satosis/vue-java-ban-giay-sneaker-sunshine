<template>
  <el-container class="fullscreen-container">
    <el-card shadow="never" class="fullscreen-card">
      <template #header>
        <div class="d-flex justify-content-between align-items-center">
          <span>Quản lý hóa đơn chờ</span>
          <el-button type="primary" :icon="Plus" @click="createInvoice" :loading="isCreating">
            Tạo hóa đơn mới
          </el-button>
        </div>
      </template>

      <!-- The table now has a fixed height of 100%, which makes it fill the card's body and become scrollable -->
      <el-table :data="invoices" stripe v-loading="isLoading" height="100%" style="width: 100%">
        <el-table-column prop="invoiceCode" label="Mã hóa đơn" width="180" />
        <el-table-column label="Ngày tạo" width="200">
          <template #default="{ row }">
            {{ formatDate(row.createdDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="employeeName" label="Nhân viên" />
        <el-table-column prop="customerName" label="Khách hàng">
           <template #default="{ row }">
            <el-tag v-if="row.customerName" type="success">{{ row.customerName }}</el-tag>
            <el-tag v-else type="info">Khách lẻ</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Hành động" width="120" align="center">
          <template #default="{ row }">
            <el-button type="success" plain size="small" @click="viewInvoiceDetails(row.id)">
              Chi tiết
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="Không có hóa đơn chờ nào." />
        </template>
      </el-table>

      <!-- Pagination is moved to the card's footer -->
      <template #footer>
        <div class="d-flex justify-content-center" v-if="pagination.totalPages > 1">
            <el-pagination
              background
              layout="prev, pager, next, jumper, ->, total"
              :total="pagination.totalElements"
              :page-size="pagination.pageSize"
              :current-page="pagination.currentPage"
              @current-change="changePage"
            />
        </div>
      </template>
    </el-card>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
// KHÔNG import axios trực tiếp ở đây nữa

// THAY ĐỔI: Import instance apiClient đã được cấu hình sẵn
// Hãy đảm bảo đường dẫn này chính xác với cấu trúc project của bạn.
// Dựa trên các file trước, có thể nó sẽ là './axiosInstance.js' nếu ở cùng thư mục.
import apiClient from '../../utils/axiosInstance.js';

import { useRouter } from 'vue-router';
import { ElMessage, ElTable, ElTableColumn, ElButton, ElPagination, ElCard, ElContainer, ElEmpty, ElTag } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';

// State variables
const invoices = ref([]);
const isLoading = ref(true);
const isCreating = ref(false);

const pagination = reactive({
  currentPage: 1, // 1-based page for UI
  totalPages: 0,
  totalElements: 0,
  pageSize: 100,
  hasPrevious: false,
  hasNext: false,
});

const employeeId = 1; // Giữ lại nếu bạn vẫn cần tham số này
const router = useRouter();

// XÓA DÒNG NÀY: const token = localStorage.getItem('jwtToken');
// Interceptor trong apiClient sẽ tự động xử lý việc này.

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  return new Date(dateStr).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const fetchInvoices = async () => {
  isLoading.value = true;
  try {
    // THAY ĐỔI: Sử dụng apiClient và xóa URL/headers thủ công
    const response = await apiClient.get('/admin/counter-sales/invoices', {
      params: {
        status: 0,
        page: pagination.currentPage - 1,
        size: pagination.pageSize,
      },
    });
    const data = response.data;
    invoices.value = data.content;
    pagination.totalPages = data.totalPages;
    pagination.totalElements = data.totalElements;
    pagination.hasPrevious = !data.first;
    pagination.hasNext = !data.last;
  } catch (error) {
    console.error('Lỗi khi lấy danh sách hóa đơn:', error);
    ElMessage.error('Không thể tải danh sách hóa đơn. Vui lòng thử lại.');
  } finally {
    isLoading.value = false;
  }
};

const changePage = (newPage) => {
  if (newPage >= 1 && newPage <= pagination.totalPages) {
    pagination.currentPage = newPage;
    fetchInvoices();
  }
};

const createInvoice = async () => {
  isCreating.value = true;
  try {
    // THAY ĐỔI: Sử dụng apiClient và xóa URL/headers thủ công
    const response = await apiClient.post(
      `/admin/counter-sales/create-empty?employeeId=${employeeId}`,
      {} // body của request POST
    );
    const newInvoice = response.data;
    ElMessage.success(`Đã tạo hóa đơn mới: ${newInvoice.invoiceCode}`);
    await fetchInvoices();
  } catch (error) {
    console.error('Lỗi khi tạo hóa đơn:', error);
    const errorMessage = error.response?.data || 'Tạo hóa đơn thất bại!';
    ElMessage.error(errorMessage);
  } finally {
    isCreating.value = false;
  }
};

const viewInvoiceDetails = (id) => {
  router.push({ name: 'CounterSalesDisplay', params: { id } });
};

onMounted(() => {
  fetchInvoices();
});
</script>


<style scoped>
.fullscreen-container {
  height: 100vh; /* Fill the entire viewport height */
  padding: 1rem;
  background-color: #f5f7fa;
  box-sizing: border-box; /* Ensures padding is included in the height calculation */
}

.fullscreen-card {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column; /* Stack header, body, and footer vertically */
}

/* Make the card's body grow to fill available space */
:deep(.el-card__body) {
  flex-grow: 1;
  padding: 0; /* Remove padding so the table can sit flush against the edges */
  overflow: hidden; /* The table will handle its own scrolling */
}

:deep(.el-card__footer) {
  padding-top: 10px;
  padding-bottom: 10px;
}

/* Helper classes */
.d-flex {
  display: flex;
}
.justify-content-between {
  justify-content: space-between;
}
.justify-content-center {
  justify-content: center;
}
.align-items-center {
  align-items: center;
}
</style>