package com.gvera.gverabot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
public class Store {
    @Id
    private UUID id;

    private String name;

    private String contactDetails;

    @ManyToOne(fetch = FetchType.EAGER)
    @ToString.Exclude
    private User owner;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST, mappedBy = "store")
    private List<Item> items = new ArrayList<>();

    public Store(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
}
