package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.security.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("SELECT u FROM AppUser u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<AppUser> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM AppUser u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
