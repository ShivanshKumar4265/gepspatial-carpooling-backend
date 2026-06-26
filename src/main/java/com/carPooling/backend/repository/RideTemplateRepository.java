package com.carPooling.backend.repository;

import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.RideTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RideTemplateRepository extends JpaRepository<RideTemplate, Long> {
    @Query("SELECT t FROM RideTemplate t WHERE t.templateStatus = 'ACTIVE' AND t.recurring = true")
    List<RideTemplate> findAllActiveRecurring();
}
