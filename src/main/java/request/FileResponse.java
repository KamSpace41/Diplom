package request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileResponse {
    private String id;
    private String filename;
    private Long size;
    private String contentType;
    private LocalDateTime uploadedAt;
}