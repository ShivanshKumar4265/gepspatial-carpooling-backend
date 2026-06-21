package com.carPooling.backend.service;

import com.carPooling.backend.dto.request.AddVehicleRequest;
import com.carPooling.backend.dto.request.CreatePreferenceRequest;
import com.carPooling.backend.dto.response.AddVehicleResponse;
import com.carPooling.backend.dto.response.CreatePreferenceResponse;
import com.carPooling.backend.dto.response.VehicleListResponse;
import jakarta.validation.Valid;

import javax.swing.*;
import java.util.List;

public interface RideService {
    CreatePreferenceResponse createPreference(CreatePreferenceRequest createPreferenceRequest);
    List<CreatePreferenceResponse> getPreferenceList();

    AddVehicleResponse  addVehicle(AddVehicleRequest addVehicleRequest);
    List<VehicleListResponse>  getVehicleListOfCurrentUser();
}
