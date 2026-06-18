package com.carPooling.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {

    private String name;
    private String email;
    private String phoneNumber;
    private String gender;
    private String profilePicture;
    private LocalDate dob;
    private String collegeCompanyName;
}
