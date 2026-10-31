package com.rms.iam.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationOtpEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public RegistrationOtpEmailService(
            JavaMailSender mailSender,
            @Value("${rms.mail.from:}") String fromAddress,
            @Value("${rms.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendRegistrationOtp(String recipient, String otp) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("SMTP_FROM must be configured with a verified sender email address");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Xác thực đăng ký tài khoản RMS");
        message.setText("Mã OTP của bạn là " + otp + ". Vui lòng nhập mã OTP để đăng ký tài khoản");
        mailSender.send(message);
    }

    public void sendCompanyApproval(String recipient, String companyName) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("SMTP_FROM must be configured with a verified sender email address");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Doanh nghiệp của bạn đã được duyệt trên RMS");
        message.setText("Xin chào,\n\nDoanh nghiệp " + companyName
                + " đã được SYS_ADMIN phê duyệt. Tài khoản HR_ADMIN của bạn đã được kích hoạt."
                + " Bạn có thể đăng nhập tại " + frontendBaseUrl + "/login bằng email và mật khẩu đã đăng ký."
                + "\n\nTrân trọng,\nRecruitment System");
        mailSender.send(message);
    }
}
