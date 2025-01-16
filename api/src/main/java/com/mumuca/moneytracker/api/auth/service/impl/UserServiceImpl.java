package com.mumuca.moneytracker.api.auth.service.impl;

import com.mumuca.moneytracker.api.auth.dto.UserDTO;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.auth.service.UserService;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDTO getUser(String userId) {
        return userRepository.findById(userId)
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getPhotoUrl(),
                        user.getGender(),
                        user.getAge()
                ))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
