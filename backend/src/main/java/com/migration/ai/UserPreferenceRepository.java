package com.migration.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByPreferenceTypeAndPreferenceKey(String type, String key);

    List<UserPreference> findByPreferenceTypeOrderByFrequencyDesc(String type);

    List<UserPreference> findAllByOrderByFrequencyDesc();
}
