package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "user")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Tạo id = 1 dãy ngẫu nhiên
    String id;
    @Column(unique = true,nullable = false,length = 50)
    String username;
    @Column(nullable = false)
    String password;
    @Column(nullable = false, length = 100)
    String fullname;

    @Column(unique = true, nullable = false, length = 100)
    String email;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    Set<Role> roles;


}
