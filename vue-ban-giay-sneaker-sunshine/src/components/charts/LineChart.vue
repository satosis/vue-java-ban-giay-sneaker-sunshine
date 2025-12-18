<template>
  <div class="chart-container">
    <Line v-if="hasData" :data="chartData" :options="finalOptions" />
    <div v-else class="no-data-placeholder">
      <p>Không có dữ liệu để hiển thị</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  Filler // ✨ MỚI: Thêm Filler để tô màu vùng dưới đường line
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, LineElement, PointElement, CategoryScale, LinearScale, Filler)

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
  return props.chartData && props.chartData.datasets && props.chartData.datasets.length > 0;
});


const defaultOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.7)',
      titleFont: { weight: 'bold' },
      bodyFont: { size: 13 },
      padding: 10,
      cornerRadius: 4,
    }
  },
  interaction: { // ✨ MỚI: Cải thiện tương tác khi di chuột
    intersect: false,
    mode: 'index',
  },
  scales: {
    x: {
      grid: {
        display: false
      }
    },
    y: {
      grid: {
        color: 'rgba(200, 200, 200, 0.2)'
      },
      ticks: {
        beginAtZero: true
      }
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