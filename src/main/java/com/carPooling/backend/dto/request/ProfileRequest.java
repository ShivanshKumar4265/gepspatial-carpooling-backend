package com.carPooling.backend.dto.request;

import com.carPooling.backend.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest{

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100,
            message = "Name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100,
            message = "Email cannot exceed 100 characters")
    private String email;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @NotNull(message = "Gender is required")
    private Gender gender;

//    @NotBlank(message = "Profile picture URL is required")
//    @Pattern(
//            regexp = "^(http|https)://.*$",
//            message = "Profile picture must be a valid URL"
//    )
    private String profilePicture;

//    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be exactly 10 digits"
    )
    private String phoneNumber;

    @Size(max = 150,
            message = "College/Company name cannot exceed 150 characters")
    private String collegeCompanyName;

    @NotBlank(message = "Emergency contact name is required")
    @Size(min = 3, max = 100,
            message = "Emergency contact name must be between 3 and 100 characters")
    private String emergencyContactName;

//    @NotBlank(message = "Emergency contact number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Emergency contact number must be exactly 10 digits"
    )
    private String emergencyContactNumber;
}