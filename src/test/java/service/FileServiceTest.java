package service;

import model.FileEntity;
import model.User;
import repository.FileRepository;
import repository.UserRepository;
import exception.FileNotFoundException;
import exception.FileStorageException;
import exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private User testUser;
    private User managedUser;
    private FileEntity testFileEntity;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .active(true)
                .build();

        managedUser = User.builder()
                .id(1L)
                .username("testuser")
                .active(true)
                .build();

        testFileEntity = FileEntity.builder()
                .id(UUID.randomUUID().toString())
                .filename("stored-file.txt")
                .originalFilename("original.txt")
                .filePath(tempDir.resolve("stored-file.txt").toString())
                .fileSize(1024L)
                .contentType("text/plain")
                .fileHash("abc123hash")
                .uploadedAt(LocalDateTime.now())
                .downloadCount(0L)
                .deleted(false)
                .user(managedUser)
                .build();

        ReflectionTestUtils.setField(fileService, "storagePath", tempDir.toString());
    }

    @Test
    void getUserFilesAsEntities_ShouldReturnListOfFiles() {
        List<FileEntity> expectedFiles = List.of(testFileEntity);
        when(fileRepository.findByUserAndDeletedFalseOrderByUploadedAtDesc(testUser))
                .thenReturn(expectedFiles);

        List<FileEntity> result = fileService.getUserFilesAsEntities(testUser, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("original.txt", result.get(0).getOriginalFilename());
        verify(fileRepository).findByUserAndDeletedFalseOrderByUploadedAtDesc(testUser);
    }

    @Test
    void uploadFile_ShouldUploadAndSaveFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileEntity result = fileService.uploadFile(multipartFile, null, testUser);

        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalFilename());
        assertEquals(11L, result.getFileSize());
        assertEquals("text/plain", result.getContentType());
        assertNotNull(result.getFileHash());
        assertFalse(result.getFileHash().isEmpty());

        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void uploadFile_WithCustomFilename_ShouldUseCustomFilename() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "original.txt",
                "text/plain",
                "Content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileEntity result = fileService.uploadFile(multipartFile, "custom-name.txt", testUser);

        assertNotNull(result);
        assertEquals("custom-name.txt", result.getOriginalFilename());
    }

    @Test
    void uploadFile_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> fileService.uploadFile(multipartFile, null, testUser));
    }

    @Test
    void findByUserAndOriginalFilename_WhenFileExists_ShouldReturnFile() {
        when(fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(testUser, "original.txt"))
                .thenReturn(Optional.of(testFileEntity));

        FileEntity result = fileService.findByUserAndOriginalFilename(testUser, "original.txt");

        assertNotNull(result);
        assertEquals("original.txt", result.getOriginalFilename());
    }

    @Test
    void findByUserAndOriginalFilename_WhenFileNotFound_ShouldThrowFileNotFoundException() {
        when(fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(testUser, "nonexistent.txt"))
                .thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> fileService.findByUserAndOriginalFilename(testUser, "nonexistent.txt"));
    }

    @Test
    void deleteFileByFilename_ShouldDeleteFileAndMarkAsDeleted() throws Exception {
        Path testFile = tempDir.resolve("stored-file.txt");
        Files.writeString(testFile, "test content");

        testFileEntity.setFilePath(testFile.toString());

        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(managedUser, "original.txt"))
                .thenReturn(Optional.of(testFileEntity));
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFileEntity);

        fileService.deleteFileByFilename("original.txt", testUser);

        assertTrue(testFileEntity.isDeleted());
        assertNotNull(testFileEntity.getDeletedAt());
        assertFalse(Files.exists(testFile));

        verify(fileRepository).save(testFileEntity);
    }

    @Test
    void deleteFileByFilename_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> fileService.deleteFileByFilename("original.txt", testUser));
    }

    @Test
    void deleteFileByFilename_WhenFileNotFound_ShouldThrowFileNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.findByUserAndOriginalFilenameAndDeletedFalse(managedUser, "nonexistent.txt"))
                .thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> fileService.deleteFileByFilename("nonexistent.txt", testUser));
    }

    @Test
    void uploadFile_WhenStorageDirectoryDoesNotExist_ShouldCreateDirectory() throws Exception {
        Path newTempDir = tempDir.resolve("new-uploads");
        ReflectionTestUtils.setField(fileService, "storagePath", newTempDir.toString());

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileEntity result = fileService.uploadFile(multipartFile, null, testUser);

        assertNotNull(result);
        assertTrue(Files.exists(newTempDir));
    }

    @Test
    void calculateHash_ShouldReturnValidSha256Hash() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managedUser));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileEntity result = fileService.uploadFile(multipartFile, null, testUser);

        assertNotNull(result.getFileHash());
        assertEquals(64, result.getFileHash().length());
    }
}