# 📚 Hướng Dẫn Chi Tiết Hệ Thống Discount (Giảm Giá)

## 📋 Mục Lục
- [Tổng Quan](#tổng-quan)
- [Loại Discount (DiscountType)](#loại-discount-discounttype)
- [Kiểu Giá Trị (DiscountValueType)](#kiểu-giá-trị-discountvaluetype)
- [Cấu Trúc Database](#cấu-trúc-database)
- [API Endpoints](#api-endpoints)
- [Ví Dụ Tạo Discount](#ví-dụ-tạo-discount)
- [Cách Tính Discount](#cách-tính-discount)
- [Flow Xử Lý Discount](#flow-xử-lý-discount)
- [Use Cases Thực Tế](#use-cases-thực-tế)

---

## 🎯 Tổng Quan

Hệ thống discount hỗ trợ **6 loại giảm giá** với **3 kiểu giá trị** khác nhau, cho phép cấu hình linh hoạt các chương trình khuyến mãi.

### Đặc Điểm Chính
- ✅ Hỗ trợ giảm giá theo %, số tiền cố định, hoặc đặt giá cố định
- ✅ Áp dụng cho món cụ thể, ngày lễ, số lượng người, bậc hóa đơn
- ✅ Giới hạn số lần sử dụng
- ✅ Giới hạn số tiền giảm tối đa
- ✅ Áp dụng theo ngày trong tuần hoặc ngày cụ thể
- ✅ Cấu hình bậc giảm giá theo tổng đơn hàng

---

## 📊 Loại Discount (DiscountType)

### 1. **PERCENTAGE** - Giảm Giá Theo Phần Trăm
Giảm giá theo % cho toàn bộ đơn hàng.

**Đặc điểm:**
- Giảm % tổng giá trị đơn hàng
- Có thể giới hạn số tiền giảm tối đa (`maxDiscountAmount`)
- Có thể yêu cầu giá trị đơn hàng tối thiểu (`minOrderAmount`)

**Ví dụ:**
```json
{
  "code": "WELCOME10",
  "name": "Giảm 10% cho khách hàng mới",
  "discountType": "PERCENTAGE",
  "valueType": "PERCENTAGE",
  "value": 10,
  "minOrderAmount": 100000,
  "maxDiscountAmount": 50000
}
```

**Cách tính:** Đơn 500,000đ → Giảm 50,000đ (10% = 50,000đ, tối đa 50,000đ)

---

### 2. **FIXED_AMOUNT** - Giảm Số Tiền Cố Định
Giảm một số tiền cố định cho đơn hàng.

**Đặc điểm:**
- Giảm số tiền cố định không phụ thuộc giá trị đơn
- Thường dùng cho freeship, voucher giảm giá

**Ví dụ:**
```json
{
  "code": "FREESHIP",
  "name": "Miễn phí ship 30k",
  "discountType": "FIXED_AMOUNT",
  "valueType": "FIXED_AMOUNT",
  "value": 30000,
  "minOrderAmount": 200000
}
```

**Cách tính:** Đơn 250,000đ → Giảm 30,000đ (cố định)

---

### 3. **ITEM_SPECIFIC** - Giảm Giá Cho Món Cụ Thể
Giảm giá chỉ áp dụng cho các món được chọn.

**Đặc điểm:**
- Áp dụng cho món cụ thể thông qua bảng `discount_items`
- Có thể giảm %, giảm số tiền, hoặc đặt giá cố định
- Field `applyToSpecificItems` = `true`

**Ví dụ 1: Giảm 20% cho Pizza**
```json
{
  "code": "PIZZA20",
  "name": "Pizza Sale 20%",
  "discountType": "ITEM_SPECIFIC",
  "valueType": "PERCENTAGE",
  "value": 20,
  "applyToSpecificItems": true
}
```

**Ví dụ 2: Phở giá ưu đãi 35,000đ**
```json
{
  "code": "PHO35K",
  "name": "Phở giá ưu đãi",
  "discountType": "ITEM_SPECIFIC",
  "valueType": "FIXED_PRICE",
  "value": 35000,
  "applyToSpecificItems": true
}
```

**Cách tính:**
- PERCENTAGE: Pizza 100,000đ → 80,000đ (giảm 20%)
- FIXED_PRICE: Phở giá gốc 60,000đ → 35,000đ (giá cố định)

---

### 4. **HOLIDAY** - Giảm Giá Ngày Lễ/Cuối Tuần
Giảm giá áp dụng cho những ngày cụ thể trong tuần hoặc ngày lễ.

**Đặc điểm:**
- Sử dụng field `applicableDays` để cấu hình
- Hỗ trợ 2 format:
  - **Ngày trong tuần:** `"MONDAY,FRIDAY,SATURDAY,SUNDAY"`
  - **Ngày cụ thể:** `"2026-01-01,2026-02-14,2026-12-25"`

**Ví dụ 1: Weekend Sale**
```json
{
  "code": "WEEKENDPARTY",
  "name": "Weekend Party 25%",
  "discountType": "HOLIDAY",
  "valueType": "PERCENTAGE",
  "value": 25,
  "applicableDays": "SATURDAY,SUNDAY",
  "minOrderAmount": 200000,
  "maxDiscountAmount": 100000
}
```

**Ví dụ 2: Tết 2026**
```json
{
  "code": "TET2026",
  "name": "Ưu đãi Tết Nguyên Đán",
  "discountType": "HOLIDAY",
  "valueType": "PERCENTAGE",
  "value": 30,
  "applicableDays": "2026-01-29,2026-01-30,2026-01-31,2026-02-01"
}
```

**Cách kiểm tra:**
- Kiểm tra ngày hiện tại có nằm trong `applicableDays` không
- Nếu là ngày trong tuần, check `DayOfWeek`
- Nếu là ngày cụ thể, check `LocalDate`

---

### 5. **PARTY_SIZE** - Giảm Giá Theo Số Người
Giảm giá dựa trên số lượng người trong nhóm.

**Đặc điểm:**
- Sử dụng `minPartySize` và `maxPartySize`
- Khuyến khích nhóm đông đặt bàn

**Ví dụ 1: Nhóm 4-6 người**
```json
{
  "code": "GROUP4",
  "name": "Giảm 10% cho nhóm 4-6 người",
  "discountType": "PARTY_SIZE",
  "valueType": "PERCENTAGE",
  "value": 10,
  "minPartySize": 4,
  "maxPartySize": 6,
  "minOrderAmount": 200000
}
```

**Ví dụ 2: Nhóm 7-10 người**
```json
{
  "code": "GROUP7",
  "name": "Giảm 15% cho nhóm 7-10 người",
  "discountType": "PARTY_SIZE",
  "valueType": "PERCENTAGE",
  "value": 15,
  "minPartySize": 7,
  "maxPartySize": 10,
  "minOrderAmount": 300000
}
```

**Cách kiểm tra:**
```java
if (partySize >= minPartySize && partySize <= maxPartySize) {
    // Áp dụng discount
}
```

---

### 6. **BILL_TIER** - Giảm Giá Theo Bậc Hóa Đơn
Giảm giá theo nhiều bậc dựa trên tổng giá trị đơn hàng.

**Đặc điểm:**
- Sử dụng field `tierConfig` (JSON format)
- Tự động chọn bậc cao nhất mà khách hàng đủ điều kiện
- Hỗ trợ cả giảm % và giảm số tiền cố định

**Ví dụ 1: Giảm theo % theo bậc**
```json
{
  "code": "BIGTIER",
  "name": "Spend More Save More",
  "discountType": "BILL_TIER",
  "valueType": "PERCENTAGE",
  "value": 0,
  "tierConfig": "{\"tier1\":{\"min\":200000,\"discount\":5},\"tier2\":{\"min\":500000,\"discount\":10},\"tier3\":{\"min\":1000000,\"discount\":15}}"
}
```

**Ví dụ 2: Giảm số tiền cố định theo bậc**
```json
{
  "code": "FIXEDTIER",
  "name": "Fixed Discount Tiers",
  "discountType": "BILL_TIER",
  "valueType": "FIXED_AMOUNT",
  "value": 0,
  "tierConfig": "{\"tier1\":{\"min\":200000,\"discount\":20000},\"tier2\":{\"min\":500000,\"discount\":60000},\"tier3\":{\"min\":1000000,\"discount\":150000}}"
}
```

**Cách tính:**
- Đơn 250,000đ → Tier 1 → Giảm 5% = 12,500đ
- Đơn 600,000đ → Tier 2 → Giảm 10% = 60,000đ
- Đơn 1,200,000đ → Tier 3 → Giảm 15% = 180,000đ

---

## 💰 Kiểu Giá Trị (DiscountValueType)

### 1. **PERCENTAGE** - Giảm Theo Phần Trăm
Giảm % trên giá trị đơn hàng hoặc món.

**Công thức:**
```java
discountAmount = orderTotal * value / 100
if (maxDiscountAmount != null && discountAmount > maxDiscountAmount) {
    discountAmount = maxDiscountAmount;
}
```

**Ví dụ:**
- Đơn 500,000đ, giảm 20%, max 80,000đ
- Tính: 500,000 × 20% = 100,000đ → Giảm 80,000đ (giới hạn max)

---

### 2. **FIXED_AMOUNT** - Giảm Số Tiền Cố Định
Trả về số tiền giảm cố định.

**Công thức:**
```java
discountAmount = value
```

**Ví dụ:**
- value = 50,000đ → Giảm 50,000đ (bất kể tổng đơn)

---

### 3. **FIXED_PRICE** - Đặt Giá Cố Định
Đặt giá cố định cho món (chỉ dùng cho ITEM_SPECIFIC).

**Công thức:**
```java
discountAmount = orderTotal - value
```

**Ví dụ:**
- Phở giá gốc 60,000đ, value = 35,000đ
- Giảm: 60,000 - 35,000 = 25,000đ
- Khách trả: 35,000đ

⚠️ **Lưu ý:** Chỉ áp dụng cho `discountType = ITEM_SPECIFIC`

---

## 🗄️ Cấu Trúc Database

### Bảng `discounts`

```sql
CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,              -- Mã giảm giá (WELCOME10, FREESHIP)
    name VARCHAR(255) NOT NULL,                     -- Tên hiển thị
    description VARCHAR(500),                       -- Mô tả chi tiết
    
    -- Loại và giá trị discount
    discount_type VARCHAR(50) NOT NULL,             -- PERCENTAGE, FIXED_AMOUNT, ITEM_SPECIFIC, HOLIDAY, PARTY_SIZE, BILL_TIER
    value_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED_AMOUNT, FIXED_PRICE
    value DECIMAL(12,2) NOT NULL,                   -- Giá trị (%, số tiền, hoặc giá cố định)
    
    -- Điều kiện áp dụng
    min_order_amount DECIMAL(12,2),                 -- Tổng đơn tối thiểu
    max_discount_amount DECIMAL(12,2),              -- Số tiền giảm tối đa
    
    -- Thời gian
    start_date TIMESTAMP,                           -- Ngày bắt đầu
    end_date TIMESTAMP,                             -- Ngày kết thúc
    
    -- Giới hạn sử dụng
    usage_limit INTEGER,                            -- Số lần sử dụng tối đa (NULL = không giới hạn)
    used_count INTEGER DEFAULT 0,                   -- Số lần đã sử dụng
    
    -- Cấu hình đặc biệt
    min_party_size INTEGER,                         -- Số người tối thiểu (PARTY_SIZE)
    max_party_size INTEGER,                         -- Số người tối đa (PARTY_SIZE)
    tier_config TEXT,                               -- JSON config bậc (BILL_TIER)
    applicable_days TEXT,                           -- Ngày áp dụng (HOLIDAY)
    apply_to_specific_items BOOLEAN DEFAULT false,  -- Flag cho ITEM_SPECIFIC
    
    -- Metadata
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint
    CONSTRAINT chk_discount_value_type CHECK (value_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FIXED_PRICE'))
);
```

### Bảng `discount_items` (Junction Table)

```sql
CREATE TABLE discount_items (
    discount_id BIGINT NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    PRIMARY KEY (discount_id, item_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Mục đích:** Liên kết discount với các món cụ thể (cho ITEM_SPECIFIC)

---

## 🔌 API Endpoints

### 1. **Tạo Discount Mới**

**POST** `/api/discounts`

**Request Body:**
```json
{
  "code": "SUMMER25",
  "name": "Summer Sale 25%",
  "description": "Giảm 25% cho mùa hè",
  "discountType": "PERCENTAGE",
  "valueType": "PERCENTAGE",
  "value": 25,
  "minOrderAmount": 150000,
  "maxDiscountAmount": 100000,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-08-31T23:59:59",
  "usageLimit": 1000,
  "active": true
}
```

**Response:**
```json
{
  "id": 13,
  "code": "SUMMER25",
  "name": "Summer Sale 25%",
  "description": "Giảm 25% cho mùa hè",
  "discountType": "PERCENTAGE",
  "valueType": "PERCENTAGE",
  "value": 25.00,
  "minOrderAmount": 150000.00,
  "maxDiscountAmount": 100000.00,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-08-31T23:59:59",
  "usageLimit": 1000,
  "usedCount": 0,
  "active": true,
  "createdAt": "2026-01-25T20:00:00",
  "updatedAt": "2026-01-25T20:00:00"
}
```

---

### 2. **Lấy Danh Sách Discount**

**GET** `/api/discounts`

**Query Parameters:**
- `active` (boolean): Lọc theo trạng thái
- `discountType` (string): Lọc theo loại discount

**Response:**
```json
[
  {
    "id": 1,
    "code": "WELCOME10",
    "name": "Welcome Discount",
    "discountType": "PERCENTAGE",
    "valueType": "PERCENTAGE",
    "value": 10.00,
    "active": true
  },
  {
    "id": 2,
    "code": "FREESHIP",
    "name": "Free Shipping",
    "discountType": "FIXED_AMOUNT",
    "valueType": "FIXED_AMOUNT",
    "value": 30000.00,
    "active": true
  }
]
```

---

### 3. **Lấy Chi Tiết Discount**

**GET** `/api/discounts/{id}`

---

### 4. **Cập Nhật Discount**

**PUT** `/api/discounts/{id}`

**Request Body:** (Tương tự POST, nhưng tất cả field đều optional)
```json
{
  "value": 30,
  "maxDiscountAmount": 120000,
  "active": true
}
```

---

### 5. **Xóa Discount**

**DELETE** `/api/discounts/{id}`

---

### 6. **Áp Dụng Discount Cho Đơn Hàng**

**POST** `/api/discounts/apply`

**Request Body:**
```json
{
  "discountCode": "WELCOME10",
  "orderTotal": 500000,
  "partySize": 5,
  "items": [
    {"itemId": 1, "quantity": 2, "price": 100000},
    {"itemId": 2, "quantity": 3, "price": 150000}
  ]
}
```

**Response:**
```json
{
  "originalTotal": 500000,
  "discountAmount": 50000,
  "finalTotal": 450000,
  "discountApplied": "WELCOME10",
  "discountName": "Welcome Discount"
}
```

---

## 📝 Ví Dụ Tạo Discount

### Ví Dụ 1: Flash Sale Cuối Tuần
```json
{
  "code": "FLASHWEEKEND",
  "name": "Flash Sale Cuối Tuần",
  "description": "Giảm 30% vào thứ 7 và chủ nhật",
  "discountType": "HOLIDAY",
  "valueType": "PERCENTAGE",
  "value": 30,
  "applicableDays": "SATURDAY,SUNDAY",
  "minOrderAmount": 200000,
  "maxDiscountAmount": 150000,
  "startDate": "2026-02-01T00:00:00",
  "endDate": "2026-02-28T23:59:59",
  "active": true
}
```

---

### Ví Dụ 2: Combo Nhóm
```json
{
  "code": "BIGGROUP",
  "name": "Ưu đãi nhóm từ 8 người",
  "description": "Giảm 20% cho nhóm từ 8-12 người",
  "discountType": "PARTY_SIZE",
  "valueType": "PERCENTAGE",
  "value": 20,
  "minPartySize": 8,
  "maxPartySize": 12,
  "minOrderAmount": 500000,
  "active": true
}
```

---

### Ví Dụ 3: Món Đặc Biệt Giá Ưu Đãi
```json
{
  "code": "STEAKDEAL",
  "name": "Steak giá đặc biệt 99k",
  "description": "Bò bít tết chỉ còn 99,000đ",
  "discountType": "ITEM_SPECIFIC",
  "valueType": "FIXED_PRICE",
  "value": 99000,
  "applyToSpecificItems": true,
  "startDate": "2026-02-01T00:00:00",
  "endDate": "2026-02-14T23:59:59",
  "usageLimit": 200,
  "active": true
}
```

**Sau khi tạo, cần thêm món vào discount:**
```sql
INSERT INTO discount_items (discount_id, item_id) 
VALUES (14, 5);  -- item_id = 5 là Bò bít tết
```

---

### Ví Dụ 4: Tích Lũy Theo Bậc
```json
{
  "code": "VIP2026",
  "name": "Ưu đãi VIP theo bậc",
  "description": "Càng mua nhiều càng giảm sâu",
  "discountType": "BILL_TIER",
  "valueType": "PERCENTAGE",
  "value": 0,
  "tierConfig": "{\"tier1\":{\"min\":300000,\"discount\":5},\"tier2\":{\"min\":600000,\"discount\":12},\"tier3\":{\"min\":1000000,\"discount\":20}}",
  "active": true
}
```

**Giải thích tierConfig:**
- Đơn 300k-599k: Giảm 5%
- Đơn 600k-999k: Giảm 12%
- Đơn từ 1M trở lên: Giảm 20%

---

## 🧮 Cách Tính Discount

### Service Class: `DiscountCalculationService`

```java
@Service
@RequiredArgsConstructor
public class DiscountCalculationService {
    
    /**
     * Tính số tiền giảm dựa trên valueType
     */
    private BigDecimal calculatePercentageDiscount(
            BigDecimal orderTotal, 
            BigDecimal value, 
            DiscountValueType valueType) {
        
        return switch (valueType) {
            case PERCENTAGE -> orderTotal
                    .multiply(value)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            case FIXED_AMOUNT -> value;
            
            case FIXED_PRICE -> orderTotal.subtract(value);
        };
    }
}
```

### Logic Chi Tiết

#### 1. PERCENTAGE Discount
```java
BigDecimal discountAmount = orderTotal.multiply(value)
    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

// Áp dụng max discount nếu có
if (maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0) {
    discountAmount = maxDiscountAmount;
}
```

**Ví dụ:**
- Đơn: 500,000đ
- Value: 20%
- Max: 80,000đ
- Tính: 500,000 × 20% = 100,000đ → **Giảm 80,000đ** (max limit)

#### 2. FIXED_AMOUNT Discount
```java
BigDecimal discountAmount = value;
```

**Ví dụ:**
- Value: 50,000đ
- **Giảm: 50,000đ** (cố định)

#### 3. FIXED_PRICE Discount
```java
BigDecimal discountAmount = orderTotal.subtract(value);
```

**Ví dụ:**
- Giá gốc món: 120,000đ
- Value (giá mới): 79,000đ
- Giảm: 120,000 - 79,000 = **41,000đ**

---

## 🔄 Flow Xử Lý Discount

### Flow 1: Admin Tạo Discount

```
1. Admin truy cập /api/discounts (POST)
   ↓
2. DiscountController nhận CreateDiscountRequest
   ↓
3. Validate dữ liệu (@Valid annotation)
   - code không trùng
   - value > 0
   - dates hợp lệ
   ↓
4. DiscountService.createDiscount()
   ↓
5. DiscountMapper.toEntity() - Convert DTO → Entity
   ↓
6. DiscountRepository.save() - Lưu vào DB
   ↓
7. DiscountMapper.toResponse() - Convert Entity → DTO
   ↓
8. Return DiscountResponse
```

---

### Flow 2: User Áp Dụng Discount Khi Đặt Món

```
1. User nhập mã discount: "WELCOME10"
   ↓
2. Frontend gọi /api/discounts/validate
   Request: { code: "WELCOME10", orderTotal: 500000 }
   ↓
3. DiscountService.validateDiscount()
   ↓
4. Kiểm tra discount tồn tại
   ↓
5. Kiểm tra active = true
   ↓
6. Kiểm tra thời gian (startDate <= now <= endDate)
   ↓
7. Kiểm tra usage limit (usedCount < usageLimit)
   ↓
8. Kiểm tra minOrderAmount
   ↓
9. Kiểm tra điều kiện đặc biệt:
   - HOLIDAY: Check applicableDays
   - PARTY_SIZE: Check minPartySize, maxPartySize
   - ITEM_SPECIFIC: Check món có trong discount_items
   - BILL_TIER: Chọn tier phù hợp
   ↓
10. DiscountCalculationService.calculate()
    - Tính discountAmount theo valueType
    - Áp dụng maxDiscountAmount
   ↓
11. Return CalculationResult
    {
      "originalTotal": 500000,
      "discountAmount": 50000,
      "finalTotal": 450000
    }
```

---

### Flow 3: Xác Nhận Đơn Hàng & Tăng used_count

```
1. User xác nhận đặt hàng
   ↓
2. OrderService.createOrder()
   ↓
3. Lưu order vào DB
   ↓
4. DiscountService.incrementUsedCount(discountId)
   ↓
5. UPDATE discounts SET used_count = used_count + 1
   WHERE id = ?
   ↓
6. Kiểm tra usedCount >= usageLimit
   → Nếu đạt limit: SET active = false
   ↓
7. Return success
```

---

## 💡 Use Cases Thực Tế

### Use Case 1: Happy Hour (17h-19h)

**Yêu cầu:** Giảm 20% từ 17h-19h mỗi ngày

**Giải pháp:**
```json
{
  "code": "HAPPYHOUR",
  "name": "Happy Hour 17h-19h",
  "discountType": "PERCENTAGE",
  "valueType": "PERCENTAGE",
  "value": 20,
  "minOrderAmount": 100000
}
```

**Logic bổ sung (trong code):**
```java
LocalTime now = LocalTime.now();
if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(19, 0))) {
    // Áp dụng discount
}
```

---

### Use Case 2: Combo Set Menu

**Yêu cầu:** Mua 2 món A + 1 món B chỉ còn 199k

**Giải pháp:**
```json
{
  "code": "COMBO199",
  "name": "Combo Set Menu 199k",
  "discountType": "ITEM_SPECIFIC",
  "valueType": "FIXED_PRICE",
  "value": 199000,
  "applyToSpecificItems": true
}
```

**Thêm logic kiểm tra combo trong code:**
```java
// Check đơn có đủ 2 món A + 1 món B không
if (hasComboItems(order, itemA, 2, itemB, 1)) {
    // Áp dụng giá 199k cho combo
}
```

---

### Use Case 3: Tích Điểm - Hạng Khách Hàng

**Yêu cầu:** 
- Bronze: Giảm 5%
- Silver: Giảm 10%
- Gold: Giảm 15%

**Giải pháp:** Tạo 3 discount riêng
```json
{
  "code": "BRONZE_MEMBER",
  "name": "Ưu đãi hạng Bronze",
  "discountType": "PERCENTAGE",
  "valueType": "PERCENTAGE",
  "value": 5
}
```

**Logic:** Kiểm tra hạng khách hàng từ bảng `users` và tự động áp dụng discount tương ứng.

---

### Use Case 4: Giảm Giá Lũy Tiến

**Yêu cầu:** 
- Đơn 200k-499k: Giảm 10k
- Đơn 500k-999k: Giảm 30k
- Đơn từ 1M: Giảm 70k

**Giải pháp:**
```json
{
  "code": "SAVETIER",
  "name": "Tiết kiệm theo bậc",
  "discountType": "BILL_TIER",
  "valueType": "FIXED_AMOUNT",
  "value": 0,
  "tierConfig": "{\"tier1\":{\"min\":200000,\"discount\":10000},\"tier2\":{\"min\":500000,\"discount\":30000},\"tier3\":{\"min\":1000000,\"discount\":70000}}"
}
```

---

### Use Case 5: Sinh Nhật Nhà Hàng

**Yêu cầu:** Giảm 40% vào ngày 15/3 hàng năm

**Giải pháp:**
```json
{
  "code": "BIRTHDAY2026",
  "name": "Sinh nhật nhà hàng năm 2026",
  "discountType": "HOLIDAY",
  "valueType": "PERCENTAGE",
  "value": 40,
  "applicableDays": "2026-03-15",
  "maxDiscountAmount": 200000,
  "startDate": "2026-03-15T00:00:00",
  "endDate": "2026-03-15T23:59:59"
}
```

---

## ⚠️ Lưu Ý Quan Trọng

### 1. Validation

- ✅ `code` phải unique
- ✅ `value` phải > 0
- ✅ `startDate` < `endDate`
- ✅ `minOrderAmount` ≥ 0
- ✅ `maxDiscountAmount` ≥ 0
- ✅ `minPartySize` < `maxPartySize`
- ✅ `tierConfig` phải là valid JSON
- ✅ `applicableDays` format đúng

### 2. Performance

- 📊 Index trên `code`, `discount_type`, `value_type`, `active`
- 📊 Composite index trên `(active, start_date, end_date)`
- 📊 Định kỳ xóa discount hết hạn

### 3. Security

- 🔒 Chỉ ADMIN mới được tạo/sửa/xóa discount
- 🔒 Validate input để tránh SQL injection
- 🔒 Rate limit API để tránh abuse

### 4. Business Logic

- 💼 Không cho phép stack nhiều discount (hoặc giới hạn)
- 💼 Ưu tiên discount có giá trị cao hơn
- 💼 Log lại lịch sử sử dụng discount
- 💼 Thông báo khi discount sắp hết hạn/hết lượt

---

## 🔧 Configuration

### application.yml
```yaml
discount:
  max-per-order: 3          # Tối đa 3 discount/đơn
  allow-stacking: false     # Không cho phép stack
  auto-deactivate: true     # Tự động deactivate khi hết hạn
  cleanup-days: 90          # Xóa discount hết hạn sau 90 ngày
```

---

## 📞 Support

Nếu cần hỗ trợ hoặc có thắc mắc, vui lòng liên hệ:
- **Email:** dev@restaurant.com
- **Slack:** #discount-system
- **Documentation:** [Wiki Link]

---

**Version:** 1.0.0  
**Last Updated:** 2026-01-25  
**Author:** Development Team
