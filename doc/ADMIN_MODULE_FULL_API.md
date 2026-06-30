# Admin Module Full API Contract

Base URL:

```text
/api/v1
```

All endpoints below require an access token for an account with role `ADMIN`, unless explicitly noted.

Common response wrapper:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

Paginated APIs return Spring `Page` inside `result`. FE should read:

```text
result.content
result.totalElements
result.totalPages
result.number
result.size
```

## FE Implementation Checklist

FE cần triển khai các màn Admin theo các nhóm sau:

| Module | FE cần làm |
| --- | --- |
| Dashboard | Gọi dashboard stats, render stat cards, storage chart, file type chart, user/revenue trend, recent activity |
| Audit Logs | Bảng logs có filter `ADMIN_ACTION`, `USER_ACTION`, `DOCUMENT_LOG`, phân trang |
| Users | Bảng user có search, role filter, status filter, lock/unlock, delete, change role, xem storage, số tài liệu, ngày tạo |
| Manual Storage Reconciliation | Trong user detail/table action, thêm modal chọn gói storage để admin cấp lại gói khi user đã thanh toán nhưng webhook lỗi |
| Documents | Bảng document có search, filter loại file, soft delete document, stat cards |
| Groups | Bảng group, detail members, update status, delete group, group stats |
| Payments | Bảng payment transaction có filter status, phân trang |
| Revenue | Chart doanh thu theo giờ/ngày/tháng/năm, ưu tiên thêm tab/thanh chọn `HOUR`, `DAY`, `MONTH`, `YEAR` |
| Plans | Danh sách gói storage, update plan metadata, dùng list plan cho manual storage reconciliation |
| AI Stats | Stat cards và chart usage theo ngày trong tuần |
| Settings | Form settings, load current settings, submit update |
| Error Handling | Đọc `code` và `message`, không chỉ dựa vào HTTP status |

Frontend route gợi ý:

| Page | Suggested route |
| --- | --- |
| Dashboard | `/admin/dashboard` |
| Logs | `/admin/logs` |
| Users | `/admin/users` |
| Documents | `/admin/documents` |
| Groups | `/admin/groups` |
| Payments/Revenue/Plans | `/admin/payments` |
| AI Stats | `/admin/ai` |
| Settings | `/admin/settings` |

Common FE rules:

- Luôn gửi `Authorization: Bearer <accessToken>`.
- Với API phân trang, giữ state `page`, `size`, `totalElements`, `totalPages`.
- Với các thao tác thay đổi dữ liệu như lock/unlock/delete/update plan/set storage plan, sau khi thành công cần refetch list hoặc update row local.
- Date-time từ BE là ISO string, FE format theo UI.
- Dung lượng là bytes, FE tự format sang MB/GB.
- Revenue amount là VND, FE format tiền Việt.

## 1. Dashboard

### Get dashboard statistics

```http
GET /admin/dashboard/stats
```

Purpose: Load overview stat cards, storage distribution, revenue/user trend, upload trend, and recent activities.

Count notes:

- `totalUsers`: all accounts counted once, including active and locked accounts.
- `activeUsers`: accounts where `accountNonLocked = true`.
- `pendingReports`: locked accounts where `accountNonLocked = false`.
- `activeUsersRightNow`: distinct users with login history in the latest activity window; no fake fallback value is returned.

FE cần làm:

- Render stat cards: tổng user, user active, user bị khóa, tổng document, tổng group, tổng doanh thu, doanh thu tháng này.
- Với `totalUsers`, không cộng `activeUsers + pendingReports` nếu đã dùng `totalUsers`; field này đã đếm tất cả account đúng 1 lần.
- Render chart file type từ `fileTypeDistribution`.
- Render chart user growth từ `monthlyUserGrowth`.
- Render revenue trend từ `monthlyRevenueTrend` hoặc dùng API revenue riêng nếu cần chart chính xác hơn.
- Render recent activity table/list từ `recentActivities`.

Response `result`:

