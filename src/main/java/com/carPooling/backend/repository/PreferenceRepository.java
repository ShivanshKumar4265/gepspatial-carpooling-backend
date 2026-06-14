package com.carPooling.backend.repository;

import com.carPooling.backend.entity.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {
    //Prefix + By + FieldName
    boolean existsByPreferenceName(String preferenceName);
}
