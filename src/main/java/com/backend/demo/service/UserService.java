package com.backend.demo.service;

import com.backend.demo.model.User;

import java.util.List;

public interface UserService {
    User createUser(User user);
    List<User> getListuser();
}
