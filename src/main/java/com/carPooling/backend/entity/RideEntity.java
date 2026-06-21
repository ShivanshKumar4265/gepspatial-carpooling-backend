package com.carPooling.backend.entity;

import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.RideStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "ride_offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RideEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Driver offering the ride
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @NotNull(message = "Driver is required")
    private User driver;

    /**
     * Vehicle used for the ride
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @NotNull(message = "Vehicle is required")
    private Vehicles vehicle;

    @ManyToMany
    @JoinTable(
            name = "ride_preferences",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "preference_id")
    )
    private Set<Preference> preferences;

    @NotBlank(message = "Pickup location is required")
    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name = "pickup_landmark")
    private String pickupLandmark;

    /**
     * For MySQL Spatial/PostGIS use Point type.
     * Keeping String initially for simplicity.
     *
     * Example:
     * POINT(77.5946 12.9716)
     */
    @Column(name = "pickup_point")
    private String pickupPoint;

    @NotBlank(message = "Destination location is required")
    @Column(name = "destination_location", nullable = false)
    private String destinationLocation;

    @Column(name = "destination_point")
    private String destinationPoint;

    /**
     * Multiple stops can be stored as JSON
     */
    @Column(name = "route_stops", columnDefinition = "TEXT")
    private String routeStops;

    @Column(name = "pickup_instructions", columnDefinition = "TEXT")
    private String pickupInstructions;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "flexible_pickup_radius_km")
    private Float flexiblePickupRadiusKm;

    @NotNull(message = "Ride date is required")
    @FutureOrPresent(message = "Ride date cannot be in the past")
    @Column(name = "ride_date", nullable = false)
    private LocalDate rideDate;

    @NotNull(message = "Departure time is required")
    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    /**
     * Return ride reference
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_ride_id")
    private RideEntity returnRide;

    @Column(name = "is_repeat")
    private boolean isRepeat = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type")
    private RepeatType repeatType;

    @Min(value = 1, message = "At least one seat must be available")
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    @Column(name = "price_per_seat", nullable = false)
    private BigDecimal pricePerSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.SCHEDULED;

    @Column(name = "share_emergency_contact")
    private boolean shareEmergencyContact = false;
}