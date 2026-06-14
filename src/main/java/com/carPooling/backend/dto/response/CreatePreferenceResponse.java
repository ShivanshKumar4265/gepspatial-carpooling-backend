package com.carPooling.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CreatePreferenceResponse {
    private Long id;
    private String preference_name;
}
