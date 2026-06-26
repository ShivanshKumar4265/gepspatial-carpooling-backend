package com.carPooling.backend.dto.response;


import com.carPooling.backend.enums.OccurrenceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lightweight summary of the return leg.
 * Driver sees: "Return ride (Noida → CP at 18:00) also created."
 * For full return ride details, call GET /rides/templates/{templateId}.
 */
@Data
public class ReturnRideSummary {

    private Long templateId;
    private Long occurrenceId;

    private LocalDate rideDate;
    private LocalTime departureTime;

    private String pickupLocation;      // = original destination
    private String destinationLocation; // = original pickup

    private Integer availableSeats;
    private BigDecimal pricePerSeat;

    private OccurrenceStatus status;
}
