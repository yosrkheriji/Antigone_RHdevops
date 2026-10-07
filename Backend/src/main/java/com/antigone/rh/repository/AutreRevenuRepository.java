package com.antigone.rh.repository;

import com.antigone.rh.entity.AutreRevenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutreRevenuRepository extends JpaRepository<AutreRevenu, Long> {
    List<AutreRevenu> findByMoisOrderByDateDesc(String mois);
}
