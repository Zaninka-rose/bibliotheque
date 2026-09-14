package com.ibizabroker.bibliotheque.exceptions;

public class ReservationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(Long id) {
        super("Réservation " + id + " introuvable");
    }
}
