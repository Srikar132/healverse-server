package com.bytehealers.healverse.repo;

import com.bytehealers.healverse.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndKey(Long userId, String key);

    @Transactional
    @Modifying
    @Query("update IdempotencyKey k set k.responseJson = :responseJson "
            + "where k.userId = :userId and k.key = :key")
    int storeResponse(@Param("userId") Long userId, @Param("key") String key,
                      @Param("responseJson") String responseJson);

    @Transactional
    @Modifying
    @Query("delete from IdempotencyKey k where k.userId = :userId and k.key = :key")
    int release(@Param("userId") Long userId, @Param("key") String key);

    @Transactional
    @Modifying
    @Query("delete from IdempotencyKey k where k.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
