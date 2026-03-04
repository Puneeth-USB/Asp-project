package com.ASP_Project.Identity_service.Controller;

import com.ASP_Project.Identity_service.DTO.MaintenanceStaffDTO;
import com.ASP_Project.Identity_service.DTO.PasswordDTO;
import com.ASP_Project.Identity_service.Entity.UserCredientials;
import com.ASP_Project.Identity_service.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;


    @PostMapping("/register")
    public String addUser(@RequestBody UserCredientials userCredientials) {
        return authService.saveUser(userCredientials);
    }

    @PostMapping("/token")
    public String generateToken(@RequestBody UserCredientials userCredientials) {
        // Check if username and password are matching and generate token with role
        return authService.generateToken(userCredientials.getUserName(), userCredientials.getPassword());
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        authService.validateToken(token);
        return"Token is valid";
    }


    @GetMapping("/maintenance-staff")
    public List<MaintenanceStaffDTO> getMaintenanceStaff() {
        return authService.getMaintenanceStaffList();

    }

    @GetMapping("/byEmail")
    public boolean checkEmail(@RequestParam("email") String email)
    {
        return authService.checkUser(email);
    }

    @PostMapping("/password")
    public String updatePassword(@RequestBody PasswordDTO passwordDTO)
    {
        return authService.savePassword(passwordDTO);
    }

    @GetMapping("/email/{name}")
    public String emailByName(@PathVariable String name){
        return authService.getEmailByName(name);
    }

}
