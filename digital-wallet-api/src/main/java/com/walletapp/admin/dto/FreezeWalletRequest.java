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
public class FreezeWalletRequest {

    @NotBlank(message = "Lý do thay đổi trạng thái ví không được để trống")
    private String reason;
}
