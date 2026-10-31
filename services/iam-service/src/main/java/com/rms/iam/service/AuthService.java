package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.dto.request.*;
import com.rms.iam.dto.response.AuthResponse;
import com.rms.iam.entity.*;
import com.rms.iam.mapper.UserMapper;
import com.rms.iam.repository.EmailVerificationTokenRepository;
import com.rms.iam.repository.PasswordResetTokenRepository;
import com.rms.iam.repository.RefreshTokenRepository;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.UserRepository;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.messaging.AccountLockedEvent;
import com.rms.iam.messaging.SecurityAlertEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamEventPublisher iamEventPublisher;
    private final RegistrationOtpEmailService registrationOtpEmailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;

    private final int maxAttempts;
    private final int lockoutDurationMinutes;
    private final long refreshTokenExpiryDays;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       IamEventPublisher iamEventPublisher,
                       RegistrationOtpEmailService registrationOtpEmailService,
                       TokenBlacklistService tokenBlacklistService,
                       UserMapper userMapper,
                       @Value("${rms.lockout.max-attempts:5}") int maxAttempts,
                       @Value("${rms.lockout.lockout-duration-minutes:15}") int lockoutDurationMinutes,
                       @Value("${rms.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.iamEventPublisher = iamEventPublisher;
        this.registrationOtpEmailService = registrationOtpEmailService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userMapper = userMapper;
        this.maxAttempts = maxAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getPrivacyAccepted())) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Privacy policy must be accepted");
        }
        
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User previous = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (previous != null && previous.getStatus() != UserStatus.REJECTED) {
            throw new RmsException(ErrorCode.ERR_EMAIL_ALREADY_EXISTS, "Email already in use");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole());
            if (role != Role.CANDIDATE && role != Role.HR_ADMIN) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid role for registration");
        }

        User user = previous != null ? previous : new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setTenant(null);
        user.setPendingCompanyName(null);
        user.setPendingContactEmail(null);
        user.setPendingTaxCode(null);
        user.setPrivacyAcceptedAt(Instant.now());
        
        if (role == Role.HR_ADMIN) {
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Company name required for HR_ADMIN");
            }
            if (request.getContactEmail() == null || request.getContactEmail().isBlank()
                    || request.getTaxCode() == null || request.getTaxCode().isBlank()) {
                throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Company contact email and tax code required");
            }
            user.setPendingCompanyName(request.getCompanyName().trim());
            user.setPendingContactEmail(request.getContactEmail().trim().toLowerCase(Locale.ROOT));
            user.setPendingTaxCode(request.getTaxCode().trim());
        }

        user = userRepository.save(user);

        issueAndSendRegistrationOtp(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_MISSING_CREDENTIALS, "Invalid credentials"));

        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(Instant.now())) {
            throw new RmsException(ErrorCode.ERR_ACCOUNT_LOCKED, "Account is locked");
        } else if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
            user.setFailedLoginAttempts(0);
            user.setStatus(UserStatus.ACTIVE);
        }

        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw new RmsException(ErrorCode.ERR_ACCOUNT_LOCKED, "Đăng ký doanh nghiệp đang chờ SYS_ADMIN duyệt");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new RmsException(ErrorCode.ERR_ACCOUNT_LOCKED, "Đăng ký doanh nghiệp đã bị từ chối; bạn có thể đăng ký lại bằng email này");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RmsException(ErrorCode.ERR_ACCOUNT_LOCKED, "User account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxAttempts) {
                Instant lockoutTime = Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES);
                user.setLockoutUntil(lockoutTime);
                user.setStatus(UserStatus.LOCKED);
                iamEventPublisher.publishAccountLockedEvent(new AccountLockedEvent(user.getId(), user.getEmail(), lockoutTime));
            }
            userRepository.save(user);
            throw new RmsException(ErrorCode.ERR_MISSING_CREDENTIALS, "Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name(), tenantId, user.getEmail());
        
        String rawRefreshToken = jwtTokenProvider.generateRandomToken();
        String hash = jwtTokenProvider.hashToken(rawRefreshToken);
        
        RefreshToken rt = new RefreshToken(user, hash, Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);

        return new AuthResponse(accessToken, rawRefreshToken, userMapper.toDto(user));
    }

    @Transactional(noRollbackFor = RmsException.class)
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(candidate -> candidate.getStatus() == UserStatus.PENDING_VERIFICATION)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid, expired, or already used OTP"));
        EmailVerificationToken evt = emailVerificationTokenRepository
                .findFirstByUser_IdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(user.getId(), Instant.now())
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid, expired, or already used OTP"));

        if (!passwordEncoder.matches(request.getOtp(), evt.getTokenHash())) {
            evt.setFailedAttempts(evt.getFailedAttempts() + 1);
            if (evt.getFailedAttempts() >= 5) {
                evt.setUsed(true);
            }
            emailVerificationTokenRepository.save(evt);
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED,
                    evt.isUsed() ? "Too many incorrect OTP attempts. Request a new OTP." : "Invalid OTP");
        }

        user.setStatus(user.getRole() == Role.HR_ADMIN ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE);

        userRepository.save(user);
        evt.setUsed(true);
        emailVerificationTokenRepository.save(evt);
        
        if (user.getStatus() == UserStatus.ACTIVE) {
            UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
            iamEventPublisher.publishUserCreatedEvent(user.getId(), user.getEmail(), user.getRole().name(), tenantId, user.getFullName());
        } else {
            for (User admin : userRepository.findByRoleAndStatusOrderByCreatedAtAsc(Role.SYS_ADMIN, UserStatus.ACTIVE)) {
                iamEventPublisher.publishCompanyRegistrationSubmitted(user.getId(), admin.getId(),
                        user.getPendingCompanyName(), user.getEmail());
            }
        }
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                issueAndSendRegistrationOtp(user);
            }
        });
    }

    private void issueAndSendRegistrationOtp(User user) {
        emailVerificationTokenRepository.invalidateAllByUserId(user.getId());
        String otp = generateSixDigitOtp();
        EmailVerificationToken verification = new EmailVerificationToken(
                user, passwordEncoder.encode(otp), Instant.now().plus(10, ChronoUnit.MINUTES));
        emailVerificationTokenRepository.save(verification);
        registrationOtpEmailService.sendRegistrationOtp(user.getEmail(), otp);
    }

    private String generateSixDigitOtp() {
        return String.format(Locale.ROOT, "%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                String rawToken = jwtTokenProvider.generateRandomToken();
                String tokenHash = jwtTokenProvider.hashToken(rawToken);

                PasswordResetToken prt = new PasswordResetToken(user, tokenHash, Instant.now().plus(30, ChronoUnit.MINUTES));
                passwordResetTokenRepository.save(prt);

                iamEventPublisher.sendPasswordResetEmail(user.getEmail(), rawToken);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = jwtTokenProvider.hashToken(request.getToken());
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(hash, Instant.now())
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid, expired, or already used token"));

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        
        // Reset lockouts if any
        user.setFailedLoginAttempts(0);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockoutUntil(null);
        }
        
        userRepository.save(user);
        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
        
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());
        emailVerificationTokenRepository.invalidateAllByUserId(user.getId());
        refreshTokenRepository.revokeAllByUserId(user.getId());
        if (tokenBlacklistService != null) {
            tokenBlacklistService.blacklistUser(user.getId(), 15 * 60);
        }
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new RmsException(ErrorCode.ERR_MISSING_CREDENTIALS, "Refresh token is missing");
        }

        String hash = jwtTokenProvider.hashToken(rawRefreshToken);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid refresh token"));

        if (rt.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(rt.getUser().getId());
            iamEventPublisher.publishSecurityAlertEvent(new SecurityAlertEvent(
                    rt.getUser().getId(),
                    "TOKEN_REPLAY_DETECTED",
                    "A revoked refresh token was used."
            ));
            throw new RmsException(ErrorCode.ERR_INVALID_TOKEN, "Revoked refresh token used");
        }

        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Expired refresh token");
        }

        User user = rt.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RmsException(ErrorCode.ERR_ACCOUNT_LOCKED, "User account is not active");
        }

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name(), tenantId, user.getEmail());
        
        String newRawRefreshToken = jwtTokenProvider.generateRandomToken();
        String newHash = jwtTokenProvider.hashToken(newRawRefreshToken);
        
        RefreshToken newRt = new RefreshToken(user, newHash, Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(newRt);

        return new AuthResponse(newAccessToken, newRawRefreshToken, userMapper.toDto(user));
    }

    @Transactional
    public void logout(String accessToken, String rawRefreshToken, UUID userId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = jwtTokenProvider.hashToken(rawRefreshToken);
            var optRt = refreshTokenRepository.findByTokenHash(hash);
            if (optRt.isPresent()) {
                var rt = optRt.get();
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            }
        }
        // Logout revokes this refresh token only. Blacklisting by user ID here
        // would also reject a newly issued access token after the next login.
        // Existing access tokens remain valid until their normal short expiry.
    }
}
