package model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "last_downloaded_at")
    private LocalDateTime lastDownloadedAt;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "is_deleted")
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        if (downloadCount == null) downloadCount = 0L;
    }
}