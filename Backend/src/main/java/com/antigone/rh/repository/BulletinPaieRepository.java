package com.antigone.rh.repository;

import com.antigone.rh.entity.BulletinPaie;
import com.antigone.rh.enums.StatutPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulletinPaieRepository extends JpaRepository<BulletinPaie, Long> {
    List<BulletinPaie> findByMois(String mois);

    Optional<BulletinPaie> findByEmployeIdAndMois(Long employeId, String mois);

    List<BulletinPaie> findByEmployeIdOrderByMoisDesc(Long employeId);

    List<BulletinPaie> findByMoisLessThanAndStatutNot(String mois, StatutPaie statut);
}
