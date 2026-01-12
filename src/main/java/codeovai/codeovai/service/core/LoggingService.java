package codeovai.codeovai.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingService {

    private final ObjectMapper objectMapper;
    
    public void logPipelineStep(String pipeline, String message, String... keyValuePairs) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("pipeline", pipeline);
        logData.put("message", message);
        logData.put("level", "INFO");
        
        // Add key-value pairs
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                logData.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        
        try {
            log.info(objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to serialize log data: {}", e.getMessage());
            log.info("Pipeline: {} - Message: {} - Data: {}", pipeline, message, logData);
        }
    }
    
    public void logError(String pipeline, String message, Throwable error) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("pipeline", pipeline);
        logData.put("message", message);
        logData.put("level", "ERROR");
        logData.put("error", error.getMessage());
        logData.put("errorType", error.getClass().getSimpleName());
        
        try {
            log.error(objectMapper.writeValueAsString(logData), error);
        } catch (Exception e) {
            log.error("Failed to serialize error log data: {}", e.getMessage());
            log.error("Pipeline: {} - Message: {} - Error: {}", pipeline, message, error.getMessage(), error);
        }
    }
    
    public void logPerformance(String pipeline, String operation, long durationMs, String... keyValuePairs) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("pipeline", pipeline);
        logData.put("operation", operation);
        logData.put("durationMs", durationMs);
        logData.put("level", "PERFORMANCE");
        
        // Add key-value pairs
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                logData.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        
        try {
            log.info(objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to serialize performance log data: {}", e.getMessage());
            log.info("Performance: {} - Operation: {} - Duration: {}ms", pipeline, operation, durationMs);
        }
    }
    
    public void logSecurityEvent(String event, String severity, String... keyValuePairs) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now().toString());
        logData.put("event", event);
        logData.put("severity", severity);
        logData.put("category", "SECURITY");
        logData.put("level", "WARN");
        
        // Add key-value pairs
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                logData.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        
        try {
            log.warn(objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.error("Failed to serialize security log data: {}", e.getMessage());
            log.warn("Security Event: {} - Severity: {}", event, severity);
        }
    }
}
