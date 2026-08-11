package com.rms.iam.service;

import com.rms.common.exception.ErrorCode;
import com.rms.common.exception.RmsException;
import com.rms.iam.dto.request.LoginRequest;
import com.rms.iam.dto.response.AuthResponse;
import com.rms.iam.dto.response.UserDto;
import com.rms.iam.entity.Role;
import com.rms.iam.entity.User;
import com.rms.iam.entity.UserStatus;
import com.rms.iam.mapper.UserMapper;
import com.rms.iam.messaging.IamEventPublisher;
import com.rms.iam.repository.EmailVerificationTokenRepository;
import com.rms.iam.repository.PasswordResetTokenRepository;
import com.rms.iam.repository.RefreshTokenRepository;
import com.rms.iam.repository.TenantRepository;
import com.rms.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private IamEventPublisher iamEventPublisher;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserMapper userMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                tenantRepository,
                emailVerificationTokenRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                iamEventPublisher,
                tokenBlacklistService,
                userMapper,
                5, // maxAttempts
                15, // lockoutDurationMinutes
                7 // refreshTokenExpiryDays
        );
    }

    @Test
    void testLogin_Success() {
        String email = "test@rms.com";
        String password = "password";
        
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("hashed_password");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.SYS_ADMIN);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), nullable(UUID.class), any())).thenReturn("jwt-token");
        when(jwtTokenProvider.generateRandomToken()).thenReturn("refresh-token");
        when(jwtTokenProvider.hashToken("refresh-token")).thenReturn("hash");
        when(userMapper.toDto(user)).thenReturn(new UserDto());
        
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        
        AuthResponse res = authService.login(req);
        
        assertNotNull(res);
        assertEquals("jwt-token", res.getAccessToken());
        verify(userRepository, times(1)).save(user); // reset failed attempts
    }

    @Test
    void testLogin_LockedUser() {
        String email = "locked@rms.com";
        
        User user = new User();
        user.setEmail(email);
        user.setStatus(UserStatus.LOCKED);
        user.setLockoutUntil(Instant.now().plusSeconds(3600)); // Future lockout
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("password");
        
        assertThrows(RmsException.class, () -> authService.login(req));
    }

    @Test
    void testRefreshToken_ReplayAttack() {
        String rawToken = "raw-refresh-token";
        String hash = "hashed-token";
        
        User user = new User();
        user.setId(UUID.randomUUID());
        
        com.rms.iam.entity.RefreshToken rt = new com.rms.iam.entity.RefreshToken();
        rt.setTokenHash(hash);
        rt.setRevoked(true); // Revoked token means it was already used
        rt.setUser(user);
        
        when(jwtTokenProvider.hashToken(rawToken)).thenReturn(hash);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(rt));
        
        RmsException exception = assertThrows(RmsException.class, () -> authService.refreshToken(rawToken));
        
        assertEquals(ErrorCode.ERR_INVALID_TOKEN, exception.getErrorCode());
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(iamEventPublisher).publishSecurityAlertEvent(any(com.rms.iam.messaging.SecurityAlertEvent.class));
    }
}
