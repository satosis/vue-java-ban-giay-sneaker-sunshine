<template>

      <el-button type="primary" @click="goToPreOrders" style="margin-top: 20px;">
        Xem những đơn hàng đặt trước
      </el-button>

</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router' // Đảm bảo đã import useRouter


const router = useRouter()


function goToPreOrders() {
  router.push('/pre-orders') // Điều hướng đến route '/pre-orders'
}

</script>

<style scoped>
/* ... các style hiện có của bạn ... */
</style>
