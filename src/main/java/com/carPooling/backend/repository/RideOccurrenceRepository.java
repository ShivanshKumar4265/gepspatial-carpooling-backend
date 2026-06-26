package com.carPooling.backend.repository;

import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.RideOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RideOccurrenceRepository extends JpaRepository<RideOccurrence, Long> {
    @Query("SELECT MAX(o.rideDate) FROM RideOccurrence o WHERE o.template.id = :templateId")
    Optional<LocalDate> findMaxDateByTemplateId(@Param("templateId") Long templateId);

    @Modifying
    @Query("""
        UPDATE RideOccurrence o
        SET o.status = 'CANCELLED'
        WHERE o.template.id = :templateId
          AND o.rideDate >= :fromDate
          AND o.status = 'SCHEDULED'
    """)
    void cancelFutureOccurrences(@Param("templateId") Long templateId,
                                 @Param("fromDate") LocalDate fromDate);

    // Used by the ride search feature
    @Query("""
        SELECT o FROM RideOccurrence o
        JOIN FETCH o.template t
        WHERE o.rideDate = :date
          AND o.status = 'SCHEDULED'
          AND t.templateStatus = 'ACTIVE'
    """)
    List<RideOccurrence> findScheduledByDate(@Param("date") LocalDate date);
}
