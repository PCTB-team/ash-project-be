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

### Payment unhappy case notes for FE

User can pay successfully but not receive quota if the PayOS webhook is missed, blocked, fails signature verification, references an unknown `orderCode`, or arrives after the transaction is no longer `PENDING`.

Recommended FE behavior:

- After payment return URL, call user storage/profile or payment status.
- If quota is not updated yet, show `Dang xac nhan thanh toan`.
- Admin can verify bank/PayOS manually and call `PUT /admin/users/{userId}/storage-plan`.
- Use `GET /admin/payments` to find transaction status and `GET /admin/plans` to select the correct plan.

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
