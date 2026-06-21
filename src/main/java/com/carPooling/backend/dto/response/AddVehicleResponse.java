package com.carPooling.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddVehicleResponse {
    private Long vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private String vehicleModel;
    private String color;
    private int totalSeats;

    private OwnerResponse owner;
}

