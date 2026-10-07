package com.antigone.rh.repository;

import com.antigone.rh.entity.ParametresPaie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametresPaieRepository extends JpaRepository<ParametresPaie, Long> {
}
