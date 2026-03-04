package com.ASP.notification.Controller;

import com.ASP.notification.DTO.EmailNotification;
import com.ASP.notification.Service.NotificationService;
import com.ASP.notification.Service.OTPService;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;


@RestController
@Validated
@RequestMapping("/notify")
@CrossOrigin(origins = "*")
public class Controller {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private OTPService otpService;


    @PostMapping("/send")
    public Mono<ResponseEntity<Map<String, String>>>  sendNotification(@RequestBody EmailNotification emailNotification) {
        return notificationService.sendMail(emailNotification.to(), emailNotification.subject(), emailNotification.body())
                .map(status -> ResponseEntity.ok(Map.of("status", status)));
    }

    @PostMapping("/generate/{email}")
    public ResponseEntity<String> generateOTP(@Email @PathVariable String email) {
        String otp = otpService.generateOtp(email);
        if ("invalid email".equalsIgnoreCase(otp)) {
            return ResponseEntity.badRequest().body("The email does not exist");
        }
        String status = notificationService.sendMail(
                email,
                "One-Time Pin",
                "OTP to reset your password: " + otp
        ).block();
        if ("success".equalsIgnoreCase(status)) {
            return ResponseEntity.ok("OTP sent to email successfully");
        } else {
            return ResponseEntity.status(500).body(status);
        }
    }

    @PostMapping("/validate/{email}")
    public ResponseEntity<String> validateOTP(@RequestParam("otp") String otp, @PathVariable String email) {
        try {
            String result = otpService.validateOtp(email, otp);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
