<template>
  <header class="d-flex justify-content-between align-items-center bg-white text-dark p-3 position-relative">
    <!-- Carousel chạy chữ quảng cáo -->
    <div id="textCarousel" class="carousel slide w-50" data-bs-ride="carousel">
      <div class="carousel-inner text-center">
        <div class="carousel-item active">
          <h5 class="fw-bold">Welcome to Sunshine Sneakers</h5>
        </div>
        <div class="carousel-item">
          <h5 class="fw-bold text-danger">Big Discounts This Week! 🔥</h5>
        </div>
        <div class="carousel-item">
          <h5 class="fw-bold text-success">Shop Now and Save More! 🛍️</h5>
        </div>
      </div>
    </div>

    <!-- Khu vực tài khoản -->
    <div class="d-flex align-items-center">
      <span>Welcome, <strong>{{ employeeName }}</strong>!</span>
      <button @click="logout" class="btn btn-danger ms-3">Logout</button>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

// Khai báo router
const router = useRouter();
const employeeName = ref('');

// Lấy thông tin người dùng từ localStorage khi trang được tải
onMounted(() => {
  const storedName = localStorage.getItem('employeeName');
  if (!storedName) {
    router.push('/login'); // Nếu không có dữ liệu => quay lại login
    return;
  }
  employeeName.value = storedName;
});

// Hàm đăng xuất
const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');

  router.push('/login');
  window.location.reload();
};
</script>
