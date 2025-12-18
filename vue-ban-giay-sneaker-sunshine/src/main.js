import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus';

// CSS
import 'bootstrap/dist/css/bootstrap.css' // hoặc .min.css, chỉ một cái!
import 'bootstrap-icons/font/bootstrap-icons.css'
import '@fortawesome/fontawesome-free/css/all.min.css'
import 'vue-toastification/dist/index.css'
import 'element-plus/dist/index.css';

// JS
import 'bootstrap/dist/js/bootstrap.bundle.min.js'  // đảm bảo có popper.js
import 'bootstrap'

// Toastification
import Toast from 'vue-toastification'

// Tạo App
const app = createApp(App)
app.use(router)
app.use(Toast)
app.mount('#app')
app.use(ElementPlus);
