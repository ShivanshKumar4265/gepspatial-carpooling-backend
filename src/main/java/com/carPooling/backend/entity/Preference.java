package com.carPooling.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "preferences", uniqueConstraints = {@UniqueConstraint(columnNames = "preferenceName")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Preference extends  BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @Column(unique = true, nullable = false)
    private String preferenceName;

    private boolean isActive = true;


    @ManyToMany(mappedBy = "preferences")
    private Set<RideEntity> rides = new HashSet<>();
}
