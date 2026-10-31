package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.dto.response.UserDto;
import com.rms.iam.entity.Role;
import com.rms.iam.entity.Tenant;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.mapper.UserMapper;
import com.rms.iam.repository.RefreshTokenRepository;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.UserRepository;
import com.rms.iam.messaging.IamEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamEventPublisher iamEventPublisher;

    public UserService(UserRepository userRepository, 
                       TokenBlacklistService tokenBlacklistService,
                       RefreshTokenRepository refreshTokenRepository,
                       UserMapper userMapper,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       IamEventPublisher iamEventPublisher) {
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.iamEventPublisher = iamEventPublisher;
    }

    @Transactional
    public UserDto createRecruiter(String email, String rawPassword, UUID tenantId) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new RmsException(ErrorCode.ERR_EMAIL_ALREADY_EXISTS, "Email already in use");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Tenant not found"));

        User recruiter = new User();
        recruiter.setEmail(normalizedEmail);
        recruiter.setPasswordHash(passwordEncoder.encode(rawPassword));
        recruiter.setRole(Role.RECRUITER);
        recruiter.setStatus(UserStatus.ACTIVE);
        recruiter.setTenant(tenant);
        recruiter.setPrivacyAcceptedAt(Instant.now());

        User saved = userRepository.save(recruiter);
        iamEventPublisher.publishUserCreatedEvent(saved.getId(), saved.getEmail(), saved.getRole().name(), tenantId);
        return userMapper.toDto(saved);
    }

    @Transactional
    public UserDto createHrAdmin(String email, String rawPassword, UUID tenantId) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new RmsException(ErrorCode.ERR_EMAIL_ALREADY_EXISTS, "Email already in use");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Tenant not found"));
        if (tenant.getStatus() != com.rms.iam.entity.TenantStatus.ACTIVE) {
            throw new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "Cannot assign an HR_ADMIN to an inactive tenant");
        }

        User hrAdmin = new User();
        hrAdmin.setEmail(normalizedEmail);
        hrAdmin.setPasswordHash(passwordEncoder.encode(rawPassword));
        hrAdmin.setRole(Role.HR_ADMIN);
        hrAdmin.setStatus(UserStatus.ACTIVE);
        hrAdmin.setTenant(tenant);
        hrAdmin.setPrivacyAcceptedAt(Instant.now());

        User saved = userRepository.save(hrAdmin);
        iamEventPublisher.publishUserCreatedEvent(saved.getId(), saved.getEmail(), saved.getRole().name(), tenantId);
        return userMapper.toDto(saved);
    }

    @Transactional
    public void deactivateUser(UUID targetUserId, UUID tenantId) {
        User user = userRepository.findByIdAndTenantId(targetUserId, tenantId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "User not found"));
        
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(targetUserId);
        tokenBlacklistService.blacklistUser(targetUserId, 900);
    }

    @Transactional
    public void deactivateUserById(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "User not found"));
        
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(targetUserId);
        tokenBlacklistService.blacklistUser(targetUserId, 900);
    }

    public UserDto getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "User not found"));
        return userMapper.toDto(user);
    }
}
