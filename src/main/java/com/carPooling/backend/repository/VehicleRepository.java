package com.carPooling.backend.repository;

import com.carPooling.backend.entity.Vehicles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface VehicleRepository extends JpaRepository<Vehicles, Long> {
    boolean existsByVehicleNumber(String vehicleNumber);
    List<Vehicles> findAll();

}
