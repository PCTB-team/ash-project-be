# ASH Project BE - UML Diagrams

Mình chọn hướng làm theo **code thật + business rules** để sơ đồ vừa đủ đầy đủ cho dự án, vừa không bị rối:
- `Class diagram`: ưu tiên **danh từ / thực thể / thành phần hệ thống**, không dùng tên kiểu động từ.
- `Use case diagram`: tách **actor rõ ràng** để không nhầm giữa khách, người dùng, trưởng nhóm, admin và các hệ thống ngoài.

## 1) Class diagram

### Phạm vi nên đưa vào

#### Tầng giao tiếp
- `AuthenController`
- `UserController`
- `DocumentController`
- `DocumentUpLoadController`
- `FolderController`
- `GroupController`
- `GroupFileController`
- `GroupMessageController`
- `NotificationController`
- `PaymentController`
- `AiChatController`
- `AdminController`
- `TrashController`

#### Tầng nghiệp vụ
- `AuthenService`
- `OtpService`
- `UserService`
- `DocumentService`
- `DocumentUploadService`
- `FolderService`
- `GroupService`
- `GroupFileService`
- `GroupMessageService`
- `NotificationService`
- `PaymentService`
- `AiChatService`
- `TrashService`
- `RedisService`
- `MailService`
- `FileValidationService`
- `StorageService`
- `CloudinaryStorageService`
- `DocumentIndexingService`
- `DocumentTextExtractorService`
- `QdrantService`
- `LogService`

#### Thực thể miền
- `User`
- `Role`
- `Document`
- `Folder`
- `StudyGroup`
- `GroupMember`
- `GroupFile`
- `GroupMessage`
- `Notification`
- `Transaction`
- `StoragePlan`
- `UserLoginHistory`
- `SystemLog`

#### Kiểu dữ liệu / enum
- `GroupRole`
- `RoleEnum`
- `NotificationType`
- `TransactionStatus`
- `UploadStatus`

### Quan hệ chính

- `User` nhiều-nhiều `Role`
- `User` một-nhiều `Document`
- `User` một-nhiều `Folder`
- `Folder` tự liên kết với `Folder` cha
- `StudyGroup` nhiều-một `User` theo vai trò `owner`
- `GroupMember` nhiều-một `StudyGroup`
- `GroupMember` nhiều-một `User`
- `GroupFile` nhiều-một `StudyGroup`
- `GroupFile` nhiều-một `User` theo `uploadedBy`
- `Notification` nhiều-một `User` theo `receiverUser`
- `Notification` nhiều-một `User` theo `actorUser`
- `Transaction` nhiều-một `User`
- `Transaction` nhiều-một `StoragePlan`
- `Document` nhiều-một `User` theo `owner`
- `Document` nhiều-một `Folder`

### PlantUML class diagram

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide circle

class AuthenController
class UserController
class DocumentController
class DocumentUpLoadController
class FolderController
class GroupController
class GroupFileController
class GroupMessageController
class NotificationController
class PaymentController
class AiChatController
class AdminController
class TrashController

class AuthenService
class OtpService
class UserService
class DocumentService
class DocumentUploadService
class FolderService
class GroupService
class GroupFileService
class GroupMessageService
class NotificationService
class PaymentService
class AiChatService
class TrashService
class RedisService
class MailService
class FileValidationService
class StorageService
class CloudinaryStorageService
class DocumentIndexingService
class DocumentTextExtractorService
class QdrantService
class LogService

class User
class Role
class Document
class Folder
class StudyGroup
class GroupMember
class GroupFile
class GroupMessage
class Notification
class Transaction
class StoragePlan
class UserLoginHistory
class SystemLog

enum GroupRole
enum RoleEnum
enum NotificationType
enum TransactionStatus
enum UploadStatus

AuthenController --> AuthenService
AuthenController --> OtpService
UserController --> UserService
UserController --> PaymentService
DocumentController --> DocumentService
DocumentUpLoadController --> DocumentUploadService
FolderController --> FolderService
GroupController --> GroupService
GroupFileController --> GroupFileService
GroupMessageController --> GroupMessageService
NotificationController --> NotificationService
PaymentController --> PaymentService
AiChatController --> AiChatService
AdminController --> UserService
AdminController --> DocumentService
AdminController --> GroupService
AdminController --> PaymentService
AdminController --> AiChatService
AdminController --> LogService
TrashController --> TrashService

AuthenService --> User
AuthenService --> Role
AuthenService --> UserLoginHistory
AuthenService --> RedisService
AuthenService --> MailService
AuthenService --> OtpService

