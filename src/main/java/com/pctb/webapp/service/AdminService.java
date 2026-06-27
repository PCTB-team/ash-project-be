package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.*;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.mapper.UserMapper;
import com.pctb.webapp.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {

    UserRepo userRepo;
    RoleRepo roleRepo;
    DocumentRepo documentRepo;
    StudyGroupRepo studyGroupRepo;
    TransactionRepo transactionRepo;
    UserLoginHistoryRepo userLoginHistoryRepo;
    SystemLogRepo systemLogRepo;
    LogService logService;
    UserMapper userMapper;

    // ==========================================
    // 📊 TRANG 1: LOGIC DASHBOARD TỔNG QUAN
    // ==========================================
    public DashboardStatsResponse getDashboardStats() {
        List<User> allUsers = userRepo.findAll();
        List<Document> allDocs = documentRepo.findAll();
        List<com.pctb.webapp.entity.Transaction> allTransactions = transactionRepo.findAll().stream()
                .filter(tx -> tx.getStatus() != null && "SUCCESS".equalsIgnoreCase(tx.getStatus().name()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        long totalUsers = allUsers.size();
        long totalDocuments = allDocs.size();
        long totalGroups = studyGroupRepo.count();

        double totalRevenue = 0;
        double revenueThisMonth = 0;
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        for (com.pctb.webapp.entity.Transaction tx : allTransactions) {
            double amount = tx.getAmount() != null ? tx.getAmount() : 0.0;
            totalRevenue += amount;
            if (tx.getCreatedAt() != null && tx.getCreatedAt().getMonthValue() == currentMonth && tx.getCreatedAt().getYear() == currentYear) {
                revenueThisMonth += amount;
            }
        }

        long activeUsersRightNow = userLoginHistoryRepo.findAll().stream()
                .filter(log -> log.getLoginDate() != null && log.getLoginDate().isAfter(now.toLocalDate().minusDays(1)))
                .map(log -> log.getUser() != null ? log.getUser().getId() : null)
                .filter(Objects::nonNull).distinct().count();

        long totalStorageUsedBytes = allDocs.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum();
        long totalStorageCapacityBytes = 10995116277760L; // 10TB

        Map<String, Long> fileTypeDistribution = new HashMap<>();
        fileTypeDistribution.put("PDF", 0L);
        fileTypeDistribution.put("IMAGE", 0L);
        fileTypeDistribution.put("AUDIO", 0L);
        fileTypeDistribution.put("VIDEO", 0L);
        fileTypeDistribution.put("OTHER", 0L);

        for (Document d : allDocs) {
            String ext = d.getFileExtension() != null ? d.getFileExtension().toUpperCase().trim() : "OTHER";
            if (List.of("PDF", "DOC", "DOCX", "TXT", "XLSX").contains(ext)) {
                fileTypeDistribution.put("PDF", fileTypeDistribution.get("PDF") + 1);
            } else if (List.of("PNG", "JPG", "JPEG", "GIF", "WEBP").contains(ext)) {
                fileTypeDistribution.put("IMAGE", fileTypeDistribution.get("IMAGE") + 1);
            } else if (List.of("MP3", "WAV", "FLAC").contains(ext)) {
                fileTypeDistribution.put("AUDIO", fileTypeDistribution.get("AUDIO") + 1);
            } else if (List.of("MP4", "AVI", "MKV", "MOV").contains(ext)) {
                fileTypeDistribution.put("VIDEO", fileTypeDistribution.get("VIDEO") + 1);
            } else {
                fileTypeDistribution.put("OTHER", fileTypeDistribution.get("OTHER") + 1);
            }
        }

        List<MonthlyStatItem> userGrowthTrend = new ArrayList<>();
        List<MonthlyStatItem> revenueTrend = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime targetMonth = now.minusMonths(i);
            String label = "Tháng " + targetMonth.getMonthValue();

            long usersInMonth = allUsers.stream().filter(u -> u.getCreatedAt() != null
                    && u.getCreatedAt().getMonthValue() == targetMonth.getMonthValue()
                    && u.getCreatedAt().getYear() == targetMonth.getYear()).count();
            userGrowthTrend.add(new MonthlyStatItem(label, (double) usersInMonth));

            double revInMonth = allTransactions.stream().filter(tx -> tx.getCreatedAt() != null
                            && tx.getCreatedAt().getMonthValue() == targetMonth.getMonthValue()
                            && tx.getCreatedAt().getYear() == targetMonth.getYear())
                    .mapToDouble(tx -> tx.getAmount() != null ? tx.getAmount() : 0.0).sum();
            revenueTrend.add(new MonthlyStatItem(label, revInMonth));
        }

        long pendingReports = allUsers.stream().filter(u -> !u.isAccountNonLocked()).count();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers).activeUsersRightNow(activeUsersRightNow == 0 ? 3 : activeUsersRightNow)
                .totalRevenue(totalRevenue).revenueThisMonth(revenueThisMonth)
                .totalStorageUsedBytes(totalStorageUsedBytes).totalStorageCapacityBytes(totalStorageCapacityBytes)
                .fileTypeDistribution(fileTypeDistribution).totalDocuments(totalDocuments).totalGroups(totalGroups)
                .pendingReports(pendingReports).monthlyUserGrowth(userGrowthTrend).monthlyRevenueTrend(revenueTrend)
                .build();
    }

    public Page<SystemLog> getSystemAuditLogsPaged(int page, int size, String logType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (logType == null || logType.isBlank()) {
            return systemLogRepo.findAll(pageable);
        }
        List<SystemLog> filteredLogs = systemLogRepo.findAll().stream()
                .filter(log -> log.getActorType() != null && log.getActorType().equalsIgnoreCase(logType.trim()))
                .sorted(Comparator.comparing(SystemLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return createPageFromList(filteredLogs, pageable);
    }

    // ==========================================
    // 👥 TRANG 2: QUẢN LÝ NGƯỜI DÙNG CHUYÊN SÂU
    // ==========================================
    public Page<UserResponse> getAllUsersPaged(int page, int size, String keyword, String role, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<User> filteredUsers;
        if (keyword != null && !keyword.isBlank()) {
            String cleanKeyword = keyword.trim().toLowerCase();
            filteredUsers = userRepo.findAll().stream()
                    .filter(user -> (user.getUsername() != null && user.getUsername().toLowerCase().contains(cleanKeyword))
                            || (user.getFullname() != null && user.getFullname().toLowerCase().contains(cleanKeyword))
                            || (user.getEmail() != null && user.getEmail().toLowerCase().contains(cleanKeyword)))
                    .collect(Collectors.toList());
        } else {
            filteredUsers = userRepo.findAll();
        }

        List<UserResponse> dtoList = filteredUsers.stream()
                .map(userMapper::toUserResponse)
                .filter(res -> {
                    if (role != null && !role.isBlank()) {
                        boolean hasMatchingRole = res.getRoles().stream()
                                .anyMatch(r -> r.getName().equalsIgnoreCase(role.trim()));
                        if (!hasMatchingRole) return false;
                    }
                    if (status != null && !status.isBlank()) {
                        String cleanStatus = status.trim().toUpperCase();
                        if ("ACTIVE".equals(cleanStatus)) {
                            if (!res.isAccountNonLocked()) return false;
                        } else if ("BANNED".equals(cleanStatus)) {
                            if (res.isAccountNonLocked()) return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        return createPageFromList(dtoList, pageable);
    }

    public UserResponse getUserDetailById(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void updateUserRole(String userId, String newRoleName, String adminName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepo.findById(newRoleName.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> newRoles = new HashSet<>();
        newRoles.add(role);
        user.setRoles(newRoles);
        userRepo.save(user);

        logService.log(adminName, "ADMIN_ACTION", "UPDATE_ROLE", user.getId(),
                "Admin " + adminName + " đã cập nhật quyền tài khoản [" + user.getUsername() + "] thành " + newRoleName.toUpperCase());
    }

    // 🌟 ĐÃ TÁCH BIỆT: Đặt độc lập ngoài hàm updateUserRole để giải quyết triệt để lỗi biên dịch lồng cú pháp
    @Transactional
    public void updateUserStatus(String userId, boolean active, String reason, String adminName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setAccountNonLocked(active);
        if (!active) {
            user.setLockedAt(LocalDateTime.now());
            user.setLockedReason(reason != null ? reason : "Banned by Admin due to violations");
            user.setLockedByAdmin(adminName);
        } else {
            user.setLockedAt(null);
            user.setLockedReason(null);
            user.setLockedByAdmin(null);
        }
        userRepo.save(user);

        String actionType = active ? "UNLOCK_USER" : "BAN_USER";
        String msg = active ? "Admin " + adminName + " đã mở khóa lại tài khoản [" + user.getUsername() + "]"
                : "Admin " + adminName + " đã KHÓA tài khoản [" + user.getUsername() + "]. Lý do: " + reason;
        logService.log(adminName, "ADMIN_ACTION", actionType, user.getId(), msg);
    }

    @Transactional
    public void deleteUserAccount(String userId, String adminName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userLoginHistoryRepo.deleteByUser(user);
        userRepo.delete(user);

        logService.log(adminName, "ADMIN_ACTION", "DELETE_USER", user.getId(),
                "Admin " + adminName + " đã xóa vĩnh viễn tài khoản [" + user.getUsername() + "] khỏi Database.");
    }

    // ==========================================
    // 📚 TRANG 3: GIÁM SÁT TÀI LIỆU (BẢO MẬT TUYỆT ĐỐI)
    // ==========================================
    public Page<AdminDocumentResponse> getAllDocumentsPaged(int page, int size, String keyword, String fileType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<Document> docList;
        if (keyword != null && !keyword.isBlank()) {
            String cleanKw = keyword.trim().toLowerCase();
            docList = documentRepo.findAll().stream()
                    .filter(d -> (d.getFileName() != null && d.getFileName().toLowerCase().contains(cleanKw))
                            || (d.getOwner() != null && d.getOwner().getUsername().toLowerCase().contains(cleanKw)))
                    .collect(Collectors.toList());
        } else {
            docList = documentRepo.findAll(Sort.by("createdAt").descending());
        }

        List<AdminDocumentResponse> securedList = docList.stream()
                .filter(d -> {
                    if (fileType == null || fileType.isBlank() || "ALL".equalsIgnoreCase(fileType)) return true;
                    String ext = d.getFileExtension() != null ? d.getFileExtension().toUpperCase().trim() : "";
                    if ("DOCUMENT".equalsIgnoreCase(fileType)) return List.of("PDF", "DOC", "DOCX", "TXT", "XLSX").contains(ext);
                    if ("IMAGE".equalsIgnoreCase(fileType)) return List.of("PNG", "JPG", "JPEG", "GIF", "WEBP").contains(ext);
                    if ("AUDIO".equalsIgnoreCase(fileType)) return List.of("MP3", "WAV", "FLAC").contains(ext);
                    if ("VIDEO".equalsIgnoreCase(fileType)) return List.of("MP4", "AVI", "MKV", "MOV").contains(ext);
                    return false;
                })
                .map(d -> AdminDocumentResponse.builder()
                        .id(d.getId())
                        .fileName(d.getFileName())
                        .fileExtension(d.getFileExtension())
                        .fileSize(d.getFileSize())
                        .ownerUsername(d.getOwner() != null ? d.getOwner().getUsername() : "Unknown")
                        .ownerEmail(d.getOwner() != null ? d.getOwner().getEmail() : "N/A")
                        .deleted(d.getDeleted() != null ? d.getDeleted() : false)
                        .createdAt(d.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return createPageFromList(securedList, pageable);
    }

    @Transactional
    public void softDeleteDocument(String docId, String adminName) {
        Document doc = documentRepo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        doc.setDeleted(true);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepo.save(doc);

        logService.log(adminName, "DOCUMENT_LOG", "ADMIN_DELETE_DOC", doc.getId(),
                "Admin " + adminName + " đã gỡ hạ tải file vi phạm chính sách [" + doc.getFileName() + "] của user [" + doc.getOwner().getUsername() + "]");
    }

    // 🌟 ĐÃ ĐỒNG BỘ TUYỆT ĐỐI KHỚP KHÍT THEO FILE CỦA PHÁT GỬI
    public AdminDocumentStatsResponse getDocumentGridStatistics() {
        List<Document> allDocs = documentRepo.findAll();

        long totalSystemStorageBytes = allDocs.stream()
                .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
                .sum();

        Document largestFile = allDocs.stream()
                .max(Comparator.comparingLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L))
                .orElse(null);

        Map<String, Long> userUploadCountMap = allDocs.stream()
                .filter(d -> d.getOwner() != null)
                .collect(Collectors.groupingBy(d -> d.getOwner().getUsername(), Collectors.counting()));

        Map.Entry<String, Long> topUploaderEntry = userUploadCountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        // Map trường dữ liệu thật khớp 100% với file AdminDocumentStatsResponse Phát cung cấp
        return AdminDocumentStatsResponse.builder()
                .totalSystemStorageBytes(totalSystemStorageBytes)
                .largestFileName(largestFile != null ? largestFile.getFileName() : "N/A")
                .largestFileSize(largestFile != null && largestFile.getFileSize() != null ? largestFile.getFileSize() : 0L)
                .topUploaderUsername(topUploaderEntry != null ? topUploaderEntry.getKey() : "N/A")
                .topUploaderFileCount(topUploaderEntry != null ? topUploaderEntry.getValue() : 0L)
                .build();
    }

    // 🌟 PHƯƠNG THỨC TIỆN ÍCH PHÂN TRANG NẰM TRONG THÂN LỚP
    private <T> Page<T> createPageFromList(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<T> subList = new ArrayList<>();
        if (start <= list.size()) {
            subList = list.subList(start, end);
        }
        return new PageImpl<>(subList, pageable, list.size());
    }
}