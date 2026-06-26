package com.carPooling.backend.entity;

import com.carPooling.backend.enums.OccurrenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "ride_occurrences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "ride_date"})
)
@Getter
@Setter
@NoArgsConstructor
public class RideOccurrence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private RideTemplate template;

    @Column(nullable = false)
    private LocalDate rideDate;

    // These are NULL unless the driver overrides for this specific date
    private Integer availableSeatsOverride;
    private BigDecimal pricePerSeatOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status = OccurrenceStatus.SCHEDULED;

    // ------ Computed helpers (no DB column) ------
    public Integer getEffectiveSeats() {
        return availableSeatsOverride != null
                ? availableSeatsOverride
                : template.getAvailableSeats();
    }

    public BigDecimal getEffectivePrice() {
        return pricePerSeatOverride != null
                ? pricePerSeatOverride
                : template.getPricePerSeat();
    }
}
