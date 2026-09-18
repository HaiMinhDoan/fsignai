package com.sunmoon.backend.repository;

import com.sunmoon.backend.entity.auth.UserRole;
import com.sunmoon.backend.entity.auth.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByUserId(UUID userId);

    // Chi lay ma vai tro - JWT khong can ca doi tuong Role
    @Query("SELECT ur.role.code FROM UserRole ur WHERE ur.user.id = :userId")
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);
}
