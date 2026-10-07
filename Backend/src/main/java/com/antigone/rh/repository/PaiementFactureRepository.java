package com.antigone.rh.repository;

import com.antigone.rh.entity.PaiementFacture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementFactureRepository extends JpaRepository<PaiementFacture, Long> {
    List<PaiementFacture> findByFactureIdOrderByDatePaiementDesc(Long factureId);
}