OtpService --> RedisService
OtpService --> User
OtpService --> Role
OtpService --> MailService

UserService --> User
UserService --> Document
UserService --> StoragePlan
UserService --> CloudinaryStorageService

DocumentService --> Document
DocumentService --> Folder
DocumentService --> User
DocumentService --> StorageService
DocumentService --> FileValidationService
DocumentService --> QdrantService
DocumentService --> DocumentIndexingService

DocumentUploadService --> Document
DocumentUploadService --> Folder
DocumentUploadService --> User
DocumentUploadService --> StorageService
DocumentUploadService --> FileValidationService
DocumentUploadService --> DocumentIndexingService
DocumentUploadService --> DocumentTextExtractorService
DocumentUploadService --> QdrantService
DocumentUploadService --> LogService

FolderService --> Folder
FolderService --> User

GroupService --> StudyGroup
GroupService --> GroupMember
GroupService --> User
GroupService --> NotificationService
GroupService --> GroupRole
GroupService --> NotificationType

GroupFileService --> GroupFile
GroupFileService --> StudyGroup
GroupFileService --> User
GroupFileService --> StorageService

GroupMessageService --> GroupMessage
GroupMessageService --> StudyGroup
GroupMessageService --> User

NotificationService --> Notification
NotificationService --> User
NotificationService --> GroupMember
NotificationService --> NotificationType

PaymentService --> Transaction
PaymentService --> StoragePlan
PaymentService --> User
PaymentService --> TransactionStatus

AiChatService --> Document
AiChatService --> Folder
AiChatService --> QdrantService
AiChatService --> DocumentTextExtractorService

TrashService --> Document
TrashService --> Folder

CloudinaryStorageService ..|> StorageService

User --> Role
Document --> User : owner
Document --> Folder
Folder --> User : owner
Folder --> Folder : parent
StudyGroup --> User : owner
GroupMember --> StudyGroup
GroupMember --> User
GroupFile --> StudyGroup
GroupFile --> User : uploadedBy
GroupMessage --> StudyGroup
GroupMessage --> User : sender
Notification --> User : receiverUser
Notification --> User : actorUser
Transaction --> User
Transaction --> StoragePlan
UserLoginHistory --> User

@enduml
```

## 2) Use case diagram

### Actor tách rõ

- `Khách`
- `Người dùng`
- `Trưởng nhóm`
- `Quản trị viên`
- `Cổng Email`
- `Dịch vụ Lưu trữ Cloud`
- `Cổng Thanh toán`
- `Nhà cung cấp AI`
- `Cơ sở dữ liệu Vector`

### Quy ước tách actor

- `Người dùng` là actor nền tảng cho các chức năng cá nhân.
- `Trưởng nhóm` là vai trò chuyên biệt của `Người dùng` với quyền quản lý group.
- `Quản trị viên` là vai trò chuyên biệt của `Người dùng` với quyền quản trị hệ thống.
- Các dịch vụ bên ngoài là actor riêng để không nhầm với chức năng nội bộ.

### Use case chính

- `Đăng ký tài khoản`
- `Xác thực OTP`
- `Đăng nhập`
- `Làm mới token`
- `Đăng xuất`
- `Quên mật khẩu`
- `Cập nhật hồ sơ`
- `Xem dung lượng storage`
- `Upload tài liệu`
- `Xem / tìm kiếm / lọc tài liệu`
- `Tải xuống / xem tài liệu`
- `Đưa tài liệu vào thùng rác`
- `Khôi phục tài liệu`
- `Xóa vĩnh viễn tài liệu`
- `Tạo folder`
- `Quản lý study group`
- `Tham gia group bằng invite`
- `Quản lý quyền group`
- `Quản lý file group`
- `Gửi tin nhắn group`
- `Xem thông báo`
- `Đánh dấu thông báo đã đọc`
- `Chat với AI`
- `Chat AI theo tài liệu`
- `Thanh toán nâng cấp storage`
- `Xử lý webhook thanh toán`
- `Xem trạng thái giao dịch`
- `Quản trị user`
- `Quản trị tài liệu`
- `Quản trị group`
- `Xem log và thống kê`

### PlantUML use case diagram

```plantuml
@startuml
left to right direction

actor Khach as Guest
actor "Nguoi dung" as User
actor "Truong nhom" as GroupLeader
actor "Quan tri vien" as Admin
actor "Cong Email" as EmailService
actor "Dich vu Luu tru Cloud" as CloudStorage
actor "Cong Thanh toan" as PaymentGateway
actor "Nha cung cap AI" as AIProvider
actor "Co so du lieu Vector" as VectorDB

