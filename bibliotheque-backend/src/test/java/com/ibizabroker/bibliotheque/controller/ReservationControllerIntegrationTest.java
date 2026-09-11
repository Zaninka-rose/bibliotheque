package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.exceptions.GlobalExceptionHandler;
import com.ibizabroker.bibliotheque.exceptions.ReservationNotFoundException;
import com.ibizabroker.bibliotheque.exceptions.ReservationNonAnnulableException;
import com.ibizabroker.bibliotheque.service.ReservationService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour ReservationController.
 * Utilise MockMvc en mode standalone (sans contexte Spring complet).
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

        responseDTO = new ReservationResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setLivreId(1L);
        responseDTO.setLivreTitre("Le Petit Prince");
        responseDTO.setAdherentId(10L);
        responseDTO.setAdherentNom("Zaninka Rose");
        responseDTO.setStatut(StatutReservation.EN_ATTENTE);
    }

    // ================================================================ POST /api/reservations
    @Test
    @DisplayName("POST /api/reservations → 400 si livreId manquant")
    void creerReservation_livreIdManquant_400() throws Exception {
        ReservationCreateDTO dto = new ReservationCreateDTO(null, 10L);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // RS-04 : adherentId n'est plus obligatoire dans le DTO — pour un ADHERENT
    // l'identité vient du token. (Le 400 pour adherentId manquant ne concerne
    // que l'Admin et est testé dans ReservationServiceImplTest.)
    @Test
    @DisplayName("POST /api/reservations → 201 sans adherentId (identité du token)")
    void creerReservation_sansAdherentId_201() throws Exception {
        ReservationCreateDTO dto = new ReservationCreateDTO(1L, null);
        when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/reservations → 201 création réussie")
    void creerReservation_succes_201() throws Exception {
        ReservationCreateDTO dto = new ReservationCreateDTO(1L, 10L);
        when(reservationService.creerReservation(any(ReservationCreateDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.livreTitre").value("Le Petit Prince"))
                .andExpect(jsonPath("$.adherentNom").value("Zaninka Rose"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    // ================================================================ GET /api/reservations
    @Test
    @DisplayName("GET /api/reservations → 200 retourne la liste")
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
    @DisplayName("GET /api/reservations?statut=EN_ATTENTE → filtre par statut")
    void listerReservations_filtreStatut() throws Exception {
        when(reservationService.listerReservations(eq(StatutReservation.EN_ATTENTE), isNull()))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/reservations").param("statut", "EN_ATTENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }

    // ================================================================ GET /api/reservations/{id}
    @Test
    @DisplayName("GET /api/reservations/{id} → 200 consultation réussie")
    void consulterReservation_200() throws Exception {
        when(reservationService.consulterReservation(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livreTitre").value("Le Petit Prince"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} → 404 si introuvable")
    void consulterReservation_404() throws Exception {
        when(reservationService.consulterReservation(999L))
                .thenThrow(new ReservationNotFoundException(999L));

        mockMvc.perform(get("/api/reservations/999"))
                .andExpect(status().isNotFound());
    }

    // ================================================================ PATCH /api/reservations/{id}/annuler
    @Test
    @DisplayName("PATCH /api/reservations/{id}/annuler → 200 annulation réussie")
    void annulerReservation_200() throws Exception {
        responseDTO.setStatut(StatutReservation.ANNULEE);
        when(reservationService.annulerReservation(1L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/reservations/1/annuler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    @Test
    @DisplayName("PATCH /api/reservations/{id}/annuler → 409 si statut terminal")
    void annulerReservation_statutTerminal_409() throws Exception {
        when(reservationService.annulerReservation(1L))
                .thenThrow(new ReservationNonAnnulableException(StatutReservation.HONOREE));

        mockMvc.perform(patch("/api/reservations/1/annuler"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("RG-05")));
    }

    // ================================================================ DELETE /api/reservations/{id}
    @Test
    @DisplayName("DELETE /api/reservations/{id} → 204 suppression réussie")
    void supprimerReservation_204() throws Exception {
        doNothing().when(reservationService).supprimerReservation(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/reservations/{id} → 404 si introuvable")
    void supprimerReservation_404() throws Exception {
        doThrow(new ReservationNotFoundException(999L))
                .when(reservationService).supprimerReservation(999L);

        mockMvc.perform(delete("/api/reservations/999"))
                .andExpect(status().isNotFound());
    }
}
