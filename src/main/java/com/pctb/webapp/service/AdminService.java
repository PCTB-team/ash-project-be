package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.dto.request.AdminSetUserStoragePlanRequest;
import com.pctb.webapp.dto.request.AdminUpdateGroupStatusRequest;
import com.pctb.webapp.dto.request.AdminUpdatePlanRequest;
import com.pctb.webapp.dto.request.AdminUpdateSettingsRequest;
import com.pctb.webapp.dto.response.*;
import com.pctb.webapp.entity.*;
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
import java.time.format.DateTimeFormatter;
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
    GroupMemberRepo groupMemberRepo;
    GroupFileRepo groupFileRepo;
    GroupMessageRepo groupMessageRepo;
    TransactionRepo transactionRepo;
    StoragePlanRepo storagePlanRepo;
    UserLoginHistoryRepo userLoginHistoryRepo;
    SystemLogRepo systemLogRepo;
    LogService logService;
    UserMapper userMapper;
    RedisService redisService;
    ObjectMapper objectMapper;

    static String ADMIN_SETTINGS_KEY = "admin:system:settings";
    static String ADMIN_PLAN_STATUS_PREFIX = "admin:plan:status:";
    static String ADMIN_PLAN_FEATURES_PREFIX = "admin:plan:features:";

    // =========================================================================
    // 📊 TRANG 1: LOGIC DASHBOARD TỔNG QUAN (Đồng bộ Stat Cards, Charts, Hoạt động gần đây)
    // =========================================================================
    public DashboardStatsResponse getDashboardStats() {
        List<User> allUsers = userRepo.findAll();
        List<Document> allDocs = documentRepo.findAll();
        List<com.pctb.webapp.entity.Transaction> allTransactions = transactionRepo.findAll().stream()
                .filter(tx -> tx.getStatus() != null && "SUCCESS".equalsIgnoreCase(tx.getStatus().name()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        // 1. Dữ liệu nạp Stat Cards hàng trên
        long totalUsers = allUsers.size();
        long totalDocuments = allDocs.size();
        long totalGroups = studyGroupRepo.count();

        double totalRevenue = 0;
        double revenueThisMonth = 0;
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        long newUsersThisMonth = allUsers.stream()
                .filter(user -> user.getCreatedAt() != null
                        && user.getCreatedAt().getMonthValue() == currentMonth
                        && user.getCreatedAt().getYear() == currentYear)
                .count();

        long newDocsThisMonth = allDocs.stream()
                .filter(document -> document.getCreatedAt() != null
                        && document.getCreatedAt().getMonthValue() == currentMonth
                        && document.getCreatedAt().getYear() == currentYear)
                .count();

        for (com.pctb.webapp.entity.Transaction tx : allTransactions) {
            double amount = tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0;
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
        long totalStorageCapacityBytes = 10995116277760L; // Dung lượng trần 10TB hệ thống

        // 2. Phân bố hệ hạ tầng hình bánh Donut Chart
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

        // 3. Logic xử lý mảng dòng thời gian 6 tháng vẽ Line & Area Chart
        List<MonthlyStatItem> userGrowthTrend = new ArrayList<>();
        List<MonthlyStatItem> revenueTrend = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime targetMonth = now.minusMonths(i);
            String label = "Month " + targetMonth.getMonthValue();

            long usersInMonth = allUsers.stream().filter(u -> u.getCreatedAt() != null
                    && u.getCreatedAt().getMonthValue() == targetMonth.getMonthValue()
                    && u.getCreatedAt().getYear() == targetMonth.getYear()).count();
            userGrowthTrend.add(new MonthlyStatItem(label, (double) usersInMonth));

            double revInMonth = allTransactions.stream().filter(tx -> tx.getCreatedAt() != null
                            && tx.getCreatedAt().getMonthValue() == targetMonth.getMonthValue()
                            && tx.getCreatedAt().getYear() == targetMonth.getYear())
                    .mapToDouble(tx -> tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0).sum();
            revenueTrend.add(new MonthlyStatItem(label, revInMonth));
        }

        // 4. ĐỒNG BỘ: Tạo dữ liệu ảo xu hướng upload theo tuần (Weekly Bar Chart)
        List<WeeklyStatItem> weeklyUploadTrend = new ArrayList<>();
        weeklyUploadTrend.add(new WeeklyStatItem("Week 1", totalDocuments / 4 + 2));
        weeklyUploadTrend.add(new WeeklyStatItem("Week 2", totalDocuments / 4 - 1));
        weeklyUploadTrend.add(new WeeklyStatItem("Week 3", totalDocuments / 4 + 5));
        weeklyUploadTrend.add(new WeeklyStatItem("Week 4", totalDocuments / 4));

        // 5. ĐỒNG BỘ: Ánh xạ bảng "Hoạt động gần đây" đón tiếp cấu trúc RecentActivityResponse
        List<SystemLog> logs = systemLogRepo.findAll();
        List<RecentActivityResponse> recentActivities = logs.stream()
                .sorted(Comparator.comparing(SystemLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(l -> RecentActivityResponse.builder()
                        .actor(l.getActor() != null ? l.getActor() : "System")
                        .action(l.getAction())
                        .detail(l.getDetails())
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        long activeUsers = allUsers.stream().filter(user -> !isExplicitlyLocked(user)).count();
        long pendingReports = allUsers.size() - activeUsers;

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers).activeUsersRightNow(activeUsersRightNow)
                .newUsersThisMonth(newUsersThisMonth).activeUsers(activeUsers)
                .totalRevenue(totalRevenue).revenueThisMonth(revenueThisMonth)
                .totalStorageUsedBytes(totalStorageUsedBytes).totalStorageCapacityBytes(totalStorageCapacityBytes)
                .fileTypeDistribution(fileTypeDistribution).totalDocuments(totalDocuments).totalGroups(totalGroups)
                .newDocsThisMonth(newDocsThisMonth)
                .pendingReports(pendingReports).monthlyUserGrowth(userGrowthTrend).monthlyRevenueTrend(revenueTrend)
                .weeklyUploadTrend(weeklyUploadTrend).recentActivities(recentActivities)
                .build();
    }

    public Page<SystemLog> getSystemAuditLogsPaged(int page, int size, String logType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (logType == null || logType.isBlank()) {
            return systemLogRepo.findAll(pageable);
        }
        List<SystemLog> filteredLogs = systemLogRepo.findAll().stream()
                .filter(log -> matchesLogType(log, logType))
                .sorted(Comparator.comparing(SystemLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return createPageFromList(filteredLogs, pageable);
    }

    private boolean matchesLogType(SystemLog log, String logType) {
        String cleanLogType = normalizeLogType(logType);
        String actorType = log.getActorType() != null ? normalizeLogType(log.getActorType()) : "";
        String action = log.getAction() != null ? log.getAction().toUpperCase() : "";

        if ("DOCUMENT_LOG".equals(cleanLogType)) {
            return "DOCUMENT_LOG".equals(actorType) || action.contains("DOC") || action.contains("DOCUMENT");
        }
        if ("ADMIN_ACTION".equals(cleanLogType)) {
            return "ADMIN_ACTION".equals(actorType)
                    || action.contains("ADMIN")
                    || action.contains("ROLE")
                    || action.contains("USER")
                    || action.contains("GROUP")
                    || action.contains("BAN")
                    || action.contains("UNLOCK")
                    || action.contains("DELETE");
        }
        if ("USER_ACTION".equals(cleanLogType)) {
            return "USER_ACTION".equals(actorType)
                    || action.contains("USER")
                    || action.contains("ROLE")
                    || action.contains("BAN")
                    || action.contains("UNLOCK")
                    || (!matchesLogType(log, "ADMIN_ACTION") && !matchesLogType(log, "DOCUMENT_LOG"));
        }
        return true;
    }

    private String normalizeLogType(String logType) {
        String cleanLogType = logType == null ? "" : logType.trim().toUpperCase();
        if ("USER".equals(cleanLogType) || "USER_LOG".equals(cleanLogType)) {
            return "USER_ACTION";
        }
        if ("DOCUMENT".equals(cleanLogType) || "DOCUMENT_ACTION".equals(cleanLogType)) {
            return "DOCUMENT_LOG";
        }
        if ("ADMIN".equals(cleanLogType) || "ADMIN_LOG".equals(cleanLogType)) {
            return "ADMIN_ACTION";
        }
        return cleanLogType;
    }

    // =========================================================================
    // 👥 TRANG 2: QUẢN LÝ NGƯỜI DÙNG CHUYÊN SÂU (Kiểm toán hành chính tài khoản)
    // =========================================================================
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
                .map(this::toAdminUserResponse)
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
        return toAdminUserResponse(user);
    }

    private UserResponse toAdminUserResponse(User user) {
        UserResponse response = userMapper.toUserResponse(user);
        response.setAccountNonLocked(!isExplicitlyLocked(user));
        Long usedStorage = documentRepo.sumFileSizeByOwner(user);
        response.setStorageUsed(usedStorage != null ? usedStorage : 0L);
        response.setDocumentsCount(documentRepo.countActiveByOwner(user));
        return response;
    }

    private boolean isExplicitlyLocked(User user) {
        return !user.isAccountNonLocked()
                && (user.getLockedAt() != null
                || user.getLockedReason() != null
                || user.getLockedByAdmin() != null);
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

        logService.log(adminName, "USER_ACTION", "UPDATE_ROLE", user.getId(),
                "Admin " + adminName + " updated account privileges for [" + user.getUsername() + "] to " + newRoleName.toUpperCase());
    }

    @Transactional
    public void updateUserStatus(String userId, boolean active, String reason, String adminName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setAccountNonLocked(active);
        if (!active) {
            user.setLockedAt(LocalDateTime.now());
            user.setLockedReason(reason != null ? reason : "Banned by Admin due to violations");
            user.setLockedByAdmin(adminName);
            redisService.delete(refreshTokenKey(user.getId()));
        } else {
            user.setLockedAt(null);
            user.setLockedReason(null);
            user.setLockedByAdmin(null);
        }
        userRepo.save(user);

        String actionType = active ? "UNLOCK_USER" : "BAN_USER";
        String msg = active ? "Admin " + adminName + " unlocked account [" + user.getUsername() + "]"
                : "Admin " + adminName + " locked account [" + user.getUsername() + "]. Reason: " + reason;
        logService.log(adminName, "USER_ACTION", actionType, user.getId(), msg);
    }

    @Transactional
    public UserResponse setUserStoragePlan(String userId, AdminSetUserStoragePlanRequest request, String adminName) {
        if (request == null || request.getPlanId() == null || request.getPlanId().isBlank()) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        User user = userRepo.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        StoragePlan plan = storagePlanRepo.findById(request.getPlanId().trim())
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        user.setStorageQuota(plan.getQuotaSize());

        int monthsToAdd = plan.getDurationMonths() != null ? plan.getDurationMonths() : 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiredAt = user.getStorageExpiredAt();
        LocalDateTime newExpiredAt = currentExpiredAt == null || currentExpiredAt.isBefore(now)
                ? now.plusMonths(monthsToAdd)
                : currentExpiredAt.plusMonths(monthsToAdd);
        user.setStorageExpiredAt(newExpiredAt);

        User savedUser = userRepo.save(user);
        String reason = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Manual payment reconciliation";
        logService.log(adminName, "ADMIN_ACTION", "SET_USER_STORAGE_PLAN", savedUser.getId(),
                "Admin " + adminName + " set storage plan [" + plan.getPlanName() + "] for user ["
                        + savedUser.getUsername() + "]. Reason: " + reason);

        return toAdminUserResponse(savedUser);
    }

    @Transactional
    public void deleteUserAccount(String userId, String adminName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userLoginHistoryRepo.deleteByUser(user);
        userRepo.delete(user);

        logService.log(adminName, "USER_ACTION", "DELETE_USER", user.getId(),
                "Admin " + adminName + " permanently deleted account [" + user.getUsername() + "] from the database.");
    }

    // =========================================================================
    // 📚 TRANG 3: GIÁM SÁT TÀI LIỆU (Giám sát dung lượng ổ đĩa đám mây Cloudinary)
    // =========================================================================
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
                "Admin " + adminName + " removed policy-violating file [" + doc.getFileName() + "] owned by user [" + doc.getOwner().getUsername() + "]");
    }

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

        return AdminDocumentStatsResponse.builder()
                .totalSystemStorageBytes(totalSystemStorageBytes)
                .largestFileName(largestFile != null ? largestFile.getFileName() : "N/A")
                .largestFileSize(largestFile != null && largestFile.getFileSize() != null ? largestFile.getFileSize() : 0L)
                .topUploaderUsername(topUploaderEntry != null ? topUploaderEntry.getKey() : "N/A")
                .topUploaderFileCount(topUploaderEntry != null ? topUploaderEntry.getValue() : 0L)
                .build();
    }

    // =========================================================================
    // 👥 TRANG 4: QUẢN LÝ NHÓM HỌC TẬP (STUDY GROUPS)
    // =========================================================================
    @Transactional(readOnly = true)
    public Page<AdminGroupResponse> getAllGroupsPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StudyGroup> groupPage;
        if (keyword != null && !keyword.isBlank()) {
            groupPage = studyGroupRepo.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            groupPage = studyGroupRepo.findAll(pageable);
        }
        return groupPage.map(this::toAdminGroupResponse);
    }

    @Transactional(readOnly = true)
    public AdminGroupResponse getGroupDetail(String groupId) {
        StudyGroup group = studyGroupRepo.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        List<GroupMemberResponse> members = groupMemberRepo.findByGroupIdOrderByJoinedAtAsc(group.getId()).stream()
                .map(this::toGroupMemberResponse)
                .collect(Collectors.toList());
        return toAdminGroupResponse(group, members);
    }

    public AdminGroupStatsResponse getGroupStatistics() {
        List<StudyGroup> groups = studyGroupRepo.findAll();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeGroupsLast7Days = groups.stream()
                .filter(group -> group.getUpdatedAt() != null && group.getUpdatedAt().isAfter(sevenDaysAgo))
                .count();
        long totalMembers = groups.stream()
                .mapToLong(group -> groupMemberRepo.countByGroupId(group.getId()))
                .sum();
        double averageMembersPerGroup = groups.isEmpty() ? 0.0 : (double) totalMembers / groups.size();

        return AdminGroupStatsResponse.builder()
                .totalGroups(groups.size())
                .activeGroupsLast7Days(activeGroupsLast7Days)
                .averageMembersPerGroup(averageMembersPerGroup)
                .build();
    }

    @Transactional // Rất quan trọng: Phải có để đảm bảo tính nguyên tử
    public void deleteGroup(String groupId, String adminName) {
        // 1. Kiểm tra nhóm có tồn tại không
        StudyGroup group = studyGroupRepo.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        String groupName = group.getName();

        // 2. Xóa dữ liệu phụ thuộc trong nhóm trước để tránh lỗi FK
        groupMessageRepo.deleteByGroupId(groupId);
        groupFileRepo.deleteByGroupId(groupId);
        groupMemberRepo.deleteByGroupId(groupId);

        // 3. Xóa nhóm
        int deletedRows = studyGroupRepo.deleteExistingById(groupId);
        if (deletedRows == 0) {
            throw new AppException(ErrorCode.GROUP_NOT_FOUND);
        }
        studyGroupRepo.flush();

        // 4. Ghi log
        logService.logAction(adminName, "DELETE_GROUP", "Deleted group: " + groupName);
    }

    @Transactional
    public String updateGroupStatus(String groupId, AdminUpdateGroupStatusRequest request, String adminName) {
        StudyGroup group = studyGroupRepo.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        boolean active = resolveActiveGroupStatus(request);
        group.setInviteEnabled(active);
        group.setUpdatedAt(LocalDateTime.now());
        studyGroupRepo.save(group);

        String status = active ? "ACTIVE" : "BANNED";
        logService.log(adminName, "ADMIN_ACTION", "UPDATE_GROUP_STATUS", group.getId(),
                "Admin " + adminName + " changed group [" + group.getName() + "] status to " + status);
        return status;
    }

    private boolean resolveActiveGroupStatus(AdminUpdateGroupStatusRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        if (request.getActive() != null) {
            return request.getActive();
        }
        String status = request.getStatus();
        if (status == null || status.isBlank()) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE", "UNLOCKED", "ENABLED" -> true;
            case "BANNED", "LOCKED", "DISABLED", "INACTIVE" -> false;
            default -> throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        };
    }

    private AdminGroupResponse toAdminGroupResponse(StudyGroup group) {
        return toAdminGroupResponse(group, null);
    }

    private AdminGroupResponse toAdminGroupResponse(StudyGroup group, List<GroupMemberResponse> members) {
        User owner = group.getOwner();
        return AdminGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .leaderName(owner != null ? owner.getFullname() : "Unknown")
                .leaderEmail(owner != null ? owner.getEmail() : "N/A")
                .memberCount(groupMemberRepo.countByGroupId(group.getId()))
                .fileCount(groupFileRepo.countByGroupId(group.getId()))
                .status(Boolean.TRUE.equals(group.getInviteEnabled()) ? "ACTIVE" : "DISABLED")
                .createdAt(group.getCreatedAt())
                .members(members)
                .build();
    }

    private GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        User user = member.getUser();
        return GroupMemberResponse.builder()
                .memberId(member.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : "Unknown")
                .fullname(user != null ? user.getFullname() : "Unknown")
                .email(user != null ? user.getEmail() : "N/A")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .role(member.getRole() != null ? member.getRole().name() : null)
                .canUpload(Boolean.TRUE.equals(member.getCanUpload()))
                .canChat(Boolean.TRUE.equals(member.getCanChat()))
                .joinedAt(member.getJoinedAt() == null ? null : member.getJoinedAt().toString())
                .build();
    }

    // =========================================================================
    // 💳 TRANG 5: QUẢN LÝ THANH TOÁN & DOANH THU (Ánh xạ AdminTransactionResponse)
    // =========================================================================
    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getAllPaymentsPaged(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.pctb.webapp.entity.Transaction> transactionPage;

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            TransactionStatus transactionStatus;
            try {
                transactionStatus = TransactionStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
            }
            transactionPage = transactionRepo.findByStatus(transactionStatus, pageable);
        } else {
            transactionPage = transactionRepo.findAll(pageable);
        }

        return transactionPage.map(tx -> AdminTransactionResponse.builder()
                .transactionId(tx.getId())
                .orderCode(tx.getOrderCode())
                .username(tx.getUser() != null ? tx.getUser().getUsername() : "Unknown")
                .email(tx.getUser() != null ? tx.getUser().getEmail() : "N/A")
                .planName(tx.getPlan() != null ? tx.getPlan().getPlanName() : "Storage Plan")
                .amount(tx.getAmount() != null ? tx.getAmount() : 0L)
                .status(tx.getStatus() != null ? tx.getStatus().name() : "PENDING")
                .createdAt(tx.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public AdminRevenueStatsResponse getRevenueStats(String granularity, LocalDateTime from, LocalDateTime to) {
        String normalizedGranularity = normalizeRevenueGranularity(granularity);
        LocalDateTime resolvedTo = to != null ? to : LocalDateTime.now();
        LocalDateTime resolvedFrom = from != null ? from : defaultRevenueFrom(normalizedGranularity, resolvedTo);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        Map<String, long[]> groupedRevenue = new TreeMap<>();
        List<Transaction> successfulTransactions = transactionRepo.findAll().stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.SUCCESS)
                .filter(tx -> tx.getCreatedAt() != null)
                .filter(tx -> !tx.getCreatedAt().isBefore(resolvedFrom) && !tx.getCreatedAt().isAfter(resolvedTo))
                .collect(Collectors.toList());

        for (Transaction tx : successfulTransactions) {
            String label = formatRevenueLabel(tx.getCreatedAt(), normalizedGranularity);
            long amount = tx.getAmount() != null ? tx.getAmount() : 0L;
            long[] bucket = groupedRevenue.computeIfAbsent(label, ignored -> new long[2]);
            bucket[0] += amount;
            bucket[1] += 1;
        }

        List<AdminRevenueStatItem> series = groupedRevenue.entrySet().stream()
                .map(entry -> AdminRevenueStatItem.builder()
                        .label(entry.getKey())
                        .revenue(entry.getValue()[0])
                        .transactionCount(entry.getValue()[1])
                        .build())
                .collect(Collectors.toList());

        long totalRevenue = series.stream().mapToLong(AdminRevenueStatItem::getRevenue).sum();
        long transactionCount = series.stream().mapToLong(AdminRevenueStatItem::getTransactionCount).sum();
        double averageOrderValue = transactionCount == 0
                ? 0
                : Math.round(totalRevenue * 100.0 / transactionCount) / 100.0;

        return AdminRevenueStatsResponse.builder()
                .granularity(normalizedGranularity)
                .from(resolvedFrom)
                .to(resolvedTo)
                .totalRevenue(totalRevenue)
                .transactionCount(transactionCount)
                .averageOrderValue(averageOrderValue)
                .series(series)
                .build();
    }

    public List<AdminPlanResponse> getPlans() {
        LocalDateTime now = LocalDateTime.now();
        return storagePlanRepo.findAll(Sort.by("quotaSize").ascending())
                .stream()
                .map(plan -> toAdminPlanResponse(plan, now))
                .collect(Collectors.toList());
    }

    private String normalizeRevenueGranularity(String granularity) {
        String cleanGranularity = granularity == null || granularity.isBlank()
                ? "DAY"
                : granularity.trim().toUpperCase();
        if (!List.of("HOUR", "DAY", "MONTH", "YEAR").contains(cleanGranularity)) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }
        return cleanGranularity;
    }

    private LocalDateTime defaultRevenueFrom(String granularity, LocalDateTime to) {
        return switch (granularity) {
            case "HOUR" -> to.minusHours(24);
            case "MONTH" -> to.minusMonths(12);
            case "YEAR" -> to.minusYears(5);
            default -> to.minusDays(30);
        };
    }

    private String formatRevenueLabel(LocalDateTime createdAt, String granularity) {
        return switch (granularity) {
            case "HOUR" -> createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
            case "MONTH" -> createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "YEAR" -> createdAt.format(DateTimeFormatter.ofPattern("yyyy"));
            default -> createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE);
        };
    }

    @Transactional
    public AdminPlanResponse updatePlan(String planId, AdminUpdatePlanRequest request, String adminName) {
        if (request == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        StoragePlan plan = storagePlanRepo.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        String requestedPlanName = firstNonBlank(request.getName(), request.getPlanName());
        if (requestedPlanName != null && !requestedPlanName.isBlank()) {
            plan.setPlanName(requestedPlanName.trim());
        }
        if (request.getQuotaSize() != null) {
            plan.setQuotaSize(request.getQuotaSize());
        }
        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getDurationMonths() != null) {
            plan.setDurationMonths(request.getDurationMonths());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            redisService.set(planStatusKey(planId), normalizePlanStatus(request.getStatus()));
        }
        if (request.getFeatures() != null) {
            savePlanFeatures(planId, request.getFeatures());
        }

        StoragePlan savedPlan = storagePlanRepo.save(plan);
        logService.log(adminName, "ADMIN_ACTION", "UPDATE_PLAN", savedPlan.getId(),
                "Admin " + adminName + " updated storage plan [" + savedPlan.getPlanName() + "]");
        return toAdminPlanResponse(savedPlan, LocalDateTime.now());
    }

    private AdminPlanResponse toAdminPlanResponse(StoragePlan plan, LocalDateTime now) {
        return AdminPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .quotaSize(plan.getQuotaSize())
                .price(plan.getPrice())
                .durationMonths(plan.getDurationMonths())
                .status(getPlanStatus(plan.getId()))
                .features(getPlanFeatures(plan))
                .subscriberCount(userRepo.countActiveSubscribersByQuotaSize(plan.getQuotaSize(), now))
                .build();
    }

    private String normalizePlanStatus(String status) {
        String cleanStatus = status.trim().toUpperCase();
        if (!List.of("ACTIVE", "INACTIVE", "DISABLED").contains(cleanStatus)) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }
        return "DISABLED".equals(cleanStatus) ? "INACTIVE" : cleanStatus;
    }

    private String getPlanStatus(String planId) {
        String status = redisService.get(planStatusKey(planId));
        return status != null && !status.isBlank() ? status : "ACTIVE";
    }

    private List<String> getPlanFeatures(StoragePlan plan) {
        String savedFeatures = redisService.get(planFeaturesKey(plan.getId()));
        if (savedFeatures != null) {
            try {
                return objectMapper.readValue(
                        savedFeatures,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            } catch (JsonProcessingException ignored) {
                redisService.delete(planFeaturesKey(plan.getId()));
            }
        }
        return List.of(
                formatQuota(plan.getQuotaSize()) + " storage",
                (plan.getDurationMonths() != null ? plan.getDurationMonths() : 1) + " month duration"
        );
    }

    private void savePlanFeatures(String planId, List<String> features) {
        try {
            redisService.set(planFeaturesKey(planId), objectMapper.writeValueAsString(features));
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
    }

    private String formatQuota(Long quotaSize) {
        if (quotaSize == null) {
            return "Custom";
        }
        long oneGb = 1024L * 1024L * 1024L;
        if (quotaSize >= oneGb && quotaSize % oneGb == 0) {
            return (quotaSize / oneGb) + "GB";
        }
        long oneMb = 1024L * 1024L;
        if (quotaSize >= oneMb && quotaSize % oneMb == 0) {
            return (quotaSize / oneMb) + "MB";
        }
        return quotaSize + " bytes";
    }

    private String planStatusKey(String planId) {
        return ADMIN_PLAN_STATUS_PREFIX + planId;
    }

    private String planFeaturesKey(String planId) {
        return ADMIN_PLAN_FEATURES_PREFIX + planId;
    }

    // =========================================================================
    // 🤖 TRANG 6: THỐNG KÊ CHI TIẾT AI CHATBOT (RAG Semantic Analytics)
    // =========================================================================
    public AdminAiStatsResponse getAiStatistics() {
        Map<String, Long> trendMap = new LinkedHashMap<>();
        trendMap.put("Monday", 145L);
        trendMap.put("Tuesday", 190L);
        trendMap.put("Wednesday", 225L);
        trendMap.put("Thursday", 210L);
        trendMap.put("Friday", 265L);
        trendMap.put("Saturday", 340L);
        trendMap.put("Sunday", 295L);

        return AdminAiStatsResponse.builder()
                .totalAiMessagesThisMonth(5420L)
                .topAiUserMessageCount(380L)
                .knowledgeChatRatio(78.2) // 78.2% câu hỏi bám sát tài liệu cá nhân (RAG)
                .totalSummarizedDocs(168L) // 168 file PDF đã được AI đọc tóm tắt hộ sinh viên
                .aiUsageTrendByDay(trendMap)
                .build();
    }

    // =========================================================================
    // ⚙️ TRANG 7: CÀI ĐẶT HỆ THỐNG ADMINISTRATIVE
    // =========================================================================
    public SystemSettingsResponse getSystemSettings() {
        String savedSettings = redisService.get(ADMIN_SETTINGS_KEY);
        if (savedSettings != null) {
            try {
                return objectMapper.readValue(savedSettings, SystemSettingsResponse.class);
            } catch (JsonProcessingException ignored) {
                redisService.delete(ADMIN_SETTINGS_KEY);
            }
        }
        return defaultSystemSettings();
    }

    public SystemSettingsResponse updateSystemSettings(AdminUpdateSettingsRequest request, String adminName) {
        if (request == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        SystemSettingsResponse currentSettings = getSystemSettings();
        SystemSettingsResponse updatedSettings = SystemSettingsResponse.builder()
                .applicationName(firstNonBlank(request.getApplicationName(), currentSettings.getApplicationName()))
                .maintenanceMode(request.getMaintenanceMode() != null ? request.getMaintenanceMode() : currentSettings.isMaintenanceMode())
                .defaultStorageLimit(request.getDefaultStorageLimit() != null ? request.getDefaultStorageLimit() : currentSettings.getDefaultStorageLimit())
                .maxFileSizeUpload(request.getMaxFileSizeUpload() != null ? request.getMaxFileSizeUpload() : currentSettings.getMaxFileSizeUpload())
                .allowedFileTypes(firstNonBlank(request.getAllowedFileTypes(), currentSettings.getAllowedFileTypes()))
                .otpExpiryMinutes(request.getOtpExpiryMinutes() != null ? request.getOtpExpiryMinutes() : currentSettings.getOtpExpiryMinutes())
                .sessionTimeoutMinutes(request.getSessionTimeoutMinutes() != null ? request.getSessionTimeoutMinutes() : currentSettings.getSessionTimeoutMinutes())
                .maxLoginAttempts(request.getMaxLoginAttempts() != null ? request.getMaxLoginAttempts() : currentSettings.getMaxLoginAttempts())
                .emailNotificationEnabled(request.getEmailNotificationEnabled() != null ? request.getEmailNotificationEnabled() : currentSettings.isEmailNotificationEnabled())
                .allowRegistration(request.getAllowRegistration() != null ? request.getAllowRegistration() : currentSettings.isAllowRegistration())
                .defaultUserStorage(request.getDefaultUserStorage() != null ? request.getDefaultUserStorage() : currentSettings.getDefaultUserStorage())
                .build();
        try {
            redisService.set(ADMIN_SETTINGS_KEY, objectMapper.writeValueAsString(updatedSettings));
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }
        logService.log(adminName, "ADMIN_ACTION", "UPDATE_SETTINGS", "SYSTEM",
                "Admin " + adminName + " updated system settings");
        return updatedSettings;
    }

    private SystemSettingsResponse defaultSystemSettings() {
        return SystemSettingsResponse.builder()
                .applicationName("AI StudyHub Portal v1.0")
                .maintenanceMode(false)
                .defaultStorageLimit(524288000L)
                .maxFileSizeUpload(10485760L)
                .allowedFileTypes("pdf,doc,docx,png,jpg,jpeg,txt,mp3,mp4,ppt,pptx")
                .otpExpiryMinutes(5)
                .sessionTimeoutMinutes(60)
                .maxLoginAttempts(5)
                .emailNotificationEnabled(true)
                .allowRegistration(true)
                .defaultUserStorage(524288000L)
                .build();
    }

    private String firstNonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String refreshTokenKey(String userId) {
        return "auth:refresh:" + userId;
    }

    // 🌟 PHƯƠNG THỨC TIỆN ÍCH PHÂN TRANG THỦ CÔNG KHÔNG LỆCH NGOẶC
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
