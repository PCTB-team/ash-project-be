# Admin FE Update Notes - 2026-07-31

## 1. Admin Logs: bỏ toggle "Chỉ của tôi"

Trang: `Admin / Nhật ký Admin`

Backend đã bỏ query param:

```http
currentAdminOnly
```

FE cần bỏ UI toggle:

```text
Chỉ của tôi
```

API hiện dùng:

```http
GET /admin/logs?page=0&size=10&logType=ADMIN_ACTION
```

Nếu vẫn cần lọc theo người thực hiện cụ thể, dùng param `actor`:

```http
GET /admin/logs?page=0&size=10&logType=ADMIN_ACTION&actor=admin
```

Không gửi `currentAdminOnly` nữa.

## 2. User Storage Plan: thêm chức năng gỡ gói

Trang: `Admin / Người dùng`

Backend có API mới để gỡ gói storage của user:

```http
DELETE /admin/users/{userId}/storage-plan?reason=Manual removal
```

`reason` là optional.

Khi gỡ gói, backend sẽ:

- Reset `storageQuota` về default storage của hệ thống, fallback là `500MB`.
- Set `storageExpiredAt = null`.
- Ghi audit log action `REMOVE_USER_STORAGE_PLAN`.
- Trả về `UserResponse` mới nhất.

FE gợi ý:

- Thêm action "Gỡ gói" ở user row hoặc user detail.
- Hiển thị confirm modal trước khi gọi API.
- Sau khi thành công, refetch user list/detail.
- Nếu có field hạn gói, `storageExpiredAt = null` nên hiển thị là gói mặc định hoặc không có hạn.

Response shape giống user detail/list response hiện tại.

## 3. Payment Expired: checkout có thời gian hết hạn

Trang: `Payment checkout` và `Admin / Thanh toán`

Checkout response có thêm field:

```json
{
  "transactionId": "tx-id",
  "checkoutUrl": "https://...",
  "amount": 100000,
  "orderCode": 123456789,
  "expiredAt": "2026-07-31T23:10:58"
}
```

Backend hiện set hạn thanh toán bằng giây, default là `30` giây:

```properties
app.payment.checkout-expiry-seconds=30
```

Admin payment list response có thêm:

```json
{
  "transactionId": "tx-id",
  "orderCode": 123456789,
  "username": "student",
  "email": "student@example.com",
  "planName": "PRO",
  "amount": 100000,
  "status": "TIMEOUT",
  "createdAt": "2026-07-31T23:10:28",
  "expiredAt": "2026-07-31T23:10:58"
}
```

Lưu ý status:

- Backend lưu expired payment là `TIMEOUT`.
- FE có thể hiển thị `TIMEOUT` là `Hết hạn`.
- Nếu FE đang dùng filter `EXPIRED`, backend vẫn map được sang `TIMEOUT`.

Filter gợi ý:

```http
GET /admin/payments?page=0&size=20&status=TIMEOUT
```

hoặc:

```http
GET /admin/payments?page=0&size=20&status=EXPIRED
```

Cả hai đều dùng được.