```json
{
  "totalUsers": 100,
  "activeUsersRightNow": 3,
  "newUsersThisMonth": 12,
  "activeUsers": 3,
  "totalRevenue": 2500000.0,
  "revenueThisMonth": 500000.0,
  "totalStorageUsedBytes": 104857600,
  "totalStorageCapacityBytes": 10995116277760,
  "fileTypeDistribution": {
    "PDF": 20,
    "IMAGE": 5,
    "AUDIO": 1,
    "VIDEO": 2,
    "OTHER": 3
  },
  "totalDocuments": 31,
  "newDocsThisMonth": 8,
  "totalGroups": 4,
  "pendingReports": 1,
  "monthlyUserGrowth": [
    { "label": "Month 6", "value": 12.0 }
  ],
  "monthlyRevenueTrend": [
    { "label": "Month 6", "value": 500000.0 }
  ],
  "weeklyUploadTrend": [
    { "label": "Week 1", "uploads": 6 }
  ],
  "recentActivities": [
    {
      "actor": "admin",
      "action": "SET_USER_STORAGE_PLAN",
      "detail": "Admin admin set storage plan [PRO] for user [student01]. Reason: Manual payment reconciliation",
      "createdAt": "2026-06-30T14:30:00"
    }
  ]
}
```

## 2. Audit Logs

### Get audit logs

```http
GET /admin/logs?page=0&size=10&logType=ADMIN_ACTION
```

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `10` |
| `logType` | string | No | `ADMIN_ACTION`, `USER_ACTION`, `DOCUMENT_LOG` |

Aliases accepted by BE:

| FE value | Normalized value |
| --- | --- |
| `ADMIN`, `ADMIN_LOG` | `ADMIN_ACTION` |
| `USER`, `USER_LOG` | `USER_ACTION` |
| `DOCUMENT`, `DOCUMENT_ACTION` | `DOCUMENT_LOG` |

Response `result.content[]`:

```json
{
  "id": 1,
  "actor": "admin",
  "actorType": "ADMIN_ACTION",
  "action": "SET_USER_STORAGE_PLAN",
  "targetId": "user-id",
  "details": "Admin admin set storage plan [PRO] for user [student01]. Reason: Manual payment reconciliation",
  "createdAt": "2026-06-30T14:30:00"
}
```

FE cần làm:

- Có tab/select filter: `ALL`, `ADMIN_ACTION`, `USER_ACTION`, `DOCUMENT_LOG`.
- Khi đổi filter, reset `page=0`.
- Hiển thị actor, action, targetId, details, createdAt.
- Dùng `details` làm nội dung chính vì đây là message audit dễ đọc nhất.

## 3. User Management

### Get users

```http
GET /admin/users?page=0&size=20&keyword=student&role=USER&status=ACTIVE
```

Purpose: Search, filter, and paginate user accounts for admin table.

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `keyword` | string | No | Searches username, fullname, email |
| `role` | string | No | Example: `USER`, `ADMIN` |
| `status` | string | No | `ACTIVE` or `BANNED` |

Response `result.content[]`:

```json
{
  "id": "user-id",
  "username": "student01",
  "fullname": "Student One",
  "email": "student@example.com",
  "roles": [
    { "name": "USER", "description": "User role" }
  ],
  "verified": true,
  "accountNonLocked": true,
  "lockedAt": null,
  "lockedReason": null,
  "lockedByAdmin": null,
  "storageUsed": 15485760,
  "storageMax": 1073741824,
  "documentsCount": 12,
  "createdAt": "2026-06-30T14:30:00"
}
```

Field notes:

| Field | Notes |
| --- | --- |
| `storageUsed` | Bytes, calculated from user's documents |
| `storageMax` | Bytes, maps from `User.storageQuota` |
| `documentsCount` | Active documents only |
| `createdAt` | ISO date-time string from `User.createdAt` |
| `accountNonLocked` | `true` means active, `false` means banned/locked |

Lock behavior:

