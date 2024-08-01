package com.gvera.gverabot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemAudit {
    @Id
    private UUID id;
    @ManyToOne
    private Item item;
    private LocalDate updatedAt;
    private Integer sold;
}
