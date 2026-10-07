package com.antigone.rh.bi.dto;

import java.util.List;

/**
 * Tableau de bord « Finance et tresorerie ».
 *
 * <p>Les montants « produits / charges / resultat » sont en base ENGAGEMENT
 * (comptable), les montants « encaissements / decaissements » en base
 * TRESORERIE (cash). Melanger les deux compterait deux fois le meme dinar.
 */
public record FinanceDashboardDTO(
        Kpis kpis,
        List<PointMensuel> evolutionMensuelle,
        List<RepartitionCharge> repartitionCharges,
        List<LigneClient> topClients,
        List<TrancheAge> balanceAgee,
        List<FactureEnRetard> facturesEnRetard) {

    public record Kpis(
            double produitsHt,
            double chargesTotales,
            double masseSalariale,
            double resultat,
            double margePourcent,
            double encaissements,
            double decaissements,
            double tresorerieNette,
            double creancesEnCours,
            double creancesEnRetard) {
    }

    public record PointMensuel(
            String mois,
            String libelle,
            double produits,
            double charges,
            double resultat,
            double encaissements,
            double decaissements) {
    }

    public record RepartitionCharge(String nature, double montant) {
    }

    public record LigneClient(
            String client,
            double caHt,
            long nombreFactures,
            double resteDu) {
    }

    public record TrancheAge(String tranche, double montant, long nombreFactures) {
    }

    public record FactureEnRetard(
            String numero,
            String client,
            String dateEcheance,
            double resteDu,
            long joursRetard) {
    }
}
