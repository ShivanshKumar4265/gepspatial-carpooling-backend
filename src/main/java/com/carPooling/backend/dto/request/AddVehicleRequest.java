package com.carPooling.backend.dto.request;


import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddVehicleRequest {

    @NotBlank(message =  "Enter valid vehicle number")
    @Pattern(
            regexp = "^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$",
            message = "Invalid vehicle number format. Example: UP16AB1234"
    )
    private String vehicleNumber;

    // tyep will be handled by enum at the client side, so no need to validate it here, but we can validate it at the server side if needed

    @NotBlank(message =  "Enter valid vehicle type")
    private String vehicleType;

    // model will be handled by enum at the client side, so no need to validate it here, but we can validate it at the server side if needed
    @NotBlank(message =  "Enter valid vehicle model")
    private String vehicleModel;


    private String color;

    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;
}
