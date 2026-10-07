package com.antigone.rh.repository;

import com.antigone.rh.entity.ChargeVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeVariableRepository extends JpaRepository<ChargeVariable, Long> {
    List<ChargeVariable> findByMoisOrderByDateDesc(String mois);
}
