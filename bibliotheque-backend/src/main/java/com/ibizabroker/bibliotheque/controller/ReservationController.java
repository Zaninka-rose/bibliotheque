package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationCreateDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Matrice de permissions (rôles DB : Admin = BIBLIOTHECAIRE, Adherent = ADHERENT) :
 *
 * | Endpoint                          | Anonyme | ADHERENT                  | BIBLIOTHECAIRE |
 * |-----------------------------------|---------|---------------------------|----------------|
 * | POST /api/reservations            | 401     | OUI (lui-même, RS-04)     | OUI (tous)     |
 * | GET /api/reservations             | 401     | OUI (les siennes, RS-05)  | OUI (toutes)   |
 * | GET /api/reservations/{id}        | 401     | OUI (si à lui, RS-03)     | OUI (toutes)   |
 * | PATCH /api/reservations/{id}/annuler | 401  | OUI (si à lui, RS-03)     | OUI (toutes)   |
 * | DELETE /api/reservations/{id}     | 401     | 403 (RS-02)               | OUI            |
 */
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Réservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // ------------------------------------------------------------------ 1
    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','Adherent')")
    @Operation(summary = "Créer une réservation",
            description = "ADHERENT : réservé pour lui-même (RS-04, l'identité vient du token). "
                    + "BIBLIOTHECAIRE : pour n'importe quel adhérent (adherentId requis).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "livreId manquant / adherentId manquant (Admin)"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "RG-01 : livre disponible / RG-02 : réservation active déjà existante / RG-03 : limite atteinte")
    })
    public ResponseEntity<ReservationResponseDTO> creerReservation(
            @Valid @RequestBody ReservationCreateDTO dto) {
        ReservationResponseDTO created = reservationService.creerReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ------------------------------------------------------------------ 2
    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','Adherent')")
    @Operation(summary = "Lister les réservations",
            description = "ADHERENT : uniquement ses propres réservations (RS-05). "
                    + "BIBLIOTHECAIRE : toutes, avec filtres optionnels statut/adherentId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré")
    })
    public ResponseEntity<List<ReservationResponseDTO>> listerReservations(
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Long adherentId) {
        List<ReservationResponseDTO> list = reservationService.listerReservations(statut, adherentId);
        return ResponseEntity.ok(list);
    }

    // ------------------------------------------------------------------ 3
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin','Adherent')")
    @Operation(summary = "Consulter une réservation",
            description = "ADHERENT : uniquement si la réservation lui appartient (RS-03, sinon 403). "
                    + "BIBLIOTHECAIRE : toutes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent (RS-03)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponseDTO> consulterReservation(@PathVariable Long id) {
        ReservationResponseDTO dto = reservationService.consulterReservation(id);
        return ResponseEntity.ok(dto);
    }

    // ------------------------------------------------------------------ 4
    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('Admin','Adherent')")
    @Operation(summary = "Annuler une réservation",
            description = "ADHERENT : uniquement si la réservation lui appartient (RS-03, sinon 403). "
                    + "BIBLIOTHECAIRE : toutes. Seules les réservations EN_ATTENTE ou DISPONIBLE sont annulables (RG-05/RG-06).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent (RS-03)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "RG-05/RG-06 : statut actuel ne permet pas l'annulation")
    })
    public ResponseEntity<ReservationResponseDTO> annulerReservation(@PathVariable Long id) {
        ReservationResponseDTO dto = reservationService.annulerReservation(id);
        return ResponseEntity.ok(dto);
    }

    // ------------------------------------------------------------------ 5
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Supprimer une réservation",
            description = "Réservé au BIBLIOTHECAIRE (Admin). Un ADHERENT reçoit 403 (RS-02). Suppression physique.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Rôle ADHERENT (RS-02)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<Void> supprimerReservation(@PathVariable Long id) {
        reservationService.supprimerReservation(id);
        return ResponseEntity.noContent().build();
    }
}
