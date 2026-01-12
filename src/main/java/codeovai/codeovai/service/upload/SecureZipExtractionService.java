package codeovai.codeovai.service.upload;

import codeovai.codeovai.service.core.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecureZipExtractionService {

    private final LoggingService loggingService;
    
    private static final String BASE_UPLOAD_DIR = "uploads";
    private static final long MAX_ZIP_SIZE = 100 * 1024 * 1024; // 100MB
    private static final long MAX_ENTRY_SIZE = 10 * 1024 * 1024; // 10MB per entry
    private static final int MAX_ENTRIES = 1000;
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "app", "deb", "pkg", "dmg", "rpm"
    );
    private static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
        "..", "./", "\\", "~", "$", "system32", "windows", "program files"
    );
    
    public String extractSecurely(MultipartFile zipFile) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("ZIP_EXTRACTION", "Starting secure ZIP extraction", 
            "filename", zipFile.getOriginalFilename(), 
            "size", String.valueOf(zipFile.getSize()));
        
        try {
            validateZipFile(zipFile);
            String systemId = UUID.randomUUID().toString();
            Path systemRoot = Paths.get(BASE_UPLOAD_DIR, systemId);
            
            Files.createDirectories(systemRoot);
            extractZipSecurely(zipFile.getInputStream(), systemRoot);
            
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("ZIP_EXTRACTION", "complete_extraction", duration, 
                "systemId", systemId, 
                "entries", "extracted");
            
            return systemId;
        } catch (Exception e) {
            loggingService.logError("ZIP_EXTRACTION", "Secure extraction failed", e);
            throw new RuntimeException("Failed to extract ZIP file securely", e);
        }
    }
    
    private void validateZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SecurityException("ZIP file is empty or null");
        }
        
        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new SecurityException("ZIP file size exceeds maximum limit of 100MB");
        }
        
        // Check file signature
        try {
            byte[] header = file.getBytes();
            if (header.length < 4) {
                throw new SecurityException("File too small to be a valid ZIP");
            }
            
            String signature = new String(header, 0, 4);
            if (!signature.startsWith("PK")) {
                throw new SecurityException("Invalid ZIP file signature");
            }
        } catch (IOException e) {
            throw new SecurityException("Failed to read ZIP file header", e);
        }
    }
    
    private void extractZipSecurely(InputStream inputStream, Path targetDir) throws IOException {
        int entryCount = 0;
        long totalExtractedSize = 0;
        
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryCount++;
                
                if (entryCount > MAX_ENTRIES) {
                    throw new SecurityException("ZIP contains too many entries (max: " + MAX_ENTRIES + ")");
                }
                
                // Validate entry name
                validateEntryName(entry.getName());
                
                // Check entry size
                if (entry.getSize() > MAX_ENTRY_SIZE) {
                    throw new SecurityException("ZIP entry too large: " + entry.getName() + " (max: " + MAX_ENTRY_SIZE + " bytes)");
                }
                
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                
                // Prevent path traversal attacks
                if (!resolvedPath.startsWith(targetDir.normalize())) {
                    loggingService.logSecurityEvent("PATH_TRAVERSAL_ATTEMPT", "HIGH", 
                        "entry", entry.getName(), 
                        "resolvedPath", resolvedPath.toString());
                    throw new SecurityException("Path traversal attempt detected: " + entry.getName());
                }
                
                // Check for dangerous file extensions
                if (hasDangerousExtension(entry.getName())) {
                    loggingService.logSecurityEvent("DANGEROUS_FILE_ATTEMPT", "MEDIUM", 
                        "entry", entry.getName());
                    continue; // Skip dangerous files
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipInputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    totalExtractedSize += Files.size(resolvedPath);
                }
                
                zipInputStream.closeEntry();
            }
        }
        
        loggingService.logPipelineStep("ZIP_EXTRACTION", "Extraction completed", 
            "entries", String.valueOf(entryCount), 
            "totalSize", String.valueOf(totalExtractedSize));
    }
    
    private void validateEntryName(String entryName) {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new SecurityException("ZIP entry name is empty");
        }
        
        // Check for suspicious patterns
        String lowerName = entryName.toLowerCase();
        for (String pattern : SUSPICIOUS_PATTERNS) {
            if (lowerName.contains(pattern)) {
                loggingService.logSecurityEvent("SUSPICIOUS_ENTRY_NAME", "MEDIUM", 
                    "entry", entryName, 
                    "pattern", pattern);
                break;
            }
        }
        
        // Check for very long paths (potential DoS)
        if (entryName.length() > 260) { // Windows MAX_PATH limit
            throw new SecurityException("ZIP entry name too long: " + entryName);
        }
    }
    
    private boolean hasDangerousExtension(String entryName) {
        String lowerName = entryName.toLowerCase();
        int lastDot = lowerName.lastIndexOf('.');
        
        if (lastDot > 0) {
            String extension = lowerName.substring(lastDot + 1);
            return DANGEROUS_EXTENSIONS.contains(extension);
        }
        
        return false;
    }
    
    public Path getSystemRoot(String systemId) {
        Path root = Paths.get(BASE_UPLOAD_DIR, systemId);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("System directory not found: " + systemId);
        }
        return root;
    }
    
    public void cleanupSystem(String systemId) {
        try {
            Path systemRoot = Paths.get(BASE_UPLOAD_DIR, systemId);
            if (Files.exists(systemRoot)) {
                org.apache.commons.io.FileUtils.deleteDirectory(systemRoot.toFile());
                loggingService.logPipelineStep("CLEANUP", "System directory cleaned up", 
                    "systemId", systemId);
            }
        } catch (IOException e) {
            loggingService.logError("CLEANUP", "Failed to cleanup system directory", e);
        }
    }
}
