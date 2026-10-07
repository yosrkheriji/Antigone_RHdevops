package com.antigone.rh.repository;

import com.antigone.rh.entity.Facture;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    List<Facture> findByTypeOrderByDateEmissionDesc(TypeDocument type);

    List<Facture> findByClientIdOrderByDateEmissionDesc(Long clientId);

    boolean existsByClientId(Long clientId);

    List<Facture> findByTypeAndStatutNot(TypeDocument type, StatutFacture statut);

    @Query("SELECT f FROM Facture f WHERE f.type = :type AND f.dateEmission BETWEEN :debut AND :fin")
    List<Facture> findByTypeAndPeriode(@Param("type") TypeDocument type,
            @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /**
     * Charge la facture avec son client en une requete.
     *
     * <p>Necessaire aux outils de l'assistant IA : ils s'executent sur un thread de
     * generation, hors requete HTTP, donc sans session Hibernate ouverte. Un acces
     * paresseux a {@code facture.client} y leverait une LazyInitializationException.
     */
    @Query("SELECT f FROM Facture f LEFT JOIN FETCH f.client WHERE f.id = :id")
    Optional<Facture> findByIdWithClient(@Param("id") Long id);

    /** Meme raison : factures non soldees, clients deja charges. */
    @Query("SELECT f FROM Facture f LEFT JOIN FETCH f.client "
            + "WHERE f.type = :type AND f.statut <> :statut")
    List<Facture> findUnpaidWithClient(@Param("type") TypeDocument type,
            @Param("statut") StatutFacture statut);
}
