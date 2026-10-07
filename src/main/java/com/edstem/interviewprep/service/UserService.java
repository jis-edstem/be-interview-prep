package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.UserResponse;
import com.edstem.interviewprep.exception.UserNotFoundException;
import com.edstem.interviewprep.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;

    public UserResponse get(Long id) {
        return repository.findById(id).map(UserResponse::from).orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<UserResponse> list() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }
}
