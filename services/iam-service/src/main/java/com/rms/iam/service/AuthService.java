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
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamEventPublisher iamEventPublisher;
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
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
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

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setPrivacyAcceptedAt(Instant.now());
        
        if (role == Role.HR_ADMIN) {
            if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
                throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Company name required for HR_ADMIN");
            }
            user.setPendingCompanyName(request.getCompanyName());
        }

        user = userRepository.save(user);

        String rawToken = jwtTokenProvider.generateRandomToken();
        String tokenHash = jwtTokenProvider.hashToken(rawToken);

        EmailVerificationToken evt = new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS));
        emailVerificationTokenRepository.save(evt);

        iamEventPublisher.sendVerificationEmail(user.getEmail(), rawToken);
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

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String hash = jwtTokenProvider.hashToken(request.getToken());
        EmailVerificationToken evt = emailVerificationTokenRepository.findByTokenHashAndUsedFalseAndExpiresAtAfter(hash, Instant.now())
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Invalid, expired, or already used token"));

        User user = evt.getUser();
        user.setStatus(UserStatus.ACTIVE);
        
        if (user.getRole() == Role.HR_ADMIN && user.getPendingCompanyName() != null) {
            Tenant tenant = new Tenant();
            tenant.setName(user.getPendingCompanyName());
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant = tenantRepository.save(tenant);
            
            user.setTenant(tenant);
            user.setPendingCompanyName(null);
        }

        userRepository.save(user);
        evt.setUsed(true);
        emailVerificationTokenRepository.save(evt);
        
        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        iamEventPublisher.publishUserCreatedEvent(user.getId(), user.getEmail(), user.getRole().name(), tenantId);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                // Invalidate old tokens (optional but good practice, could just let them expire or add a revoked flag)
                emailVerificationTokenRepository.invalidateAllByUserId(user.getId());
                // For simplicity, we just create a new one.
                String rawToken = jwtTokenProvider.generateRandomToken();
                String tokenHash = jwtTokenProvider.hashToken(rawToken);

                EmailVerificationToken evt = new EmailVerificationToken(user, tokenHash, Instant.now().plus(24, ChronoUnit.HOURS));
                emailVerificationTokenRepository.save(evt);

                iamEventPublisher.sendVerificationEmail(user.getEmail(), rawToken);
            }
        });
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
        
        refreshTokenRepository.revokeAllByUserId(user.getId());
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
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }
}
