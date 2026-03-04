package com.ASP_Project.Identity_service.Repository;

import com.ASP_Project.Identity_service.Entity.UserCredientials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepo extends JpaRepository<UserCredientials, Long> {

    List<UserCredientials> findAllByRole(String role);

    boolean existsByEmail(String email);
}
