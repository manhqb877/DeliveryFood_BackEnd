/**
 * ==============================================================================
 * 🚀 DELIVERYFOOD - ALL-IN-ONE IP UPDATER
 * ==============================================================================
 * Tự động đồng bộ địa chỉ IP máy tính (LAN/Wi-Fi) cho toàn bộ hệ thống:
 *  1. AppCustomer (Mobile Khách hàng & Thanh toán SePay VietQR)
 *  2. AppShipper (Mobile Tài xế)
 *  3. Web Portal (Khách hàng Next.js)
 *  4. Dashboard (Admin & Shop Vite React)
 * ==============================================================================
 */

const fs = require('fs');
const path = require('path');
const os = require('os');
const http = require('http');

// ─── 1. Tự động phát hiện IP LAN (Wi-Fi) của máy tính ────────────────────────
function getLocalIP() {
  const interfaces = os.networkInterfaces();
  const priorityNames = ['en0', 'en1', 'wlan0', 'eth0', 'Wi-Fi', 'Ethernet'];

  for (const name of priorityNames) {
    if (interfaces[name]) {
      for (const iface of interfaces[name]) {
        if (iface.family === 'IPv4' && !iface.internal && iface.address !== '127.0.0.1') {
          return iface.address;
        }
      }
    }
  }

  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal && iface.address !== '127.0.0.1') {
        return iface.address;
      }
    }
  }

  return '127.0.0.1';
}

const localIp = getLocalIP();
const rootDir = path.join(__dirname, '..');

console.log('\x1b[36m%s\x1b[0m', '================================================================================');
console.log('\x1b[1m\x1b[32m%s\x1b[0m', ' 🚀 [DELIVERYFOOD] TỰ ĐỘNG ĐỒNG BỘ IP CHO TOÀN BỘ WEB & MOBILE & THANH TOÁN');
console.log('\x1b[36m%s\x1b[0m', '================================================================================');
console.log(`📡 Địa chỉ IP mạng LAN hiện tại: \x1b[1m\x1b[32m${localIp}\x1b[0m`);
console.log(`🌐 Cổng API Gateway:              \x1b[36mhttp://${localIp}:8080/api/v1\x1b[0m`);
console.log(`💳 Cổng Thanh toán SePay:         \x1b[36mhttp://${localIp}:8080/api/v1/payments\x1b[0m\n`);

// ─── Helper cập nhật nội dung file an toàn ────────────────────────────────────
function replaceInFile(filePath, regex, replacement, label) {
  if (fs.existsSync(filePath)) {
    const content = fs.readFileSync(filePath, 'utf8');
    const newContent = content.replace(regex, replacement);
    if (content !== newContent) {
      fs.writeFileSync(filePath, newContent, 'utf8');
      console.log(`  ✅ Đã cập nhật: \x1b[33m${label}\x1b[0m`);
      return true;
    } else {
      console.log(`  ➖ Không đổi:   \x1b[90m${label}\x1b[0m`);
      return false;
    }
  } else {
    console.log(`  ⚠️  Không tìm thấy: ${label}`);
    return false;
  }
}

function updateEnvKey(envPath, key, value, label) {
  if (fs.existsSync(envPath)) {
    let content = fs.readFileSync(envPath, 'utf8');
    const reg = new RegExp(`^${key}=.*$`, 'm');
    if (reg.test(content)) {
      content = content.replace(reg, `${key}=${value}`);
    } else {
      content += `\n${key}=${value}\n`;
    }
    fs.writeFileSync(envPath, content, 'utf8');
    console.log(`  ✅ Đã cập nhật .env: \x1b[33m${label}\x1b[0m (${key}=${value})`);
  } else {
    fs.writeFileSync(envPath, `${key}=${value}\n`, 'utf8');
    console.log(`  ✅ Đã tạo mới .env:  \x1b[33m${label}\x1b[0m (${key}=${value})`);
  }
}

// ─── 2. Cập nhật AppCustomer (Mobile Khách Hàng) ──────────────────────────────
console.log('📱 1. Cập nhật AppCustomer (Mobile Khách Hàng & Thanh Toán SePay VietQR):');
const customerDir = path.join(rootDir, 'DeliveryFood_Mobile/AppCustomer');
updateEnvKey(path.join(customerDir, '.env'), 'LOCAL_IP', localIp, 'AppCustomer/.env');

const customerApiClient = path.join(customerDir, 'src/api/apiClient.js');
replaceInFile(
  customerApiClient,
  /export const GATEWAY_URL = 'http:\/\/[^:]+:8080';/g,
  `export const GATEWAY_URL = 'http://${localIp}:8080';`,
  'AppCustomer/src/api/apiClient.js (GATEWAY_URL)'
);
replaceInFile(
  customerApiClient,
  /http:\/\/(localhost|\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):8080\/api\/v1/g,
  `http://${localIp}:8080/api/v1`,
  'AppCustomer/src/api/apiClient.js (BASE_URL)'
);

