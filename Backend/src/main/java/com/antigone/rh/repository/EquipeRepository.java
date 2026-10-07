package com.antigone.rh.repository;

import com.antigone.rh.entity.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    List<Equipe> findByProjetId(Long projetId);

    /**
     * Retire un employé de toutes les équipes sans charger les membres (évite les
     * proxies fantômes).
     */
    @Modifying
    @Query(value = "DELETE FROM equipe_employes WHERE employe_id = :employeId", nativeQuery = true)
    void removeEmployeFromAllEquipes(@Param("employeId") Long employeId);
}
