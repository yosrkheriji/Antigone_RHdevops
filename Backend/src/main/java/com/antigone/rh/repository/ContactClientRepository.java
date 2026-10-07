package com.antigone.rh.repository;

import com.antigone.rh.entity.ContactClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactClientRepository extends JpaRepository<ContactClient, Long> {
    List<ContactClient> findByClientId(Long clientId);
}
