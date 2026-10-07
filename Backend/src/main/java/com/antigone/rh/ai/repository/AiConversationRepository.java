package com.antigone.rh.ai.repository;

import com.antigone.rh.ai.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Toutes les méthodes filtrent sur {@code compteId} : l'isolation est appliquée
 * dans la requête SQL elle-même, pas dans le service, pour qu'aucun chemin
 * d'appel ne puisse l'oublier.
 */
@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    @Query("""
            SELECT c FROM AiConversation c
            WHERE c.compteId = :compteId AND c.deletedAt IS NULL
            """)
    Page<AiConversation> findOwnedBy(@Param("compteId") Long compteId, Pageable pageable);

    @Query("""
            SELECT c FROM AiConversation c
            WHERE c.id = :id AND c.compteId = :compteId AND c.deletedAt IS NULL
            """)
    Optional<AiConversation> findOwnedById(@Param("id") Long id, @Param("compteId") Long compteId);
}