- Locked users cannot login with password.
- Locked users cannot login with Google.
- Locked users cannot refresh token.
- Existing access tokens from locked users are blocked by backend security filter and return `ACCOUNT_IS_LOCKED`.
- System automatically locks inactive non-admin accounts after `app.admin.inactive-lock-days` days. Default is `60` days.

FE cần làm:

- Bảng user cần hiển thị: fullname/username, email, roles, trạng thái, storage progress, documentsCount, createdAt.
- Storage progress tính bằng `storageUsed / storageMax * 100`; nếu `storageMax <= 0`, hiển thị 0%.
- Status UI:
  - `accountNonLocked=true`: Active.
  - `accountNonLocked=false`: Locked/Banned.
- Filter status gửi `ACTIVE` hoặc `BANNED`.
- Lock action phải mở modal nhập `reason`.
- Unlock action nên confirm trước khi gọi API.
- Delete user là hard delete, cần confirm rõ.
- Khi API trả `ACCOUNT_IS_LOCKED` ở phía user session, FE user app nên logout hoặc đưa về màn login với message "Tài khoản đã bị khóa".

### Get user detail

```http
GET /admin/users/{userId}
```

Purpose: Load one administrative user profile. Response shape is the same as one item from `GET /admin/users`.

### Change user role

```http
PUT /admin/users/{userId}/role?roleName=ADMIN
```

Purpose: Replace the user's current roles with the requested role.

Response:

```json
{
  "code": 1000,
  "message": "User privilege updated successfully",
  "result": "UPDATED"
}
```

### Lock user

```http
PUT /admin/users/{userId}/lock
Content-Type: application/json
```

Request:

```json
{
  "reason": "Policy violation"
}
```

Purpose: Ban/lock account and write audit log.

Response `result`: `BANNED`

### Unlock user

```http
PUT /admin/users/{userId}/unlock
```

Purpose: Unlock account and clear lock metadata.

Response `result`: `ACTIVE`

### Delete user

```http
DELETE /admin/users/{userId}
```

Purpose: Permanently delete user account from database.

Response `result`: `DELETED`

### Manually set user storage plan

```http
PUT /admin/users/{userId}/storage-plan
Content-Type: application/json
```

Purpose: Admin reconciliation tool for cases where the user already paid but quota was not added because PayOS webhook failed, was missed, or could not be verified.

Request:

```json
{
  "planId": "pro-monthly",
  "reason": "PayOS webhook missed. Bank transfer confirmed by admin."
}
```

Behavior:

- Finds `storage_plan` by `planId`.
- Sets `user.storageQuota = plan.quotaSize`.
- Extends `user.storageExpiredAt` by `plan.durationMonths`.
- If current expiry is null or expired, expiry starts from current server time.
- Writes audit log action `SET_USER_STORAGE_PLAN`.
- Does not create a fake transaction, so revenue reports remain based on real payment transactions.

Response `result`:

```json
{
  "id": "user-id",
  "username": "student01",
  "fullname": "Student One",
  "email": "student@example.com",
  "roles": [
    { "name": "USER" }
  ],
  "verified": true,
  "accountNonLocked": true,
  "storageUsed": 15485760,
  "storageMax": 10737418240,
  "documentsCount": 12,
  "createdAt": "2026-06-30T14:30:00"
}
```

Errors:

| Case | Error |
| --- | --- |
| `planId` missing | `REQUEST_BODY_INVALID` |
| User not found | `USER_NOT_FOUND` |
| Plan not found | `PLAN_NOT_FOUND` |

FE cần làm cho manual storage reconciliation:

- Thêm action trong User table/detail: `Set storage plan` hoặc `Grant plan`.
- Khi bấm action, mở modal:
  - Select plan từ `GET /admin/plans`.
  - Textarea reason, ví dụ: "PayOS webhook missed, bank transfer confirmed".
  - Submit gọi `PUT /admin/users/{userId}/storage-plan`.
- Sau khi thành công:
  - Đóng modal.
  - Refetch user list/detail.
  - Có thể show toast: "Đã cập nhật gói dung lượng cho user".
