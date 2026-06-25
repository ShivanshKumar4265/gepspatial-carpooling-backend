package com.carPooling.backend.entity;

import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.RideStatus;
import com.carPooling.backend.utils.Coordinate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Table(name = "ride_offers")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Driver ----------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    // ---------- Vehicle ----------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicles vehicle;

    // ---------- Preferences ----------
    @ManyToMany
    @JoinTable(
            name = "ride_preferences",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "preference_id")
    )
    private Set<Preference> preferences;

    // ---------- LOCATION (CLEAN STRUCTURE) ----------

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "pickup_lng"))
    })
    private Coordinate pickupPoint;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name = "pickup_landmark")
    private String pickupLandmark;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "destination_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "destination_lng"))
    })
    private Coordinate destinationPoint;

    @Column(name = "destination_location", nullable = false)
    private String destinationLocation;

    @Column(name = "destination_landmark")
    private String destinationLandmark;

    // ---------- ROUTE STOPS ----------
    /**
     * A standard SQL database column is only designed to hold one single value (like one number, one string, or one date). It doesn't know how to store a Java
     * List<String> containing multiple stops (e.g., ["Delhi", "Noida", "Agra"])
     * inside a single cell.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_stops")
    private List<String> routeStops;

    // ---------- EXTRA ----------
    @Column(columnDefinition = "TEXT")
    private String pickupInstructions;

    private Float flexiblePickupRadiusKm;

    // ---------- TIMING ----------
    @Column(nullable = false)
    private LocalDate rideDate;

    @Column(nullable = false)
    private LocalTime departureTime;

    // ---------- RETURN RIDE ----------

    private boolean isReturnRide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_ride_id")
    private RideEntity returnRide;

    // ---------- REPEAT ----------
    private boolean repeatRide;

    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;

    // ---------- SEATS & PRICE ----------
    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    // ---------- STATUS ----------
    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.SCHEDULED;

    // ---------- SAFETY ----------
    private boolean shareEmergencyContact;
}