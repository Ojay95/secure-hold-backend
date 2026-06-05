package com.prymo.authservice.application.usecase;

import com.prymo.authservice.OtpService;
import org.springframework.stereotype.Service;

@Service
public class ManageOtpUseCase {

    private final OtpService otpService;

    public ManageOtpUseCase(OtpService otpService) {
        this.otpService = otpService;
    }

    public String sendOtp(String phoneNumber) {
        return otpService.generateOtp(phoneNumber);
    }

    public boolean verifyOtp(String phoneNumber, String otpCode) {
        return otpService.verifyOtp(phoneNumber, otpCode);
    }
}
