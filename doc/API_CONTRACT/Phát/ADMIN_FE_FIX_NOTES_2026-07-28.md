# Admin FE Fix Notes - 2026-07-28

File này tổng hợp các thay đổi backend và các việc FE cần sửa cho admin UI.

## 1. Dashboard - Chart Tổng quan

Endpoint:

```http
GET /admin/dashboard/stats
```

### Người dùng mới

Field response:

```json
{
  "monthlyUserGrowth": [
    {
      "name": "08/2025",
      "value": 0,
      "users": 0,
      "revenue": null
    }
  ]
}
```

FE cần map chart:

- Trục X: `name`
- Data key: `users`
- Tooltip: `users`
- Label UI: `12 THÁNG`

Lưu ý nghiệp vụ:

- User chỉ được tính vào chart sau khi verify OTP thành công.
- Gọi `/auth/register` mới chỉ lưu pending trong Redis, chưa insert vào bảng `user`.
- Sau khi gọi `/auth/otp-verification`, user mới xuất hiện trong dashboard.

### Doanh thu theo tháng ở Dashboard

Field response:

```json
{
  "monthlyRevenueTrend": [
    {
      "name": "02/2026",
      "value": 0,
      "users": null,
      "revenue": 0
    }
  ]
}
```

FE cần map chart:

- Trục X: `name`
- Data key: `revenue`
- Tooltip: `revenue`
- Label UI: `6 THÁNG`

### Upload theo tuần

Field response:

```json
{
  "weeklyUploadTrend": [
    {
      "week": "Week 1",
      "uploads": 0
    }
  ]
}
```

FE cần map chart:

- Trục X: `week`
- Data key: `uploads`

Backend đã bỏ dữ liệu ảo và chuyển sang đếm document upload thật theo 4 tuần gần nhất.

### Phân bổ loại file

Field response:

```json
{
  "fileTypeDistribution": {
    "PDF": 10,
    "IMAGE": 5,
    "AUDIO": 0,
    "VIDEO": 0,
    "OTHER": 2
  }
}
```

Backend chỉ tính document chưa bị xóa mềm.

## 2. Quản lý nhóm học tập

Endpoint:

```http
GET /admin/groups/statistics
```

Response có thêm:

```json
{
  "totalGroups": 44,
  "activeGroupsLast7Days": 4,
  "totalMembers": 52,
  "averageMembersPerGroup": 1
}
```

FE cần sửa card "TRUNG BÌNH THÀNH VIÊN":

- Nếu vẫn giữ tên "Trung bình thành viên": dùng `averageMembersPerGroup`, backend đã round nên không còn `1.2`.
- Nếu muốn dễ hiểu hơn: đổi card thành "TỔNG THÀNH VIÊN" và dùng `totalMembers`.

Khuyến nghị UI: dùng `totalMembers`, vì với nhóm học tập admin thường cần biết tổng member hơn là số trung bình.

## 3. Quản lý tài liệu

Endpoint:

```http
GET /admin/documents
```

Backend đã ẩn field `deleted` khỏi JSON response.

FE cần xóa cột:

- `TRẠNG THÁI`
- Badge `Hoạt động`

Lý do:

- Tài liệu chỉ có 2 trạng thái nghiệp vụ: đang hiện hữu hoặc đã bị xóa mềm.
- Trang quản lý tài liệu admin đang hiển thị tài liệu hiện hữu.
- Badge "Hoạt động" không có ý nghĩa vì tài liệu không có trạng thái lỗi/active như user hoặc group.

Bảng nên còn các cột:

- `TÀI LIỆU`
- `CHỦ SỞ HỮU`
- `DUNG LƯỢNG`
- `NGÀY TẠO`
- Action menu

## 4. Thanh toán - Chart Doanh thu

Endpoint chart monthly:

```http
GET /admin/payments/revenue/monthly
```

Response:

```json
{
  "granularity": "MONTH",
  "totalRevenue": 136000,
  "transactionCount": 7,
  "averageOrderValue": 19428.57,
  "series": [
    {
      "label": "2026-02",
      "revenue": 0,
      "transactionCount": 0
    },
    {
      "label": "2026-03",
      "revenue": 0,
      "transactionCount": 0
    }
  ]
}
```

FE cần map chart:

- Trục X: `label`
- Data key: `revenue`
- Tooltip: `revenue`
- Legend: `Doanh thu`

Backend đã sửa:

- Default monthly range là 6 tháng gần nhất.
- Các tháng không có giao dịch vẫn được trả về với `revenue = 0`.
- Chart không còn chỉ hiện 2 điểm như `2026-06` và `2026-07`.

## 5. Nhật ký Admin

Vị trí UI đề xuất:

- Thêm menu sidebar riêng dưới `Thanh toán`.
- Tên menu: `Nhật ký Admin` hoặc `Lịch sử thao tác`.
- Icon lucide đề xuất: `History`, `ScrollText`, hoặc `ClipboardList`.

Không nên nhét vào dashboard chính vì log cần bảng dài, filter, phân trang.

Endpoint lấy log của admin đang đăng nhập:

```http
GET /admin/logs?currentAdminOnly=true&page=0&size=10
```

Endpoint lấy toàn bộ log hệ thống:

```http
GET /admin/logs?page=0&size=10
```

Endpoint lọc theo loại log:

```http
GET /admin/logs?currentAdminOnly=true&logType=ADMIN_ACTION&page=0&size=10
GET /admin/logs?currentAdminOnly=true&logType=DOCUMENT_LOG&page=0&size=10
GET /admin/logs?currentAdminOnly=true&logType=USER_ACTION&page=0&size=10
```

Response là `Page<SystemLog>`.

FE map bảng từ `result.content[]`:

- `createdAt`: Thời gian
- `actor`: Admin
- `action`: Hành động
- `targetId`: Đối tượng
- `details`: Chi tiết

Gợi ý filter UI:

- Tabs: `Tất cả`, `Admin action`, `User action`, `Document log`
- Toggle: `Chỉ của tôi`

## 6. AI Statistics

Endpoint:

```http
GET /admin/ai/statistics
```

Backend đã bỏ dữ liệu hard-code và lấy từ `AiChatHistory`.

FE map:

- `totalAiMessagesThisMonth`
- `topAiUserMessageCount`
- `knowledgeChatRatio`
- `totalSummarizedDocs`
- `aiUsageTrendByDay`

`aiUsageTrendByDay` là map theo ngày:

```json
{
  "Monday": 0,
  "Tuesday": 2,
  "Wednesday": 0,
  "Thursday": 0,
  "Friday": 0,
  "Saturday": 0,
  "Sunday": 0
}
```

## 7. Ghi chú kiểm tra nhanh cho FE

Sau khi sửa FE, test các case sau:

1. Tạo user mới rồi verify OTP, refresh dashboard, chart `Người dùng mới` phải tăng ở tháng hiện tại.
2. Trang tài liệu không còn cột `Trạng thái`.
3. Trang nhóm không còn hiện `1.2`; dùng số nguyên hoặc đổi sang `Tổng thành viên`.
4. Trang thanh toán chart doanh thu có đủ 6 tháng, tháng không có giao dịch là 0.
5. Sidebar có `Nhật ký Admin`, gọi `currentAdminOnly=true` để admin xem lại thao tác của mình.
6. AI statistics không còn số mẫu cố định.
