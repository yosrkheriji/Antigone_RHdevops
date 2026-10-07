package com.antigone.rh.repository;

import com.antigone.rh.entity.DettePaiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DettePaiementRepository extends JpaRepository<DettePaiement, Long> {
    List<DettePaiement> findByDetteIdOrderByDatePaiementDesc(Long detteId);
}
