package com.pctb.webapp.controller;

import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.service.GroupFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupFileController {
    GroupFileService groupFileService;

    /**
     * Member upload file vao group.
     * Backend se check member APPROVED va canUpload=true truoc khi luu file.
     */
    @PostMapping(value = "/{groupId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupFileResponse> uploadFile(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Upload group file successfully")
                .result(groupFileService.uploadFile(groupId, file, authentication))
                .build();
    }

    /**
     * Member da APPROVED xem danh sach file trong group.
     */
    @GetMapping("/{groupId}/files")
    public ApiResponse<List<GroupFileResponse>> getGroupFiles(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupFileResponse>>builder()
                .message("Get group files successfully")
                .result(groupFileService.getGroupFiles(groupId, authentication))
                .build();
    }
}
