package codeovai.codeovai.service.classification;

import codeovai.codeovai.enumeration.InputType;
import codeovai.codeovai.service.core.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.Imaging;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class InputClassifierService {

    private final LoggingService loggingService;

    private static final Set<String> ZIP_SIGNATURES = Set.of(
            "\u0050\u004B\u0003\u0004", // PK\03\04
            "\u0050\u004B\u0005\u0006", // PK\05\06
            "\u0050\u004B\u0007\u0008"  // PK\07\08
    );
    
    private static final Set<String> PDF_SIGNATURES = Set.of(
        "%PDF-"
    );
    
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp"
    );
    
    private static final Set<String> SUPPORTED_TEXT_EXTENSIONS = Set.of(
        "txt", "md", "json", "xml", "yaml", "yml"
    );
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public InputType classifyInput(MultipartFile file) {
        loggingService.logPipelineStep("INPUT_CLASSIFICATION", "Starting file classification", 
            "filename", file.getOriginalFilename(), 
            "size", String.valueOf(file.getSize()));
        
        try {
            validateFile(file);
            InputType type = determineFileType(file);
            
            loggingService.logPipelineStep("INPUT_CLASSIFICATION", "File classified successfully", 
                "filename", file.getOriginalFilename(), 
                "type", type.name());
            
            return type;
        } catch (Exception e) {
            loggingService.logError("INPUT_CLASSIFICATION", "File classification failed", e);
            return InputType.UNSUPPORTED;
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
        }
        
        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new IllegalArgumentException("File name is missing");
        }
    }
    
    private InputType determineFileType(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String filename = file.getOriginalFilename().toLowerCase();
        
        // Check file signatures first (more reliable than extension)
        if (isZipFile(fileBytes)) {
            return InputType.ZIP_ARCHIVE;
        }
        
        if (isPdfFile(fileBytes)) {
            return InputType.PDF_DOCUMENT;
        }
        
        // Check image files
        if (isImageFile(fileBytes, filename)) {
            return InputType.IMAGE_FILE;
        }
        
        // Check text files
        if (isTextFile(filename)) {
            return InputType.TEXT_FILE;
        }
        
        return InputType.UNSUPPORTED;
    }
    
    private boolean isZipFile(byte[] fileBytes) {
        if (fileBytes.length < 4) return false;
        
        String header = new String(fileBytes, 0, Math.min(4, fileBytes.length));
        return ZIP_SIGNATURES.stream().anyMatch(header::startsWith);
    }
    
    private boolean isPdfFile(byte[] fileBytes) {
        if (fileBytes.length < 5) return false;
        
        String header = new String(fileBytes, 0, Math.min(5, fileBytes.length));
        return PDF_SIGNATURES.stream().anyMatch(header::startsWith);
    }
    
    private boolean isImageFile(byte[] fileBytes, String filename) {
        try {
            ImageFormat format = Imaging.guessFormat(fileBytes);
            if (format != null) {
                if (!format.equals(ImageFormats.BMP) &&
                        !format.equals(ImageFormats.GIF) &&
                        !format.equals(ImageFormats.JPEG) &&
                        !format.equals(ImageFormats.PNG) &&
                        !format.equals(ImageFormats.TIFF)) {
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.debug("Failed to detect image format using Apache Imaging", e);
        }
        String extension = getFileExtension(filename);
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }
    
    private boolean isTextFile(String filename) {
        String extension = getFileExtension(filename);
        return SUPPORTED_TEXT_EXTENSIONS.contains(extension);
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
    
    public boolean isSupportedType(InputType type) {
        return type != InputType.UNSUPPORTED;
    }
}
