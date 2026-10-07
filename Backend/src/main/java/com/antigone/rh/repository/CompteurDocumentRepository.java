package com.antigone.rh.repository;

import com.antigone.rh.entity.CompteurDocument;
import com.antigone.rh.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompteurDocumentRepository extends JpaRepository<CompteurDocument, Long> {
    Optional<CompteurDocument> findByTypeAndAnnee(TypeDocument type, Integer annee);
}
