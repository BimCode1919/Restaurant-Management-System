# 🍽️ Restaurant QR Order - Complete Flow Guide

## 📋 Overview
Guide hoàn chỉnh cho flow đặt bàn → gọi món → thanh toán trong hệ thống Restaurant QR Order.

---

## 🔐 1. Authentication Flow

### 1.1 Đăng ký tài khoản (Customer)
```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Nguyen Van A",
  "email": "customer@example.com",
  "password": "password123",
  "phone": "0901234567"
}
```

**Response:**
```json
{
  "statusCode": 201,
  "message": "User registered successfully",
  "data": {
    "user": {
      "id": 1,
      "fullName": "Nguyen Van A",
      "email": "customer@example.com",
      "role": "CUSTOMER"
    },
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc..."
  }
}
```

### 1.2 Đăng nhập
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}
```

---

## 📅 2. Reservation Flow (Đặt bàn)

### 2.1 Tạo đặt bàn mới
```http
POST /api/reservations
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "reservationTime": "2026-02-15T19:00:00",
  "partySize": 4,
  "customerName": "Nguyen Van A",
  "customerPhone": "0901234567",
  "customerEmail": "customer@example.com",
  "notes": "Cần bàn gần cửa sổ",
  "tableIds": [1, 2]
}
```

**Response:**
```json
{
  "id": 1,
  "reservationTime": "2026-02-15T19:00:00",
  "partySize": 4,
  "status": "PENDING",
  "customerName": "Nguyen Van A",
  "customerPhone": "0901234567",
  "tables": [
    {
      "id": 1,
      "tableNumber": "T01",
      "capacity": 4
    }
  ],
  "createdAt": "2026-02-01T10:00:00"
}
```

### 2.2 Staff xác nhận đặt bàn
```http
PUT /api/reservations/1/confirm
Authorization: Bearer {staffToken}
```

**Response:**
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "message": "Reservation confirmed successfully"
}
```

### 2.3 Check-in khi khách đến
```http
PUT /api/reservations/1/checkin
Authorization: Bearer {staffToken}
```

**Response:**
```json
{
  "id": 1,
  "status": "SEATED",
  "billId": 100,
  "message": "Customer checked in, bill created"
}
```

**Note:** Khi check-in, hệ thống tự động:
- Tạo Bill mới
- Link Bill với Reservation
- Link Bill với Tables
- Cập nhật table status thành OCCUPIED

---

## 🍕 3. Order Flow (Gọi món)

### 3.1 Lấy danh sách menu
```http
GET /api/items?available=true
Authorization: Bearer {token}
```

**Response:**
```json
{
  "statusCode": 200,
  "data": [
    {
      "id": 1,
      "name": "Phở Bò",
      "price": 50000,
      "categoryName": "Món Chính",
      "available": true,
      "imageUrl": "https://..."
    }
  ]
}
```

### 3.2 Tạo Order (Gọi món)
```http
POST /api/orders
Authorization: Bearer {staffToken}
Content-Type: application/json

{
  "billId": 100,
  "orderType": "AT_TABLE",
  "items": [
    {
      "itemId": 1,
      "quantity": 2,
      "notes": "Ít ớt"
    },
    {
      "itemId": 5,
      "quantity": 1,
      "notes": "Không đường"
    }
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "billId": 100,
  "orderType": "AT_TABLE",
  "totalAmount": 120000,
  "items": [
    {
      "itemName": "Phở Bò",
      "quantity": 2,
      "price": 50000,
      "subtotal": 100000
    },
    {
      "itemName": "Trà Đá",
      "quantity": 1,
      "price": 20000,
      "subtotal": 20000
    }
  ],
  "createdAt": "2026-02-15T19:15:00"
}
```