// ─── 3. Cập nhật AppShipper (Mobile Tài Xế) ───────────────────────────────────
console.log('\n🛵 2. Cập nhật AppShipper (Mobile Tài Xế Giao Hàng):');
const shipperDir = path.join(rootDir, 'DeliveryFood_Mobile/AppShipper');
updateEnvKey(path.join(shipperDir, '.env'), 'LOCAL_IP', localIp, 'AppShipper/.env');

const shipperApiClient = path.join(shipperDir, 'src/lib/apiClient.js');
replaceInFile(
  shipperApiClient,
  /const BASE_URL = 'http:\/\/[^:]+:8080\/api\/v1';/g,
  `const BASE_URL = 'http://${localIp}:8080/api/v1';`,
  'AppShipper/src/lib/apiClient.js (BASE_URL)'
);
replaceInFile(
  shipperApiClient,
  /http:\/\/(localhost|\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):8080\/api\/v1/g,
  `http://${localIp}:8080/api/v1`,
  'AppShipper/src/lib/apiClient.js (Fallback URL)'
);

// ─── 4. Cập nhật Web Portal (Next.js Customer Web) ────────────────────────────
console.log('\n💻 3. Cập nhật Web Customer (Next.js apps/web):');
const webEnvPath = path.join(rootDir, 'DeliveryFood_FrontEnd/apps/web/.env.local');
if (fs.existsSync(webEnvPath)) {
  let content = fs.readFileSync(webEnvPath, 'utf8');
  // Hỗ trợ cả localhost hoặc IP máy tính khi truy cập từ thiết bị khác trong mạng
  content = content
    .replace(/^NEXT_PUBLIC_API_URL=.*$/m, `NEXT_PUBLIC_API_URL=http://${localIp}:8080/api/v1`)
    .replace(/^NEXT_PUBLIC_AI_AGENT_URL=.*$/m, `NEXT_PUBLIC_AI_AGENT_URL=http://${localIp}:8088`);
  fs.writeFileSync(webEnvPath, content, 'utf8');
  console.log(`  ✅ Đã cập nhật: apps/web/.env.local (API_URL=http://${localIp}:8080/api/v1)`);
} else {
  console.log('  ➖ Không tìm thấy apps/web/.env.local');
}

// ─── 5. Cập nhật Dashboard (Admin & Shop Manager) ─────────────────────────────
console.log('\n📊 4. Cập nhật Dashboard (Admin & Shop Portal):');
const dashEnvPath = path.join(rootDir, 'DeliveryFood_FrontEnd/Delivery_Dashboard/.env');
updateEnvKey(dashEnvPath, 'VITE_API_IP', localIp, 'Delivery_Dashboard/.env');

// ─── 6. Kiểm tra kết nối Backend API Gateway ─────────────────────────────────
console.log('\n🔍 5. Kiểm tra trạng thái Backend (API Gateway port 8080):');
const req = http.get(`http://${localIp}:8080/actuator/health`, { timeout: 2000 }, (res) => {
  console.log(`  🟢 Backend API Gateway đang hoạt động! (HTTP ${res.statusCode})`);
  printSummary();
});

req.on('error', () => {
  console.log('  ⚠️  \x1b[33mBackend API Gateway chưa bật trên port 8080.\x1b[0m');
  console.log('     👉 Hãy khởi động Backend Spring Boot trước khi test trên điện thoại!');
  printSummary();
});

req.on('timeout', () => {
  req.destroy();
  console.log('  ⚠️  \x1b[33mKết nối đến port 8080 bị timeout (Kiểm tra lại Firewall).\x1b[0m');
  printSummary();
});

// ─── 7. In bảng thông tin tổng hợp ───────────────────────────────────────────
function printSummary() {
  console.log('\n\x1b[36m%s\x1b[0m', '================================================================================');
  console.log('\x1b[1m\x1b[32m%s\x1b[0m', '🎉 [HOÀN TẤT ĐỒNG BỘ] THÔNG TIN HỆ THỐNG');
  console.log('\x1b[36m%s\x1b[0m', '================================================================================');
  console.log(`📱 AppCustomer (Khách hàng):  cd DeliveryFood_Mobile/AppCustomer && npx expo start -c`);
  console.log(`🛵 AppShipper (Tài xế):       cd DeliveryFood_Mobile/AppShipper && npx expo start -c`);
  console.log(`💻 Web Portal:                http://${localIp}:3001  (hoặc http://localhost:3001)`);
  console.log(`📊 Dashboard:                 http://${localIp}:5173  (hoặc http://localhost:5173)`);
  console.log(`💳 SePay VietQR (Tự động):    MBBank 025452790502 (NGUYEN THAI AN)`);
  console.log('================================================================================\n');
}
