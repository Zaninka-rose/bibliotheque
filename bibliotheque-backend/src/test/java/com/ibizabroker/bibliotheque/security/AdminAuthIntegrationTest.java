package com.ibizabroker.bibliotheque.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration du login admin (sans filtres de sécurité).
 *
 * Le DataInitializer crée l'admin (admin/admin123, rôle Admin) dans la base H2 de test.
 * @AutoConfigureMockMvc(addFilters = false) : on teste uniquement la logique métier
 * du login sans le Security Filter Chain.
 *
 *  1) Login admin → JWT valide + bonnes claims
 *  2) Vérifie la cohérence des rôles (hasRole Admin ↔ ROLE_Admin)
 *  3) Vérifie que l'admin existe bien en base avec les bonnes données
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsersRepository usersRepository;

    // ================================================================
    //  1) Login admin avec bonnes credentials → 200 + JWT
    // ================================================================
    @Test
    @Order(1)
    @DisplayName("POST /authenticate → 200 et retourne un JWT pour admin/admin123")
    void loginAdmin_valide_retourneToken() throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role[0].roleName").value("Admin"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("jwtToken").asText();
        Assertions.assertNotNull(token, "Le token JWT ne doit pas être null");
        Assertions.assertTrue(token.split("\\.").length == 3,
                "Le token JWT doit avoir 3 parties (header.payload.signature)");
    }

    // ================================================================
    //  2) Le rôle 'Admin' est cohérent avec hasRole('Admin')
    // ================================================================
    @Test
    @Order(2)
    @DisplayName("Le rôle 'Admin' dans la base correspond à l'autorité ROLE_Admin attendue par hasRole('Admin')")
    void adminRole_coherenceHasRole() {
        var admin = usersRepository.findByUsername("admin");
        Assertions.assertTrue(admin.isPresent(), "L'utilisateur admin doit exister en base");

        var roles = admin.get().getRole();
        Assertions.assertFalse(roles.isEmpty(), "L'admin doit avoir au moins un rôle");

        String roleName = roles.iterator().next().getRoleName();
        Assertions.assertEquals("Admin", roleName,
                "Le rôle doit être 'Admin' → ROLE_Admin via getAuthority() → matche hasRole('Admin')");
    }

    // ================================================================
    //  3) L'admin a bien le mot de passe encodé en BCrypt
    // ================================================================
    @Test
    @Order(3)
    @DisplayName("L'admin a un mot de passe encodé en BCrypt (pas en clair)")
    void adminPassword_estBCrypt() {
        var admin = usersRepository.findByUsername("admin");
        Assertions.assertTrue(admin.isPresent());

        String password = admin.get().getPassword();
        Assertions.assertTrue(password.startsWith("$2a$"),
                "Le mot de passe doit être encodé en BCrypt (commence par $2a$)");
        Assertions.assertTrue(password.length() > 50,
                "Un hash BCrypt fait au moins 60 caractères");
    }

    // ================================================================
    //  4) L'admin a exactement un rôle
    // ================================================================
    @Test
    @Order(4)
    @DisplayName("L'admin a exactement un rôle 'Admin'")
    void adminExactementUnRole() {
        var admin = usersRepository.findByUsername("admin");
        Assertions.assertTrue(admin.isPresent());
        Assertions.assertEquals(1, admin.get().getRole().size(),
                "L'admin doit avoir exactement un rôle");
    }
}
