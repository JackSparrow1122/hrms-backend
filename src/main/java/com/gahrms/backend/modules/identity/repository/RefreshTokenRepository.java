package com.gahrms.backend.modules.identity.repository;

import com.gahrms.backend.modules.identity.entity.RefreshToken;
import com.gahrms.backend.modules.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
