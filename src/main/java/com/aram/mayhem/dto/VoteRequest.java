package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VoteRequest {

    @NotBlank(message = "voteType is required")
    @Pattern(regexp = "UP|DOWN", message = "voteType must be UP or DOWN")
    private String voteType;
}