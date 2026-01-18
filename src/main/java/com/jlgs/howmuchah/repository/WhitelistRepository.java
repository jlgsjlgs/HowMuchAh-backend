package com.jlgs.howmuchah.repository;

import com.jlgs.howmuchah.entity.Whitelist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhitelistRepository extends JpaRepository<Whitelist, String> {

    // Check to see if email is already whitelisted
    boolean existsByEmail(String email);
}
