package com.pctb.webapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "actor_id")
    String actorId;

    String actor;

    @Column(name = "actor_type")
    String actorType;

    @Column(name = "action_group")
    String actionGroup;

    String action;
    String targetId;

    @Column(columnDefinition = "TEXT")
    String details;

    LocalDateTime createdAt;
}
