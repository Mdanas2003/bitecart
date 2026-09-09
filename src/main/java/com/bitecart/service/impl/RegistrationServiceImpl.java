package com.bitecart.service.impl;

import com.bitecart.dto.RegistrationRequest;
import com.bitecart.entity.Role;
import com.bitecart.entity.User;
import com.bitecart.exception.DuplicateEmailException;
import com.bitecart.repository.UserRepository;
import com.bitecart.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegistrationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setRole(Role.USER);
        user.setEnabled(true);

        return userRepository.save(user);
    }
}
