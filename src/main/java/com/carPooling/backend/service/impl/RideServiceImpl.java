package com.carPooling.backend.service.impl;


import com.carPooling.backend.dto.request.AddVehicleRequest;
import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.response.AddVehicleResponse;
import com.carPooling.backend.dto.response.CreatePreferenceResponse;
import com.carPooling.backend.dto.response.OwnerResponse;
import com.carPooling.backend.dto.response.VehicleListResponse;
import com.carPooling.backend.entity.Preference;
import com.carPooling.backend.entity.User;
import com.carPooling.backend.entity.Vehicles;
import com.carPooling.backend.exception.custom_exception.ConflictException;
import com.carPooling.backend.exception.custom_exception.InvalidRequestException;
import com.carPooling.backend.exception.custom_exception.UnauthorizedException;
import com.carPooling.backend.repository.PreferenceRepository;
import com.carPooling.backend.repository.UserRepository;
import com.carPooling.backend.repository.VehicleRepository;
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

    private final PreferenceRepository preferenceRepository;
    private final VehicleRepository vehicleRepository;
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

    /**
     * for list of vehicles by current user
     * @return
     */
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
}
