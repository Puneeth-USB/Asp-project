package com.ASP.notification.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class OTPService {

    private static final long OTP_TTL_SECONDS= 180;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ScheduledExecutorService scheduledExecutorService= Executors.newSingleThreadScheduledExecutor();
    @Autowired
    private WebClient.Builder webClientBuilder;

    private static class OtpRecords{
        private final String otp;
        private final Instant otpTtl;
        public OtpRecords(String otp, Instant otpTtl){
            this.otp = otp;
            this.otpTtl = otpTtl;
        }
    }

   private final Map<String, OtpRecords> otpRecords = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        if (email == null || email.isBlank()) {
            return "invalid email";
        }
        String url = "http://localhost:3001/auth/byEmail?email=" + email;
        WebClient webClient = webClientBuilder.build();
        Boolean emailExists;
        try {
            emailExists = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        } catch (Exception ex) {
            // Log the error if you have a logger, or print stack trace
            // logger.error("Error calling auth service", ex);
            return "error validating email: " + ex.getMessage();
        }
        if (!Boolean.TRUE.equals(emailExists)) {
            return "invalid email";
        }
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        Instant otpTtl = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        otpRecords.put(email, new OtpRecords(otp, otpTtl));
        scheduledExecutorService.schedule(() -> otpRecords.remove(email), 300, TimeUnit.SECONDS);
        return otp;
    }

    public String validateOtp(String email,String otp) throws Exception {
        OtpRecords records = otpRecords.get(email);
        if(records == null){
           throw new Exception(" no records found Retry");
        }
       if (Instant.now().isAfter(records.otpTtl)){
           throw new Exception("OPT expired");
       }
       boolean valid = Objects.equals(records.otp, otp);
       if(!valid) {
           throw new Exception("Invalid otp");
       }
       return "otp validted successfully";
    }
}