User <|-- GroupLeader
User <|-- Admin

rectangle "ASH Project BE" {
  usecase "Dang ky tai khoan" as UC_Register
  usecase "Xac thuc OTP" as UC_OTP
  usecase "Dang nhap" as UC_Login
  usecase "Lam moi token" as UC_Refresh
  usecase "Dang xuat" as UC_Logout
  usecase "Quen mat khau" as UC_Forgot
  usecase "Cap nhat ho so" as UC_Profile
  usecase "Xem dung luong storage" as UC_Storage

  usecase "Upload tai lieu" as UC_Upload
  usecase "Xem / tim kiem / loc tai lieu" as UC_BrowseDocs
  usecase "Tai xuong / xem tai lieu" as UC_Download
  usecase "Dua tai lieu vao thung rac" as UC_Trash
  usecase "Khoi phuc tai lieu" as UC_Restore
  usecase "Xoa vinh vien tai lieu" as UC_Purge
  usecase "Tao folder" as UC_CreateFolder

  usecase "Quan ly study group" as UC_Groups
  usecase "Tham gia group bang invite" as UC_JoinGroup
  usecase "Quan ly quyen group" as UC_GroupPerm
  usecase "Quan ly file group" as UC_GroupFiles
  usecase "Gui tin nhan group" as UC_GroupMsg

  usecase "Xem thong bao" as UC_Notif
  usecase "Danh dau thong bao da doc" as UC_MarkRead

  usecase "Chat voi AI" as UC_AiChat
  usecase "Chat AI theo tai lieu" as UC_AiKnow

  usecase "Thanh toan nang cap storage" as UC_Payment
  usecase "Xu ly webhook thanh toan" as UC_Webhook
  usecase "Xem trang thai giao dich" as UC_TxStatus

  usecase "Quan tri user" as UC_AdminUsers
  usecase "Quan tri tai lieu" as UC_AdminDocs
  usecase "Quan tri group" as UC_AdminGroups
  usecase "Xem log va thong ke" as UC_AdminStats
}

Guest --> UC_Register
Guest --> UC_Login
Guest --> UC_OTP
Guest --> UC_Forgot

User --> UC_Logout
User --> UC_Profile
User --> UC_Storage
User --> UC_Upload
User --> UC_BrowseDocs
User --> UC_Download
User --> UC_Trash
User --> UC_Restore
User --> UC_Purge
User --> UC_CreateFolder
User --> UC_Notif
User --> UC_MarkRead
User --> UC_AiChat
User --> UC_AiKnow
User --> UC_Payment
User --> UC_TxStatus

GroupLeader --> UC_Groups
GroupLeader --> UC_JoinGroup
GroupLeader --> UC_GroupPerm
GroupLeader --> UC_GroupFiles
GroupLeader --> UC_GroupMsg

Admin --> UC_AdminUsers
Admin --> UC_AdminDocs
Admin --> UC_AdminGroups
Admin --> UC_AdminStats
Admin --> UC_Payment

UC_Register ..> UC_OTP : <<include>>
UC_Forgot ..> UC_OTP : <<include>>
UC_Login ..> UC_Refresh : <<extend>>
UC_Upload ..> UC_CreateFolder : <<include>>
UC_Trash ..> UC_Restore : <<extend>>
UC_Trash ..> UC_Purge : <<extend>>
UC_Groups ..> UC_JoinGroup : <<include>>
UC_AiKnow ..> UC_AiChat : <<include>>
UC_Payment ..> UC_TxStatus : <<include>>

UC_Register --> EmailService
UC_OTP --> EmailService
UC_Forgot --> EmailService
UC_Upload --> CloudStorage
UC_Download --> CloudStorage
UC_Payment --> PaymentGateway
UC_Webhook --> PaymentGateway
UC_AiChat --> AIProvider
UC_AiKnow --> AIProvider
UC_AiKnow --> VectorDB

@enduml
```

## 3) Mình đánh giá phần này là đủ tốt cho dự án vì

- Bám sát code thật.
- Bao hết core business rules của file bạn gửi.
- Actor use case không bị nhập nhằng.
- Class diagram chỉ dùng thành phần dạng danh từ, dễ chép vào Visual Paradigm.

Nếu bạn muốn, mình có thể làm tiếp bản **rút gọn để nộp nhanh** hoặc bản **vẽ chi tiết từng module**.

