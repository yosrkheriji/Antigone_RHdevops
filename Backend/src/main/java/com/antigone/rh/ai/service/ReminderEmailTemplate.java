package com.antigone.rh.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gabarit HTML de l'email de relance.
 *
 * <p>Reprend la charte des emails deja envoyes par l'application (identifiants,
 * reinitialisation de mot de passe) : meme violet {@code #683b77}, meme structure
 * en tableaux, memes rayons. Un client qui recoit une relance doit reconnaitre
 * Antigone, pas decouvrir un second style.
 *
 * <p>Personnalisation : si le client destinataire a un logo enregistre dans sa
 * fiche, il apparait sous l'en-tete avec son nom — l'email reste celui d'Antigone,
 * mais affiche a qui il s'adresse. Le logo est servi par l'endpoint public existant
 * ({@code /api/clients/{id}/logo}), en URL absolue : les clients mail ne portent
 * aucune authentification pour charger une image distante.
 *
 * <p>Mise en page en tableaux et styles en ligne : c'est la seule forme que les
 * clients mail rendent de maniere fiable — Outlook ignore une bonne part du CSS
 * moderne, et les feuilles de style externes sont couramment retirees.
 */
@Component
public class ReminderEmailTemplate {

    private static final String BRAND = "#683b77";
    private static final String BRAND_DARK = "#562f64";
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.public-backend-url}")
    private String publicBackendUrl;

    /** Couleur d'accent du bandeau, graduee selon la fermete du rappel. */
    private static String accent(InvoiceLateInfo.Tone tone) {
        return switch (tone) {
            case SOFT -> BRAND;
            case FIRM -> "#c4320a";
            case FORMAL -> "#b42318";
        };
    }

    private static String toneLabel(InvoiceLateInfo.Tone tone) {
        return switch (tone) {
            case SOFT -> "Rappel";
            case FIRM -> "Relance";
            case FORMAL -> "Relance formelle";
        };
    }

    /**
     * Compose l'email complet.
     *
     * @param body corps redige par l'assistant, en texte brut
     */
    public String render(InvoiceLateInfo info, String body) {
        String accent = accent(info.tone());
        String safeBody = escape(body).replace("\n", "<br/>");
        String echeance = info.dateEcheance() == null ? "—" : info.dateEcheance().format(DATE_FR);
        String retard = info.joursDeRetard() > 0
                ? info.joursDeRetard() + " jour" + (info.joursDeRetard() > 1 ? "s" : "")
                : "Échéance du jour";
        String personalisation = personalizationRow(info);

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s — facture %s</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f5f4f1; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f4f1; padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border:1px solid #e8e6e0; border-radius:16px; box-shadow:0 4px 16px rgba(0,0,0,0.05);">

                          <!-- EN-TETE -->
                          <tr>
                            <td style="background-color:%s; background:linear-gradient(135deg, %s 0%%, %s 100%%); border-radius:15px 15px 0 0; padding:32px 40px; text-align:center;">
                              <h1 style="margin:0; font-size:24px; font-weight:800; color:#ffffff; letter-spacing:-0.5px;">Antigone</h1>
                              <p style="margin:6px 0 0; font-size:13px; color:#f3e8ff; font-weight:500;">%s — facture %s</p>
                            </td>
                          </tr>

                          <!-- PERSONNALISATION -->
                          %s

                          <!-- RECAPITULATIF -->
                          <tr>
                            <td style="padding:28px 40px 0;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#faf8fc; border:1px solid #f3ebf7; border-radius:12px;">
                                <tr>
                                  <td style="padding:16px 20px; width:50%%; border-right:1px solid #f0e6f5;">
                                    <p style="margin:0 0 4px; font-size:11px; font-weight:700; color:%s; text-transform:uppercase; letter-spacing:1px;">Montant dû</p>
                                    <p style="margin:0; font-size:20px; font-weight:800; color:#1a1814;">%s DT</p>
                                  </td>
                                  <td style="padding:16px 20px; width:50%%;">
                                    <p style="margin:0 0 4px; font-size:11px; font-weight:700; color:%s; text-transform:uppercase; letter-spacing:1px;">Échéance</p>
                                    <p style="margin:0; font-size:15px; font-weight:700; color:#1a1814;">%s</p>
                                    <p style="margin:2px 0 0; font-size:12px; color:#5c5a55;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- MESSAGE -->
                          <tr>
                            <td style="padding:24px 40px 32px; font-size:15px; color:#1a1814; line-height:1.7;">
                              %s
                            </td>
                          </tr>

                          <!-- PIED -->
                          <tr>
                            <td style="background-color:#faf9f7; border-top:1px solid #f1f0ec; border-radius:0 0 15px 15px; padding:24px 40px; text-align:center;">
                              <p style="margin:0 0 6px; font-size:12px; color:#5c5a55;">
                                Une question sur cette facture ? Répondez simplement à cet e-mail.
                              </p>
                              <p style="margin:0 0 12px; font-size:14px; font-weight:700; color:%s;">Antigone</p>
                              <div style="height:1px; background-color:#e8e6e0; margin:0 40px 12px;"></div>
                              <p style="margin:0; font-size:11px; color:#9c9a94;">
                                &copy; %d Antigone. Tous droits réservés.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .formatted(
                        toneLabel(info.tone()), escape(info.numero()),
                        accent, accent, BRAND_DARK,
                        toneLabel(info.tone()), escape(info.numero()),
                        personalisation,
                        accent, format(info.resteDu()),
                        accent, echeance, retard,
                        safeBody,
                        BRAND,
                        LocalDate.now().getYear());
    }

    /**
     * URL absolue du logo du client destinataire, ou {@code null} s'il n'en a pas.
     * Partagee avec le DTO rendu au widget : l'apercu affiche avant envoi doit
     * montrer le meme logo que l'email reellement expedie.
     */
    public String clientLogoUrl(InvoiceLateInfo info) {
        if (!info.clientHasLogo() || info.clientId() == null) {
            return null;
        }
        return publicBackendUrl.replaceAll("/+$", "") + "/api/clients/" + info.clientId() + "/logo";
    }

    /**
     * Bandeau « adresse a » sous l'en-tete : logo du client (s'il en a un) et son
     * nom. Vide si le client n'a pas de nom connu — rien a personnaliser, mieux
     * vaut ne rien afficher qu'un espace vide.
     */
    private String personalizationRow(InvoiceLateInfo info) {
        String name = escape(info.clientNom());
        if (name.isBlank()) {
            return "";
        }
        String logoUrl = clientLogoUrl(info);
        String avatar = logoUrl != null
                ? """
                  <img src="%s" alt="%s" width="32" height="32" \
                  style="display:inline-block; vertical-align:middle; margin-right:10px; \
                  border-radius:8px; border:1px solid #f0e6f5; object-fit:cover;">\
                  """.formatted(logoUrl, name)
                : "";
        return """
                <tr>
                  <td style="padding:20px 40px 0; text-align:center;">
                    <span style="display:inline-block; background-color:#faf8fc; border:1px solid #f0e6f5; border-radius:999px; padding:8px 18px 8px 10px;">
                      %s<span style="font-size:13px; font-weight:700; color:#3a2a40; vertical-align:middle;">%s</span>
                    </span>
                  </td>
                </tr>
                """.formatted(avatar, name);
    }

    private static String format(double amount) {
        return String.format(Locale.FRANCE, "%,.3f", amount).replace(' ', ' ');
    }

    /** Le corps vient du modele : il est echappe avant toute insertion dans le HTML. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
