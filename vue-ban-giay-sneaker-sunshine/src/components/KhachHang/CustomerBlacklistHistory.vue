<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="text-xl font-semibold">📛 Danh sách khách hàng xấu</span>
        </div>
      </template>

      <el-table :data="badCustomers" stripe border style="width: 100%">
        <el-table-column prop="customerCode" label="Mã KH" width="120" />
        <el-table-column prop="customerName" label="Họ tên" />
        <el-table-column prop="email" label="Email" />
        <el-table-column prop="phone" label="SĐT" width="140" />

        <!-- <el-table-column label="Điểm tin cậy" width="130">
          <template #default="{ row }">
            <el-tag :type="getTrustColor(row.trustScore)">
              {{ row.trustScore }}
            </el-tag>
          </template>
        </el-table-column> -->

        <el-table-column prop="blacklistReason" label="Lý do bị cấm" />

        <el-table-column prop="blacklistExpiryDate" label="Hết hạn cấm" width="180">
          <template #default="{ row }">
            <span>{{ formatDate(row.blacklistExpiryDate) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="200">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row.id)"
              class="mr-2"
            >
              Chi tiết
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="removeFromBlacklist(row.id)"
            >
              Gỡ cấm
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient from '@/utils/axiosInstance'

const router = useRouter()
const badCustomers = ref([])

const fetchBadCustomers = async () => {
  try {
    const res = await apiClient.get('/admin/customers/bad')
    badCustomers.value = res.data || []
  } catch (err) {
    if (err?.response?.status === 403) {
      router.push('/error')
      return
    }
    console.error('Lỗi khi tải khách hàng xấu:', err)
    ElMessage.error('Lỗi khi tải khách hàng xấu')
    badCustomers.value = []
  }
}


const removeFromBlacklist = async (id) => {
  try {
    await apiClient.put(`/admin/customers/${id}/unblacklist`)
    ElMessage.success('Đã gỡ khỏi blacklist')
    fetchBadCustomers()
  } catch (err) {
    console.error('Lỗi khi gỡ blacklist:', err)
    ElMessage.error('Không thể gỡ blacklist. Vui lòng thử lại.')
  }
}

const viewDetail = (customerId) => {
  router.push({ name: 'BlacklistHistoryDetail', params: { id: customerId } })
}

const getTrustColor = (score) => {
  if (score >= 80) return 'success'
  if (score >= 50) return 'warning'
  return 'danger'
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return isNaN(d) ? '' : d.toLocaleString('vi-VN')
}

onMounted(fetchBadCustomers)
</script>

<style scoped></style>