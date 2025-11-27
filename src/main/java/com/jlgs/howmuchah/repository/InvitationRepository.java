package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    // Find all invitations for a group
    List<Invitation> findByGroupId(UUID groupId);

    // Find all invitations for an email
    List<Invitation> findByInvitedEmail(String invitedEmail);
}
