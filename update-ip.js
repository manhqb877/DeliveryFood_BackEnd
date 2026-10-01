/**
 * ==============================================================================
 * 🚀 DELIVERYFOOD - ALL-IN-ONE URL / IP UPDATER (NGROK & LAN)
 * ==============================================================================
 * Tự động đồng bộ URL Ngrok hoặc địa chỉ IP máy tính cho toàn bộ hệ thống:
 *  1. AppCustomer (Mobile Khách hàng & Thanh toán SePay VietQR)
 *  2. AppShipper (Mobile Tài xế)
 *  3. Web Portal (Khách hàng Next.js apps/web)
 *  4. Dashboard (Admin & Shop Vite React)
 * ==============================================================================
 * Mặc định sử dụng URL Ngrok: https://unentwined-johanne-biasedly.ngrok-free.dev
 * Để quay lại dùng IP LAN máy tính, chạy: node update-ip.js --lan
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

const args = process.argv.slice(2);
const useLan = args.includes('--lan');
const customNgrokArg = args.find((a) => a.startsWith('http://') || a.startsWith('https://'));

const DEFAULT_NGROK = 'https://unentwined-johanne-biasedly.ngrok-free.dev';
const targetNgrokUrl = customNgrokArg || DEFAULT_NGROK;
const localIp = getLocalIP();
const rootDir = path.join(__dirname, '..');

const effectiveGatewayUrl = useLan ? `http://${localIp}:8080` : targetNgrokUrl;
const effectiveApiUrl = `${effectiveGatewayUrl}/api/v1`;

console.log('\x1b[36m%s\x1b[0m', '================================================================================');
console.log('\x1b[1m\x1b[32m%s\x1b[0m', ' 🚀 [DELIVERYFOOD] ĐỒNG BỘ URL GATEWAY CHO TOÀN BỘ MOBILE, WEB & THANH TOÁN');
console.log('\x1b[36m%s\x1b[0m', '================================================================================');
console.log(` Mode:                            \x1b[1m\x1b[35m${useLan ? 'LAN IP' : 'NGROK TUNNEL (Khuyên dùng)'}\x1b[0m`);
console.log(`🌐 Gateway Base URL:              \x1b[1m\x1b[32m${effectiveGatewayUrl}\x1b[0m`);
console.log(`📡 API V1 Base URL:               \x1b[36m${effectiveApiUrl}\x1b[0m`);
console.log(`💳 SePay VietQR Webhook URL:      \x1b[1m\x1b[33m${effectiveApiUrl}/payments/sepay/webhook\x1b[0m\n`);

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
      console.log(`  ➖ Đã đúng chuẩn: \x1b[90m${label}\x1b[0m`);
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
if (fs.existsSync(customerDir)) {
  const hostVal = useLan ? localIp : targetNgrokUrl.replace(/^https?:\/\//, '');
  updateEnvKey(path.join(customerDir, '.env'), 'LOCAL_IP', hostVal, 'AppCustomer/.env (LOCAL_IP)');
  updateEnvKey(path.join(customerDir, '.env'), 'GATEWAY_URL', effectiveGatewayUrl, 'AppCustomer/.env (GATEWAY_URL)');
  const customerApiClient = path.join(customerDir, 'src/api/apiClient.js');
  replaceInFile(
    customerApiClient,
    /export const GATEWAY_URL = '[^']+';/g,
    `export const GATEWAY_URL = '${effectiveGatewayUrl}';`,
    'AppCustomer/src/api/apiClient.js (GATEWAY_URL)'
  );
} else {
  console.log('  ➖ Không tìm thấy thư mục DeliveryFood_Mobile/AppCustomer (bỏ qua)');
}

// ─── 3. Cập nhật AppShipper (Mobile Tài Xế) ───────────────────────────────────
console.log('\n🛵 2. Cập nhật AppShipper (Mobile Tài Xế Giao Hàng):');
const shipperDir = path.join(rootDir, 'DeliveryFood_Mobile/AppShipper');
if (fs.existsSync(shipperDir)) {
  const hostVal = useLan ? localIp : targetNgrokUrl.replace(/^https?:\/\//, '');
  updateEnvKey(path.join(shipperDir, '.env'), 'LOCAL_IP', hostVal, 'AppShipper/.env (LOCAL_IP)');
  updateEnvKey(path.join(shipperDir, '.env'), 'GATEWAY_URL', effectiveGatewayUrl, 'AppShipper/.env (GATEWAY_URL)');
  const shipperApiClient = path.join(shipperDir, 'src/lib/apiClient.js');
  replaceInFile(
    shipperApiClient,
    /export const GATEWAY_URL = '[^']+';/g,
    `export const GATEWAY_URL = '${effectiveGatewayUrl}';`,
    'AppShipper/src/lib/apiClient.js (GATEWAY_URL)'
  );
  replaceInFile(
    shipperApiClient,
    /const BASE_URL = 'http:\/\/[^:]+:8080\/api\/v1';/g,
    `export const GATEWAY_URL = '${effectiveGatewayUrl}';\nconst BASE_URL = \`\${GATEWAY_URL}/api/v1\`;`,
    'AppShipper/src/lib/apiClient.js (BASE_URL)'
  );
} else {
  console.log('  ➖ Không tìm thấy thư mục DeliveryFood_Mobile/AppShipper (bỏ qua)');
}

// ─── 4. Cập nhật Web Portal (Next.js Customer Web) ────────────────────────────
console.log('\n💻 3. Cập nhật Web Customer (Next.js apps/web):');
const webEnvPath = path.join(rootDir, 'DeliveryFood_FrontEnd/apps/web/.env.local');
if (fs.existsSync(webEnvPath)) {
  let content = fs.readFileSync(webEnvPath, 'utf8');
  content = content
    .replace(/^NEXT_PUBLIC_API_URL=.*$/m, `NEXT_PUBLIC_API_URL=${effectiveApiUrl}`)
    .replace(/^NEXT_PUBLIC_AI_AGENT_URL=.*$/m, `NEXT_PUBLIC_AI_AGENT_URL=${effectiveGatewayUrl}`);
  fs.writeFileSync(webEnvPath, content, 'utf8');
  console.log(`  ✅ Đã cập nhật: apps/web/.env.local (API_URL=${effectiveApiUrl})`);
} else {
  console.log('  ➖ Không tìm thấy apps/web/.env.local');
}

// ─── 5. Cập nhật Dashboard (Admin & Shop Manager) ─────────────────────────────
console.log('\n📊 4. Cập nhật Dashboard (Admin & Shop Portal):');
const dashEnvPath = path.join(rootDir, 'DeliveryFood_FrontEnd/Delivery_Dashboard/.env');
updateEnvKey(dashEnvPath, 'VITE_API_IP', useLan ? localIp : 'localhost', 'Delivery_Dashboard/.env');

// ─── 6. Kiểm tra kết nối Backend API Gateway ─────────────────────────────────
let summaryPrinted = false;
function safePrintSummary() {
  if (!summaryPrinted) {
    summaryPrinted = true;
    printSummary();
  }
}

const req = http.get(`http://localhost:8080/actuator/health`, { timeout: 2000 }, (res) => {
  console.log(`  🟢 Backend API Gateway đang hoạt động trên port 8080! (HTTP ${res.statusCode})`);
  res.resume();
  safePrintSummary();
});

req.on('error', () => {
  console.log('  ⚠️  \x1b[33mBackend API Gateway chưa bật trên port 8080.\x1b[0m');
  safePrintSummary();
});

req.on('timeout', () => {
  req.destroy();
  console.log('  ⚠️  \x1b[33mKết nối đến port 8080 bị timeout.\x1b[0m');
  safePrintSummary();
});

// ─── 7. In bảng thông tin tổng hợp ───────────────────────────────────────────
function printSummary() {
  console.log('\n\x1b[36m%s\x1b[0m', '================================================================================');
  console.log('\x1b[1m\x1b[32m%s\x1b[0m', '🎉 [HOÀN TẤT ĐỒNG BỘ] THÔNG TIN KẾT NỐI HỆ THỐNG');
  console.log('\x1b[36m%s\x1b[0m', '================================================================================');
  if (!useLan) {
    console.log(`⚡ LỆNH BẬT NGROK TRÊN TERMINAL (CỔNG GATEWAY 8080):`);
    console.log(`   \x1b[1m\x1b[32mnpx ngrok http --url=${targetNgrokUrl} 8080\x1b[0m\n`);
    console.log(`🎯 URL CẤU HÌNH WEBHOOK TRÊN SEPAY (my.sepay.vn/webhooks):`);
    console.log(`   \x1b[1m\x1b[33m${effectiveApiUrl}/payments/sepay/webhook\x1b[0m\n`);
  }
  console.log(`📱 AppCustomer:   cd DeliveryFood_Mobile/AppCustomer && npx expo start -c`);
  console.log(`🛵 AppShipper:    cd DeliveryFood_Mobile/AppShipper && npx expo start -c`);
  console.log(`💻 Web Portal:    cd DeliveryFood_FrontEnd/apps/web && npm run dev (cổng 3000)`);
  console.log(`📊 Dashboard:     cd DeliveryFood_FrontEnd/Delivery_Dashboard && npm run dev (cổng 5173)`);
  console.log('================================================================================\n');
}
