package com.rabbit.server.service;

import com.rabbit.common.dto.SuccessAuthDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto createUser(UserDto userDto, String password, UserRole creatorRole, Long projectId) {
        if (creatorRole == UserRole.MANAGER) {
            userDto.setRole(UserRole.WORKER);
        } else if (creatorRole == UserRole.TEAM_LEADER) {
            userDto.setRole(UserRole.TEAM_LEADER);
        } else {
            throw new SecurityException("Only Manager or TeamLead can create users");
        }

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String passwordHash = hashPassword(password);

        Long userId = userRepository.save(userDto, passwordHash);
        userDto.setId(userId);

        if (projectId != null) {
            userRepository.addUserToProject(userDto.getId(), projectId);
        }

        return userDto;
    }

    public void deleteUser(Long userId, Long requestingUserId) {
        if (!userId.equals(requestingUserId)) {
            throw new SecurityException("You can only delete your own account");
        }
        userRepository.deleteById(userId);
    }

    public UserDto updateUser(Long userId, UserDto updatedData, String newPassword, Long requestingUserId) {
        if (!userId.equals(requestingUserId)) {
            throw new SecurityException("You can only update your own account");
        }

        UserDto existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (updatedData.getName() != null && !updatedData.getName().trim().isEmpty()) {
            existingUser.setName(updatedData.getName());
        }
        if (updatedData.getNickname() != null && !updatedData.getNickname().trim().isEmpty()) {
            existingUser.setNickname(updatedData.getNickname());
        }
        if (updatedData.getEmail() != null && !updatedData.getEmail().trim().isEmpty()) {
            if (userRepository.findByEmail(updatedData.getEmail()).isPresent() &&
                    !updatedData.getEmail().equals(existingUser.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            existingUser.setEmail(updatedData.getEmail());
        }

        userRepository.update(existingUser);

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            userRepository.updatePassword(userId, hashPassword(newPassword));
        }

        return existingUser;
    }

    public UserDto getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<UserDto> getAllUsersFromProject(Long projectId) {
        return userRepository.findAllByProjectId(projectId);
    }

    public void addUserToProject(Long userId, Long projectId) {
        userRepository.addUserToProject(userId, projectId);
    }

    public void removeUserFromProject(Long userId, Long projectId) {
        userRepository.removeUserFromProject(userId, projectId);
    }

    public void removeUserFromAllProjects(Long userId) {
        userRepository.removeUserFromAllProjects(userId);
    }

    public SuccessAuthDto loginUser(String email, String password){
        Optional<UserDto> optionalUser = userRepository.findByEmailAndPassword(
            email,
            hashPassword(password)
        );

        if (optionalUser.isEmpty())
            return null;

        return new SuccessAuthDto(AuthMiddleware.getInstanse().createToken(optionalUser.get().getId().intValue()), optionalUser.get());

    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

}
