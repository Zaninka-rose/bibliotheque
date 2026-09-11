package com.ibizabroker.bibliotheque.dto;

import jakarta.validation.constraints.NotNull;

public class ReservationCreateDTO {

    @NotNull(message = "livreId est requis")
    private Long livreId;

    /**
     * RS-04 : ignoré pour un ADHERENT (l'identité vient du token).
     * Requis uniquement pour un BIBLIOTHECAIRE (Admin) qui réserve
     * au nom d'un adhérent.
     */
    private Long adherentId;

    public ReservationCreateDTO() {
    }

    public ReservationCreateDTO(Long livreId, Long adherentId) {
        this.livreId = livreId;
        this.adherentId = adherentId;
    }

    public Long getLivreId() {
        return livreId;
    }

    public void setLivreId(Long livreId) {
        this.livreId = livreId;
    }

    public Long getAdherentId() {
        return adherentId;
    }

    public void setAdherentId(Long adherentId) {
        this.adherentId = adherentId;
    }
}
