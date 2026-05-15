package service;

import request.FileResponse;
import model.FileEntity;
import model.User;
import repository.FileRepository;
import repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Value("${file.storage.path:./uploads}")
    private String storagePath;

    @Transactional(readOnly = true)
    public List<FileResponse> getUserFiles(User user, int limit) {
        return fileRepository.findByUserAndDeletedFalseOrderByUploadedAtDesc(user)
                .stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FileResponse uploadFile(MultipartFile file, String customFilename, User user) throws IOException {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + user.getId()));

        log.info("Starting upload for user: {}", managedUser.getUsername());

        Path uploadPath = Paths.get(storagePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename;
        if (customFilename != null && !customFilename.isEmpty()) {
            originalFilename = customFilename;
        } else {
            originalFilename = file.getOriginalFilename();
        }

        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath);

        String fileHash = calculateHash(file.getBytes());

        FileEntity fileEntity = FileEntity.builder()
                .id(UUID.randomUUID().toString())
                .filename(storedFilename)
                .originalFilename(originalFilename)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .fileHash(fileHash)
                .uploadedAt(LocalDateTime.now())
                .downloadCount(0L)
                .deleted(false)
                .user(managedUser)
                .build();

        fileRepository.save(fileEntity);
        log.info("File uploaded successfully: {} by user {}", storedFilename, managedUser.getUsername());

        return toResponse(fileEntity);
    }

    @Transactional(readOnly = true)
    public FileEntity findByUserAndOriginalFilename(User user, String originalFilename) {
        return fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(user, originalFilename)
                .orElseThrow(() -> new RuntimeException("File not found: " + originalFilename));
    }

    @Transactional
    public void deleteFileByFilename(String filename, User user) throws IOException {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + user.getId()));

        FileEntity fileEntity = fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(managedUser, filename)
                .orElseThrow(() -> new RuntimeException("File not found: " + filename));

        Path filePath = Paths.get(fileEntity.getFilePath());
        Files.deleteIfExists(filePath);

        fileEntity.setDeleted(true);
        fileEntity.setDeletedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);

        log.info("File deleted: {} by user {}", filename, managedUser.getUsername());
    }

    private String calculateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating hash", e);
            return null;
        }
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