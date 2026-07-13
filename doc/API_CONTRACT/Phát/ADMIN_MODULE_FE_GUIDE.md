# Admin Module API Guide for Frontend

Base URL:

```text
/api/v1
```

All admin APIs require a Bearer access token from an account with role `ADMIN`.

Common response wrapper:

```json
{
  "code": 1000,
  "message": "Success message",
  "result": {}
}
```

Error responses use the same wrapper:

```json
{
  "code": 1213,
  "message": "Group not found"
}
```

## Page 1: Dashboard

### Get Dashboard Stats

```http
GET /admin/dashboard/stats
```

Response `result`:

```json
{
  "totalUsers": 25,
  "activeUsersRightNow": 3,
  "totalRevenue": 120000,
  "revenueThisMonth": 20000,
  "totalStorageUsedBytes": 10485760,
  "totalStorageCapacityBytes": 10995116277760,
  "fileTypeDistribution": {
    "PDF": 10,
    "IMAGE": 5,
    "AUDIO": 0,
    "VIDEO": 1,
    "OTHER": 2
  },
  "totalDocuments": 18,
  "totalGroups": 4,
  "pendingReports": 0,
  "monthlyUserGrowth": [
    { "label": "Month 6", "value": 3 }
  ],
  "monthlyRevenueTrend": [
    { "label": "Month 6", "value": 20000 }
  ],
  "weeklyUploadTrend": [
    { "label": "Week 1", "uploads": 6 }
  ],
  "recentActivities": [
    {
      "actor": "admin",
      "action": "DELETE_USER",
      "detail": "Admin admin permanently deleted account [user1] from the database.",
      "createdAt": "2026-06-29T07:30:00"
    }
  ]
}
```

## Audit Logs

### Get Audit Logs

```http
GET /admin/logs?page=0&size=10&logType=DOCUMENT_LOG
```

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `10` |
| `logType` | string | No | `ADMIN_ACTION`, `USER_ACTION`, `DOCUMENT_LOG` |

Supported aliases:

| FE value | Normalized value |
| --- | --- |
| `ADMIN`, `ADMIN_LOG` | `ADMIN_ACTION` |
| `USER`, `USER_LOG` | `USER_ACTION` |
| `DOCUMENT`, `DOCUMENT_ACTION` | `DOCUMENT_LOG` |

Response `result` is a Spring Page:

```json
{
  "content": [
    {
      "id": 1,
      "actor": "student01",
      "actorType": null,
      "action": "USER_UPLOAD_DOCUMENT",
      "targetId": "document-id",
      "details": "User student01 uploaded document [lesson.pdf]",
      "createdAt": "2026-06-29T07:30:00"
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

Notes:

- User document upload logs are visible under both `USER_ACTION` and `DOCUMENT_LOG` because action is `USER_UPLOAD_DOCUMENT`.
- Old logs created before logging was added for uploads will not appear retroactively.

## Page 2: Users

### Get Users

```http
GET /admin/users?page=0&size=20&keyword=john&role=USER&status=ACTIVE
```

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `keyword` | string | No | Searches username, full name, email |
| `role` | string | No | Example: `USER`, `ADMIN` |
| `status` | string | No | `ACTIVE` or `BANNED` |

Response `result.content[]`:

```json
{
  "id": "user-id",
  "username": "student01",
  "email": "student@example.com",
  "fullname": "Student One",
  "avatarUrl": "https://...",
  "verified": true,
  "accountNonLocked": true,
  "roles": [
    { "name": "USER", "description": "User role" }
  ],
  "createdAt": "2026-06-29T07:30:00"
}
```

### Get User Detail

```http
GET /admin/users/{userId}
```

### Change User Role

```http
PUT /admin/users/{userId}/role?roleName=ADMIN
```

Response:

```json
{
  "message": "User privilege updated successfully",
  "result": "UPDATED"
}
```

### Lock User

```http
PUT /admin/users/{userId}/lock
Content-Type: application/json
```

Request body:

```json
{
  "reason": "Policy violation"
}
```

Response `result`: `BANNED`

### Unlock User

```http
PUT /admin/users/{userId}/unlock
```

Response `result`: `ACTIVE`

### Delete User

```http
DELETE /admin/users/{userId}
```

Response `result`: `DELETED`

## Page 3: Documents

### Get Documents

```http
GET /admin/documents?page=0&size=20&keyword=lesson&fileType=DOCUMENT
```

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
  "createdAt": "2026-06-29T07:30:00"
}
```

### Move Document To System Trash

```http
DELETE /admin/documents/{docId}
```

Response:

```json
{
  "message": "Document moved to system trash successfully",
  "result": "MOVED_TO_TRASH"
}
```

### Get Document Stats

```http
GET /admin/documents/statistics
```

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

## Page 4: Study Groups

### Get Groups

```http
GET /admin/groups?page=0&size=20&keyword=math
```

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `keyword` | string | No | Searches group name |

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
  "createdAt": "2026-06-29T07:30:00",
  "members": null
}
```

### Get Group Detail

```http
GET /admin/groups/{groupId}
```

Response `result` includes `members`:

```json
{
  "id": "group-id",
  "name": "Math Group",
  "leaderName": "Leader Name",
  "leaderEmail": "leader@example.com",
  "memberCount": 4,
  "fileCount": 2,
  "status": "ACTIVE",
  "createdAt": "2026-06-29T07:30:00",
  "members": [
    {
      "memberId": "member-id",
      "userId": "user-id",
      "username": "student01",
      "fullname": "Student One",
      "email": "student@example.com",
      "avatarUrl": "https://...",
      "role": "MEMBER",
      "canUpload": true,
      "canChat": true,
      "joinedAt": "2026-06-29T07:30:00"
    }
  ]
}
```

If the group was already deleted:

```json
{
  "code": 1213,
  "message": "Group not found"
}
```

### Get Group Stats

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

### Delete Group

```http
DELETE /admin/groups/{groupId}
```

Response:

```json
{
  "message": "Study group dismantled successfully",
  "result": "GROUP_DELETED"
}
```

If the same group ID is deleted again, the API returns `GROUP_NOT_FOUND`.

## Page 5: Payments

### Get Payments

```http
GET /admin/payments?page=0&size=20&status=SUCCESS
```

Query params:

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `page` | number | No | Default `0` |
| `size` | number | No | Default `20` |
| `status` | string | No | `ALL`, `PENDING`, `SUCCESS`, `FAILED`, depending on enum values |

Response `result.content[]`:

```json
{
  "transactionId": "transaction-id",
  "orderCode": 123456,
  "username": "student01",
  "email": "student@example.com",
  "planName": "PRO Plan - 1 Month (10GB quota)",
  "amount": 10000,
  "status": "SUCCESS",
  "createdAt": "2026-06-29T07:30:00"
}
```

## Page 6: AI Statistics

### Get AI Stats

```http
GET /admin/ai/statistics
```

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

## Page 7: System Settings

### Get Settings

```http
GET /admin/settings
```

Response `result`:

```json
{
  "applicationName": "AI StudyHub Portal v1.0",
  "maintenanceMode": false
}
```

## Frontend Notes

- All paginated APIs currently return Spring `Page` JSON directly. Use `result.content`, `result.totalElements`, `result.totalPages`, `result.number`, and `result.size`.
- Admin group list does not load `members`; use group detail API for members.
- Deleted groups are hard-deleted from DB. After successful delete, a second delete or detail request returns `Group not found`.
- Document upload by normal users creates audit action `USER_UPLOAD_DOCUMENT` after the latest backend change.
- Existing data created before the latest logging changes will not have matching audit rows unless the relevant action is performed again.