### 3.3 Lấy thông tin Bill hiện tại
```http
GET /api/bills/100
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": 100,
  "totalPrice": 120000,
  "discountAmount": 0,
  "finalPrice": 120000,
  "status": "OPEN",
  "partySize": 4,
  "tables": ["T01", "T02"],
  "orders": [
    {
      "id": 1,
      "orderType": "AT_TABLE",
      "items": [...]
    }
  ],
  "createdAt": "2026-02-15T19:00:00"
}
```

---

## 💰 4. Payment Flow (Thanh toán)

### 4.1 Áp dụng discount (Optional)
```http
POST /api/bills/100/apply-discount
Authorization: Bearer {staffToken}
Content-Type: application/json

{
  "discountCode": "WELCOME10"
}
```

**Response:**
```json
{
  "id": 100,
  "totalPrice": 120000,
  "discountAmount": 12000,
  "finalPrice": 108000,
  "discount": {
    "code": "WELCOME10",
    "name": "Welcome Discount 10%",
    "valueType": "PERCENTAGE",
    "value": 10
  }
}
```

### 4.2 Tạo Payment (Thanh toán MoMo)
```http
POST /api/payments
Authorization: Bearer {token}
Content-Type: application/json

{
  "billId": 100,
  "paymentMethod": "MOMO",
  "amount": 108000,
  "customerName": "Nguyen Van A",
  "customerPhone": "0901234567"
}
```

**Response:**
```json
{
  "id": 1,
  "billId": 100,
  "paymentMethod": "MOMO",
  "amount": 108000,
  "status": "PENDING",
  "momoPayUrl": "https://test-payment.momo.vn/v2/gateway/pay?orderId=BILL_100_...",
  "orderId": "BILL_100_1738478400000",
  "transactionId": null,
  "createdAt": "2026-02-15T20:00:00"
}
```

### 4.3 Customer thanh toán qua MoMo
- Khách hàng click vào `momoPayUrl`
- Quét QR hoặc nhập thông tin trên app MoMo
- Xác nhận thanh toán

### 4.4 MoMo gửi IPN callback (Automatic)
```http
POST /api/payments/momo/notify
Content-Type: application/json

{
  "orderId": "BILL_100_1738478400000",
  "transId": "13012583444",
  "resultCode": "0",
  "message": "Successful",
  "signature": "..."
}
```

**Hệ thống tự động:**
- Cập nhật Payment status thành COMPLETED
- Cập nhật Bill status thành PAID
- Cập nhật Table status về AVAILABLE
- Gửi notification cho staff

### 4.5 Kiểm tra trạng thái thanh toán
```http
GET /api/payments/1/status
Authorization: Bearer {token}
```

**Response:**
```json
{
  "id": 1,
  "billId": 100,
  "status": "COMPLETED",
  "amount": 108000,
  "paymentMethod": "MOMO",
  "transactionId": "13012583444",
  "paidAt": "2026-02-15T20:05:00"
}
```

### 4.6 Đóng Bill
```http
PUT /api/bills/100/close
Authorization: Bearer {staffToken}
```

**Response:**
```json
{
  "id": 100,
  "status": "CLOSED",
  "closedAt": "2026-02-15T20:10:00",
  "message": "Bill closed successfully"
}
```

---

## 🔄 5. Alternative Flows

### 5.1 Walk-in Customer (Không đặt bàn trước)

#### Step 1: Staff tạo Bill trực tiếp
```http
POST /api/bills
Authorization: Bearer {staffToken}
Content-Type: application/json

{
  "tableIds": [3],
  "partySize": 2
}
```

#### Step 2: Tiếp tục flow Order và Payment như bình thường

### 5.2 Thanh toán tiền mặt (Cash)
```http
POST /api/payments
Content-Type: application/json

{
  "billId": 100,
  "paymentMethod": "CASH",
  "amount": 108000
}
```

**Response:** Payment status ngay lập tức là COMPLETED

### 5.3 Hủy đặt bàn
```http
PUT /api/reservations/1/cancel
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Khách thay đổi kế hoạch"
}
```

