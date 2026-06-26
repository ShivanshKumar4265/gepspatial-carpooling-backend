package com.carPooling.backend.dto.response;




import com.carPooling.backend.dto.request.CreateRideRequest;
import com.carPooling.backend.dto.response.*;
import com.carPooling.backend.entity.*;
import com.carPooling.backend.enums.TemplateStatus;
import com.carPooling.backend.utils.Coordinate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class RideMapper {

    // =========================================================
    //  REQUEST → FORWARD TEMPLATE
    // =========================================================

    public RideTemplate toTemplate(CreateRideRequest req,
                                   User driver,
                                   Vehicles vehicle,
                                   Set<Preference> preferences) {

        RideTemplate t = new RideTemplate();
        t.setDriver(driver);
        t.setVehicle(vehicle);
        t.setPreferences(preferences);

        // Pickup
        t.setPickupPoint(new Coordinate(req.getPickupLat(), req.getPickupLng()));
        t.setPickupLocation(req.getPickupLocation());
        t.setPickupLandmark(req.getPickupLandmark());
        t.setPickupInstructions(req.getPickupInstructions());
        t.setFlexiblePickupRadiusKm(req.getFlexiblePickupRadiusKm());

        // Destination
        t.setDestinationPoint(new Coordinate(req.getDestinationLat(), req.getDestinationLng()));
        t.setDestinationLocation(req.getDestinationLocation());
        t.setDestinationLandmark(req.getDestinationLandmark());

        // Route
        t.setRouteStops(req.getRouteStops());

        // Timing
        t.setDepartureTime(req.getDepartureTime());
        t.setRepeatStartDate(req.getRideDate());

        // Seats & Price
        t.setAvailableSeats(req.getAvailableSeats());
        t.setPricePerSeat(req.getPricePerSeat());

        // Safety
        t.setShareEmergencyContact(Boolean.TRUE.equals(req.getShareEmergencyContact()));

        // Recurrence
        applyRecurrence(t, req);

        t.setTemplateStatus(TemplateStatus.ACTIVE);
        return t;
    }

    // =========================================================
    //  REQUEST → RETURN TEMPLATE  (pickup ↔ destination swapped)
    // =========================================================

    /**
     * Builds the return leg of a round trip.
     *
     * Swap rules:
     *   pickup    ← original destination (lat/lng/location/landmark)
     *   destination ← original pickup    (lat/lng/location/landmark)
     *   routeStops  ← reversed list
     *   departureTime ← req.returnDepartureTime
     *   repeatStartDate ← req.returnRideDate ?? req.rideDate (same date if not specified)
     *
     * Everything else (seats, price, recurrence, safety) mirrors the forward ride.
     */
    public RideTemplate toReturnTemplate(CreateRideRequest req,
                                         User driver,
                                         Vehicles vehicle,
                                         Set<Preference> preferences) {

        RideTemplate t = new RideTemplate();
        t.setDriver(driver);
        t.setVehicle(vehicle);
        t.setPreferences(preferences);

        // Pickup = original DESTINATION
        t.setPickupPoint(new Coordinate(req.getDestinationLat(), req.getDestinationLng()));
        t.setPickupLocation(req.getDestinationLocation());
        t.setPickupLandmark(req.getDestinationLandmark());
        t.setPickupInstructions(null); // destination had no pickup instructions
        t.setFlexiblePickupRadiusKm(req.getFlexiblePickupRadiusKm());

        // Destination = original PICKUP
        t.setDestinationPoint(new Coordinate(req.getPickupLat(), req.getPickupLng()));
        t.setDestinationLocation(req.getPickupLocation());
        t.setDestinationLandmark(req.getPickupLandmark());

        // Route stops reversed
        if (req.getRouteStops() != null && !req.getRouteStops().isEmpty()) {
            List<String> reversed = new ArrayList<>(req.getRouteStops());
            Collections.reverse(reversed);
            t.setRouteStops(reversed);
        }

        // Return-specific timing
        t.setDepartureTime(req.getReturnDepartureTime());
        // returnRideDate defaults to the same day as the forward ride
        t.setRepeatStartDate(
                req.getReturnRideDate() != null ? req.getReturnRideDate() : req.getRideDate()
        );

        // Same seats, price, safety as forward ride
        t.setAvailableSeats(req.getAvailableSeats());
        t.setPricePerSeat(req.getPricePerSeat());
        t.setShareEmergencyContact(Boolean.TRUE.equals(req.getShareEmergencyContact()));

        // Same recurrence rule as forward ride
        applyRecurrence(t, req);

        t.setTemplateStatus(TemplateStatus.ACTIVE);
        return t;
    }

    // =========================================================
    //  ENTITY → RESPONSE  (one-way ride)
    // =========================================================

    public CreateRideResponse toResponse(RideTemplate template,
                                         RideOccurrence firstOccurrence) {

        CreateRideResponse res = buildBaseResponse(template, firstOccurrence);
        res.setReturnRide(null); // explicitly no return ride
        return res;
    }

    // =========================================================
    //  ENTITY → RESPONSE  (round trip: forward + return)
    // =========================================================

    public CreateRideResponse toResponse(RideTemplate forwardTemplate,
                                         RideOccurrence forwardFirstOccurrence,
                                         RideTemplate returnTemplate,
                                         RideOccurrence returnFirstOccurrence) {

        CreateRideResponse res = buildBaseResponse(forwardTemplate, forwardFirstOccurrence);
        res.setReturnRide(buildReturnSummary(returnTemplate, returnFirstOccurrence));
        return res;
    }

    // =========================================================
    //  PRIVATE HELPERS
    // =========================================================

    private CreateRideResponse buildBaseResponse(RideTemplate template,
                                                 RideOccurrence firstOccurrence) {
        CreateRideResponse res = new CreateRideResponse();

        // IDs
        res.setTemplateId(template.getId());
        res.setOccurrenceId(firstOccurrence.getId());

        // Timing
        res.setRideDate(firstOccurrence.getRideDate());
        res.setDepartureTime(template.getDepartureTime());

        // Seats & Price (effective: occurrence override ?? template default)
        res.setAvailableSeats(firstOccurrence.getEffectiveSeats());
        res.setPricePerSeat(firstOccurrence.getEffectivePrice());

        // Recurrence
        res.setRepeatRide(template.isRecurring());
        res.setRepeatType(template.getRepeatType());
        res.setRepeatUntil(template.getRepeatUntil());

        // Status
        res.setOccurrenceStatus(firstOccurrence.getStatus());
        res.setTemplateStatus(template.getTemplateStatus());

        // Metadata
        res.setCreatedAt(template.getCreatedAt());
        res.setMessage("Ride created successfully");

        // Pickup
        res.setPickupLocation(template.getPickupLocation());
        if (template.getPickupPoint() != null) {
            res.setPickupLat(template.getPickupPoint().getLat());
            res.setPickupLng(template.getPickupPoint().getLng());
        }
        res.setPickupLandmark(template.getPickupLandmark());
        res.setPickupInstructions(template.getPickupInstructions());
        res.setFlexiblePickupRadiusKm(template.getFlexiblePickupRadiusKm());

        // Destination
        res.setDestinationLocation(template.getDestinationLocation());
        if (template.getDestinationPoint() != null) {
            res.setDestinationLat(template.getDestinationPoint().getLat());
            res.setDestinationLng(template.getDestinationPoint().getLng());
        }
        res.setDestinationLandmark(template.getDestinationLandmark());

        // Route
        res.setRouteStops(template.getRouteStops());

        // Driver
        if (template.getDriver() != null) {
            res.setDriverDetails(mapUser(template.getDriver()));
        }

        // Vehicle
        if (template.getVehicle() != null) {
            res.setVehicleDetail(mapVehicle(template.getVehicle()));
        }

        // Preferences
        if (template.getPreferences() != null && !template.getPreferences().isEmpty()) {
            res.setPreferences(
                    template.getPreferences().stream()
                            .map(p -> new CreatePreferenceResponse(p.getId(), p.getPreferenceName()))
                            .toList()
            );
        }

        // Safety
        res.setShareEmergencyContact(template.isShareEmergencyContact());

        return res;
    }

    /**
     * Nested summary for the return leg — contains just enough info
     * for the client to display "Return ride also created" without a second API call.
     */
    private ReturnRideSummary buildReturnSummary(RideTemplate returnTemplate,
                                                 RideOccurrence returnFirstOccurrence) {

        ReturnRideSummary summary = new ReturnRideSummary();

        summary.setTemplateId(returnTemplate.getId());
        summary.setOccurrenceId(returnFirstOccurrence.getId());

        summary.setRideDate(returnFirstOccurrence.getRideDate());
        summary.setDepartureTime(returnTemplate.getDepartureTime());

        summary.setPickupLocation(returnTemplate.getPickupLocation());
        summary.setDestinationLocation(returnTemplate.getDestinationLocation());

        summary.setAvailableSeats(returnFirstOccurrence.getEffectiveSeats());
        summary.setPricePerSeat(returnFirstOccurrence.getEffectivePrice());

        summary.setStatus(returnFirstOccurrence.getStatus());

        return summary;
    }

    private OwnerResponse mapUser(User user) {
        OwnerResponse r = new OwnerResponse();
        r.setName(user.getName());
        r.setEmail(user.getEmail());
        r.setPhoneNumber(user.getPhoneNumber());
        r.setGender(user.getGender() != null ? user.getGender().name() : null);
        r.setProfilePicture(user.getProfilePicture());
        r.setDob(user.getDob());
        r.setCollegeCompanyName(user.getCollegeCompanyName());
        return r;
    }

    private AddVehicleResponse mapVehicle(Vehicles vehicle) {
        AddVehicleResponse v = new AddVehicleResponse();
        v.setVehicleId(vehicle.getId());
        v.setVehicleNumber(vehicle.getVehicleNumber());
        v.setVehicleType(vehicle.getVehicleType());
        v.setVehicleModel(vehicle.getVehicleModel());
        v.setColor(vehicle.getColor());
        v.setTotalSeats(vehicle.getTotalSeats());
        if (vehicle.getUser() != null) {
            v.setOwner(mapUser(vehicle.getUser()));
        }
        return v;
    }

    /**
     * Shared recurrence logic — used by both toTemplate and toReturnTemplate.
     * Guards against isRepeatRide=true with null repeatType slipping through validation.
     */
    private void applyRecurrence(RideTemplate t, CreateRideRequest req) {
        boolean isRecurring = Boolean.TRUE.equals(req.getIsRepeatRide())
                && req.getRepeatType() != null;

        t.setRecurring(isRecurring);
        t.setRepeatType(isRecurring ? req.getRepeatType() : null);
        t.setRepeatUntil(isRecurring ? req.getRepeatUntil() : null);
    }
}