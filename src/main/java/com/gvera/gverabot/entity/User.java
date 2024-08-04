package com.gvera.gverabot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@Entity(name = "users")
public class User {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserState state;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "owner")
    private List<Store> stores;

    public User(Long id, UserState state) {
        this.id = id;
        this.state = state;
    }
}
