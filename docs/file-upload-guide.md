# File Upload Guide (Presigned URL)

## Tổng quan

Thay vì upload file qua backend, frontend upload **trực tiếp lên MinIO** thông qua presigned URL. Backend chỉ cấp URL, không xử lý file.

```
FE → [1] POST /api/files/presigned-url  → BE tạo presigned URL
FE → [2] PUT {presignedUrl}             → Upload thẳng lên MinIO
FE → [3] Lưu publicUrl vào item.imageUrl (hoặc field khác)
```

---

## Bước 1 — Lấy presigned URL

**Endpoint:** `POST /api/files/presigned-url`
**Auth:** Bearer Token (ADMIN hoặc STAFF)

### Request

```json
{
  "fileName": "pizza.jpg",
  "contentType": "image/jpeg"
}
```

| Field | Mô tả |
|-------|-------|
| `fileName` | Tên file gốc, dùng để lấy extension |
| `contentType` | MIME type của file |

### Response

```json
{
  "statusCode": 200,
  "message": "Presigned URL generated successfully",
  "data": {
    "presignedUrl": "http://localhost:9000/restaurant-images/images/a1b2c3d4-...jpg?X-Amz-Algorithm=...",
    "publicUrl": "http://localhost:9000/restaurant-images/images/a1b2c3d4-...jpg",
    "expiresInMinutes": 15
  }
}
```

| Field | Mô tả |
|-------|-------|
| `presignedUrl` | URL để PUT file lên (có hiệu lực trong 15 phút) |
| `publicUrl` | URL công khai để truy cập ảnh sau khi upload |
| `expiresInMinutes` | Thời gian hết hạn của presignedUrl |

---

## Bước 2 — Upload file lên MinIO

Dùng `presignedUrl` để **PUT** file trực tiếp. Lưu ý **không** kèm Authorization header.

### Fetch API

```javascript
async function uploadFile(file) {
  const token = localStorage.getItem('accessToken');

  // Bước 1: Lấy presigned URL
  const res = await fetch('/api/files/presigned-url', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({
      fileName: file.name,
      contentType: file.type,
    }),
  });

  const { data } = await res.json();
  const { presignedUrl, publicUrl } = data;

  // Bước 2: Upload thẳng lên MinIO (không cần token)
  await fetch(presignedUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  });

  // Bước 3: Trả về publicUrl để lưu vào DB
  return publicUrl;
}
```

### Axios

```javascript
async function uploadFile(file) {
  const token = localStorage.getItem('accessToken');

  // Bước 1: Lấy presigned URL
  const { data } = await axios.post('/api/files/presigned-url',
    { fileName: file.name, contentType: file.type },
    { headers: { Authorization: `Bearer ${token}` } }
  );

  const { presignedUrl, publicUrl } = data.data;

  // Bước 2: Upload thẳng lên MinIO
  await axios.put(presignedUrl, file, {
    headers: { 'Content-Type': file.type },
  });

  return publicUrl;
}
```

---

## Bước 3 — Lưu publicUrl

Sau khi upload xong, dùng `publicUrl` làm giá trị cho `imageUrl` khi tạo/cập nhật item:

```javascript
const imageUrl = await uploadFile(selectedFile);

await axios.post('/api/items', {
  name: 'Pizza Margherita',
  price: 120000,
  imageUrl: imageUrl,   // <-- public URL từ MinIO
  categoryId: 1,
});
```

---

## Ví dụ với React + input file

```jsx
function ItemImageUpload({ onImageUploaded }) {
  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      const publicUrl = await uploadFile(file);
      onImageUploaded(publicUrl);
    } catch (err) {
      console.error('Upload failed:', err);
    }
  };

  return <input type="file" accept="image/*" onChange={handleFileChange} />;
}
```

---

## Content-Type phổ biến

| Loại file | contentType |
|-----------|-------------|
| JPEG | `image/jpeg` |
| PNG | `image/png` |
| WebP | `image/webp` |
| GIF | `image/gif` |
| SVG | `image/svg+xml` |

---

## Lưu ý

- Presigned URL **hết hạn sau 15 phút** — lấy URL xong phải upload ngay.
- Upload phải dùng method **PUT**, không phải POST.
- Header `Content-Type` khi PUT **phải khớp** với `contentType` đã gửi lên BE.
- Ảnh sau khi upload là **public** — ai cũng truy cập được qua `publicUrl`.
- Bucket name mặc định: `restaurant-images`, MinIO endpoint: `http://localhost:9000`.