- Chỉ dùng chức năng này cho case admin đã đối soát được user đã thanh toán nhưng chưa được cộng quota.
- Không dùng API này để ghi nhận doanh thu; doanh thu vẫn lấy từ transaction `SUCCESS`.

## 4. Document Management

### Get documents

```http
GET /admin/documents?page=0&size=20&keyword=lesson&fileType=DOCUMENT
```

Purpose: Search, filter, and paginate all system documents.

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `keyword` | string | No | Searches file name and owner username |
| `fileType` | string | No | `ALL`, `DOCUMENT`, `IMAGE`, `AUDIO`, `VIDEO` |

Response `result.content[]`:

```json
{
  "id": "document-id",
  "fileName": "lesson.pdf",
  "fileExtension": "pdf",
  "fileSize": 1048576,
  "ownerUsername": "student01",
  "ownerEmail": "student@example.com",
  "deleted": false,
  "createdAt": "2026-06-30T14:30:00"
}
```

### Move document to system trash

```http
DELETE /admin/documents/{docId}
```

Purpose: Soft-delete a policy-violating document.

Response:

```json
{
  "code": 1000,
  "message": "Document moved to system trash successfully",
  "result": "MOVED_TO_TRASH"
}
```

### Get document statistics

```http
GET /admin/documents/statistics
```

Purpose: Load document management stat cards.

Response `result`:

```json
{
  "totalSystemStorageBytes": 10485760,
  "largestFileName": "big.pdf",
  "largestFileSize": 5242880,
  "topUploaderUsername": "student01",
  "topUploaderFileCount": 12
}
```

FE cần làm:

- Bảng document cần search `keyword` và filter `fileType`.
- File type filter gợi ý: `ALL`, `DOCUMENT`, `IMAGE`, `AUDIO`, `VIDEO`.
- Hiển thị `fileSize` dưới dạng KB/MB/GB.
- Soft delete document cần confirm.
- Sau khi delete document thành công, refetch list và stats.

## 5. Study Group Management

### Get groups

```http
GET /admin/groups?page=0&size=20&keyword=math
```

Purpose: Search and paginate study groups.

Response `result.content[]`:

```json
{
  "id": "group-id",
  "name": "Math Group",
  "leaderName": "Leader Name",
  "leaderEmail": "leader@example.com",
  "memberCount": 4,
  "fileCount": 2,
  "status": "ACTIVE",
  "createdAt": "2026-06-30T14:30:00",
  "members": null
}
```

### Get group detail

```http
GET /admin/groups/{groupId}
```

Purpose: Load one group with member list.

Response `result`:

```json
{
  "id": "group-id",
  "name": "Math Group",
  "leaderName": "Leader Name",
  "leaderEmail": "leader@example.com",
  "memberCount": 4,
  "fileCount": 2,
  "status": "ACTIVE",
  "createdAt": "2026-06-30T14:30:00",
  "members": [
    {
      "memberId": "member-id",
      "userId": "user-id",
      "username": "student01",
      "fullname": "Student One",
      "email": "student@example.com",
      "avatarUrl": "https://example.com/avatar.jpg",
      "role": "MEMBER",
      "canUpload": true,
      "canChat": true,
      "joinedAt": "2026-06-30T14:30:00"
    }
  ]
}
```

### Get group statistics

```http
GET /admin/groups/statistics
```

Response `result`:

```json
{
  "totalGroups": 5,
  "activeGroupsLast7Days": 2,
  "averageMembersPerGroup": 3.4
}
```

### Update group status

```http
PUT /admin/groups/{groupId}/status
Content-Type: application/json
```

Request:

```json
{
  "status": "BANNED"
}
```

Accepted active values: `ACTIVE`, `UNLOCKED`, `ENABLED`.

Accepted disabled values: `BANNED`, `LOCKED`, `DISABLED`, `INACTIVE`.

Response `result`: `ACTIVE` or `BANNED`

### Delete group

```http
DELETE /admin/groups/{groupId}
```

Purpose: Permanently delete a group and its dependent messages/files/members.

Response `result`: `GROUP_DELETED`

