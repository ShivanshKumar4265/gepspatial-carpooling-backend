package com.carPooling.backend.service.impl;


import com.carPooling.backend.dto.request.AddVehicleRequest;
import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.request.CreateRideRequest;
import com.carPooling.backend.dto.response.*;
import com.carPooling.backend.entity.*;
import com.carPooling.backend.enums.OccurrenceStatus;
import com.carPooling.backend.enums.TemplateStatus;
import com.carPooling.backend.exception.custom_exception.ConflictException;
import com.carPooling.backend.exception.custom_exception.InvalidRequestException;
import com.carPooling.backend.exception.custom_exception.ResourceNotFoundException;
import com.carPooling.backend.repository.*;
import com.carPooling.backend.service.RideService;
import com.carPooling.backend.utils.CurrentUserService;
import com.carPooling.backend.utils.OccurrenceGenerator;
import com.carPooling.backend.utils.StringFormat;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j                          // ← Lombok generates: private static final Logger log = ... Simple Logging Facade
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {


    /**
     * The Implicit Injection RuleWhenever
     * a Spring-managed class (like your @Service)
     * has exactly one constructor, Spring automatically
     * assumes that constructor should be used for dependency
     * injection.Because there is only one constructor,
     *
     * @Autowired is 100% optional. Spring will look at the parameters
     * (PreferenceRepository, VehicleRepository, etc.), find those beans
     * in its context, and inject them automatically.
     */
//    private final RideRepository templateRepository;
    private final RideTemplateRepository templateRepository;
    private final RideOccurrenceRepository occurrenceRepository;
    private final VehicleRepository vehicleRepository;
    private final PreferenceRepository preferenceRepository;
    private final CurrentUserService currentUserService;
    private final OccurrenceGenerator occurrenceGenerator;
    private final RideMapper rideMapper;


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
     * <p>
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
     * @Override public CreatePreferenceResponse createPreference(CreatePreferenceRequest req) {
     * User user = userRepository.findByEmail(email)
     * .orElseThrow(() -> new UnauthorizedException("Unauthorized: User not found"));
     * if (preferenceRepository.existsByPreferenceName(req.getPreference_name())) {
     * throw new ConflictException("Preference Already Exist");
     * }
     * Preference preference = new Preference();
     * preference.setPreferenceName(req.getPreference_name());
     * Preference saved = preferenceRepository.save(preference); // single write
     * return new CreatePreferenceResponse(saved.getId(), saved.getPreferenceName());
     * }
     * <p>
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
     * @Override
     * @Transactional public RideOfferResponse createRideOfferWithPreferences(CreateRideOfferRequest req) {
     * Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
     * .orElseThrow(() -> new NotFoundException("Vehicle not found"));
     * <p>
     * RideOffer offer = new RideOffer();
     * offer.setVehicle(vehicle);
     * offer.setDepartureTime(req.getDepartureTime());
     * offer.setAvailableSeats(req.getAvailableSeats());
     * RideOffer savedOffer = rideOfferRepository.save(offer); // write #1
     * <p>
     * for (Long prefId : req.getPreferenceIds()) {
     * Preference pref = preferenceRepository.findById(prefId)
     * .orElseThrow(() -> new NotFoundException("Preference not found: " + prefId));
     * rideOfferPreferenceRepository.save(new RideOfferPreference(savedOffer, pref)); // write #2..N
     * }
     * return toResponse(savedOffer);
     * }
     * <p>
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
    public CreatePreferenceResponse createPreference(CreatePreferenceRequest createPreferenceRequest) {

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
        } catch (RuntimeException e) {
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

    @Override
    public AddVehicleResponse addVehicle(AddVehicleRequest addVehicleRequest) {

        User user = currentUserService.getCurrentUser();

        if (vehicleRepository.existsByVehicleNumber(addVehicleRequest.getVehicleNumber())) {
            throw new ConflictException("Vehicle already exist");
        }

        Vehicles vehicles = new Vehicles();
        vehicles.setVehicleModel(addVehicleRequest.getVehicleModel());
        vehicles.setVehicleType(addVehicleRequest.getVehicleType());
        vehicles.setUser(user);
        vehicles.setColor(addVehicleRequest.getColor());
        vehicles.setTotalSeats(addVehicleRequest.getTotalSeats());
        vehicles.setVehicleNumber(addVehicleRequest.getVehicleNumber());

        try {
            vehicles = vehicleRepository.save(vehicles);
        } catch (RuntimeException e) {
            log.debug(
                    "add vehicle {} : " + e.getMessage()
            );
            throw new InvalidRequestException("Somethign went wrong while adding vehicle " + e.getMessage());
        }

        AddVehicleResponse addVehicleResponse = new AddVehicleResponse();
        addVehicleResponse.setVehicleId(vehicles.getId());
        addVehicleResponse.setVehicleModel(vehicles.getVehicleModel());
        addVehicleResponse.setVehicleType(vehicles.getVehicleType());
        addVehicleResponse.setColor(vehicles.getColor());
        addVehicleResponse.setTotalSeats(vehicles.getTotalSeats());
        addVehicleResponse.setVehicleNumber(vehicles.getVehicleNumber());
        // Safely convert gender to a String for the OwnerResponse (handle nulls and non-String enums)
        String genderString = (user.getGender() == null) ? "" : user.getGender().toString();
        addVehicleResponse.setOwner(new OwnerResponse(user.getName(), user.getEmail(), user.getPhoneNumber(), genderString, user.getProfilePicture(), user.getDob(), user.getCollegeCompanyName()));
        return addVehicleResponse;
    }


    @Override
    public List<VehicleListResponse> getVehicleListOfCurrentUser() {
        User user = currentUserService.getCurrentUser();
        List<Vehicles> vehiclesList;
        List<VehicleListResponse> vehicleListResponse = new ArrayList<>();
        try {
            vehiclesList = vehicleRepository.findAll();

            if (vehiclesList.isEmpty()) {
                return new ArrayList<>();
            }

            for (int i = 0; i < vehiclesList.size(); i++) {
                VehicleListResponse vehicleListData = new VehicleListResponse();

                vehicleListData.setVehicleNumber(vehiclesList.get(i).getVehicleNumber());
                vehicleListData.setVehicleId(vehiclesList.get(i).getId());
                vehicleListData.setVehicleType(vehiclesList.get(i).getVehicleType());
                vehicleListData.setVehicleModel(vehiclesList.get(i).getVehicleModel());
                vehicleListData.setColor(vehiclesList.get(i).getColor());
                vehicleListData.setTotalSeats(vehiclesList.get(i).getTotalSeats());

                vehicleListResponse.add(vehicleListData);
            }

        } catch (RuntimeException e) {
            throw new RuntimeException("Vehicel list exception " + e.getMessage());
        }
        return vehicleListResponse;
    }

    @Override
    @Transactional
    public CreateRideResponse createRideRequest(CreateRideRequest req) {

        User user = currentUserService.getCurrentUser();

        Vehicles vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        List<Preference> prefs = preferenceRepository.findAllById(req.getPreferenceIds());
        if (prefs.size() != req.getPreferenceIds().size()) {
            throw new ResourceNotFoundException("One or more preferences not found");
        }

        // 1. Persist the template (single row, always)
        RideTemplate template = rideMapper.toTemplate(req, user, vehicle, new HashSet<>(prefs));
        RideTemplate saved = templateRepository.save(template);

        // 2. Materialize the first window of occurrences
        List<RideOccurrence> occurrences = occurrenceGenerator.generateInitialWindow(saved);
        occurrenceRepository.saveAll(occurrences);      // 1 batch INSERT

        log.info("Created template {} with {} initial occurrences", saved.getId(), occurrences.size());

        return rideMapper.toResponse(saved, occurrences.get(0));
    }

    // --- Update / Cancel operations (now trivial) ---

    @Transactional
    public void updateSeriesPrice(Long templateId, BigDecimal newPrice) {
        RideTemplate t = findTemplate(templateId);
        t.setPricePerSeat(newPrice);
        templateRepository.save(t);
        // All future occurrences without an override automatically reflect this — no loop needed
    }

    @Transactional
    public void overrideOccurrencePrice(Long occurrenceId, BigDecimal newPrice) {
        RideOccurrence o = findOccurrence(occurrenceId);
        o.setPricePerSeatOverride(newPrice);
        occurrenceRepository.save(o);
    }

    @Transactional
    public void cancelOccurrence(Long occurrenceId) {
        RideOccurrence o = findOccurrence(occurrenceId);
        o.setStatus(OccurrenceStatus.CANCELLED);
        occurrenceRepository.save(o);
    }

    @Transactional
    public void cancelSeries(Long templateId) {
        RideTemplate t = findTemplate(templateId);
        t.setTemplateStatus(TemplateStatus.ENDED);
        templateRepository.save(t);
        occurrenceRepository.cancelFutureOccurrences(templateId, LocalDate.now());
    }

    private RideTemplate findTemplate(Long id) {
        return templateRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Template not found"));
    }

    private RideOccurrence findOccurrence(Long id) {
        return occurrenceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Occurrence not found"));
    }

}