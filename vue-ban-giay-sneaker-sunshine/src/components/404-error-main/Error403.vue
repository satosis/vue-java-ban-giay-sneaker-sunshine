<template>
  <div class="google-403-container">
    <div class="content-wrapper">
      <div class="google-logo-wrapper">
        <span class="google-logo-text">ShunShine</span>
      </div>

      <p class="error-code-line">403. That's an error.</p>

      <p class="error-message">
        We're sorry, but you do not have access to this document.
      </p>
      <p class="error-details">
        That's all we know.
      </p>

      <RouterLink to="/" class="home-link">
        Quay về trang chủ
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
// Không cần logic JavaScript cho trang lỗi này.
</script>

<style scoped>
/* --- Đặt lại cơ bản & Khử răng cưa font --- */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-family: Arial, sans-serif;
  background-color: #fff;
  overflow: hidden;
}

/* --- Container chính của trang lỗi --- */
.google-403-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center; /* Căn giữa theo chiều ngang */
  align-items: center;   /* Căn giữa theo chiều dọc */
  background-color: #fff;
}

/* --- Wrapper cho toàn bộ nội dung (logo, text, link) --- */
.content-wrapper {
  max-width: 580px; /* Chiều rộng tối đa giống Google */
  width: 90%;      /* Đáp ứng trên các màn hình nhỏ hơn */
  padding: 40px 20px; /* Khoảng cách bên trong */
  text-align: left; /* **Căn chỉnh toàn bộ văn bản sang trái** */
}

/* --- Kiểu dáng Logo (chữ) --- */
.google-logo-wrapper {
  margin-bottom: 25px;
}

.google-logo-text {
  font-family: 'Product Sans', Arial, sans-serif; /* Thử Product Sans, nếu không có thì Arial */
  font-size: 30px;
  font-weight: bold;
  /* Hiệu ứng màu gradient cho chữ */
  background: linear-gradient(to right, #4285F4 0%, #4285F4 25%, #EA4335 25%, #EA4335 50%, #FBBC05 50%, #FBBC05 75%, #34A853 75%, #34A853 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  color: #000; /* Màu dự phòng */
  display: inline-block; /* Quan trọng cho gradient hoạt động */
}

/* --- Kiểu dáng văn bản lỗi --- */
.error-code-line {
  font-size: 1.25rem;
  color: #3c4043;
  margin-bottom: 20px;
}

.error-message {
  font-size: 1.5rem;
  color: #202124;
  font-weight: normal;
  margin-bottom: 10px;
}

.error-details {
  font-size: 0.95rem;
  color: #5f6368;
  margin-bottom: 25px;
}

/* --- Kiểu dáng liên kết về trang chủ --- */
.home-link {
  display: inline-block;
  color: #1a73e8;
  text-decoration: none;
  font-size: 1rem;
  font-weight: 500;
  padding-bottom: 2px;
  border-bottom: 2px solid transparent; /* Gạch chân động */
  transition: border-bottom-color 0.2s ease-in-out, color 0.2s ease-in-out;
}

.home-link:hover {
  color: #174ea6;
  border-bottom-color: #1a73e8;
}

/* --- Điều chỉnh đáp ứng (Responsive) --- */
@media (max-width: 768px) {
  .content-wrapper {
    padding: 30px 15px;
  }
}

@media (max-width: 480px) {
  .google-logo-text {
    font-size: 25px;
  }
  .error-code-line {
    font-size: 1.1rem;
  }
  .error-message {
    font-size: 1.3rem;
  }
  .error-details {
    font-size: 0.85rem;
  }
  .home-link {
    font-size: 0.9rem;
  }
}
</style>