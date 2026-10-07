package com.edstem.interviewprep.service;

import com.edstem.interviewprep.config.JwtProperties;
import com.edstem.interviewprep.config.SecurityConfig;
import com.edstem.interviewprep.dto.LoginRequest;
import com.edstem.interviewprep.dto.RegisterRequest;
import com.edstem.interviewprep.dto.TokenResponse;
import com.edstem.interviewprep.dto.UserResponse;
import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.exception.InvalidCredentialsException;
import com.edstem.interviewprep.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final String unknownUserHash;

    public AuthService(
            UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public UserResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
        try {
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            if (repository.existsByEmail(email)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw e;
        }
    }

    public TokenResponse login(LoginRequest request) {
        Optional<User> user = repository.findByEmail(User.normalizeEmail(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return issueToken(user.get());
    }

    private TokenResponse issueToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.ttl()))
                .claim(SecurityConfig.ROLES_CLAIM, List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, TOKEN_TYPE, jwtProperties.ttl().toSeconds());
    }
}