FE cần làm:

- Group list không cần render members vì `members=null`; khi cần members gọi `GET /admin/groups/{groupId}`.
- Detail modal/page hiển thị members, role, canUpload, canChat, joinedAt.
- Update group status dùng select/toggle:
  - Active gửi `ACTIVE`.
  - Disable/ban gửi `BANNED` hoặc `DISABLED`.
- Delete group là hard delete, cần confirm rõ.
- Sau khi delete/update status, refetch list.

## 6. Payments, Plans, and Revenue

### Get payments

```http
GET /admin/payments?page=0&size=20&status=SUCCESS
```

Purpose: Paginate payment transactions.

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `status` | string | No | `ALL`, `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `TIMEOUT` |

Response `result.content[]`:

```json
{
  "transactionId": "transaction-id",
  "orderCode": 123456789,
  "username": "student01",
  "email": "student@example.com",
  "planName": "PRO 10GB",
  "amount": 100000,
  "status": "SUCCESS",
  "createdAt": "2026-06-30T14:30:00"
}
```

FE cần làm cho payments:

- Render transaction table: orderCode, username, email, planName, amount, status, createdAt.
- Status filter: `ALL`, `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `TIMEOUT`.
- Chỉ xem `SUCCESS` là giao dịch đã tạo doanh thu.
- Với transaction `PENDING` quá lâu, FE có thể đánh dấu "Cần đối soát" để admin kiểm tra PayOS/ngân hàng.

### Get revenue statistics

```http
GET /admin/payments/revenue?granularity=DAY&from=2026-06-01T00:00:00&to=2026-06-30T23:59:59
```

Purpose: Load detailed revenue charts by hour, day, month, or year.

Only `SUCCESS` transactions are counted as revenue.

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `granularity` | string | No | `HOUR`, `DAY`, `MONTH`, `YEAR`. Default `DAY` |
| `from` | ISO date-time | No | Example `2026-06-01T00:00:00` |
| `to` | ISO date-time | No | Example `2026-06-30T23:59:59` |

Default range if `from` is omitted:

| Granularity | Default range |
| --- | --- |
| `HOUR` | Last 24 hours |
| `DAY` | Last 30 days |
| `MONTH` | Last 12 months |
| `YEAR` | Last 5 years |

Response `result`:

```json
{
  "granularity": "DAY",
  "from": "2026-06-01T00:00:00",
  "to": "2026-06-30T23:59:59",
  "totalRevenue": 2500000,
  "transactionCount": 25,
  "averageOrderValue": 100000.0,
  "series": [
    {
      "label": "2026-06-01",
      "revenue": 300000,
      "transactionCount": 3
    },
    {
      "label": "2026-06-02",
      "revenue": 200000,
      "transactionCount": 2
    }
  ]
}
```

Label format:

| Granularity | Label example |
| --- | --- |
| `HOUR` | `2026-06-30 14:00` |
| `DAY` | `2026-06-30` |
| `MONTH` | `2026-06` |
| `YEAR` | `2026` |

FE cần làm cho revenue:

- Tạo chart/tab chọn granularity: `HOUR`, `DAY`, `MONTH`, `YEAR`.
- Với tab tháng có thể gọi thẳng `/admin/payments/revenue/monthly`.
- Chart dùng `series[].label` làm trục X, `series[].revenue` làm trục Y.
- Có thể render thêm `transactionCount` trên tooltip.
- Render summary cards: `totalRevenue`, `transactionCount`, `averageOrderValue`.
- Nếu FE không truyền `from/to`, BE tự dùng default range.

### Get monthly revenue statistics

```http
GET /admin/payments/revenue/monthly?from=2026-01-01T00:00:00&to=2026-12-31T23:59:59
```

Purpose: FE shortcut endpoint for monthly revenue charts. It is equivalent to:

```http
GET /admin/payments/revenue?granularity=MONTH
```

Response shape is the same as `GET /admin/payments/revenue`, with:

