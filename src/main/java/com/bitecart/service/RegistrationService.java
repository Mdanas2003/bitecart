package com.bitecart.service;

import com.bitecart.dto.RegistrationRequest;
import com.bitecart.entity.User;

public interface RegistrationService {

    User register(RegistrationRequest request);
}
