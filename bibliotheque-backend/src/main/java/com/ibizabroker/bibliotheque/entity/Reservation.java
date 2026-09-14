package com.ibizabroker.bibliotheque.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "Reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "livre_id", nullable = false)
    private Books livre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "adherent_id", nullable = false)
    private Users adherent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutReservation statut;

    public Reservation() {
    }

    public Reservation(Books livre, Users adherent, StatutReservation statut) {
        this.livre = livre;
        this.adherent = adherent;
        this.statut = statut;
    }

    @PrePersist
    protected void onCreation() {
        this.dateReservation = LocalDateTime.now();
        this.dateExpiration = this.dateReservation.plusDays(7);
    }

    // ---- Getters / Setters ----

    public Long getId() {
        return id;
    }

    public Books getLivre() {
        return livre;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLivre(Books livre) {
        this.livre = livre;
    }

    public Users getAdherent() {
        return adherent;
    }

    public void setAdherent(Users adherent) {
        this.adherent = adherent;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public void setStatut(StatutReservation statut) {
        this.statut = statut;
    }

    // ---- equals / hashCode (sur la clé métier) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", livre=" + (livre != null ? livre.getBookId() : null) +
                ", adherent=" + (adherent != null ? adherent.getUserId() : null) +
                ", dateReservation=" + dateReservation +
                ", dateExpiration=" + dateExpiration +
                ", statut=" + statut +
                '}';
    }
}
