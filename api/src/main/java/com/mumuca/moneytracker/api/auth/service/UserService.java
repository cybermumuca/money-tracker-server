package com.mumuca.moneytracker.api.auth.service;

import com.mumuca.moneytracker.api.auth.dto.UserDTO;

public interface UserService {
    UserDTO getUser(String userId);
}
