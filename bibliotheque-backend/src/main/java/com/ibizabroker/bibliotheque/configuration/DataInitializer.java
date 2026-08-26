package com.ibizabroker.bibliotheque.configuration;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usersRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setRoleName("Admin");

            Users admin = new Users();
            admin.setUsername("admin");
            admin.setName("Administrateur");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Set.of(adminRole));

            usersRepository.save(admin);
            System.out.println("=== Admin user created: admin / admin123 ===");
        }
    }
}
