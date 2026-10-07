package com.antigone.rh.ai.rag;

import com.antigone.rh.ai.entity.AiDocumentChunk;

/**
 * Chunk retenu après fusion, avec la trace de sa provenance.
 *
 * <p>{@code fromDense} / {@code fromSparse} rendent la fusion observable : c'est ce
 * qui permet de démontrer, sur une requête mixte, que les deux branches ont bien
 * contribué au contexte final.
 */
public record RagHit(AiDocumentChunk chunk,
                     double fusedScore,
                     Integer denseRank,
                     Integer sparseRank) {

    public boolean fromDense() {
        return denseRank != null;
    }

    public boolean fromSparse() {
        return sparseRank != null;
    }

    public String provenance() {
        if (fromDense() && fromSparse()) {
            return "dense+lexical";
        }
        return fromDense() ? "dense" : "lexical";
    }
}
