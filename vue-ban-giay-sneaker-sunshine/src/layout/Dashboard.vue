<template>
  <div class="d-flex min-vh-100">
    <!-- Sidebar -->
    <SidebarLayout />

    <!-- Main Area -->
    <div class="d-flex flex-column w-100">
      <!-- Header -->
      <HeaderLayout />

      <!-- Nội dung chính -->
      <div class="p-3 flex-grow-1">
        <router-view />
      </div>

      <!-- Footer -->
      <FooterLayout />
    </div>
  </div>
</template>

<script setup>
import FooterLayout from './FooterLayout.vue';
import HeaderLayout from './HeaderLayout.vue';
import SidebarLayout from './SidebarLayout.vue';
</script>
