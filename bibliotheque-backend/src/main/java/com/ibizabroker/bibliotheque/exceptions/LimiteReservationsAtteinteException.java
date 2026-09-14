package com.ibizabroker.bibliotheque.exceptions;

public class LimiteReservationsAtteinteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LimiteReservationsAtteinteException() {
        super("RG-03 : l'adhérent a atteint la limite de 3 réservations actives");
    }
}
