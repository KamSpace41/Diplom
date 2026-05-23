package controller;

import request.FileResponse;
import model.FileEntity;
import model.User;
import service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> getFiles(
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
            HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        List<FileEntity> files = fileService.getUserFilesAsEntities(currentUser, limit);
        List<FileResponse> response = files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/file")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String customFilename,
            HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        FileEntity fileEntity = fileService.uploadFile(file, customFilename, currentUser);
        return ResponseEntity.ok(toResponse(fileEntity));
    }

    @GetMapping(value = "/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("filename") String filename,
            HttpServletRequest request) throws IOException {
        User currentUser = (User) request.getAttribute("currentUser");

        FileEntity fileEntity = fileService.findByUserAndOriginalFilename(currentUser, filename);

        Path filePath = Paths.get(fileEntity.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @RequestParam("filename") String filename,
            HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        fileService.deleteFileByFilename(filename, currentUser);
        return ResponseEntity.ok().build();
    }

    private FileResponse toResponse(FileEntity file) {
        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getOriginalFilename())
                .size(file.getFileSize())
                .contentType(file.getContentType())
                .uploadedAt(file.getUploadedAt())
                .build();
    }
}