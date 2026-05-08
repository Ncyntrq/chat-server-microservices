package com.chatsever.file.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity bảng file_metadata — lưu thông tin file đã upload.
 */
@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_uploader", columnList = "uploader"),
        @Index(name = "idx_channel", columnList = "channelId")
})
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String originalName;        // tên file gốc

    @Column(nullable = false, unique = true, length = 255)
    private String storedName;          // UUID-based name trên MinIO

    @Column(nullable = false, length = 100)
    private String contentType;         // MIME type: image/jpeg, application/pdf

    @Column(nullable = false)
    private Long fileSize;              // bytes

    @Column(nullable = false, length = 100)
    private String bucket = "chat-files";

    @Column(length = 255)
    private String thumbnailKey;        // key thumbnail trên MinIO (null nếu không phải ảnh)

    @Column(nullable = false, length = 100)
    private String uploader;            // username người upload

    private Long channelId;             // channel liên quan (nullable)

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Constructors ---

    public FileMetadata() {}

    public FileMetadata(String originalName, String storedName, String contentType,
                        Long fileSize, String bucket, String thumbnailKey,
                        String uploader, Long channelId) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.bucket = bucket;
        this.thumbnailKey = thumbnailKey;
        this.uploader = uploader;
        this.channelId = channelId;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getter & Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getThumbnailKey() { return thumbnailKey; }
    public void setThumbnailKey(String thumbnailKey) { this.thumbnailKey = thumbnailKey; }

    public String getUploader() { return uploader; }
    public void setUploader(String uploader) { this.uploader = uploader; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
