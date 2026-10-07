package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.RegisterRequest;
import com.edstem.interviewprep.dto.UserResponse;
import com.edstem.interviewprep.entity.Role;
import com.edstem.interviewprep.entity.User;
import com.edstem.interviewprep.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
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

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
