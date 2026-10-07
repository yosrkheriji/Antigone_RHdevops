package com.antigone.rh.repository;

import com.antigone.rh.entity.BaremeIrpp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BaremeIrppRepository extends JpaRepository<BaremeIrpp, Long> {
    List<BaremeIrpp> findAllByOrderByEffectiveFromDesc();

    Optional<BaremeIrpp> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate mois);
}
