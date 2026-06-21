package com.carPooling.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "vehicles")
public class Vehicles extends  BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    // many vehicle in this table can own by may user/owner
    @JoinColumn(name = "user_id", nullable = false) // this work for database level, it will create a foreign key column named user_id in vehicles table
    @ManyToOne
    @NotNull // this works at java application layer for validataion, it will check if user is null or not before saving the vehicle entity
    private User user;

    @Column(name = "vehicle_number", unique = true, nullable = false)
    private  String vehicleNumber;

    @Column(name = "vehicle_type", nullable = false)
    private  String vehicleType;

    @Column(name =  "vehicle_model", nullable = false)
    private  String vehicleModel;

    @Column(name = "vehicle_color")
    private  String color;

    // here not blank won't  work because of primitive data type int, so we can use min value to check if it is less than 1
    @Column(name = "total_seats", nullable = false)
    private  int totalSeats;


    /**
     *   vehicles {
     *     bigint id PK
     *     bigint user_id FK
     *     varchar vehicle_number UK
     *     varchar vehicle_type
     *     varchar vehicle_model
     *     varchar color
     *     int total_seats
     *     boolean is_verified
     *     datetime created_at
     *     datetime updated_at
     *     datetime deleted_at
     *   }
     */

}
