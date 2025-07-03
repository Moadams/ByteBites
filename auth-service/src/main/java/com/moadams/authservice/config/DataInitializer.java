package com.moadams.authservice.config;

import com.moadams.authservice.enums.RoleName;
import com.moadams.authservice.model.Role;
import com.moadams.authservice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            Arrays.stream(RoleName.values()).forEach(roleName -> {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = Role.builder().name(roleName).build();
                    roleRepository.save(role);
                    System.out.println("Created role: " + roleName);
                }
            });
        };
    }
}