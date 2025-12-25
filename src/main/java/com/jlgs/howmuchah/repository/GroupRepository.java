package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Group;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // Find all groups that user belongs to
    @Query("SELECT DISTINCT g FROM Group g " +
            "LEFT JOIN FETCH g.owner " +
            "LEFT JOIN GroupMember gm ON g.id = gm.group.id " +
            "WHERE g.owner.id = :userId OR gm.user.id = :userId")
    List<Group> findAllGroupsForUser(@Param("userId") UUID userId);

    // Single group fetch with owner
    @Query("SELECT g FROM Group g " +
            "LEFT JOIN FETCH g.owner " +
            "WHERE g.id = :groupId")
    Optional<Group> findByIdWithOwner(@Param("groupId") UUID groupId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Group g WHERE g.id = :groupId")
    Optional<Group> findByIdWithLock(@Param("groupId") UUID groupId);
}
