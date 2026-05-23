package service;

import request.FileResponse;
import model.FileEntity;
import model.User;
import repository.FileRepository;
import repository.UserRepository;
import exception.FileNotFoundException;
import exception.FileStorageException;
import exception.UserNotFoundException;
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
    public List<FileEntity> getUserFilesAsEntities(User user, int limit) {
        return fileRepository.findByUserAndDeletedFalseOrderByUploadedAtDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional
    public FileEntity uploadFile(MultipartFile file, String customFilename, User user) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + user.getId()));

        log.info("Starting upload for user: {}", managedUser.getUsername());

        try {
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

            byte[] fileBytes = file.getBytes();
            String fileHash = calculateHash(fileBytes);

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

            FileEntity savedEntity = fileRepository.save(fileEntity);
            log.info("File uploaded successfully: {} by user {}", storedFilename, managedUser.getUsername());
            return savedEntity;

        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public FileEntity findByUserAndOriginalFilename(User user, String originalFilename) {
        return fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(user, originalFilename)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + originalFilename));
    }

    @Transactional
    public void deleteFileByFilename(String filename, User user) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + user.getId()));

        FileEntity fileEntity = fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(managedUser, filename)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + filename));

        try {
            Path filePath = Paths.get(fileEntity.getFilePath());
            Files.deleteIfExists(filePath);

            fileEntity.setDeleted(true);
            fileEntity.setDeletedAt(LocalDateTime.now());
            fileRepository.save(fileEntity);

            log.info("File deleted: {} by user {}", filename, managedUser.getUsername());

        } catch (IOException e) {
            log.error("Error deleting file", e);
            throw new FileStorageException("Failed to delete file: " + e.getMessage(), e);
        }
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
            log.error("SHA-256 algorithm not found", e);
            throw new FileStorageException("Failed to calculate file hash", e);
        }
    }
}