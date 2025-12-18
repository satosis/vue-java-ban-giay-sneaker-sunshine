import { createRouter, createWebHistory } from 'vue-router'

import MaterialList from '@/components/ChatLieu/MaterialList.vue'
import StyleList from '@/components/CoGiay/StyleList.vue'
import CounterSales from '@/components/CounterSales/CounterSales.vue'
import CounterSalesDisplay from '@/components/CounterSales/CounterSalesDisplay.vue'
import CategoryList from '@/components/DanhMuc/CategoryList.vue'
import SoleList from '@/components/DeGiay/SoleList.vue'
import TrangChu from '@/components/Home/TrangChu.vue'
import InvoiceList from '@/components/invoice/InvoiceList.vue'
import SizeList from '@/components/KichThuoc/SizeList.vue'
import Login from '@/components/Login/Login.vue'
import VoucherList from '@/components/MaGiamGia/VoucherList.vue'
import ColorList from '@/components/MauSac/ColorList.vue'
import SupplierList from '@/components/NhaCungCap/SupplierList.vue'
import AddProduct from '@/components/SanPham/AddProduct.vue'
import DetailProduct from '@/components/SanPham/DetailProduct.vue'
import ProductHistory from '@/components/SanPham/ProductHistory.vue'
import ProductList from '@/components/SanPham/ProductList.vue'
import UpdateProduct from '@/components/SanPham/UpdateProduct.vue'
import BrandList from '@/components/ThuongHieu/BrandList.vue'
import Dashboard from '@/layout/Dashboard.vue'

