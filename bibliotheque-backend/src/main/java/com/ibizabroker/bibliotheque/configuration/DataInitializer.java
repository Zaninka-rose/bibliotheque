package com.ibizabroker.bibliotheque.configuration;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsersRepository usersRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Transaction unique : le rôle recherché via roleRepository doit
    // rester MANAGED pendant usersRepository.save(admin), sinon son
    // cascade ALL déclenche "Detached entity passed to persist: Role"
    // (chaque appel de repository ouvre sinon sa propre transaction).
    @Override
    @Transactional
    public void run(String... args) {
        if (usersRepository.findByUsername("admin").isEmpty()) {
            // Le rôle 'Admin' peut déjà exister (créé par data.sql sur un volume
            // neuf) : on le réutilise au lieu d'en insérer un doublon, sinon
            // l'insertion du compte admin échouerait.
            Role adminRole = roleRepository.findByRoleName("Admin")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setRoleName("Admin");
                        return roleRepository.save(r);
                    });

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
