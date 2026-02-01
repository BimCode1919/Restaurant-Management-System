# 🚀 HƯỚNG DẪN CHẠY DỰ ÁN

## Bước 1: Khởi động Database và Redis

```bash
docker-compose up -d
```

Kiểm tra:
```bash
docker-compose ps
```

## Bước 2: Chạy ứng dụng

```bash
mvn spring-boot:run
```

## Bước 3: Test API

Mở trình duyệt: **http://localhost:8080/api/swagger-ui.html**

### Login để lấy token:
1. Tìm endpoint **POST /auth/login**
2. Click **Try it out**
3. Nhập:
   ```json
   {
     "email": "admin@restaurant.com",
     "password": "admin123"
   }
   ```
4. Click **Execute**
5. Copy `accessToken`
6. Click nút **Authorize** ở đầu trang
7. Nhập: `Bearer {token}`

## Tài khoản mặc định

**Admin**: admin@restaurant.com / admin123  
**Staff**: staff@restaurant.com / admin123

## Dữ liệu mẫu

Database tự động tạo:
- 2 roles, 2 users
- 3 categories
- 6 món ăn
- 4 nguyên liệu
- 3 bàn với QR code

## Dừng ứng dụng

Spring Boot: `Ctrl + C`  
Docker: `docker-compose down`  
Reset DB: `docker-compose down -v` rồi `docker-compose up -d`

## Xử lý lỗi

**Port 8080 đã được dùng:**
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Không kết nối được PostgreSQL:**
```bash
docker-compose restart postgres
docker-compose logs postgres
```

**Build lỗi:**
```bash
mvn clean
mvn clean compile
```

## Kết nối Database

**PostgreSQL**  
Host: localhost:5432  
Database: restaurant_qr_db  
User: restaurant_user  
Pass: restaurant_pass

**Redis**  
Host: localhost:6379
