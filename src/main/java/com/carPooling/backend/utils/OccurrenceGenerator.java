package com.carPooling.backend.utils;

import com.carPooling.backend.entity.RideOccurrence;
import com.carPooling.backend.entity.RideTemplate;
import com.carPooling.backend.enums.OccurrenceStatus;
import com.carPooling.backend.enums.RepeatType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class OccurrenceGenerator {

    private static final int DEFAULT_WINDOW_DAYS = 30;

    /**
     * Called on ride creation. Generates occurrences up to
     * min(repeatUntil, today + windowDays).
     */
    public List<RideOccurrence> generateInitialWindow(RideTemplate template) {
        LocalDate windowEnd = LocalDate.now().plusDays(DEFAULT_WINDOW_DAYS);
        LocalDate effectiveEnd = cap(template.getRepeatUntil(), windowEnd);
        return buildOccurrences(template, template.getRepeatStartDate(), effectiveEnd);
    }

    /**
     * Called by the daily scheduler to extend the window forward.
     */
    public List<RideOccurrence> generateFrom(RideTemplate template,
                                             LocalDate from, LocalDate to) {
        return buildOccurrences(template, from, to);
    }

    private List<RideOccurrence> buildOccurrences(RideTemplate template,
                                                  LocalDate from, LocalDate to) {
        if (!template.isRecurring()) {
            // One-time ride: single occurrence on repeatStartDate
            return List.of(newOccurrence(template, template.getRepeatStartDate()));
        }

        List<RideOccurrence> result = new ArrayList<>();
        LocalDate cursor = from;

        while (!cursor.isAfter(to)) {
            result.add(newOccurrence(template, cursor));
            cursor = advance(cursor, template.getRepeatType());
        }

        return result;
    }

    private LocalDate advance(LocalDate date, RepeatType type) {
        return switch (type) {
            case DAILY   -> date.plusDays(1);
            case WEEKLY  -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
        };
    }

    private LocalDate cap(LocalDate repeatUntil, LocalDate windowEnd) {
        if (repeatUntil == null) return windowEnd;
        return repeatUntil.isBefore(windowEnd) ? repeatUntil : windowEnd;
    }

    private RideOccurrence newOccurrence(RideTemplate template, LocalDate date) {
        RideOccurrence o = new RideOccurrence();
        o.setTemplate(template);
        o.setRideDate(date);
        o.setStatus(OccurrenceStatus.SCHEDULED);
        return o;
    }
}
