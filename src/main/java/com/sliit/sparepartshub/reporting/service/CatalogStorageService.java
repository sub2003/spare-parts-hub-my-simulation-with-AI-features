package com.sliit.sparepartshub.reporting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogStorageService {
    private static final long MAX_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "csv", "xlsx");
    private static final Map<String, Set<String>> CONTENT_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "csv", Set.of("text/csv", "application/csv", "application/vnd.ms-excel", "text/plain"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")
    );

    private final Path catalogRoot;

    public CatalogStorageService(@Value("${app.catalog-upload-dir:uploads/catalogs}") String configuredPath) {
        this.catalogRoot = Path.of(configuredPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        validate(file);
        String extension = extension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = resolveStoredName(storedName);
        try {
            Files.createDirectories(catalogRoot);
            Path temp = Files.createTempFile(catalogRoot, "catalog-", ".tmp");
            try {
                file.transferTo(temp);
                move(temp, target);
            } catch (IOException ex) {
                Files.deleteIfExists(temp);
                throw ex;
            } catch (RuntimeException ex) {
                Files.deleteIfExists(temp);
                throw ex;
            }
            return storedName;
        } catch (IOException ex) {
            throw new IllegalStateException("Catalog upload could not be saved. Please try again.", ex);
        }
    }

    public Resource load(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("No catalog is attached to this request.");
        }
        Path target = resolveStoredName(storedName);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("Catalog file is not available on this server.");
        }
        return new PathResource(target);
    }

    /**
     * Moves a catalog to an internal temporary name before the database row is
     * changed. The caller can restore this staged file if its transaction rolls
     * back, or permanently delete the staged copy after commit. This avoids a
     * database row pointing at a file that was already permanently deleted.
     */
    public StagedDeletion stageForDeletion(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return null;
        }
        Path source = resolveStoredName(storedName);
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Catalog file is not available on this server.");
        }

        try {
            Files.createDirectories(catalogRoot);
            String stagedName = ".delete-" + UUID.randomUUID() + ".tmp";
            Path staged = resolveStoredName(stagedName);
            move(source, staged);
            return new StagedDeletion(storedName, stagedName);
        } catch (IOException ex) {
            throw new IllegalStateException("Catalog could not be prepared for removal. Please try again.", ex);
        }
    }

    public void restore(StagedDeletion stagedDeletion) {
        if (stagedDeletion == null) {
            return;
        }
        Path staged = resolveStoredName(stagedDeletion.stagedName());
        Path original = resolveStoredName(stagedDeletion.originalName());
        if (!Files.exists(staged)) {
            return;
        }
        try {
            if (Files.exists(original)) {
                throw new IllegalStateException("Catalog rollback could not restore the original file because the target already exists.");
            }
            move(staged, original);
        } catch (IOException ex) {
            throw new IllegalStateException("Catalog rollback could not restore the original file.", ex);
        }
    }

    public void finalizeDeletion(StagedDeletion stagedDeletion) {
        if (stagedDeletion == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredName(stagedDeletion.stagedName()));
        } catch (IOException ex) {
            throw new IllegalStateException("Catalog removal committed but the staged file could not be deleted.", ex);
        }
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.isBlank()) return;
        try {
            Files.deleteIfExists(resolveStoredName(storedName));
        } catch (Exception ignored) {
        }
    }

    public String extensionOfStoredName(String storedName) {
        return extension(storedName);
    }

    private Path resolveStoredName(String storedName) {
        String safeName = Path.of(storedName).getFileName().toString();
        if (!safeName.equals(storedName)) {
            throw new IllegalArgumentException("Invalid catalog reference.");
        }
        Path target = catalogRoot.resolve(safeName).normalize();
        if (!target.startsWith(catalogRoot)) {
            throw new IllegalArgumentException("Invalid catalog file path.");
        }
        return target;
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF, CSV or XLSX product catalog is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Catalog file must not exceed 10 MB.");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Catalog must be a PDF, CSV or XLSX file.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (!CONTENT_TYPES.get(extension).contains(normalized)) {
                throw new IllegalArgumentException("Catalog content type does not match the selected file type.");
            }
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        String safe = Path.of(filename).getFileName().toString();
        int dot = safe.lastIndexOf('.');
        return dot < 0 ? "" : safe.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StagedDeletion(String originalName, String stagedName) {
    }
}
