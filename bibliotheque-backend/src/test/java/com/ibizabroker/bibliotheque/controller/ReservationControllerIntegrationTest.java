package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.exceptions.*;
import com.ibizabroker.bibliotheque.service.ReservationService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests du endpoint REST /api/reservations — couche contrôleur.
 *
 * Principe : MockMvc standalone, {@link ReservationService} entièrement
 * mocké (aucune base de données, aucun contexte Spring). On vérifie ici
 * le CONTRAT HTTP de chaque endpoint : codes de statut, corps JSON,
 * routage des paramètres, mapping des exceptions métier vers les codes
 * HTTP (400/403/404/409) via {@link GlobalExceptionHandler}.
 *
 * La sécurité (401/403 par rôle) est couverte à part dans
 * ReservationSecurityIntegrationTest / ReservationEndpointSecurityIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerIntegrationTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    private ReservationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        responseDTO = new ReservationResponseDTO(
                1L, 1L, "Le Petit Prince", 10L, "Zaninka Rose",
                LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 8, 10, 0),
                StatutReservation.EN_ATTENTE);
    }

    // ================================================================ POST /api/reservations
    @Nested
    @DisplayName("POST /api/reservations")
    class Creer {

        @Test
        @DisplayName("400 si livreId manquant (@Valid rejeté avant le service)")
        void creerReservation_livreIdManquant_400() throws Exception {
            ReservationCreateDTO dto = new ReservationCreateDTO(null, 10L);

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            // Le contrôleur ne doit jamais appeler le service avec un DTO invalide
            verify(reservationService, never()).creerReservation(any());
        }

        @Test
        @DisplayName("400 avec message JSON si le service lève ValidationException (adherentId Admin manquant)")
        void creerReservation_validationService_400() throws Exception {
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenThrow(new ValidationException("adherentId est requis"));

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReservationCreateDTO(1L, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("adherentId est requis"));
        }

        @Test
        @DisplayName("201 sans adherentId (identité du token — RS-04)")
        void creerReservation_sansAdherentId_201() throws Exception {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.adherentId").value(10));
        }

        @Test
        @DisplayName("201 création réussie : corps JSON complet (id, livre, adhérent, statut, dates)")
        void creerReservation_succes_201() throws Exception {
            ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.livreId").value(1))
                    .andExpect(jsonPath("$.livreTitre").value("Le Petit Prince"))
                    .andExpect(jsonPath("$.adherentId").value(10))
                    .andExpect(jsonPath("$.adherentNom").value("Zaninka Rose"))
                    .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                    .andExpect(jsonPath("$.dateReservation").exists())
                    .andExpect(jsonPath("$.dateExpiration").exists());
        }

        @Test
        @DisplayName("201 : le DTO reçu du client est transmis intact au service")
        void creerReservation_transmetLeDtoAuService() throws Exception {
            ReservationCreateDTO dto = new ReservationCreateDTO(7L, 42L);
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<ReservationCreateDTO> captor =
                    ArgumentCaptor.forClass(ReservationCreateDTO.class);
            verify(reservationService).creerReservation(captor.capture());
            assertThat(captor.getValue().getLivreId()).isEqualTo(7L);
            assertThat(captor.getValue().getAdherentId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("404 si le livre ou l'adhérent visé est introuvable")
        void creerReservation_livreIntrouvable_404() throws Exception {
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenThrow(new NotFoundException("Livre avec id 999 introuvable"));

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReservationCreateDTO(999L, null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("999")));
        }

        @Test
        @DisplayName("409 RG-01 : livre disponible → réservation refusée")
        void creerReservation_livreDisponible_409() throws Exception {
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenThrow(new LivreDisponibleException("L'Étranger"));

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReservationCreateDTO(2L, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RG-01")));
        }

        @Test
        @DisplayName("409 RG-02 : doublon de réservation active sur le même livre")
        void creerReservation_doublon_409() throws Exception {
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenThrow(new ReservationDejaActiveException());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReservationCreateDTO(1L, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RG-02")));
        }

        @Test
        @DisplayName("409 RG-03 : limite de 3 réservations actives atteinte")
        void creerReservation_limite_409() throws Exception {
            when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                    .thenThrow(new LimiteReservationsAtteinteException());

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReservationCreateDTO(1L, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RG-03")));
        }
    }

    // ================================================================ GET /api/reservations
    @Nested
    @DisplayName("GET /api/reservations")
    class Lister {

        @Test
        @DisplayName("200 retourne la liste complète")
        void listerReservations_200() throws Exception {
            when(reservationService.listerReservations(isNull(), isNull()))
                    .thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].livreTitre").value("Le Petit Prince"));
        }

        @Test
        @DisplayName("200 liste vide quand aucune réservation")
        void listerReservations_vide_200() throws Exception {
            when(reservationService.listerReservations(isNull(), isNull()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("?statut=EN_ATTENTE est routé vers le service")
        void listerReservations_filtreStatut() throws Exception {
            when(reservationService.listerReservations(eq(StatutReservation.EN_ATTENTE), isNull()))
                    .thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/reservations").param("statut", "EN_ATTENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
        }

        @Test
        @DisplayName("?adherentId= est routé vers le service (usage Admin ; ignoré pour un ADHERENT)")
        void listerReservations_filtreAdherent() throws Exception {
            when(reservationService.listerReservations(isNull(), eq(99L)))
                    .thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/reservations").param("adherentId", "99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(reservationService).listerReservations(isNull(), eq(99L));
        }

        @Test
        @DisplayName("400 si statut inconnu (enum invalide)")
        void listerReservations_statutInconnu_400() throws Exception {
            mockMvc.perform(get("/api/reservations").param("statut", "PAS_UN_STATUT"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================ GET /api/reservations/{id}
    @Nested
    @DisplayName("GET /api/reservations/{id}")
    class Consulter {

        @Test
        @DisplayName("200 consultation réussie")
        void consulterReservation_200() throws Exception {
            when(reservationService.consulterReservation(1L)).thenReturn(responseDTO);

            mockMvc.perform(get("/api/reservations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.livreTitre").value("Le Petit Prince"));
        }

        @Test
        @DisplayName("404 si la réservation n'existe pas")
        void consulterReservation_404() throws Exception {
            when(reservationService.consulterReservation(999L))
                    .thenThrow(new ReservationNotFoundException(999L));

            mockMvc.perform(get("/api/reservations/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 si la réservation appartient à un autre adhérent (RS-03)")
        void consulterReservation_autreAdherent_403() throws Exception {
            when(reservationService.consulterReservation(5L))
                    .thenThrow(new AccesRefuseException(
                            "Vous n'avez pas accès à cette réservation (RS-03)"));

            mockMvc.perform(get("/api/reservations/5"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RS-03")));
        }
    }

    // ================================================================ PATCH /api/reservations/{id}/annuler
    @Nested
    @DisplayName("PATCH /api/reservations/{id}/annuler")
    class Annuler {

        @Test
        @DisplayName("200 annulation réussie, statut passe à ANNULEE")
        void annulerReservation_200() throws Exception {
            responseDTO.setStatut(StatutReservation.ANNULEE);
            when(reservationService.annulerReservation(1L)).thenReturn(responseDTO);

            mockMvc.perform(patch("/api/reservations/1/annuler"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("ANNULEE"));
        }

        @Test
        @DisplayName("409 si statut terminal (RG-05/RG-06)")
        void annulerReservation_statutTerminal_409() throws Exception {
            when(reservationService.annulerReservation(1L))
                    .thenThrow(new ReservationNonAnnulableException(StatutReservation.HONOREE));

            mockMvc.perform(patch("/api/reservations/1/annuler"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RG-05")));
        }

        @Test
        @DisplayName("404 si la réservation est introuvable")
        void annulerReservation_404() throws Exception {
            when(reservationService.annulerReservation(999L))
                    .thenThrow(new ReservationNotFoundException(999L));

            mockMvc.perform(patch("/api/reservations/999/annuler"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 si la réservation appartient à un autre adhérent (RS-03)")
        void annulerReservation_autreAdherent_403() throws Exception {
            when(reservationService.annulerReservation(5L))
                    .thenThrow(new AccesRefuseException(
                            "Vous n'avez pas accès à cette réservation (RS-03)"));

            mockMvc.perform(patch("/api/reservations/5/annuler"))
                    .andExpect(status().isForbidden());
        }
    }

    // ================================================================ DELETE /api/reservations/{id}
    @Nested
    @DisplayName("DELETE /api/reservations/{id}")
    class Supprimer {

        @Test
        @DisplayName("204 suppression réussie, corps vide")
        void supprimerReservation_204() throws Exception {
            doNothing().when(reservationService).supprimerReservation(1L);

            mockMvc.perform(delete("/api/reservations/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("404 si la réservation est introuvable")
        void supprimerReservation_404() throws Exception {
            doThrow(new ReservationNotFoundException(999L))
                    .when(reservationService).supprimerReservation(999L);

            mockMvc.perform(delete("/api/reservations/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 si un ADHERENT tente la suppression (RS-02, défense en profondeur du service)")
        void supprimerReservation_adherent_403() throws Exception {
            doThrow(new AccesRefuseException("Suppression réservée au BIBLIOTHECAIRE (RS-02)"))
                    .when(reservationService).supprimerReservation(1L);

            mockMvc.perform(delete("/api/reservations/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("RS-02")));
        }
    }
}
