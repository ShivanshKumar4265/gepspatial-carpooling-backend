package com.carPooling.backend.entity;

import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.TemplateStatus;
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

@Entity
@Table(name = "ride_templates")
@Getter @Setter @NoArgsConstructor
public class RideTemplate extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicles vehicle;

    // ---------- Location ----------
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "pickup_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "pickup_lng"))
    })
    private Coordinate pickupPoint;

    private String pickupLocation;
    private String pickupLandmark;

    @Column(columnDefinition = "TEXT")
    private String pickupInstructions;

    private Float flexiblePickupRadiusKm;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "destination_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "destination_lng"))
    })
    private Coordinate destinationPoint;

    private String destinationLocation;
    private String destinationLandmark;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_stops")
    private List<String> routeStops;

    // ---------- Timing ----------
    @Column(nullable = false)
    private LocalTime departureTime;

    // ---------- Seats & Price (defaults; overridable per occurrence) ----------
    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    // ---------- Safety ----------
    private boolean shareEmergencyContact;

    // ---------- Recurrence ----------
    private boolean recurring;

    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;           // null for one-time rides

    @Column(nullable = false)
    private LocalDate repeatStartDate;

    private LocalDate repeatUntil;           // null = rolling window, no end date

    // ---------- Status ----------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus templateStatus = TemplateStatus.ACTIVE;

    // ---------- Preferences ----------
    @ManyToMany
    @JoinTable(
            name = "template_preferences",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "preference_id")
    )
    private Set<Preference> preferences;
}