package com.pctb.webapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    String name;
    String description;

//    @Override
//    public String toString() {
//        return "Role{" +
//                "name='" + name + '\'' +
//                ", description='" + description + '\'' +
//                '}';
//    }
}