import AddVoucher from '@/components/MaGiamGia/AddVoucher.vue'
import UpdateVoucher from '@/components/MaGiamGia/UpdateVoucher.vue'
import EmployeeList from '@/components/NhanVien/EmployeeList.vue'
import AddEmployee from '@/components/NhanVien/AddEmployee.vue'
import UpdateEmployee from '@/components/NhanVien/UpdateEmployee.vue'
import CustomerList from '@/components/KhachHang/CustomerList.vue'
import AddCustomer from '@/components/KhachHang/AddCustomer.vue'
import UpdateCustomer from '@/components/KhachHang/UpdateCustomer.vue'
import Error403 from '@/components/404-error-main/Error403.vue'
import OnlineSaleList from "@/components/OnlineSales/OnlineSaleList.vue";
import OrderStatus from "@/components/OnlineSales/OrderStatus.vue";
import DiscountCampaignList from '@/components/DotGiamGia/DiscountCampaignList.vue'
import DiscountCampaignAdd from '@/components/DotGiamGia/DiscountCampaignAdd.vue'
import DonHangDatTruoc from '@/components/Home/DonHangDatTruoc.vue'
import NhanVienXuLy from '@/components/ThongKe/NhanVienXuLy.vue'
import CustomerBlacklistHistory from '@/components/KhachHang/CustomerBlacklistHistory.vue'
import CustomerBlacklistHistoryDetail from '@/components/KhachHang/CustomerBlacklistHistoryDetail.vue'
import DiscountCampaignDetails from '@/components/DotGiamGia/DiscountCampaignDetails.vue'
import DiscountCampaignUpdate from '@/components/DotGiamGia/DiscountCampaignUpdate.vue'
import DashboardStats from '@/components/ThongKe/DashboardStats.vue'
import VoucherHistory from '@/components/MaGiamGia/VoucherHistory.vue'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },

  {
    path: '/error',
    name: 'Error403',
    component: Error403,
  },

  // Các trang bên trong Dashboard - cần đăng nhập mới vào được
  {
    path: '/',
    component: Dashboard,
    meta: { requiresAuth: true, breadcrumb: 'Dashboard' },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: DashboardStats,
        meta: {
          breadcrumb: 'Trang chủ',
          parent: 'Dashboard',
        },
      },

      { path: 'product', name: 'ProductList', component: ProductList },
      { path: 'product/add', name: 'AddProduct', component: AddProduct },
      { path: 'product/update/:id', name: 'UpdateProduct', component: UpdateProduct },
      { path: 'product/detail/:id', name: 'DetailProduct', component: DetailProduct },
      { path: 'product/history', name: 'ProductHistory', component: ProductHistory },

      { path: 'categories', name: 'Category', component: CategoryList },

      { path: 'voucher', name: 'VoucherList', component: VoucherList },
      { path: 'voucher-history/:id', name: 'VoucherHistory', component: VoucherHistory },
      { path: 'voucher/add', name: 'AddVoucher', component: AddVoucher },
      { path: 'voucher/update/:id', name: 'UpdateVoucher', component: UpdateVoucher },

      { path: '/sales-counter/list', name: 'CounterSales', component: CounterSales },
      {
        path: '/sales-counter/:id',
        name: 'CounterSalesDisplay',
        component: CounterSalesDisplay,
        props: true,
      },

      // { path: "sales-online", name: "OnlineSaleDisplay", component: OnlineSale },

      { path: 'supplier', name: 'NhaCungCap', component: SupplierList },
      { path: 'style', name: 'KieuDang', component: StyleList },
      { path: 'sole', name: 'DeGiay', component: SoleList },
      { path: 'size', name: 'KichThuoc', component: SizeList },
      { path: 'material', name: 'ChatLieu', component: MaterialList },

      { path: 'brand', name: 'Brand', component: BrandList },

      { path: 'invoices', name: 'InvoiceList', component: InvoiceList },

      { path: 'color', name: 'Color', component: ColorList },

      { path: 'voucher', name: 'Voucher', component: VoucherList },

      { path: 'employee', name: 'Employee', component: EmployeeList },
      { path: 'employee/add', name: 'AddEmployee', component: AddEmployee },
      { path: 'employee/update/:id', name: 'UpdateEmployee', component: UpdateEmployee },

      { path: 'customer/add', name: 'AddCustomer', component: AddCustomer },
      { path: 'customer', name: 'CustomerList', component: CustomerList },
      { path: 'customer/update/:id', name: 'UpdateCustomer', component: UpdateCustomer },
      { path: 'customer', name: 'CustomerList', component: CustomerList },
      { path: 'customer/update/:id', name: 'UpdateCustomer', component: UpdateCustomer },

      { path: 'dashboard', name: 'Dashboard', component: DashboardStats },

      { path: 'sales-online', name: 'SaleOnlines', component: OnlineSaleList },
      { path: 'sales-online/:invoiceId', name: 'InvoiceStatus', component: OrderStatus },

      { path: '/discount-campaigns', name: 'DiscountCampaignList', component: DiscountCampaignList },
      { path: '/discount-campaigns/add', name: 'DiscountCampaignAdd', component: DiscountCampaignAdd },
      { path: '/discount-campaigns/update/:id', name: 'DiscountCampaignUpdate', component: DiscountCampaignUpdate },
      { path: '/discount-campaigns/detail/:id', name: 'DiscountCampaignDetail', component: DiscountCampaignDetails },

      { path: '/pre-orders', name: 'DonHangDatTruoc', component: DonHangDatTruoc },
      { path: '/nhan-vien-xu-ly', name: 'NhanVienXuLy', component: NhanVienXuLy },

      { path: 'blacklist-history', name: 'BlacklistHistory', component: CustomerBlacklistHistory },
      { path: 'blacklist-history/:id', name: 'BlacklistHistoryDetail', component: CustomerBlacklistHistoryDetail },

    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Middleware kiểm tra token trước khi vào các trang cần đăng nhập
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path === '/login' && token) {
    // Nếu đã login, không cho vào lại trang login, chuyển về home
    next('/home')
  } else if (to.meta.requiresAuth && !token) {
    // Nếu chưa login mà vào trang yêu cầu auth thì chuyển về login
    next('/login')
  } else {
    next()
  }
})

export default router
