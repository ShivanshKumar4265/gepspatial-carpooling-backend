package com.carPooling.backend.dto.request;

import com.carPooling.backend.enums.RepeatType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateRideRequest {

    @NotNull(message = "Vehicle id is required")
    @Positive(message = "Vehicle id must be positive")
    private Long vehicleId;

    // ---------- Pickup ----------
    @NotNull(message = "Pickup latitude is required")
    private Double pickupLat;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLng;

    @Size(max = 255, message = "Pickup location cannot exceed 255 characters")
    private String pickupLocation;

    @Size(max = 255, message = "Pickup landmark cannot exceed 255 characters")
    private String pickupLandmark;

    // ---------- Destination ----------
    @NotNull(message = "Destination latitude is required")
    private Double destinationLat;

    @NotNull(message = "Destination longitude is required")
    private Double destinationLng;

    @Size(max = 255, message = "Destination location cannot exceed 255 characters")
    private String destinationLocation;

    @Size(max = 255, message = "Destination landmark cannot exceed 255 characters")
    private String destinationLandmark;

    // ---------- Route ----------
    private List<String> routeStops;

    @Size(max = 500, message = "Pickup instructions cannot exceed 500 characters")
    private String pickupInstructions;

    @DecimalMin(value = "0.0", inclusive = true, message = "Pickup radius cannot be negative")
    private Float flexiblePickupRadiusKm;

    // ---------- Ride Timing ----------
    @NotNull(message = "Ride date is required")
    @FutureOrPresent(message = "Ride date cannot be in the past")
    private LocalDate rideDate;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    // ---------- Return Ride ----------
    @NotNull(message = "isReturnRide flag is required")
    private Boolean isReturnRide = false;

    @Positive(message = "Return ride id must be positive")
    private Long returnRideId;

    // ---------- Repeat Ride ----------
    @NotNull(message = "isRepeatRide flag is required")
    private Boolean isRepeatRide = false;

//    @Null(message = "Repeat type must be null when repeat ride is false")
    private RepeatType repeatType;

    // ---------- Seats & Price ----------
    @NotNull(message = "Available seats are required")
    @Min(value = 1, message = "Minimum 1 seat is required")
    private Integer availableSeats;

    @NotNull(message = "Price per seat is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 20, fraction = 2, message = "Invalid price format")
    private BigDecimal pricePerSeat;

    // ---------- Preferences ----------
    @NotEmpty(message = "At least one preference is required")
    private List<Long> preferenceIds;

    // ---------- Safety ----------
    @NotNull(message = "Emergency contact flag is required")
    private Boolean shareEmergencyContact = false;
}