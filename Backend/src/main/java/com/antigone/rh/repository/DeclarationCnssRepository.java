package com.antigone.rh.repository;

import com.antigone.rh.entity.DeclarationCnss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeclarationCnssRepository extends JpaRepository<DeclarationCnss, Long> {
    List<DeclarationCnss> findByAnneeOrderByTrimestreAsc(Integer annee);

    Optional<DeclarationCnss> findByAnneeAndTrimestre(Integer annee, Integer trimestre);
}
