package com.carPooling.backend.controller;


import com.carPooling.backend.dto.GenricDTO;
import com.carPooling.backend.dto.request.AddVehicleRequest;
import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.request.CreateRideRequest;
import com.carPooling.backend.dto.response.AddVehicleResponse;
import com.carPooling.backend.dto.response.CreatePreferenceResponse;
import com.carPooling.backend.dto.response.CreateRideResponse;
import com.carPooling.backend.dto.response.VehicleListResponse;
import com.carPooling.backend.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ride")
@RequiredArgsConstructor
public class RideController {
    private final RideService rideService;

    @PostMapping("/preference")
    public ResponseEntity<GenricDTO<CreatePreferenceResponse>> createPreference( @Valid @RequestBody CreatePreferenceRequest createPreferenceRequest) {

        log.debug("Create PReference Response: {}", createPreferenceRequest.toString());
        CreatePreferenceResponse response = rideService.createPreference(createPreferenceRequest);

        log.debug(
                "Preference added succesfully : {}. Response: {}",
                response.getId(),
                response.getPreference_name()
        );
        return ResponseEntity.ok(
                new GenricDTO<>(
                        true,
                        "Preference Created successfully",
                        response
                )
        );
    }

    @GetMapping("/preferences")
    public ResponseEntity<GenricDTO<List<CreatePreferenceResponse>>> getPreferenceList( ) {

        List<CreatePreferenceResponse> response = rideService.getPreferenceList();

        log.debug(
                "Preference list : {}. Response: {}",
                response.size()
        );
        return ResponseEntity.ok(
                new GenricDTO<>(
                        true,
                        "Preference List Fetched successfully",
                        response
                )
        );
    }

    @PostMapping("/vehicle")
    public ResponseEntity<GenricDTO<AddVehicleResponse>> addVehicle(
            @Valid @RequestBody AddVehicleRequest addVehicleRequest
            ) {

        AddVehicleResponse response = rideService.addVehicle(addVehicleRequest);

        log.debug(
                "addVehicle Response: {}",
                response
        );
        return ResponseEntity.ok(
                new GenricDTO<>(
                        true,
                        "Vehicle Added successfully",
                        response
                )
        );
    }


    @GetMapping("/vehicles")
    public ResponseEntity<GenricDTO<List<VehicleListResponse>>> getUsersVehicle(

    ) {

        List<VehicleListResponse> response = rideService.getVehicleListOfCurrentUser();

        log.debug(
                "VehicleListResponse Response: {}",
                response
        );
        return ResponseEntity.ok(
                new GenricDTO<>(
                        true,
                        "Vehicles fetched successfully",
                        response
                )
        );
    }


    @PostMapping("/ride")
    public ResponseEntity<GenricDTO<CreateRideResponse>> createRide(
            @Valid @RequestBody CreateRideRequest request
    ) {

        CreateRideResponse response = rideService.createRideRequest(request);
        log.debug("Ride created successfully for vehicleId: {}", request.getVehicleId());

        return ResponseEntity.ok(
                new GenricDTO<>(
                        true,
                        "Ride created successfully",
                        response
                )
        );
    }
}
