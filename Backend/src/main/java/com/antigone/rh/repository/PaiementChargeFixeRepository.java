package com.antigone.rh.repository;

import com.antigone.rh.entity.PaiementChargeFixe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementChargeFixeRepository extends JpaRepository<PaiementChargeFixe, Long> {
    List<PaiementChargeFixe> findByChargeFixeIdOrderByMoisDesc(Long chargeFixeId);

    List<PaiementChargeFixe> findByMois(String mois);
}
