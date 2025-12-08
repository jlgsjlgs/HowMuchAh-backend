package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // Find all groups that user belongs to
    @Query("SELECT DISTINCT g FROM Group g " +
            "LEFT JOIN GroupMember gm ON g.id = gm.group.id " +
            "WHERE g.owner.id = :userId OR gm.user.id = :userId")
    List<Group> findAllGroupsForUser(@Param("userId") UUID userId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);
}
