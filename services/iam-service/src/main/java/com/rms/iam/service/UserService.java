package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.dto.response.UserDto;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.mapper.UserMapper;
import com.rms.iam.repository.RefreshTokenRepository;
import com.rms.iam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, 
                       TokenBlacklistService tokenBlacklistService,
                       RefreshTokenRepository refreshTokenRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
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

    public UserDto getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RmsException(ErrorCode.ERR_VALIDATION_FAILED, "User not found"));
        return userMapper.toDto(user);
    }
}
