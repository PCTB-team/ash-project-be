package com.pctb.webapp.dto.request;

import com.pctb.webapp.entity.GroupVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupCreationRequest {

    @NotBlank(message = "Group name cannot be empty")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;

    @NotNull(message = "Visibility cannot be null")
    GroupVisibility visibility;

    // === THÊM TRƯỜNG NÀY ĐỂ ĐẶT PASS LÚC TẠO NHÓM ===
    String password;
}