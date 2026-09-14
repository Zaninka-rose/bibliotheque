package com.ibizabroker.bibliotheque.dto;

import jakarta.validation.constraints.NotNull;

public class ReservationCreateDTO {

    @NotNull(message = "livreId est requis")
    private Long livreId;

    @NotNull(message = "adherentId est requis")
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
