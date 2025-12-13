package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.GroupMember;
import com.jlgs.howmuchah.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    // Find all members for a group
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") UUID groupId);

    // Count members in a group
    long countByGroupId(UUID groupId);

    // Check if user is already a member of a group
    @Query("SELECT CASE WHEN COUNT(gm) > 0 THEN true ELSE false END " +
            "FROM GroupMember gm " +
            "WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    boolean existsByGroupIdAndUserId(@Param("groupId") UUID groupId,
                                     @Param("userId") UUID userId);
}
