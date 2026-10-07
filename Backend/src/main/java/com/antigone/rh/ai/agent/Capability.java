package com.antigone.rh.ai.agent;

/**
 * Capacite de l'assistant vers laquelle une demande est routee.
 *
 * <p>L'assistant reste unique cote utilisateur : le routage sert a choisir le
 * prompt systeme et le sous-ensemble d'outils pertinent, pas a exposer trois
 * chatbots distincts.
 */
public enum Capability {

    /** Generation ou ajustement d'un media plan mensuel. */
    MEDIA_PLAN,

    /** Redaction d'une relance de facture impayee. */
    REMINDER,

    /** Explication d'un bulletin de paie. */
    PAYSLIP,

    /** Questions RH, reglement interieur, tout le reste. */
    GENERAL
}
