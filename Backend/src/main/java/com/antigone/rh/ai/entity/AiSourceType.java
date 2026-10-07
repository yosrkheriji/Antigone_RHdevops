package com.antigone.rh.ai.entity;

/** Origine métier d'un chunk indexé pour le RAG. */
public enum AiSourceType {
    /** Identité / activité / positionnement / objectifs d'un client (marque). */
    BRAND,
    /** Projet et actions prévues sur la période. */
    PROJECT,
    /** Ligne de media plan passée — sert la continuité éditoriale. */
    MEDIA_PLAN,
    /** Contenu effectivement publié. */
    CONTENT,
    /** Article du règlement intérieur — document transverse, non rattaché à un client. */
    POLICY
}