---

## 📊 6. Complete Flow Diagram

```
┌─────────────┐
│  Customer   │
└──────┬──────┘
       │
       │ 1. Đăng ký/Đăng nhập
       ▼
┌─────────────────────────────────┐
│   Authentication Successful     │
└──────────────┬──────────────────┘
               │
               │ 2. Tạo Reservation
               ▼
┌─────────────────────────────────┐
│   Reservation (PENDING)         │
└──────────────┬──────────────────┘
               │
               │ 3. Staff Confirm
               ▼
┌─────────────────────────────────┐
│   Reservation (CONFIRMED)       │
└──────────────┬──────────────────┘
               │
               │ 4. Customer arrives
               │    Staff Check-in
               ▼
┌─────────────────────────────────┐
│   Bill Created (OPEN)           │
│   Tables → OCCUPIED             │
└──────────────┬──────────────────┘
               │
               │ 5. Staff/Customer Order món
               ▼
┌─────────────────────────────────┐
│   Order 1, Order 2,... created  │
│   Bill totalPrice updated       │
└──────────────┬──────────────────┘
               │
               │ 6. (Optional) Apply Discount
               ▼
┌─────────────────────────────────┐
│   Bill: discount applied        │
│   finalPrice calculated         │
└──────────────┬──────────────────┘
               │
               │ 7. Create Payment
               ▼
┌─────────────────────────────────┐
│   Payment (PENDING)             │
│   Generate MoMo Payment URL     │
└──────────────┬──────────────────┘
               │
               │ 8. Customer pays via MoMo
               ▼
┌─────────────────────────────────┐
│   MoMo IPN Callback             │
└──────────────┬──────────────────┘
               │
               │ 9. Update Payment & Bill
               ▼
┌─────────────────────────────────┐
│   Payment (COMPLETED)           │
│   Bill (PAID)                   │
└──────────────┬──────────────────┘
               │
               │ 10. Staff Close Bill
               ▼
┌─────────────────────────────────┐
│   Bill (CLOSED)                 │
│   Tables → AVAILABLE            │
└─────────────────────────────────┘
```

---

## 🔑 7. API Endpoints Summary

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/refresh` - Refresh token

### Reservations
- `POST /api/reservations` - Tạo đặt bàn
- `GET /api/reservations/{id}` - Xem chi tiết
- `PUT /api/reservations/{id}/confirm` - Xác nhận (Staff)
- `PUT /api/reservations/{id}/checkin` - Check-in (Staff)
- `PUT /api/reservations/{id}/cancel` - Hủy

### Tables
- `GET /api/tables` - Danh sách bàn (Staff)
- `GET /api/tables/available` - Bàn trống
- `PATCH /api/tables/{id}/status` - Đổi trạng thái (Staff)

### Items (Menu)
- `GET /api/items` - Danh sách món
- `GET /api/items/{id}` - Chi tiết món
- `POST /api/items` - Thêm món (Admin)

### Bills
- `POST /api/bills` - Tạo bill (Staff)
- `GET /api/bills/{id}` - Chi tiết bill
- `POST /api/bills/{id}/apply-discount` - Áp discount (Staff)
- `PUT /api/bills/{id}/close` - Đóng bill (Staff)

### Orders
- `POST /api/orders` - Tạo order
- `GET /api/orders/{id}` - Chi tiết order
- `GET /api/bills/{billId}/orders` - Orders của bill

### Payments
- `POST /api/payments` - Tạo payment
- `GET /api/payments/{id}` - Chi tiết payment
- `GET /api/payments/{id}/status` - Kiểm tra trạng thái
- `POST /api/payments/momo/notify` - MoMo IPN callback
- `GET /api/payments/momo/return` - MoMo return URL

### Discounts
- `GET /api/discounts` - Danh sách discount
- `POST /api/discounts/validate` - Validate discount code

---

## ⚙️ 8. Configuration

### 8.1 Environment Variables (.env)
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/restaurant_qr_db
DB_USERNAME=restaurant_user
DB_PASSWORD=restaurant_pass

# JWT
JWT_SECRET=MySecretKeyForRestaurantQROrderSystemMinimum256BitsLongForHS256Algorithm
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# MoMo Payment Gateway
MOMO_PARTNER_CODE=MOMOXXXX2023
MOMO_ACCESS_KEY=your_access_key_here
MOMO_SECRET_KEY=your_secret_key_here
MOMO_ENDPOINT=https://test-payment.momo.vn/v2/gateway/api/create
MOMO_RETURN_URL=http://localhost:8080/api/payments/momo/return
MOMO_NOTIFY_URL=https://your-domain.com/api/payments/momo/notify

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Application
APP_BASE_URL=http://localhost:3000
```

