// ─── CreateRideResponse.java ─────────────────────────────────────────────────
package com.carPooling.backend.dto.response;

import com.carPooling.backend.enums.OccurrenceStatus;
import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.TemplateStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response for POST /rides.
 *
 * templateId   → use to update/cancel the whole series
 * occurrenceId → use to book a seat on this specific date
 *
 * If isReturnRide=true, the returnRide field is populated with
 * a ReturnRideSummary. Otherwise it is null.
 */
@Data
public class CreateRideResponse {

    // IDs
    private Long templateId;
    private Long occurrenceId;

    // Timing
    private LocalDate rideDate;
    private LocalTime departureTime;

    // Seats & Price (effective values: override ?? template default)
    private Integer availableSeats;
    private BigDecimal pricePerSeat;

    // Recurrence
    private boolean repeatRide;
    private RepeatType repeatType;
    private LocalDate repeatUntil;

    // Status
    private OccurrenceStatus occurrenceStatus;
    private TemplateStatus templateStatus;

    // Metadata
    private LocalDateTime createdAt;
    private String message;

    // Pickup
    private String pickupLocation;
    private Double pickupLat;
    private Double pickupLng;
    private String pickupLandmark;
    private String pickupInstructions;
    private Float flexiblePickupRadiusKm;

    // Destination
    private String destinationLocation;
    private Double destinationLat;
    private Double destinationLng;
    private String destinationLandmark;

    // Route
    private List<String> routeStops;

    // Nested objects
    private OwnerResponse driverDetails;
    private AddVehicleResponse vehicleDetail;
    private List<CreatePreferenceResponse> preferences;

    // Safety
    private boolean shareEmergencyContact;

    /**
     * Populated only when isReturnRide=true.
     * Contains just enough info to confirm the return ride was created
     * without requiring a second API call.
     */
    private ReturnRideSummary returnRide;
}
