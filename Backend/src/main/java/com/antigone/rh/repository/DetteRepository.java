package com.antigone.rh.repository;

import com.antigone.rh.entity.Dette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetteRepository extends JpaRepository<Dette, Long> {
    List<Dette> findAllByOrderByDateEcheanceAsc();
}
