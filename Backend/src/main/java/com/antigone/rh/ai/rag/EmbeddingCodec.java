package com.antigone.rh.ai.rag;

import java.util.StringJoiner;

/**
 * Sérialisation des vecteurs.
 *
 * <p>Le format {@code [0.1,0.2,...]} est à la fois du JSON valide (colonne TEXT
 * portable) et un littéral {@code vector} accepté par pgvector : une seule
 * représentation alimente donc les deux modes de recherche dense.
 */
public final class EmbeddingCodec {

    private EmbeddingCodec() {
    }

    public static String encode(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }

    public static float[] decode(String encoded) {
        if (encoded == null || encoded.length() < 2) {
            return new float[0];
        }
        String body = encoded.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    /** Similarité cosinus, bornée à [-1, 1]. Retourne 0 si un vecteur est nul. */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
