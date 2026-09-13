package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.RoleRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * Inscription publique : crée un compte ADHERENT.
 *
 * Sécurité :
 *  - endpoint public (voir WebSecurityConfiguration) ;
 *  - le rôle est FORCÉ côté serveur à « Adherent » : le client ne
 *    peut pas s'auto-attribuer le rôle Admin ;
 *  - le mot de passe est haché avant persistance.
 */
@RestController
public class RegisterController {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegisterController(UsersRepository usersRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String name = payload.get("name");
        String password = payload.get("password");

        if (username == null || username.isBlank()
                || name == null || name.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Tous les champs (name, username, password) sont obligatoires."));
        }

        if (usersRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Ce nom d'utilisateur est déjà pris."));
        }

        Role adherentRole = roleRepository.findByRoleName("Adherent")
                .orElseThrow(() -> new IllegalStateException("Rôle 'Adherent' introuvable. Vérifiez data.sql."));

        Users user = new Users();
        user.setUsername(username);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Set.of(adherentRole));

        usersRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Compte créé. Vous pouvez vous connecter."));
    }
}
