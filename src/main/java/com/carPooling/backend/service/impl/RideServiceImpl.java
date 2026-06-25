package com.carPooling.backend.service.impl;


import com.carPooling.backend.dto.request.AddVehicleRequest;
import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.request.CreateRideRequest;
import com.carPooling.backend.dto.response.*;
import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.RideEntity;
import com.carPooling.backend.entity.User;
import com.carPooling.backend.entity.Vehicles;
import com.carPooling.backend.enums.RepeatType;
import com.carPooling.backend.enums.RideStatus;
import com.carPooling.backend.exception.custom_exception.ConflictException;
import com.carPooling.backend.exception.custom_exception.InvalidRequestException;
import com.carPooling.backend.exception.custom_exception.ResourceNotFoundException;
import com.carPooling.backend.repository.PreferenceRepository;
import com.carPooling.backend.repository.RideRepository;
import com.carPooling.backend.repository.VehicleRepository;
import com.carPooling.backend.service.RideService;
import com.carPooling.backend.utils.Coordinate;
import com.carPooling.backend.utils.CurrentUserService;
import com.carPooling.backend.utils.StringFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j                          // ← Lombok generates: private static final Logger log = ... Simple Logging Facade
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {


    /**
     *
     * The Implicit Injection RuleWhenever
     * a Spring-managed class (like your @Service)
     * has exactly one constructor, Spring automatically
     * assumes that constructor should be used for dependency
     * injection.Because there is only one constructor,
     * @Autowired is 100% optional. Spring will look at the parameters
     * (PreferenceRepository, VehicleRepository, etc.), find those beans
     * in its context, and inject them automatically.
     *
     */
    private final PreferenceRepository preferenceRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;
    private final RideRepository rideRepository;


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

    @Override
    public AddVehicleResponse addVehicle(AddVehicleRequest addVehicleRequest) {

        User user = currentUserService.getCurrentUser();

        if(vehicleRepository.existsByVehicleNumber(addVehicleRequest.getVehicleNumber())){
            throw  new ConflictException("Vehicle already exist");
        }

        Vehicles vehicles = new Vehicles();
        vehicles.setVehicleModel(addVehicleRequest.getVehicleModel());
        vehicles.setVehicleType(addVehicleRequest.getVehicleType());
        vehicles.setUser(user);
        vehicles.setColor(addVehicleRequest.getColor());
        vehicles.setTotalSeats(addVehicleRequest.getTotalSeats());
        vehicles.setVehicleNumber(addVehicleRequest.getVehicleNumber());

        try{
            vehicles = vehicleRepository.save(vehicles);
        }catch (RuntimeException e){
            log.debug(
                    "add vehicle {} : " + e.getMessage()
            );
            throw new InvalidRequestException("Somethign went wrong while adding vehicle " +e.getMessage());
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
        List<Vehicles>  vehiclesList;
        List<VehicleListResponse>  vehicleListResponse = new ArrayList<>();
        try{
            vehiclesList = vehicleRepository.findAll();

            if(vehiclesList.isEmpty()){
                return new ArrayList<>();
            }

            for(int i = 0; i < vehiclesList.size();i++){
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
            throw  new RuntimeException("Vehicel list exception "+ e.getMessage());
        }
        return vehicleListResponse;
    }

    @Override
    public CreateRideResponse createRideRequest(CreateRideRequest req) {


        User user = currentUserService.getCurrentUser();

        Vehicles vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));


        RideEntity baseRide = new RideEntity();

        baseRide.setDriver(user);
        baseRide.setVehicle(vehicle);

        // ---------- Pickup ----------
        baseRide.setPickupLocation(req.getPickupLocation());
        baseRide.setPickupPoint(new Coordinate(req.getPickupLat(), req.getPickupLng()));
        baseRide.setPickupLandmark(req.getPickupLandmark());
        baseRide.setPickupInstructions(req.getPickupInstructions());
        baseRide.setFlexiblePickupRadiusKm(req.getFlexiblePickupRadiusKm());

        // ---------- Destination ----------
        baseRide.setDestinationLocation(req.getDestinationLocation());
        baseRide.setDestinationPoint(new Coordinate(req.getDestinationLat(), req.getDestinationLng()));
        baseRide.setDestinationLandmark(req.getDestinationLandmark());

        // ---------- Route ----------
        baseRide.setRouteStops(req.getRouteStops());

        // ---------- Timing ----------
        baseRide.setRideDate(req.getRideDate());
        baseRide.setDepartureTime(req.getDepartureTime());

        // ---------- Seats & Price ----------
        baseRide.setAvailableSeats(req.getAvailableSeats());
        baseRide.setPricePerSeat(req.getPricePerSeat());

        // ---------- Preferences (IDs → Entities mapping) ----------
        List<Preference> prefs = preferenceRepository.findAllById(req.getPreferenceIds());

        if (prefs.size() != req.getPreferenceIds().size()) {
            throw new ResourceNotFoundException("One or more preferences not found");
        }

        baseRide.setPreferences(new HashSet<>(prefs));

        // ---------- Safety ----------
        baseRide.setShareEmergencyContact(Boolean.TRUE.equals(req.getShareEmergencyContact()));

        // ---------- Status ----------
        baseRide.setStatus(RideStatus.SCHEDULED);

        // ---------- Repeat Flag ----------
        baseRide.setRepeatRide(Boolean.TRUE.equals(req.getIsRepeatRide()));
        baseRide.setRepeatType(req.getRepeatType());

        RideEntity savedBaseRide;
        try{
            savedBaseRide = rideRepository.save(baseRide);
        }catch (Exception e){
            throw  new RuntimeException("Somethign went try again");
        }

        log.debug(
                "isRepeatRide: {}, repeatType: {}",
                req.getIsRepeatRide(),
                req.getRepeatType()
        );

        if (Boolean.TRUE.equals(req.getIsRepeatRide()) && req.getRepeatType().name() != null) {
            generateRecurringRides(savedBaseRide);
        }


        return mapToResponse(savedBaseRide);
    }



    /**
     * Generates recurring rides based on repeat type.
     * Each ride is an independent entity (NO shared references).
     */
    private void generateRecurringRides(RideEntity baseRide) {

        RepeatType type = baseRide.getRepeatType();

        switch (type) {

            case DAILY -> {
                for (int i = 1; i <= 15; i++) {
                    RideEntity ride = cloneRide(baseRide);
                    ride.setRideDate(baseRide.getRideDate().plusDays(i));
                    rideRepository.save(ride);
                }
            }

            case WEEKLY -> {
                for (int i = 1; i <= 8; i++) {
                    RideEntity ride = cloneRide(baseRide);
                    ride.setRideDate(baseRide.getRideDate().plusWeeks(i));
                    rideRepository.save(ride);
                }
            }

            case MONTHLY -> {
                for (int i = 1; i <= 6; i++) {
                    RideEntity ride = cloneRide(baseRide);
                    ride.setRideDate(baseRide.getRideDate().plusMonths(i));
                    rideRepository.save(ride);
                }
            }
        }
    }

    /**
     * Creates a deep copy of RideEntity.
     * IMPORTANT: Avoids JPA entity identity conflicts.
     */
    private RideEntity cloneRide(RideEntity original) {

        RideEntity ride = new RideEntity();

        // ---------- Core Relations ----------
        ride.setDriver(original.getDriver());
        ride.setVehicle(original.getVehicle());

        // ---------- Locations ----------
        ride.setPickupLocation(original.getPickupLocation());
        ride.setPickupPoint(original.getPickupPoint());
        ride.setPickupLandmark(original.getPickupLandmark());

        ride.setDestinationLocation(original.getDestinationLocation());
        ride.setDestinationPoint(original.getDestinationPoint());
        ride.setDestinationLandmark(original.getDestinationLandmark());

        // ---------- Route ----------
        ride.setRouteStops(
                original.getRouteStops() == null
                        ? null
                        : new ArrayList<>(original.getRouteStops())
        );
        // ---------- Instructions ----------
        ride.setPickupInstructions(original.getPickupInstructions());

        // ---------- Radius ----------
        ride.setFlexiblePickupRadiusKm(original.getFlexiblePickupRadiusKm());

        // ---------- Timing ----------
        ride.setDepartureTime(original.getDepartureTime());

        // ---------- Pricing ----------
        ride.setAvailableSeats(original.getAvailableSeats());
        ride.setPricePerSeat(original.getPricePerSeat());

        // ---------- Preferences ----------
        ride.setPreferences(new HashSet<>(original.getPreferences()));
        // ---------- Safety ----------
        ride.setShareEmergencyContact(original.isShareEmergencyContact());

        // ---------- Status ----------
        ride.setStatus(RideStatus.SCHEDULED);

        // IMPORTANT: child rides are NOT repeat templates
        ride.setRepeatRide(false);
        ride.setRepeatType(null);

        return ride;
    }


    private CreateRideResponse mapToResponse(RideEntity ride) {

        CreateRideResponse res = new CreateRideResponse();

        // ================= RIDE =================
        res.setRideId(ride.getId());
        res.setRideDate(ride.getRideDate());
        res.setDepartureTime(ride.getDepartureTime());

        res.setAvailableSeats(ride.getAvailableSeats());
        res.setPricePerSeat(ride.getPricePerSeat());

        res.setRepeatRide(ride.isRepeatRide());
        res.setRepeatType(ride.getRepeatType());
        res.setRideStatus(ride.getStatus());
        res.setCreatedAt(ride.getCreatedAt());

        res.setMessage("Ride fetched successfully");

        // ================= PICKUP =================
        res.setPickupLocation(ride.getPickupLocation());

        if (ride.getPickupPoint() != null) {
            res.setPickupLat(ride.getPickupPoint().getLat());
            res.setPickupLng(ride.getPickupPoint().getLng());
        }

        res.setPickupLandmark(ride.getPickupLandmark());
        res.setPickupInstructions(ride.getPickupInstructions());
        res.setFlexiblePickupRadiusKm(ride.getFlexiblePickupRadiusKm());

        // ================= DESTINATION =================
        res.setDestinationLocation(ride.getDestinationLocation());

        if (ride.getDestinationPoint() != null) {
            res.setDestinationLat(ride.getDestinationPoint().getLat());
            res.setDestinationLng(ride.getDestinationPoint().getLng());
        }

        res.setDestinationLandmark(ride.getDestinationLandmark());

        res.setRouteStops(ride.getRouteStops());

        // ================= RETURN RIDE =================
        if (ride.getReturnRide() != null) {
            res.setReturnRideId(ride.getReturnRide().getId());
        }

        // ================= DRIVER =================
        if (ride.getDriver() != null) {

            OwnerResponse driver = new OwnerResponse();
            driver.setName(ride.getDriver().getName());
            driver.setEmail(ride.getDriver().getEmail());
            driver.setPhoneNumber(ride.getDriver().getPhoneNumber());
            driver.setGender(ride.getDriver().getGender() != null ? ride.getDriver().getGender().name() : null);
            driver.setProfilePicture(ride.getDriver().getProfilePicture());
            driver.setDob(ride.getDriver().getDob());
            driver.setCollegeCompanyName(ride.getDriver().getCollegeCompanyName());

            res.setDriverDetails(driver);
        }

        // ================= VEHICLE =================
        if (ride.getVehicle() != null) {

            AddVehicleResponse vehicle = new AddVehicleResponse();

            vehicle.setVehicleId(ride.getVehicle().getId());
            vehicle.setVehicleNumber(ride.getVehicle().getVehicleNumber());
            vehicle.setVehicleType(ride.getVehicle().getVehicleType());
            vehicle.setVehicleModel(ride.getVehicle().getVehicleModel());
            vehicle.setColor(ride.getVehicle().getColor());
            vehicle.setTotalSeats(ride.getVehicle().getTotalSeats());

            // OWNER (SAFE)
            if (ride.getVehicle().getUser() != null) {

                OwnerResponse owner = new OwnerResponse();
                owner.setName(ride.getVehicle().getUser().getName());
                owner.setEmail(ride.getVehicle().getUser().getEmail());
                owner.setPhoneNumber(ride.getVehicle().getUser().getPhoneNumber());
                owner.setGender(ride.getVehicle().getUser().getGender() != null
                        ? ride.getVehicle().getUser().getGender().name()
                        : null);
                owner.setProfilePicture(ride.getVehicle().getUser().getProfilePicture());
                owner.setDob(ride.getVehicle().getUser().getDob());
                owner.setCollegeCompanyName(ride.getVehicle().getUser().getCollegeCompanyName());

                vehicle.setOwner(owner);
            }

            res.setVehicleDetail(vehicle);
        }

        // ================= PREFERENCES =================
        if (ride.getPreferences() != null && !ride.getPreferences().isEmpty()) {

            List<CreatePreferenceResponse> prefs = ride.getPreferences()
                    .stream()
                    .map(p -> new CreatePreferenceResponse(
                            p.getId(),
                            p.getPreferenceName()
                    ))
                    .toList();

            res.setPreferenceIds(prefs);
        }

        res.setShareEmergencyContact(ride.isShareEmergencyContact());

        return res;
    }}