```json
{
  "granularity": "MONTH",
  "series": [
    {
      "label": "2026-06",
      "revenue": 2500000,
      "transactionCount": 25
    }
  ]
}
```

### Get storage plans

```http
GET /admin/plans
```

Purpose: Load all storage plans for payment management and manual user reconciliation.

Response `result[]`:

```json
{
  "id": "pro-monthly",
  "planName": "PRO 10GB",
  "quotaSize": 10737418240,
  "price": 100000,
  "durationMonths": 1,
  "status": "ACTIVE",
  "features": [
    "10GB storage",
    "1 month duration"
  ],
  "subscriberCount": 12
}
```

FE cần làm cho plans:

- Render list/card/table các gói storage.
- Dùng `quotaSize` để hiển thị GB/MB.
- Dùng `price` để hiển thị VND.
- Dùng `durationMonths` để hiển thị chu kỳ.
- Dùng `subscriberCount` để hiển thị số user đang subscribe active.
- Dùng list plan này cho modal manual storage reconciliation ở User module.

### Update storage plan

```http
PUT /admin/plans/{planId}
Content-Type: application/json
```

Request:

```json
{
  "planName": "PRO 10GB",
  "quotaSize": 10737418240,
  "price": 100000,
  "durationMonths": 1,
  "status": "ACTIVE",
  "features": [
    "10GB storage",
    "1 month duration"
  ]
}
```

Purpose: Update plan metadata. `status` and `features` are stored through Redis-backed admin settings.

Response `result`: same shape as one item from `GET /admin/plans`.

FE cần làm khi update plan:

- Form update plan nên cho sửa: planName, quotaSize, price, durationMonths, status, features.
- `features` là array string.
- Sau khi update thành công, refetch `GET /admin/plans`.

### Payment unhappy case notes for FE

User can pay successfully but not receive quota if the PayOS webhook is missed, blocked, fails signature verification, references an unknown `orderCode`, or arrives after the transaction is no longer `PENDING`.

Recommended FE behavior:

- After payment return URL, call user storage/profile or payment status.
- If quota is not updated yet, show `Dang xac nhan thanh toan`.
- Admin can verify bank/PayOS manually and call `PUT /admin/users/{userId}/storage-plan`.
- Use `GET /admin/payments` to find transaction status and `GET /admin/plans` to select the correct plan.

FE payment reconciliation workflow gợi ý:

1. Admin mở Payments, lọc `PENDING` hoặc tìm theo user/orderCode.
2. Admin xác nhận ngoài hệ thống rằng tiền đã vào tài khoản.
3. Admin mở User detail của user đó.
4. Admin bấm `Set storage plan`.
5. Admin chọn đúng plan đã thanh toán và nhập reason.
6. FE gọi `PUT /admin/users/{userId}/storage-plan`.
7. FE refetch user list để thấy `storageMax` cập nhật.

## 7. AI Statistics

### Get AI statistics

```http
GET /admin/ai/statistics
```

Purpose: Load AI chatbot analytics cards and day-of-week trend.

Response `result`:

```json
{
  "totalAiMessagesThisMonth": 5420,
  "topAiUserMessageCount": 380,
  "knowledgeChatRatio": 78.2,
  "totalSummarizedDocs": 168,
  "aiUsageTrendByDay": {
    "Monday": 145,
    "Tuesday": 190,
    "Wednesday": 225,
    "Thursday": 210,
    "Friday": 265,
    "Saturday": 340,
    "Sunday": 295
  }
}
```

FE cần làm:

- Render stat cards: totalAiMessagesThisMonth, topAiUserMessageCount, knowledgeChatRatio, totalSummarizedDocs.
- Render chart từ `aiUsageTrendByDay`.
- Hiện tại dữ liệu AI stats trong BE đang là thống kê phục vụ dashboard, không có pagination detail conversation trong file contract này.

## 8. System Settings

### Get settings

```http
GET /admin/settings
```

Response `result`:

