package com.antigone.rh.repository;

import com.antigone.rh.entity.AcompteSalaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcompteSalaireRepository extends JpaRepository<AcompteSalaire, Long> {
    List<AcompteSalaire> findByEmployeIdAndMoisOrderByDateAsc(Long employeId, String mois);
}
