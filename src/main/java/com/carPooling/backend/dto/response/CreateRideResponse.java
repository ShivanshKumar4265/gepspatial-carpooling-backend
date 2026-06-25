package com.carPooling.backend.dto.response;

import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.Vehicles;
import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.RideStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateRideResponse {

    // ---------- Identity ----------
    private Long rideId;
    private OwnerResponse  driverDetails;
    private AddVehicleResponse vehicleDetail;

    // ---------- Pickup ----------
    private String pickupLocation;
    private Double pickupLat;
    private Double pickupLng;
    private String pickupLandmark;

    // ---------- Destination ----------
    private String destinationLocation;
    private Double destinationLat;
    private Double destinationLng;
    private String destinationLandmark;

    // ---------- Route ----------
    private List<String> routeStops;

    // ---------- Instructions ----------
    private String pickupInstructions;
    private Float flexiblePickupRadiusKm;

    // ---------- Timing ----------
    private LocalDate rideDate;
    private LocalTime departureTime;

    // ---------- Return Ride ----------
    private Long returnRideId;

    // ---------- Repeat ----------
    private Boolean repeatRide;
    private RepeatType repeatType;

    // ---------- Capacity ----------
    private Integer availableSeats;
    private BigDecimal pricePerSeat;

    // ---------- Preferences ----------
    private List<CreatePreferenceResponse> preferenceIds;

    // ---------- Safety ----------
    private Boolean shareEmergencyContact;

    // ---------- Status ----------
    private RideStatus rideStatus;

    // ---------- Metadata ----------
    private LocalDateTime createdAt;

    private String message;
}