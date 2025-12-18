<template>
  <div class="chart-container">
    <Bar v-if="hasData" :data="chartData" :options="finalOptions" />
    <div v-else class="no-data-placeholder">
      <p>Không có dữ liệu để hiển thị</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

// ✨ MỚI: Props được chuẩn hóa, chấp nhận cả data và options
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

// ✨ MỚI: Kiểm tra xem có dữ liệu hợp lệ không
const hasData = computed(() => {
  return props.chartData && props.chartData.datasets && props.chartData.datasets.length > 0;
});


// ✨ MỚI: Các tùy chọn mặc định để biểu đồ trông đẹp hơn
const defaultOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    },
    title: {
      display: false,
      text: 'Bar Chart'
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.7)',
      titleFont: { weight: 'bold' },
      bodyFont: { size: 13 },
      padding: 10,
      cornerRadius: 4,
    }
  },
  scales: {
    x: {
      grid: {
        display: false // Ẩn lưới trục X cho gọn
      }
    },
    y: {
      grid: {
        color: 'rgba(200, 200, 200, 0.2)' // Làm mờ lưới trục Y
      },
      ticks: {
        beginAtZero: true
      }
    }
  }
}

// ✨ MỚI: Gộp options mặc định và options từ props
const finalOptions = computed(() => {
  // Sử dụng spread operator để gộp, options từ props sẽ ghi đè lên default
  return { ...defaultOptions, ...props.chartOptions };
})
</script>

<style scoped>
.chart-container {
  position: relative;
  height: 350px; /* Hoặc bất kỳ chiều cao nào bạn muốn */
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