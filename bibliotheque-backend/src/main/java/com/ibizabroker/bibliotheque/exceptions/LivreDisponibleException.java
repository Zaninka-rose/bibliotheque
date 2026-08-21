package com.ibizabroker.bibliotheque.exceptions;

public class LivreDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LivreDisponibleException(String bookName) {
        super("RG-01 : le livre \"" + bookName + "\" est disponible, la réservation est impossible");
    }
}
