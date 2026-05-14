package com.aram.mayhem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateStrategyRequest {

    @NotNull(message = "heroId is required")
    private Long heroId;

    @NotBlank(message = "title is required")
    @Size(min = 1, max = 100, message = "title length must be between 1 and 100")
    private String title;

    @NotBlank(message = "description is required")
    @Size(min = 10, message = "description must be at least 10 characters")
    private String description;

    private List<Long> augmentIds;

    private List<Long> itemIds;
}