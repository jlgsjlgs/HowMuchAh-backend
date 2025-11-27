package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.GroupMember;
import com.jlgs.howmuchah.entity.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    // Find all members for a group
    List<GroupMember> findByGroupId(UUID groupId);

    // Count members in a group
    long countByGroupId(UUID groupId);
}
