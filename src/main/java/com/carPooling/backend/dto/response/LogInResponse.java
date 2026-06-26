package com.carPooling.backend.dto.response;

import com.carPooling.backend.dto.BaseAuthResponse;
import com.carPooling.backend.entity.User;
import com.carPooling.backend.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogInResponse extends BaseAuthResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePicture;
    private LocalDate dob;
    private Gender gender;
    private boolean isPhoneVerified;
    private String collegeCompanyName;
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String city;

//    private List<VehicleListResponse> vehicles;
}
