package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrapMarkRequest {

    @NotNull(message = "isVersionTrap 不能为空")
    private Boolean isVersionTrap;
}
