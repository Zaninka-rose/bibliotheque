package com.ibizabroker.bibliotheque.entity;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;

import tools.jackson.databind.annotation.JsonSerialize;

@Data
@Entity @EntityListeners(AuditingEntityListener.class)
@Table(name = "Borrow")
public class Borrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer borrowId;
    Integer bookId;
    Integer userId;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date issueDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date returnDate;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using=JsonDataSerializer.class)
    Date dueDate;

}
