<template>
  <div class="p-4">
    <el-card shadow="hover">
      <template #header>
        <div class="flex justify-between items-center">
          <el-button @click="$router.back()" type="blue" size="small">Quay lại</el-button>
        </div>
      </template>

      <el-table :data="blacklistHistory" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="reason" label="Lý do" />
        <el-table-column prop="startTime" label="Bắt đầu" width="200">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="Kết thúc" width="200">
          <template #default="{ row }">
            {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import apiClient from '@/utils/axiosInstance'

const route = useRoute()
const customerId = route.params.id
const blacklistHistory = ref([])

const fetchBlacklistHistory = async () => {
  try {
    const res = await apiClient.get(`/admin/customers/${customerId}/blacklist-history`)
    blacklistHistory.value = res.data
  } catch (err) {
    console.error(err)
    ElMessage.error('Lỗi khi tải lịch sử blacklist')
  }
}

const formatDate = (dateStr) => {
  const d = new Date(dateStr)
  return isNaN(d) ? '' : d.toLocaleString('vi-VN')
}

onMounted(fetchBlacklistHistory)
</script>

<style scoped></style>