# MoMo Payment Integration Setup Guide

## 📋 Tổng quan

Hướng dẫn tích hợp MoMo Payment Gateway vào hệ thống Restaurant QR Order. MoMo yêu cầu IPN (Instant Payment Notification) URL phải có thể truy cập công khai để gửi thông báo thanh toán.

## 🔧 Cấu hình

### 1. Cấu hình MoMo trong `.env`

Copy từ `.env.example` và cập nhật thông tin:

```bash
# MoMo Credentials (get from https://business.momo.vn/)
MOMO_PARTNER_CODE=MOMOBKUN20180529
MOMO_ACCESS_KEY=klm05TvNBzhg7h7j
MOMO_SECRET_KEY=at67qH6mk8w5Y1nAyMoYKMWACiEi2bsa
MOMO_API_URL=https://test-payment.momo.vn/v2/gateway/api/create

# MoMo Callback URLs
MOMO_RETURN_URL=http://localhost:8080/api/payments/momo/return
MOMO_NOTIFY_URL=http://localhost:8080/api/payments/momo/ipn
```


## 🌐 Sử dụng Ngrok cho Local Development

### Tại sao cần Ngrok?

MoMo server cần gọi IPN callback về server của bạn để xác nhận thanh toán. Trong môi trường local development, server của bạn chạy trên `localhost` không thể truy cập từ internet. Ngrok tạo một tunnel công khai để MoMo có thể gọi về local server.

### Cài đặt Ngrok

#### Windows (PowerShell):

```powershell
# Dùng Chocolatey
choco install ngrok

# Hoặc download từ https://ngrok.com/download
# Extract và add vào PATH
```

#### Hoặc dùng npm:

```bash
npm install -g ngrok
```

### Đăng ký và Xác thực

1. Đăng ký tài khoản tại: https://dashboard.ngrok.com/signup
2. Lấy authtoken từ: https://dashboard.ngrok.com/get-started/your-authtoken
3. Cấu hình authtoken:

```bash
ngrok config add-authtoken YOUR_AUTHTOKEN_HERE
```

### Khởi động Ngrok

Giả sử backend chạy trên port **8080** (hoặc 8081 nếu đã cấu hình):

```bash
# Mở terminal mới và chạy
ngrok http 8080
```

Hoặc với port 8081:

```bash
ngrok http 8081
```

Ngrok sẽ hiển thị output:

```
Session Status                online
Account                       your-email@example.com
Version                       3.x.x
Region                        Asia Pacific (ap)
Forwarding                    https://abc123.ngrok-free.app -> http://localhost:8080
```

### Cập nhật MOMO_NOTIFY_URL

Copy URL forwarding từ ngrok (ví dụ: `https://abc123.ngrok-free.app`) và cập nhật trong `.env`:

```bash
MOMO_NOTIFY_URL=https://abc123.ngrok-free.app/api/payments/momo/ipn
```

**Quan trọng:** Mỗi lần khởi động lại ngrok, URL sẽ thay đổi (trừ khi dùng paid plan). Nhớ cập nhật lại `MOMO_NOTIFY_URL`.

## � Sử dụng Ngrok với Docker (Khuyến nghị)

### Ưu điểm

- Tự động khởi động cùng với database và redis
- Không cần cài đặt ngrok locally
- Dễ dàng quản lý với Docker Compose
- Ngrok web UI luôn có sẵn tại `http://localhost:4040`

### Cấu hình

#### 1. Lấy Ngrok Authtoken

1. Đăng ký tài khoản tại: https://dashboard.ngrok.com/signup
2. Lấy authtoken từ: https://dashboard.ngrok.com/get-started/your-authtoken
3. Copy authtoken

#### 2. Cập nhật file `.env`

Tạo file `.env` từ `.env.example` và thêm authtoken:

```bash
# Copy từ .env.example
cp .env.example .env

# Thêm vào .env
NGROK_AUTHTOKEN=your_actual_ngrok_authtoken_here
```

#### 3. Khởi động Docker Compose

```bash
# Start all services (postgres, redis, ngrok)
docker-compose up -d

# Check logs
docker-compose logs -f ngrok

# Hoặc chỉ start ngrok
docker-compose up -d ngrok
```

