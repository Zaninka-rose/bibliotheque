package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.StatutReservation;

import java.time.LocalDateTime;

public class ReservationResponseDTO {

    private Long id;
    private Long livreId;
    private String livreTitre;
    private Long adherentId;
    private String adherentNom;
    private LocalDateTime dateReservation;
    private LocalDateTime dateExpiration;
    private StatutReservation statut;

    public ReservationResponseDTO() {
    }

    public ReservationResponseDTO(Long id, Long livreId, String livreTitre,
                                  Long adherentId, String adherentNom,
                                  LocalDateTime dateReservation,
                                  LocalDateTime dateExpiration,
                                  StatutReservation statut) {
        this.id = id;
        this.livreId = livreId;
        this.livreTitre = livreTitre;
        this.adherentId = adherentId;
        this.adherentNom = adherentNom;
        this.dateReservation = dateReservation;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLivreId() {
        return livreId;
    }

    public void setLivreId(Long livreId) {
        this.livreId = livreId;
    }

    public String getLivreTitre() {
        return livreTitre;
    }

    public void setLivreTitre(String livreTitre) {
        this.livreTitre = livreTitre;
    }

    public Long getAdherentId() {
        return adherentId;
    }

    public void setAdherentId(Long adherentId) {
        this.adherentId = adherentId;
    }

    public String getAdherentNom() {
        return adherentNom;
    }

    public void setAdherentNom(String adherentNom) {
        this.adherentNom = adherentNom;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public void setStatut(StatutReservation statut) {
        this.statut = statut;
    }
}
