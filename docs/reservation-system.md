# Reservation System — Chi tiết Flow & Code

## Mục lục
1. [State Machine](#1-state-machine)
2. [API Endpoints](#2-api-endpoints)
3. [CREATE — Tạo reservation](#3-create--post-reservations)
4. [DEPOSIT — Xác nhận đặt cọc](#4-deposit--put-reservationsiddeposit-paid)
5. [CONFIRM — Xác nhận reservation](#5-confirm--put-reservationsidconfirm)
6. [CHECK-IN — Khách đến](#6-check-in--put-reservationsidcheck-in)
7. [CANCEL — Hủy reservation](#7-cancel--put-reservationsidcancel)
8. [NO-SHOW — Khách không đến](#8-no-show--put-reservationsidno-show)
9. [UPDATE — Cập nhật reservation](#9-update--put-reservationsid)
10. [QUERY — Truy vấn](#10-query)
11. [Schedulers tự động](#11-schedulers-tự-động)
12. [Deposit Calculation](#12-deposit-calculation)
13. [Email Notifications](#13-email-notifications)
14. [Full Flow từ FE](#14-full-flow-từ-fe)

---

## 1. State Machine

```
                  ┌──────────────────────────┐
                  │         PENDING           │  ← Vừa tạo xong
                  └──────┬──────────┬─────────┘
                         │          │
              (deposit    │          │ cancel
               paid hoặc  │          ▼
               ko cần)    │      CANCELLED ◄──── CONFIRMED
                         │
                         ▼
                  ┌──────────────┐
                  │  CONFIRMED   │  ← Xác nhận, email gửi
                  └──┬───────┬───┘
                     │       │
             check-in│       │ no-show / cancel
                     ▼       ▼
                  SEATED   NO_SHOW
                     │
              (bill close)
                     ▼
                 COMPLETED
```

### Luật chuyển trạng thái

| Từ | Sang | Điều kiện |
|----|------|-----------|
| `PENDING` | `CONFIRMED` | Nếu `depositRequired=true` → phải `depositPaid=true` trước |
| `PENDING` | `CANCELLED` | Được phép bất kỳ lúc nào |
| `CONFIRMED` | `SEATED` | Check-in thủ công bởi staff |
| `CONFIRMED` | `CANCELLED` | Được phép |
| `CONFIRMED` | `NO_SHOW` | Phải qua grace period (15 phút mặc định) |
| `SEATED` | `COMPLETED` | Khi bill thanh toán xong |
| `SEATED` | `CANCELLED` | **Không cho phép** — khách đang ngồi |
| `COMPLETED/NO_SHOW` | bất kỳ | **Không cho phép** |

### TableStatus theo từng bước

| Sự kiện | TableStatus |
|---------|-------------|
| Tạo reservation **có deposit** | `RESERVED` ngay lập tức |
| Tạo reservation **không deposit** | `AVAILABLE` (scheduler xử lý sau) |
| Scheduler chạy 2h trước giờ (CONFIRMED) | `AVAILABLE` → `RESERVED` |
| Check-in | `RESERVED` → `OCCUPIED` |
| Cancel / No-show | `RESERVED` → `AVAILABLE` (bỏ qua OCCUPIED) |

---

## 2. API Endpoints

| Method | URL | Role | Mô tả |
|--------|-----|------|-------|
| `POST` | `/reservations` | ADMIN, STAFF, CUSTOMER, MANAGER, CASHIER | Tạo reservation |
| `GET` | `/reservations/{id}` | ADMIN, STAFF | Lấy 1 reservation |
| `GET` | `/reservations?status=` | ADMIN, STAFF | Danh sách theo status |
| `GET` | `/reservations/availability?date=` | Tất cả | Slots đã đặt (không lộ PII) |
| `PUT` | `/reservations/{id}` | ADMIN, STAFF | Cập nhật (chỉ PENDING) |
| `PUT` | `/reservations/{id}/confirm` | ADMIN, STAFF | Xác nhận reservation |
| `PUT` | `/reservations/{id}/check-in` | ADMIN, STAFF | Khách đến, check-in |
| `PUT` | `/reservations/{id}/cancel?reason=` | ADMIN, STAFF, MANAGER | Hủy reservation |
| `PUT` | `/reservations/{id}/no-show?reason=` | ADMIN, STAFF, MANAGER | Đánh no-show |
| `PUT` | `/reservations/{id}/deposit-paid` | ADMIN, CASHIER | Xác nhận đã trả deposit |
| `POST` | `/reservations/reserve-tables` | ADMIN | Trigger scheduler thủ công |

---

## 3. CREATE — `POST /reservations`

### Request Body (`CreateReservationRequest`)

```json
{
  "customerName": "Nguyen Van A",
  "customerPhone": "0901234567",
  "customerEmail": "a@example.com",
  "partySize": 4,
  "reservationTime": "2026-03-20T18:00:00",
  "note": "Cần bàn view sân vườn",
  "requestedTableIds": [1, 2],
  "preOrderItems": [
    { "itemId": 5, "quantity": 2 },
    { "itemId": 8, "quantity": 1 }
  ]
}
```

| Field | Bắt buộc | Validation |
|-------|----------|-----------|
| `customerName` | Có | NotBlank |
| `customerPhone` | Có | Pattern `^(\+84\|0)[0-9]{9,10}$` |
| `customerEmail` | Không | Email format |
| `partySize` | Có | Min=1, Max=50 |
| `reservationTime` | Có | @Future |
| `note` | Không | — |
| `requestedTableIds` | Không | null = auto-assign |
| `preOrderItems` | Không | null/empty = không pre-order |

### Logic nội bộ — 12 bước

```
Step 1: validateReservationTime(reservationTime)
        ├── reservationTime >= now + 1h     (báo trước tối thiểu 1 tiếng)
        ├── reservationTime <= now + 3 days  (không đặt quá xa)
        └── hour phải trong [9, 22)          (9:00 AM – 10:00 PM)

Step 2: findAndValidateTables(requestedTableIds, partySize, reservationTime, excludeId=0L)
        ├── Nếu requestedTableIds != null:
        │   ├── tableRepository.findAllById() → kiểm tra tất cả tồn tại
        │   └── findConflictingReservations(tableIds, endTime, startMinus2h, 0L)
        │       Overlap: existingStart < newEnd  AND  existingStart > newStart-2h
        │       Status check: IN ('PENDING', 'CONFIRMED', 'SEATED')
        └── Nếu null/empty: tự động pick 1 bàn AVAILABLE trong window thời gian

Step 3: resolveUser(creatorEmail)
        └── Lấy User entity từ JWT email

Step 4: resolvePreOrderItems(preOrderItems)
        ├── Với mỗi item: kiểm tra itemId tồn tại
        ├── Kiểm tra item.available = true
        └── Trả về List<ResolvedPreOrderItem(item, quantity)>

Step 5: Tính deposit
        ├── isLargeGroup    = partySize > 10
        ├── hasPreOrders    = preOrderItems không rỗng
        ├── requiresDeposit = isLargeGroup OR hasPreOrders
        └── depositAmount   = (preOrderTotal + tableCount × 300,000) × 10%
                              (null nếu không cần deposit)

Step 6: Save Reservation
        └── status=PENDING, depositPaid=false, depositRequired=requiresDeposit

Step 7: Tính billTotal
        └── requiresDeposit ? max(preOrderTotal - depositAmount, 0) : 0

Step 8: Create Bill (status=OPEN, totalPrice=billTotal)

Step 9: Link Bill ↔ Reservation (2 chiều)

Step 10: linkBillTables() — tạo BillTable join records cho mỗi bàn

Step 11: createPreOrders() — nếu có preOrderItems
         ├── Tạo Order (type=PRE_ORDER, linked reservation + bill)
         └── Tạo OrderDetail cho từng item

Step 12: Nếu requiresDeposit → set tables RESERVED ngay
         Nếu không → tables vẫn AVAILABLE (scheduler xử lý 2h trước giờ đặt)
```

### Response (`ReservationResponse`)

```json
{
  "id": 42,
  "customerName": "Nguyen Van A",
  "customerPhone": "0901234567",
  "customerEmail": "a@example.com",
  "partySize": 4,
  "reservationTime": "2026-03-20T18:00:00",
  "status": "PENDING",
  "note": "Cần bàn view sân vườn",
  "depositRequired": true,
  "depositAmount": 80000,
  "depositPaid": false,
  "gracePeriodMinutes": 15,
  "arrivalTime": null,
  "cancelledAt": null,
  "cancellationReason": null,
  "tableNumbers": ["T1", "T2"],
  "billId": 99,
  "createdAt": "2026-03-19T10:00:00",
  "updatedAt": "2026-03-19T10:00:00",
  "canCheckIn": false,
  "canCancel": true,
  "canMarkNoShow": false
}
```

| Field UI | Ý nghĩa |
|----------|---------|
| `canCheckIn` | `true` khi status = `CONFIRMED` |
| `canCancel` | `true` khi status = `PENDING` hoặc `CONFIRMED` |
| `canMarkNoShow` | `true` khi đủ điều kiện no-show (CONFIRMED + quá grace period) |

---

## 4. DEPOSIT — `PUT /reservations/{id}/deposit-paid`

**Role:** ADMIN, CASHIER

### Guards (theo thứ tự)
1. `depositRequired` phải `true` → 400 nếu không
2. `depositPaid` phải `false` → 400 nếu đã trả rồi
3. Status không phải `CANCELLED` hoặc `NO_SHOW` → 400

### Logic
- Set `depositPaid = true`
- Save và trả về ReservationResponse

> Sau bước này, staff mới có thể gọi `/confirm`.

---

## 5. CONFIRM — `PUT /reservations/{id}/confirm`

**Role:** ADMIN, STAFF

### Guards (theo thứ tự)
1. Status phải `PENDING` → 400 "Only PENDING reservations can be confirmed"
2. Nếu `depositRequired=true` AND `depositPaid=false` → 400 "deposit chưa trả"

### Logic
- Set `status = CONFIRMED`
- Save
- **Gửi email xác nhận** (`sendReservationConfirmedMail`)

---

## 6. CHECK-IN — `PUT /reservations/{id}/check-in`

**Role:** ADMIN, STAFF

### Guard
- Status phải `CONFIRMED` → 400 nếu không

### Logic
- `reservation.markAsSeated()` → status = `SEATED`, `arrivalTime = now`
- Tất cả tables của reservation → `OCCUPIED`
- `tableRepository.saveAll(tables)` (bắt buộc saveAll vì không có cascade @ManyToMany)

---

## 7. CANCEL — `PUT /reservations/{id}/cancel?reason=...`

**Role:** ADMIN, STAFF, MANAGER

### Guards (từ chối nếu status là)
- `COMPLETED` — reservation đã hoàn tất
- `NO_SHOW` — đã bị đánh no-show
- `SEATED` — khách đang ngồi, bill đang active

### Logic
1. `status = CANCELLED`, `cancelledAt = now`, `cancellationReason = reason`
2. `freeUpTables()` — chỉ table có status `RESERVED` mới reset về `AVAILABLE`
   - Skip table `OCCUPIED` (có thể bàn đó đang được dùng bởi party khác)
3. Bill → `status = CANCELLED`
4. **Gửi email hủy** (`sendReservationCancelledMail`)

---

## 8. NO-SHOW — `PUT /reservations/{id}/no-show?reason=...`

**Role:** ADMIN, STAFF, MANAGER

### Guard
`reservation.isNoShowEligible()` phải `true`:
- `status == CONFIRMED`
- `reservationTime + gracePeriodMinutes < now` (15 phút mặc định)

### Logic
1. `reservation.markAsNoShow(reason)` → `status = NO_SHOW`
2. `freeUpTables()` → RESERVED → AVAILABLE
3. **Gửi email no-show** (`sendReservationNoShowMail`)

---

## 9. UPDATE — `PUT /reservations/{id}`

**Role:** ADMIN, STAFF

### Request Body (`UpdateReservationRequest`)

```json
{
  "customerName": "Nguyen Van B",
  "customerPhone": "0987654321",
  "customerEmail": "b@example.com",
  "partySize": 6,
  "reservationTime": "2026-03-20T19:00:00",
  "note": "Sinh nhật"
}
```

Tất cả field đều **optional** (chỉ cập nhật field nào có giá trị).

### Guard
- Status phải `PENDING` → 400 nếu không

### Logic đặc biệt khi đổi `reservationTime`
```
1. validateReservationTime(newTime)
2. findAndValidateTables(
       currentTableIds,       // giữ nguyên bàn hiện tại
       reservation.partySize,
       newTime,
       excludeId = reservation.getId()  // loại trừ chính nó khỏi conflict check
   )
3. reservation.setReservationTime(newTime)
```

---

## 10. QUERY

### `GET /reservations/{id}`
**Role:** ADMIN, STAFF
Trả về 1 `ReservationResponse` đầy đủ.

### `GET /reservations?status=`

| Query param | Kết quả |
|-------------|---------|
| `status=PENDING` | Tất cả reservation PENDING |
| `status=CONFIRMED` | Tất cả reservation CONFIRMED |
| `status=` (bỏ trống / null) | Tất cả reservation **hôm nay** (00:00 → 23:59) |

### `GET /reservations/availability?date=2026-03-20`
**Role:** Tất cả (kể cả CUSTOMER)

**Input:** `date` optional (ISO date `yyyy-MM-dd`), default = hôm nay

**Output — `List<BookedSlotResponse>`** (không có PII):
```json
[
  {
    "reservationTime": "2026-03-20T18:00:00",
    "reservationEndTime": "2026-03-20T20:00:00",
    "partySize": 4,
    "tableNumbers": ["T1", "T2"],
    "status": "CONFIRMED"
  }
]
```

> Chỉ trả về status `PENDING`, `CONFIRMED`, `SEATED`.
> `CANCELLED` và `NO_SHOW` bị lọc ra — khách không thấy.

---

## 11. Schedulers tự động

### Scheduler 1 — Auto No-Show

```
Cron: 0 */10 * * * *   (mỗi 10 phút)
```

**Flow:**
```
1. findOverdueReservations(cutoff = now - 15 phút)
   JPQL: status = 'CONFIRMED' AND reservationTime < :cutoff

2. Với mỗi reservation trong danh sách:
   if (reservation.isNoShowEligible()) {
       self.markAsNoShow(id, "Auto: customer did not arrive within grace period")
   }

3. Nếu 1 reservation fail → rollback riêng cái đó, không ảnh hưởng các cái khác
```

**Tại sao dùng `self.markAsNoShow()` thay vì `this.`?**

- `this.method()` = gọi trực tiếp trên object → bypass Spring AOP proxy → `@Transactional` **không hoạt động**
- `self.method()` = gọi qua Spring proxy → mỗi call có **transaction độc lập**
- Scheduler không có `@Transactional` → tránh 1 big transaction bọc toàn bộ batch

### Scheduler 2 — Auto Reserve Tables

```
Cron: 0 */5 * * * *   (mỗi 5 phút)
```

**Flow:**
```
1. findReservationsToReserveTable(now, now + 2h)
   JPQL: status = 'CONFIRMED'
         AND reservationTime BETWEEN :now AND :twoHourLater
         AND table.status = 'AVAILABLE'

2. Với mỗi reservation:
   - Lấy các bàn có status AVAILABLE
   - Set AVAILABLE → RESERVED
   - tableRepository.saveAll()
```

**Mục đích:** Đảm bảo bàn được "giữ chỗ" (RESERVED) trước khi khách đến, ngay cả với các reservation không cần deposit.

---

## 12. Deposit Calculation

### Công thức
```
depositAmount = (preOrderTotal + tableCount × 300,000) × 10%

Hằng số:
  TABLE_FEE    = 300,000 VND / bàn
  DEPOSIT_RATE = 10%
  LARGE_GROUP  = partySize > 10
```

### Khi nào cần deposit?
- `partySize > 10` (nhóm lớn)
- **HOẶC** có `preOrderItems`

### Ví dụ tính toán

```
Scenario: 4 người, 2 bàn, pre-order 2× item 50k + 1× item 100k

preOrderTotal = 2×50,000 + 1×100,000 = 200,000
tableFee      = 2 × 300,000          = 600,000
base          = 200,000 + 600,000    = 800,000
depositAmount = 800,000 × 10%        = 80,000 VND

billTotal     = max(200,000 - 80,000, 0) = 120,000 VND
```

### Khi không cần deposit
```
depositAmount = null
billTotal     = 0
```

---

## 13. Email Notifications

| Sự kiện | Method | Subject |
|---------|--------|---------|
| Confirm | `sendReservationConfirmedMail` | "Reservation Confirmed - Table Booking" |
| Cancel | `sendReservationCancelledMail` | "Reservation Cancelled" |
| No-show | `sendReservationNoShowMail` | "Reservation Marked As No Show" |

### Nội dung email
HTML template hiển thị:
- Tên khách, SĐT
- Thời gian đặt bàn
- Số người
- Số bàn
- Trạng thái

### Xử lý lỗi email
- Nếu `customerEmail` trống → skip (log warn), không throw
- Nếu gửi fail (`MessagingException`) → log error, **không rollback transaction**
  - Email failure không được phá vỡ business logic

---

## 14. Full Flow từ FE

### Flow cơ bản (không deposit)

```
[Khách / Staff]
  1. POST /reservations
     → 201 PENDING, tables vẫn AVAILABLE

[Staff]
  2. GET /reservations?status=PENDING (xem danh sách)
  3. PUT /reservations/{id}/confirm
     → CONFIRMED, email gửi khách

[Scheduler tự động — 2h trước giờ đặt]
  4. Tables → RESERVED

[Staff — khi khách đến]
  5. PUT /reservations/{id}/check-in
     → SEATED, tables → OCCUPIED

[Khách ăn, gọi thêm món qua bill bình thường]

[Thanh toán]
  6. Bill → PAID → COMPLETED
```

### Flow có deposit

```
[Staff tạo reservation]
  1. POST /reservations  →  201 PENDING, tables → RESERVED ngay

[Thu ngân nhận tiền]
  2. PUT /reservations/{id}/deposit-paid  →  depositPaid=true

[Staff xác nhận]
  3. PUT /reservations/{id}/confirm  →  CONFIRMED, email gửi

[Các bước tiếp theo giống flow cơ bản từ bước 4]
```

### Flow khách không đến

```
[Scheduler — mỗi 10 phút]
  1. Tìm CONFIRMED reservation quá 15 phút
  2. Auto-mark NO_SHOW
     → tables RESERVED → AVAILABLE
     → email no-show gửi khách

[Hoặc staff mark thủ công]
  PUT /reservations/{id}/no-show?reason=...
```

### Flow hủy

```
[Staff / Manager]
  PUT /reservations/{id}/cancel?reason=...
  → status CANCELLED
  → tables RESERVED → AVAILABLE
  → bill CANCELLED
  → email hủy gửi khách
```

### Khách xem lịch trống trước khi đặt

```
[Khách / FE — không cần login hoặc mọi role]
  GET /reservations/availability?date=2026-03-20
  → Danh sách slots đã đặt trong ngày
  → Chỉ thấy giờ, số người, bàn — không có thông tin khách khác
```

---

## 15. Conflict Detection — Chi tiết JPQL

```sql
SELECT r FROM Reservation r JOIN r.tables t
WHERE t.id IN :tableIds
  AND r.id != :excludeId
  AND r.status IN ('PENDING', 'CONFIRMED', 'SEATED')
  AND r.reservationTime < :end          -- existing bắt đầu trước khi new kết thúc
  AND r.reservationTime > :startMinus2h -- existing bắt đầu sau (new bắt đầu - 2h)
```

**Tham số truyền vào:**
```
end          = newReservationTime + 2h
startMinus2h = newReservationTime - 2h
excludeId    = 0L (khi tạo mới) | reservation.getId() (khi update)
```

**Overlap condition giải thích:**
```
Mỗi reservation chiếm 2h.
Hai slot [A, A+2h] và [B, B+2h] bị overlap khi:
  A < B+2h  AND  B < A+2h
  ↔
  existingStart < newEnd  AND  existingStart > newStart - 2h
```

---


