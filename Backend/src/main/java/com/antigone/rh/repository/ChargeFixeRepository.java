package com.antigone.rh.repository;

import com.antigone.rh.entity.ChargeFixe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeFixeRepository extends JpaRepository<ChargeFixe, Long> {
    List<ChargeFixe> findAllByArchivedFalse();
}
