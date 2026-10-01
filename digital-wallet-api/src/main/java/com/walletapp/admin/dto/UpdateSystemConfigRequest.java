package com.walletapp.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemConfigRequest {

    @NotBlank(message = "Giá trị cấu hình không được để trống")
    private String value;

    private String reason;
}
