package com.aram.mayhem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    private boolean success;
    private LocalDateTime syncTime;
    private int heroInserted;
    private int heroUpdated;
    private int augmentInserted;
    private int augmentUpdated;
    private int validationPassed;
    private int validationFailed;
    private String errorMessage;
    private long durationMs;

    public static SyncResult fail(String errorMessage) {
        return SyncResult.builder()
                .success(false)
                .syncTime(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
    }

    public static SyncResult success(int heroInserted, int heroUpdated,
                                     int augmentInserted, int augmentUpdated,
                                     int validationPassed, int validationFailed,
                                     long durationMs) {
        return SyncResult.builder()
                .success(true)
                .syncTime(LocalDateTime.now())
                .heroInserted(heroInserted)
                .heroUpdated(heroUpdated)
                .augmentInserted(augmentInserted)
                .augmentUpdated(augmentUpdated)
                .validationPassed(validationPassed)
                .validationFailed(validationFailed)
                .durationMs(durationMs)
                .build();
    }
}
