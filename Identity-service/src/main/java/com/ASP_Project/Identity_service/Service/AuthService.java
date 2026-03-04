package com.ASP_Project.Identity_service.Service;

import com.ASP_Project.Identity_service.DTO.MaintenanceStaffDTO;
import com.ASP_Project.Identity_service.DTO.PasswordDTO;
import com.ASP_Project.Identity_service.Entity.UserCredientials;
import com.ASP_Project.Identity_service.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.configurers.PasswordManagementConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;


    public String saveUser(UserCredientials userCredientials) {
        if (userCredientials.getRole() == null || userCredientials.getRole().trim().isEmpty()) {
            return "Error: Role must be selected and cannot be null or empty.";
        }
        userCredientials.setPassword(passwordEncoder.encode(userCredientials.getPassword()));
        userRepo.save(userCredientials);
        return "User saved successfully";
    }

    public String generateToken(String username, String password) {
        UserCredientials user = userRepo.findAll().stream()
                .filter(u -> u.getUserName().equals(username))
                .findFirst()
                .orElse(null);
        if (user == null) {
            return "User not found";
        }
        if (passwordEncoder.matches(password, user.getPassword())) {
            // Pass the user's role to the JWT
            return jwtService.generateToken(username, user.getRole(),user.getId(), user.getUserName(), user.getEmail());
        } else {
            return "Invalid password";
        }
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    // Return only id, name, email for maintenance staff by mapping to a DTO
    public List<MaintenanceStaffDTO> getMaintenanceStaffList() {
        return userRepo.findAllByRole("Maintenance").stream()
                .map(u -> new MaintenanceStaffDTO(u.getId(), u.getUserName(), u.getEmail()))
                .collect(Collectors.toList());
    }

    public boolean checkUser(String email) {

        return userRepo.existsByEmail(email);
    }

    public String savePassword(PasswordDTO password) {
        UserCredientials userCredientials = userRepo.findAll().stream()
                .filter(u->u.getEmail().equals(password.email()))
                .findFirst()
                .orElse(null);
        if (userCredientials == null) {
            return "User not found";
        }
        userCredientials.setPassword(passwordEncoder.encode(password.password()));
        String decpassword= userCredientials.getPassword();

        userRepo.save(userCredientials);
        return "Password saved successfully";

    }

    public String getEmailByName(String name) {
      String email= userRepo.findAll().stream()
                .filter(u -> u.getUserName().equalsIgnoreCase(name))
                .map(UserCredientials::getEmail)
                .findFirst()
                .orElse(null);
      if(email == null) {
          return "User not found";
      }
      return email;

    }
}