#### 4. Lấy Ngrok Public URL

**Option 1: Ngrok Web UI** (Khuyến nghị)

Mở browser: http://localhost:4040

- Xem tunnel URL tại trang chủ
- Monitor requests realtime
- Inspect và replay requests

**Option 2: API**

```bash
# Get tunnel URL via API
curl http://localhost:4040/api/tunnels | jq '.tunnels[0].public_url'

# Or use PowerShell
(Invoke-RestMethod http://localhost:4040/api/tunnels).tunnels[0].public_url
```

**Option 3: Docker logs**

```bash
docker-compose logs ngrok | grep "started tunnel"
```

#### 5. Cập nhật MOMO_NOTIFY_URL

Copy ngrok URL (ví dụ: `https://abc123.ngrok-free.app`) và cập nhật `.env`:

```bash
MOMO_NOTIFY_URL=https://abc123.ngrok-free.app/api/payments/momo/ipn
```

**Restart backend** để áp dụng thay đổi.

### Quản lý Ngrok Container

```bash
# Stop ngrok
docker-compose stop ngrok

# Start ngrok
docker-compose start ngrok

# Restart ngrok (get new URL)
docker-compose restart ngrok

# View logs
docker-compose logs -f ngrok

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Troubleshooting Docker Ngrok

**Ngrok container không start:**
```bash
# Check logs
docker-compose logs ngrok

# Common issue: invalid authtoken
# → Update NGROK_AUTHTOKEN in .env
```

**Không kết nối được backend:**
```bash
# Verify backend is running on port 8080
netstat -an | findstr 8080  # Windows
lsof -i :8080              # macOS/Linux
```

**URL thay đổi sau mỗi lần restart:**
- Free plan của ngrok sẽ tạo random URL
- Upgrade lên paid plan để có static domain
- Hoặc: không restart ngrok container

## �🔄 Payment Flow

### 1. Tạo Payment (Client → Backend)

```bash
POST /api/payments
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "billId": 123,
  "paymentMethod": "MOMO"
}
```

**Response:**

```json
{
  "data": {
    "id": 456,
    "billId": 123,
    "method": "MOMO",
    "amount": 150000,
    "status": "PENDING",
    "paymentUrl": "https://test-payment.momo.vn/v2/gateway/pay?partnerCode=...",
    "momoOrderId": "BILL_123_1234567890",
    "momoRequestId": "uuid-here"
  }
}
```

### 2. Redirect User to MoMo (Frontend)

Frontend redirect user đến `paymentUrl` để thực hiện thanh toán trên MoMo app/web.

### 3. User Complete Payment on MoMo

User hoàn tất thanh toán trên MoMo (QR scan, app, hoặc web).

### 4. MoMo IPN Callback (MoMo → Backend)

MoMo server gọi về `MOMO_NOTIFY_URL`:

```bash
POST https://your-ngrok-url.ngrok-free.app/api/payments/momo/ipn
Content-Type: application/json

{
  "partnerCode": "MOMOBKUN20180529",
  "orderId": "BILL_123_1234567890",
  "requestId": "uuid-here",
  "amount": 150000,
  "orderInfo": "Payment for Bill #123",
  "orderType": "momo_wallet",
  "transId": "2847391048",
  "resultCode": "0",
  "message": "Successful.",
  "payType": "qr",
  "responseTime": "2026-03-07 10:30:00",
  "extraData": "",
  "signature": "signature-hash-here"
}
```

Backend xử lý:
- Verify signature (TODO: implement trong production)
- Cập nhật trạng thái payment thành `COMPLETED` hoặc `FAILED`
- Cập nhật bill thành `PAID` nếu thanh toán thành công

### 5. MoMo Return URL (MoMo → User → Backend)

Sau khi thanh toán, MoMo redirect user về `MOMO_RETURN_URL`:

```
GET http://localhost:8081/api/payments/momo/return?orderId=BILL_123_1234567890&resultCode=0&message=Successful
```

**Response (JSON):**

```json
{
  "status": "success",
  "message": "Payment successful!",
  "orderId": "BILL_123_1234567890",
  "resultCode": "0"
}
```

**Production:** Nên redirect về frontend:

```javascript
// Uncomment trong PaymentController.java
return ResponseEntity.status(HttpStatus.FOUND)
    .header("Location", "http://localhost:3000/payment/success?orderId=BILL_123_1234567890")
    .build();
