package com.aram.mayhem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BulletinListVO {

    private Long id;

    private String type;

    private String title;

    private String content;

    private String imageUrl;

    private Integer isPinned;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;
}
