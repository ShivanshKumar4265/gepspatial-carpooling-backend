package com.carPooling.backend.dto.request;

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

    @NotBlank(message = "Pickup location is required")
    @Size(min = 3, max = 255,
            message = "Pickup location must be between 3 and 255 characters")
    private String pickupLocation;

    @Size(max = 255,
            message = "Pickup landmark cannot exceed 255 characters")
    private String pickupLandmark;

    /**
     * Latitude,Longitude
     * Example: 28.6139,77.2090
     */
    private String pickupPoint;

    @NotBlank(message = "Destination location is required")
    @Size(min = 3, max = 255,
            message = "Destination location must be between 3 and 255 characters")
    private String destinationLocation;

    /**
     * Latitude,Longitude
     * Example: 28.5355,77.3910
     */
    private String destinationPoint;

    /**
     * Optional route stops
     */
    private List<String> routeStops;

    @Size(max = 500,
            message = "Pickup instructions cannot exceed 500 characters")
    private String pickupInstructions;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Pickup radius cannot be negative"
    )
    @DecimalMax(
            value = "50.0",
            message = "Pickup radius cannot exceed 50 KM"
    )
    private Float flexiblePickupRadiusKm;

    @NotNull(message = "Ride date is required")
    @FutureOrPresent(message = "Ride date cannot be in the past")
    private LocalDate rideDate;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    /**
     * Optional return ride
     */
    @Positive(message = "Return ride id must be positive")
    private Long returnRideId;

    private Boolean repeatRide = false;

    /**
     * DAILY, WEEKLY, MONTHLY
     */
    private String repeatType;

    @NotNull(message = "Available seats are required")
    @Min(value = 1, message = "Minimum 1 seat is required")
    @Max(value = 10, message = "Maximum 10 seats are allowed")
    private Integer availableSeats;

    @NotNull(message = "Price per seat is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Price per seat must be greater than 0"
    )
    @Digits(
            integer = 6,
            fraction = 2,
            message = "Invalid price format"
    )
    private BigDecimal pricePerSeat;

    /**
     * Preference ids from preferences table
     */
    @NotEmpty(message = "At least one preference must be selected")
    private List<Long> preferenceIds;

    private Boolean shareEmergencyContact = false;
}