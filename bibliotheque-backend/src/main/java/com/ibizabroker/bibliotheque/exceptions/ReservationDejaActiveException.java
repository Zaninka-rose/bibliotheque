package com.ibizabroker.bibliotheque.exceptions;

public class ReservationDejaActiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservationDejaActiveException() {
        super("RG-02 : une réservation active existe déjà pour cet adhérent sur ce livre");
    }
}