### 8.2 MoMo Test Credentials
Để test MoMo, đăng ký tại: https://business.momo.vn/

**Test Mode:**
- Partner Code: Nhận từ MoMo
- Access Key: Nhận từ MoMo  
- Secret Key: Nhận từ MoMo

---

## 📱 9. QR Code Flow (Bonus)

### 9.1 Customer scan QR tại bàn
```
1. Customer scan QR code tại bàn
2. QR dẫn đến: http://localhost:3000/table/{qrCode}
3. Frontend call API: GET /api/tables/qr/{qrCode}
4. Hiển thị menu
5. Customer tự order (nếu cho phép)
```

### 9.2 Self-order by Customer
```http
POST /api/orders/self-order
Authorization: Bearer {customerToken}
Content-Type: application/json

{
  "qrCode": "TABLE_ABC123XYZ",
  "items": [
    {
      "itemId": 1,
      "quantity": 2
    }
  ]
}
```

---

## 🧪 10. Testing Flow

### 10.1 Start Docker
```bash
docker-compose up -d
```

### 10.2 Start Application
```bash
mvn spring-boot:run
```

### 10.3 Test Sequence (Postman/curl)
```bash
# 1. Register & Login
# 2. Create Reservation
# 3. Confirm Reservation (as Staff)
# 4. Check-in (as Staff) → Bill created
# 5. Create Order
# 6. Apply Discount
# 7. Create Payment → Get MoMo URL
# 8. Simulate MoMo callback
# 9. Check Payment Status
# 10. Close Bill
```

---

## 📝 Notes

### Business Rules
1. **Reservation:**
   - Chỉ tạo được reservation cho thời gian tương lai (>= current time)
   - Phải có ít nhất 1 table
   - Party size phải match với tổng capacity của tables

2. **Order:**
   - Chỉ order được khi Bill đang OPEN
   - Item phải có available = true
   - Quantity phải > 0

3. **Payment:**
   - Chỉ tạo payment khi Bill chưa PAID
   - Amount phải match với Bill finalPrice
   - MoMo payment timeout sau 15 phút

4. **Bill:**
   - Chỉ close được khi đã PAID
   - Khi close, tables tự động về AVAILABLE

### Security
- Customer chỉ xem được reservation/bill của mình
- Staff/Admin xem được tất cả
- Payment callback phải verify signature (TODO: implement)

### Database Relationships
```
Reservation → Bill (1-1)
Bill → Tables (M-M via BillTable)
Bill → Orders (1-M)
Order → OrderDetails → Items (M-M)
Bill → Payment (1-1)
Bill → Discount (M-1)
```

---

## 🚀 Quick Start Commands

```bash
# 1. Setup Database
docker-compose up -d postgres redis

# 2. Run application
mvn clean spring-boot:run

# 3. Access Swagger UI
http://localhost:8080/api/swagger-ui.html

# 4. Test full flow with curl scripts
./scripts/test-full-flow.sh
```

---

**Last Updated:** 2026-02-01
**Version:** 1.0.0
