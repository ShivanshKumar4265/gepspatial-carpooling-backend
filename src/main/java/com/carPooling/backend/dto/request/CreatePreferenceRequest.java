package com.carPooling.backend.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor  // THIS IS MANDATORY FOR RESPONSE/REQUEST, REQUEST ESPECIALLY
public class CreatePreferenceRequest {
    @NotBlank(message = "Preference Type cant be blank" )
    @NotNull(message = "Preference Name cant be null")
    @JsonProperty("preference_name")
    private String preferenceName;


    @Override
    public String toString() {
        return "CreatePreferenceRequest{" +
                "preference_name='" + preferenceName + '\'' +
                '}';
    }
}
