package com.carPooling.backend.service.impl;


import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.response.CreatePreferenceResponse;
import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.User;
import com.carPooling.backend.exception.custom_exception.ConflictException;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import com.carPooling.backend.repository.PreferenceRepository;
import com.carPooling.backend.repository.UserRepository;
import com.carPooling.backend.service.RideService;
import com.carPooling.backend.utils.CurrentUserService;
import com.carPooling.backend.utils.StringFormat;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Slf4j                          // ← Lombok generates: private static final Logger log = ... Simple Logging Facade
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final UserRepository userRepository;
    private final PreferenceRepository preferenceRepository;
    private final CurrentUserService currentUserService;


    /**
     * TRANSACTION USAGE NOTES
     * ------------------------
     * A transaction groups one or more database operations into a single atomic
     * unit: either all of them commit, or (on any unchecked exception) all of them
     * roll back, leaving the database exactly as it was before the method ran.
     * Spring Data JPA's SimpleJpaRepository already wraps each individual repository
     * method (save, findById, existsBy..., delete, etc.) in its own short-lived
     * transaction. Adding @Transactional at the SERVICE layer is only needed when
     * a single business operation must treat MULTIPLE repository calls as one
     * all-or-nothing unit, OR when the method relies on dirty checking / lazy
     * loading after the initial fetch.
     *
     * WHEN @Transactional IS NOT NEEDED — example: createPreference()
     * -----------------------------------------------------------------
     * createPreference() does two reads (findByEmail, existsByPreferenceName)
     * followed by exactly ONE write (save). Each already runs in its own
     * transaction. If the write fails, the prior reads have nothing to undo
     * (they wrote nothing), so the database can never be left in an inconsistent
     * state. The result DTO is built immediately from scalar fields on the saved
     * entity, so no lazy-loaded associations are touched after the fact.
     * Conclusion: @Transactional is harmless but not load-bearing here.
     *
     *   @Override
     *   public CreatePreferenceResponse createPreference(CreatePreferenceRequest req) {
     *       User user = userRepository.findByEmail(email)
     *               .orElseThrow(() -> new UnauthorizedException("Unauthorized: User not found"));
     *       if (preferenceRepository.existsByPreferenceName(req.getPreference_name())) {
     *           throw new ConflictException("Preference Already Exist");
     *       }
     *       Preference preference = new Preference();
     *       preference.setPreferenceName(req.getPreference_name());
     *       Preference saved = preferenceRepository.save(preference); // single write
     *       return new CreatePreferenceResponse(saved.getId(), saved.getPreferenceName());
     *   }
     *
     * WHEN @Transactional IS REQUIRED — example: createRideOfferWithPreferences()
     * -----------------------------------------------------------------------------
     * This method performs TWO writes that are logically one operation: saving the
     * RideOffer, then saving each RideOfferPreference link row. Without
     * @Transactional, these run as two separate transactions. If the second write
     * (linking preferences) fails — e.g. an invalid preference ID throws
     * NotFoundException — the RideOffer from the first write has ALREADY committed,
     * leaving an orphaned ride offer with no preferences attached. Wrapping the
     * whole method in @Transactional ensures both writes share one transaction:
     * if step 2 throws, step 1's insert is rolled back too, so the database never
     * shows a half-created ride offer.
     *
     *   @Override
     *   @Transactional
     *   public RideOfferResponse createRideOfferWithPreferences(CreateRideOfferRequest req) {
     *       Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
     *               .orElseThrow(() -> new NotFoundException("Vehicle not found"));
     *
     *       RideOffer offer = new RideOffer();
     *       offer.setVehicle(vehicle);
     *       offer.setDepartureTime(req.getDepartureTime());
     *       offer.setAvailableSeats(req.getAvailableSeats());
     *       RideOffer savedOffer = rideOfferRepository.save(offer); // write #1
     *
     *       for (Long prefId : req.getPreferenceIds()) {
     *           Preference pref = preferenceRepository.findById(prefId)
     *                   .orElseThrow(() -> new NotFoundException("Preference not found: " + prefId));
     *           rideOfferPreferenceRepository.save(new RideOfferPreference(savedOffer, pref)); // write #2..N
     *       }
     *       return toResponse(savedOffer);
     *   }
     *
     * RULE OF THUMB: ask "if this method throws halfway through, would the DB be
     * left in a state that violates business rules?" If yes → @Transactional.
     * If the method only reads, or does exactly one write with no follow-up lazy
     * access, it's optional. Note: @Transactional does NOT solve race conditions
     * from concurrent requests (e.g. duplicate-name checks) — that requires a DB
     * unique constraint plus catching DataIntegrityViolationException, or explicit
     * locking (@Version / SELECT FOR UPDATE) for read-modify-write counters like
     * availableSeats.
     */
    @Override
    public CreatePreferenceResponse createPreference(  CreatePreferenceRequest createPreferenceRequest) {

        User user = currentUserService.getCurrentUser();

        // Read operation 2
        log.debug(
                "Create preferedne request " + createPreferenceRequest.toString()
        );

        String preferenceName =
                StringFormat.toTitleCase(
                        createPreferenceRequest.getPreferenceName()
                );

        if (preferenceRepository.existsByPreferenceName(preferenceName)) {
            throw new ConflictException("Preference Already Exist");
        }

        Preference preference = new Preference();
        preference.setPreferenceName(preferenceName);
        //Write operation 1
        try {
            preference = preferenceRepository.save(preference);
        }catch (RuntimeException e){
            throw new ConflictException("Preference already exist");
        }

        return new CreatePreferenceResponse(preference.getId(), preference.getPreferenceName());
    }


    @Override
    public List<CreatePreferenceResponse> getPreferenceList() {

        User user = currentUserService.getCurrentUser();

        List<Preference> preferences = preferenceRepository.findAll();

        List<CreatePreferenceResponse> responseList = new ArrayList<>();

        /**
         *
         * return preferenceRepository.findAll()
         *             .stream()
         *             .map(preference -> new CreatePreferenceResponse(
         *                     preference.getId(),
         *                     preference.getPreferenceName()
         *             ))
         *             .toList();
         *
         */

        for (Preference preference : preferences) {
            responseList.add(
                    new CreatePreferenceResponse(
                            preference.getId(),
                            preference.getPreferenceName()
                    )
            );
        }

        return responseList;
    }
}
