package com.prymo.authservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final StringRedisTemplate redisTemplate;
    
    // Fallback in-memory store for local testing when Redis is down
    private final Map<String, String> localOtpStore = new ConcurrentHashMap<>();
    
    private final Random random = new Random();

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp(String phoneNumber) {
        String otp = String.format("%06d", random.nextInt(1000000));
        
        try {
            redisTemplate.opsForValue().set("otp:" + phoneNumber, otp, Duration.ofMinutes(5));
            log.info("OTP saved in Redis for number: {}", phoneNumber);
        } catch (Exception e) {
            log.warn("Redis is not available. Falling back to local in-memory OTP cache. Error: {}", e.getMessage());
            localOtpStore.put(phoneNumber, otp);
            
            // Clean up OTP from memory after 5 minutes in a separate thread (simple TTL)
            new Thread(() -> {
                try {
                    Thread.sleep(300000); // 5 mins
                    localOtpStore.remove(phoneNumber);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        // Log it to console so developer can see the OTP if there's no SMS API connected
        log.info("----------------------------------------");
        log.info("Generated OTP for {}: {}", phoneNumber, otp);
        log.info("----------------------------------------");
        
        return otp;
    }

    public boolean verifyOtp(String phoneNumber, String inputOtp) {
        if (inputOtp == null || inputOtp.isBlank()) {
            return false;
        }

        try {
            String cachedOtp = redisTemplate.opsForValue().get("otp:" + phoneNumber);
            if (cachedOtp != null) {
                boolean isValid = cachedOtp.equals(inputOtp);
                if (isValid) {
                    redisTemplate.delete("otp:" + phoneNumber);
                }
                return isValid;
            }
        } catch (Exception e) {
            log.warn("Redis is not available. Reading OTP from local store fallback. Error: {}", e.getMessage());
        }

        // Check local store fallback
        String localOtp = localOtpStore.get(phoneNumber);
        if (localOtp != null && localOtp.equals(inputOtp)) {
            localOtpStore.remove(phoneNumber);
            return true;
        }

        return false;
    }
}
```,Description:
