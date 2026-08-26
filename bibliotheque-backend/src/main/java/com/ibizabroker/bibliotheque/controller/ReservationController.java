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

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('Admin')")
@Tag(name = "Réservations", description = "Gestion des réservations de livres")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // ------------------------------------------------------------------ 1
    @PostMapping
    @Operation(summary = "Créer une réservation", description = "Crée une nouvelle réservation pour un livre indisponible. Le statut initial est EN_ATTENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Champ requis manquant (livreId / adherentId)"),
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
    @Operation(summary = "Lister les réservations", description = "Retourne la liste des réservations, avec filtres optionnels par statut et/ou adhérent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    })
    public ResponseEntity<List<ReservationResponseDTO>> listerReservations(
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Long adherentId) {
        List<ReservationResponseDTO> list = reservationService.listerReservations(statut, adherentId);
        return ResponseEntity.ok(list);
    }

    // ------------------------------------------------------------------ 3
    @GetMapping("/{id}")
    @Operation(summary = "Consulter une réservation", description = "Retourne les détails d'une réservation par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<ReservationResponseDTO> consulterReservation(@PathVariable Long id) {
        ReservationResponseDTO dto = reservationService.consulterReservation(id);
        return ResponseEntity.ok(dto);
    }

    // ------------------------------------------------------------------ 4
    @PatchMapping("/{id}/annuler")
    @Operation(summary = "Annuler une réservation", description = "Passe le statut à ANNULEE. Seules les réservations EN_ATTENTE ou DISPONIBLE sont annulables (RG-05/RG-06).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "RG-05/RG-06 : statut actuel ne permet pas l'annulation")
    })
    public ResponseEntity<ReservationResponseDTO> annulerReservation(@PathVariable Long id) {
        ReservationResponseDTO dto = reservationService.annulerReservation(id);
        return ResponseEntity.ok(dto);
    }

    // ------------------------------------------------------------------ 5
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une réservation", description = "Suppression physique de la réservation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<Void> supprimerReservation(@PathVariable Long id) {
        reservationService.supprimerReservation(id);
        return ResponseEntity.noContent().build();
    }
}