```

## 📝 API Endpoints

| Endpoint | Method | Mô tả | Access |
|----------|--------|-------|--------|
| `/api/payments` | POST | Tạo payment mới | Authenticated |
| `/api/payments/{id}` | GET | Get payment details | Admin/Staff |
| `/api/payments/bill/{billId}` | GET | Get payment by bill ID | Admin/Staff |
| `/api/payments/momo/ipn` | POST | MoMo IPN callback | Public (MoMo only) |
| `/api/payments/momo/notify` | POST | Alias for IPN | Public (MoMo only) |
| `/api/payments/momo/return` | GET | MoMo return URL | Public |

## 🧪 Test Payment Flow

### 1. Start Services

```bash
# Terminal 1: Start backend
./mvnw spring-boot:run

# Terminal 2: Start ngrok
ngrok http 8080

# Terminal 3: Update .env with ngrok URL and restart backend if needed
```

### 2. Create Payment

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "billId": 1,
    "paymentMethod": "MOMO"
  }'
```

### 3. Copy `paymentUrl` và mở trong browser

Trên MoMo test environment, bạn có thể dùng test credentials để hoàn tất thanh toán.

### 4. Check Logs

Monitor backend logs để xem IPN callback:

```bash
# Sẽ thấy log
INFO  c.r.q.c.PaymentController - Received MoMo IPN callback: {orderId=BILL_1_..., resultCode=0, ...}
INFO  c.r.q.s.PaymentService - MoMo payment completed for order: BILL_1_..., Bill ID: 1
```

## 🔒 Security Notes

### Production Checklist:

1. **Verify MoMo Signature:**
   - Uncomment signature verification trong `PaymentController.momoIpn()`
   - Implement `paymentService.verifyMoMoSignature()`

2. **Use HTTPS:**
   - Production server phải dùng HTTPS
   - Đăng ký domain và SSL certificate

3. **Rate Limiting:**
   - Implement rate limiting cho IPN endpoint
   - Tránh abuse từ bot/hackers

4. **IP Whitelist:**
   - Chỉ accept IPN callback từ MoMo IP addresses
   - MoMo cung cấp list IP servers trong docs

5. **Logging & Monitoring:**
   - Log tất cả IPN requests
   - Set up alerts cho failed payments

## 🐛 Troubleshooting

### MoMo không gọi được IPN

**Check:**
- Ngrok có đang chạy không?
- `MOMO_NOTIFY_URL` có đúng ngrok URL không?
- Backend có đang chạy không?
- Check ngrok web interface: `http://localhost:4040` để xem requests

### Payment không update status

**Check:**
- Backend logs có nhận IPN callback không?
- `orderId` trong IPN có khớp với DB không?
- Check `resultCode`: 0 = success, khác 0 = failed

### Ngrok URL thay đổi liên tục

**Solution:**
- Free plan của ngrok sẽ tạo random URL mỗi lần
- Upgrade lên paid plan để có static domain
- Hoặc dùng localtunnel: `npm install -g localtunnel`

## 📚 References

- [MoMo Developer Portal](https://developers.momo.vn/)
- [MoMo API Documentation](https://developers.momo.vn/v3/)
- [Ngrok Documentation](https://ngrok.com/docs)
- [MoMo Test Credentials](https://developers.momo.vn/v3/#/docs/testcase)

## 🚀 Production Deployment

Khi deploy lên production server (VPS, Cloud):

1. Server phải có public IP và domain
2. Cấu hình SSL certificate (Let's Encrypt)
3. Update `MOMO_NOTIFY_URL` thành production URL:
   ```
   MOMO_NOTIFY_URL=https://api.yourdomain.com/api/payments/momo/ipn
   ```
4. Không cần ngrok trên production
5. Register production URL với MoMo support team

---

**Last Updated:** March 7, 2026
