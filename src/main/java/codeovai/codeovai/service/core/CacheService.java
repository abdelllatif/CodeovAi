package codeovai.codeovai.service.core;

import codeovai.codeovai.service.core.LoggingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoggingService loggingService;
    
    private static final String CACHE_PREFIX = "codeovai:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration SHORT_TTL = Duration.ofMinutes(30);
    private static final Duration LONG_TTL = Duration.ofHours(24);
    
    public <T> T get(String key, Class<T> type) {
        try {
            String cacheKey = buildCacheKey(key);
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            
            if (cached != null) {
                loggingService.logPipelineStep("CACHE", "Cache hit", 
                    "key", key, 
                    "type", type.getSimpleName());
                
                return objectMapper.convertValue(cached, type);
            }
            
            loggingService.logPipelineStep("CACHE", "Cache miss", 
                "key", key, 
                "type", type.getSimpleName());
            
            return null;
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to retrieve from cache", e);
            return null;
        }
    }
    
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }
    
    public <T> void put(String key, T value, Duration ttl) {
        try {
            String cacheKey = buildCacheKey(key);
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
            
            loggingService.logPipelineStep("CACHE", "Cache entry stored", 
                "key", key, 
                "ttl", ttl.toString(), 
                "type", value.getClass().getSimpleName());
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to store in cache", e);
        }
    }
    
    public void evict(String key) {
        try {
            String cacheKey = buildCacheKey(key);
            redisTemplate.delete(cacheKey);
            
            loggingService.logPipelineStep("CACHE", "Cache entry evicted", 
                "key", key);
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to evict from cache", e);
        }
    }
    
    public void evictPattern(String pattern) {
        try {
            String cachePattern = buildCacheKey(pattern);
            var keys = redisTemplate.keys(cachePattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                loggingService.logPipelineStep("CACHE", "Cache pattern evicted", 
                    "pattern", pattern, 
                    "keys", String.valueOf(keys.size()));
            }
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to evict pattern from cache", e);
        }
    }
    
    public boolean exists(String key) {
        try {
            String cacheKey = buildCacheKey(key);
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return exists != null && exists;
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to check cache existence", e);
            return false;
        }
    }
    
    public String generateFileHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            loggingService.logError("CACHE", "Failed to generate file hash", e);
            throw new RuntimeException("Hash generation failed", e);
        }
    }
    
    public String buildAnalysisCacheKey(String fileHash, String analysisType) {
        return String.format("analysis:%s:%s", fileHash, analysisType);
    }
    
    public String buildSystemCacheKey(String systemId) {
        return String.format("system:%s", systemId);
    }
    
    public String buildPromptCacheKey(String contextHash) {
        return String.format("prompt:%s", contextHash);
    }
    
    public String buildResponseCacheKey(String promptHash) {
        return String.format("response:%s", promptHash);
    }
    
    public void cacheAnalysisResult(String fileHash, String analysisType, Object result) {
        String key = buildAnalysisCacheKey(fileHash, analysisType);
        Duration ttl = determineTTL(analysisType);
        put(key, result, ttl);
    }
    
    public <T> T getCachedAnalysisResult(String fileHash, String analysisType, Class<T> resultType) {
        String key = buildAnalysisCacheKey(fileHash, analysisType);
        return get(key, resultType);
    }
    
    public void cacheSystemContext(String systemId, Object context) {
        String key = buildSystemCacheKey(systemId);
        put(key, context, LONG_TTL);
    }
    
    public <T> T getCachedSystemContext(String systemId, Class<T> contextType) {
        String key = buildSystemCacheKey(systemId);
        return get(key, contextType);
    }
    
    public void cleanupExpiredEntries() {
        try {
            // Redis automatically handles TTL expiration, but we can log statistics
            var keys = redisTemplate.keys(buildCacheKey("*"));
            if (keys != null) {
                loggingService.logPipelineStep("CACHE", "Cache statistics", 
                    "totalKeys", String.valueOf(keys.size()));
            }
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to cleanup expired entries", e);
        }
    }
    
    private String buildCacheKey(String key) {
        return CACHE_PREFIX + key;
    }
    
    private Duration determineTTL(String analysisType) {
        String lowerType = analysisType.toLowerCase();
        
        if (lowerType.contains("zip") || lowerType.contains("project")) {
            return LONG_TTL;
        } else if (lowerType.contains("ocr") || lowerType.contains("image")) {
            return SHORT_TTL;
        } else {
            return DEFAULT_TTL;
        }
    }
    
    public void warmCache() {
        // Implement cache warming logic for common patterns
        loggingService.logPipelineStep("CACHE", "Cache warming initiated");
        
        // This could pre-load common analysis results or frequently accessed data
        // Implementation depends on specific use cases and usage patterns
    }
    
    public long getCacheSize() {
        try {
            var keys = redisTemplate.keys(buildCacheKey("*"));
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to get cache size", e);
            return 0;
        }
    }
    
    public void clearAllCache() {
        try {
            var keys = redisTemplate.keys(buildCacheKey("*"));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                loggingService.logPipelineStep("CACHE", "All cache cleared", 
                    "keys", String.valueOf(keys.size()));
            }
        } catch (Exception e) {
            loggingService.logError("CACHE", "Failed to clear all cache", e);
        }
    }
}
