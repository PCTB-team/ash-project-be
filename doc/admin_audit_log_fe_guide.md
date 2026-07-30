# Admin Audit Log FE Guide

## API

Use:

```http
GET /api/v1/admin/logs?page=0&size=10&logType=ADMIN
```

Optional params:

| Param | Type | Note |
| --- | --- | --- |
| `page` | number | Zero-based page index. |
| `size` | number | Page size. |
| `logType` | string | Use `ADMIN` for Admin Journal. Backward-compatible values: `ADMIN_ACTION`, `ADMIN_LOG`. |
| `actor` | string | Admin user id. Use only when filtering a specific admin. |
| `currentAdminOnly` | boolean | If `true`, backend ignores `actor` and filters by current JWT admin id. |

Response is `ApiResponse<Page<SystemLog>>`.

Important log fields:

| Field | Type | FE usage |
| --- | --- | --- |
| `id` | number | Row key. |
| `actorId` | string | Admin user id. |
| `actor` | string | Admin display name. |
| `actorType` | string | Should be `ADMIN` on Admin Journal. |
| `actionGroup` | string | Use to group/filter action menu. |
| `action` | string | Use to display action label. |
| `targetId` | string | Target user/document/group/system id. |
| `details` | string | Description/detail text. |
| `createdAt` | string | Timestamp. |

## Action Groups

Render action menu by `actionGroup`.

| Group value | UI label |
| --- | --- |
| `USER_MANAGEMENT` | User Management |
| `DOCUMENT_MANAGEMENT` | Document Management |
| `GROUP_MANAGEMENT` | Group Management |
| `SYSTEM_MANAGEMENT` | System Management |

## Action Labels

Map `action` to Vietnamese label.

### User Management

| Action value | UI label |
| --- | --- |
| `BAN_USER` | Ban |
| `UNBAN_USER` | Unban |
| `LOCK_USER` | Khóa tài khoản |
| `DELETE_USER` | Xóa tài khoản |
| `UPDATE_ROLE` | Đổi quyền |
| `SET_USER_STORAGE_PLAN` | Đổi gói dung lượng |

### Document Management

| Action value | UI label |
| --- | --- |
| `DELETE_DOCUMENT` | Xóa tài liệu |
| `APPROVE_DOCUMENT` | Duyệt tài liệu |
| `REJECT_DOCUMENT` | Từ chối tài liệu |

Note: backend currently only has the real admin document action `DELETE_DOCUMENT`.
`APPROVE_DOCUMENT` and `REJECT_DOCUMENT` are reserved action names for FE display/spec, but there is no real approve/reject document API in the current backend yet.

### Group Management

| Action value | UI label |
| --- | --- |
| `LOCK_GROUP` | Khóa nhóm |
| `UNLOCK_GROUP` | Mở khóa nhóm |
| `DELETE_GROUP` | Xóa nhóm |

### System Management

| Action value | UI label |
| --- | --- |
| `ADMIN_LOGIN` | Đăng nhập Admin |
| `ADMIN_LOGOUT` | Đăng xuất Admin |
| `UPDATE_SETTINGS` | Thay đổi cấu hình |
| `UPDATE_HOMEPAGE_CONFIG` | Thay đổi cấu hình trang chủ |
| `UPDATE_PLAN` | Thay đổi gói dung lượng |

## Recommended UI Behavior

- The Admin Journal page should request `logType=ADMIN`.
- Group/filter dropdown should use `actionGroup`, not substring matching on `action`.
- Action label should use the mapping above. If an unknown action appears, display the raw `action`.
- Do not infer admin logs from `details` text. Backend now sends `actorType=ADMIN` and `actionGroup`.
- If showing "current admin only", call with `currentAdminOnly=true`.

## Backend Coverage

Current backend writes admin audit logs for:

- User role change: `USER_MANAGEMENT / UPDATE_ROLE`
- User lock: `USER_MANAGEMENT / LOCK_USER`
- User unlock/unban: `USER_MANAGEMENT / UNBAN_USER`
- User delete: `USER_MANAGEMENT / DELETE_USER`
- User storage plan assignment: `USER_MANAGEMENT / SET_USER_STORAGE_PLAN`
- Document delete: `DOCUMENT_MANAGEMENT / DELETE_DOCUMENT`
- Group lock: `GROUP_MANAGEMENT / LOCK_GROUP`
- Group unlock: `GROUP_MANAGEMENT / UNLOCK_GROUP`
- Group delete: `GROUP_MANAGEMENT / DELETE_GROUP`
- Admin login/logout: `SYSTEM_MANAGEMENT / ADMIN_LOGIN`, `SYSTEM_MANAGEMENT / ADMIN_LOGOUT`
- System settings update: `SYSTEM_MANAGEMENT / UPDATE_SETTINGS`
- Homepage config update: `SYSTEM_MANAGEMENT / UPDATE_HOMEPAGE_CONFIG`
- Storage plan update: `SYSTEM_MANAGEMENT / UPDATE_PLAN`
