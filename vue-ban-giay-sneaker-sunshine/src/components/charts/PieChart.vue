<template>
  <div class="chart-container">
    <Pie v-if="hasData" :data="chartData" :options="finalOptions" />
    <div v-else class="no-data-placeholder">
      <p>Không có dữ liệu để hiển thị</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Pie } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement } from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, ArcElement)

const props = defineProps({
  chartData: {
    type: Object,
    required: true
  },
  chartOptions: {
    type: Object,
    default: () => ({})
  }
})

const hasData = computed(() => {
  return props.chartData && props.chartData.datasets && props.chartData.datasets[0]?.data?.length > 0;
});

const defaultOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'right', // ✨ MỚI: Chú thích bên phải phù hợp hơn cho biểu đồ tròn
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.7)',
      titleFont: { weight: 'bold' },
      bodyFont: { size: 13 },
      padding: 10,
      cornerRadius: 4,
    }
  }
}

const finalOptions = computed(() => {
  return { ...defaultOptions, ...props.chartOptions };
})
</script>

<style scoped>
.chart-container {
  position: relative;
  height: 350px;
}
.no-data-placeholder {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #909399;
  background-color: #f9f9f9;
  border-radius: 8px;
}
</style>