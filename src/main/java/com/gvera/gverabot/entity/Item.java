package com.gvera.gverabot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
public class Item {
    @Id
    private UUID id;

    private String name;

    private Integer quantity;

    private Double price;

    private Double discount;

    @ManyToOne
    private Store store;

    @CreationTimestamp
    private LocalDate date;

    @Override
    public String toString() {
        String str = name + ", " + price + ", in stock: " + quantity;
        if (discount != null)
            str += ", price with discount: " + price * (discount/100) + ".\n";
        return str;
    }
}
