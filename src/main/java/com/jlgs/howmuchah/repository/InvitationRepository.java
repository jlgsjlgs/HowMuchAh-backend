package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Invitation;
import com.jlgs.howmuchah.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    // Find all invitations for a group
    List<Invitation> findByGroupId(UUID groupId);

    // Fetch invitations with group and invitedBy eagerly loaded
    @Query("SELECT i FROM Invitation i " +
            "JOIN FETCH i.group " +
            "JOIN FETCH i.invitedBy " +
            "WHERE i.group.id = :groupId")
    List<Invitation> findByGroupIdWithDetails(@Param("groupId") UUID groupId);

    // Single invitation with details
    @Query("SELECT i FROM Invitation i " +
            "JOIN FETCH i.group " +
            "JOIN FETCH i.invitedBy " +
            "WHERE i.id = :invitationId")
    Optional<Invitation> findByIdWithDetails(@Param("invitationId") UUID invitationId);

    // Find pending invitations for an email
    @Query("SELECT i FROM Invitation i " +
            "JOIN FETCH i.group " +
            "JOIN FETCH i.invitedBy " +
            "WHERE i.invitedEmail = :email " +
            "AND i.status = :status")
    List<Invitation> findByInvitedEmailAndStatus(
            @Param("email") String email,
            @Param("status") InvitationStatus status
    );

    // Find invitation by group and email (for duplicate checking)
    Optional<Invitation> findByGroup_IdAndInvitedEmail(UUID groupId, String email);
}
