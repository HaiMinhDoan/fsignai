package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.notification.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findByKey(String key);

    boolean existsByKey(String key);

    List<SystemSetting> findAllByOrderByKeyAsc();
}