```json
{
  "applicationName": "AI StudyHub Portal v1.0",
  "maintenanceMode": false,
  "defaultStorageLimit": 524288000,
  "maxFileSizeUpload": 10485760,
  "allowedFileTypes": "pdf,doc,docx,png,jpg,jpeg,txt,mp3,mp4,ppt,pptx",
  "otpExpiryMinutes": 5,
  "sessionTimeoutMinutes": 60,
  "maxLoginAttempts": 5,
  "emailNotificationEnabled": true,
  "allowRegistration": true,
  "defaultUserStorage": 524288000
}
```

### Update settings

```http
PUT /admin/settings
Content-Type: application/json
```

Request:

```json
{
  "applicationName": "AI StudyHub Portal v1.0",
  "maintenanceMode": false,
  "defaultStorageLimit": 524288000,
  "maxFileSizeUpload": 10485760,
  "allowedFileTypes": "pdf,doc,docx,png,jpg,jpeg,txt,mp3,mp4,ppt,pptx",
  "otpExpiryMinutes": 5,
  "sessionTimeoutMinutes": 60,
  "maxLoginAttempts": 5,
  "emailNotificationEnabled": true,
  "allowRegistration": true,
  "defaultUserStorage": 524288000
}
```

Purpose: Update system settings in Redis. Missing fields keep current values.

Response `result`: same shape as `GET /admin/settings`.

FE cần làm:

- Load settings trước bằng `GET /admin/settings`.
- Form nên map đủ field response.
- Field boolean dùng toggle: maintenanceMode, emailNotificationEnabled, allowRegistration.
- Field số dùng numeric input: defaultStorageLimit, maxFileSizeUpload, otpExpiryMinutes, sessionTimeoutMinutes, maxLoginAttempts, defaultUserStorage.
- `allowedFileTypes` hiện là string comma-separated.
- Khi submit có thể gửi toàn bộ form; BE sẽ giữ giá trị cũ nếu field bị thiếu.

## 9. Error Handling Summary

Common admin errors:

| Error | Meaning |
| --- | --- |
| `REQUEST_BODY_INVALID` | Missing or invalid request body |
| `REQUEST_PARAMETER_INVALID` | Invalid query/path parameter |
| `USER_NOT_FOUND` | User does not exist |
| `PLAN_NOT_FOUND` | Storage plan does not exist |
| `TRANSACTION_NOT_FOUND` | Payment transaction does not exist |
| `GROUP_NOT_FOUND` | Study group does not exist |
| `DOCUMENT_NOT_FOUND` | Document does not exist |
| `PAYMENT_GATEWAY_ERROR` | PayOS gateway/webhook verification failed |

FE should not rely only on HTTP status. Use `code` and `message` from the wrapper for user-facing errors.

FE error handling yêu cầu:

- Nếu `code=1402` hoặc message `Account is locked. Please contact support`, user app nên clear token và chuyển về login.
- Nếu admin API trả `REQUEST_PARAMETER_INVALID`, kiểm tra query params như `status`, `granularity`, `from`, `to`.
- Nếu `PLAN_NOT_FOUND`, refetch lại `GET /admin/plans`.
- Nếu `USER_NOT_FOUND`, refetch lại user list vì user có thể đã bị xóa.
- Với destructive actions, luôn confirm trước khi gọi API.

## 10. QA Checklist For FE

FE nên test tối thiểu các case sau:

- Dashboard không double count user: `totalUsers` không cộng thêm active/locked.
- User locked không login được, không refresh token được, access token cũ gọi API bị `ACCOUNT_IS_LOCKED`.
- Lock user từ Admin xong, row đổi sang locked và filter `BANNED` thấy user đó.
- Unlock user xong, row đổi sang active và filter `ACTIVE` thấy user đó.
- Storage progress hiển thị đúng bytes to MB/GB.
- Manual set storage plan cập nhật `storageMax` trên user table/detail.
- Revenue monthly chart gọi `/admin/payments/revenue/monthly` và render label dạng `yyyy-MM`.
- Payment table chỉ tính doanh thu cho status `SUCCESS`.
- Document delete refetch list và stat.
- Group detail chỉ load members khi gọi detail API.
