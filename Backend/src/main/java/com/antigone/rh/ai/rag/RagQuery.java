package com.antigone.rh.ai.rag;

import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.security.AiAccessScope.ClientScope;

import java.util.Set;

/**
 * Requête de recherche hybride.
 *
 * @param text        texte libre de l'utilisateur
 * @param scope       périmètre client autorisé — appliqué en SQL, jamais en post-filtrage
 * @param sourceTypes types de sources à interroger ; vide = toutes
 * @param topK        nombre de chunks souhaités après fusion ; null = valeur configurée
 */
public record RagQuery(String text, ClientScope scope, Set<AiSourceType> sourceTypes, Integer topK) {

    public static RagQuery of(String text, ClientScope scope, Set<AiSourceType> sourceTypes) {
        return new RagQuery(text, scope, sourceTypes, null);
    }
}
