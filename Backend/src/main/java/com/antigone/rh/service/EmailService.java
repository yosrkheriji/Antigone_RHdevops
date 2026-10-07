package com.antigone.rh.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

  @Value("${app.mail.from:antigone.rh.app@gmail.com}")
  private String fromEmail;

  @Value("${app.mail.from-name:Antigone RH}")
  private String fromName;

  @Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  @Value("${app.mail.brevo-api-key:}")
  private String brevoApiKey;

  @Autowired(required = false)
  private JavaMailSender mailSender;

  private final RestTemplate restTemplate = new RestTemplate();

  public void sendCredentials(String toEmail, String employeNom, String username, String password) {
    String subject = "Antigone RH - Vos identifiants de connexion";
    String htmlBody = buildCredentialsHtml(employeNom, username, password);
    sendEmail(toEmail, subject, htmlBody, employeNom);
  }

  public void sendPasswordReset(String toEmail, String employeNom, String resetLink) {
    String subject = "Antigone RH - Réinitialisation de votre mot de passe";
    String htmlBody = buildResetPasswordHtml(employeNom, resetLink);
    sendEmail(toEmail, subject, htmlBody, employeNom);
  }

  /**
   * Envoi generique d'un email HTML. Expose pour les relances clients redigees par
   * l'assistant IA, qui produisent leur propre sujet et corps.
   */
  public void sendHtml(String toEmail, String subject, String htmlBody, String recipientName) {
    sendEmail(toEmail, subject, htmlBody, recipientName);
  }

  private void sendEmail(String toEmail, String subject, String htmlBody, String recipientName) {

    if (!mailEnabled) {
      log.info("=== EMAIL (mode simulation) ===");
      log.info("À: {}", toEmail);
      log.info("Sujet: {}", subject);
      log.info("Corps HTML généré pour: {}", recipientName);
      log.info("=== FIN EMAIL ===");
      return;
    }

    if (!brevoApiKey.isBlank()) {
      sendViaBrevoApi(toEmail, subject, htmlBody, recipientName);
    } else if (mailSender != null) {
      sendViaSmtp(toEmail, subject, htmlBody, recipientName);
    } else {
      log.error("Envoi d'email désactivé de fait : app.mail.enabled=true mais ni BREVO_API_KEY ni la configuration "
          + "SMTP (spring.mail.*) ne sont renseignées. Configurez l'un des deux.");
    }
  }

  private void sendViaBrevoApi(String toEmail, String subject, String htmlBody, String recipientName) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("api-key", brevoApiKey);

      Map<String, Object> body = Map.of(
          "sender", Map.of("name", fromName, "email", fromEmail),
          "to", List.of(Map.of("email", toEmail, "name", recipientName)),
          "subject", subject,
          "htmlContent", htmlBody);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(
          "https://api.brevo.com/v3/smtp/email", request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("Email envoyé (Brevo API) à {} - {}", toEmail, subject);
      } else {
        log.error("Erreur Brevo API: {} - {}", response.getStatusCode(), response.getBody());
      }
    } catch (Exception e) {
      log.error("Erreur lors de l'envoi de l'email (Brevo API) à {}: {}", toEmail, e.getMessage());
    }
  }

  private void sendViaSmtp(String toEmail, String subject, String htmlBody, String recipientName) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromEmail, fromName);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("Email envoyé (SMTP) à {} - {}", toEmail, subject);
    } catch (Exception e) {
      log.error("Erreur lors de l'envoi de l'email (SMTP) à {}: {}", toEmail, e.getMessage());
    }
  }

  private String buildCredentialsHtml(String employeNom, String username, String password) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Antigone RH - Vos identifiants de connexion</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f8fafc; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc; padding:40px 0;">
            <tr>
              <td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border:1px solid #e2e8f0; border-radius:16px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);">
                  
                  <!-- HEADER -->
                  <tr>
                    <td style="background-color:#683b77; background:linear-gradient(135deg, #683b77 0%%, #8a4f9e 100%%); border-radius:15px 15px 0 0; padding:40px 40px; text-align:center;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td align="center">
                            <div style="width:56px; height:56px; background-color:rgba(255,255,255,0.15); border-radius:14px; display:inline-block; line-height:56px; margin-bottom:16px; text-align:center;">
                              <span style="font-size:28px; color:#ffffff; vertical-align:middle; line-height:56px;">&#128188;</span>
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td align="center">
                            <h1 style="margin:0; font-size:26px; font-weight:800; color:#ffffff; letter-spacing:-0.5px;">Antigone RH</h1>
                            <p style="margin:6px 0 0; font-size:14px; color:#f3e8ff; font-weight:500;">Gestion des Ressources Humaines</p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- BODY -->
                  <tr>
                    <td style="padding:40px 40px 30px; background-color:#ffffff;">
                      <h2 style="margin:0 0 12px; font-size:20px; font-weight:700; color:#0f172a;">
                        Bonjour %s 👋
                      </h2>
                      <p style="margin:0 0 24px; font-size:15px; color:#475569; line-height:1.6;">
                        Votre compte a été créé avec succès sur la plateforme <strong style="color:#683b77;">Antigone RH</strong>. Vous trouverez ci-dessous vos identifiants pour vous connecter.
                      </p>

                      <!-- Credentials Card -->
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#faf8fc; border:1px solid #f3ebf7; border-radius:12px; overflow:hidden;">
                        <tr>
                          <td style="padding:20px 24px 16px;">
                            <p style="margin:0 0 6px; font-size:12px; font-weight:700; color:#683b77; text-transform:uppercase; letter-spacing:1px;">Nom d'utilisateur</p>
                            <p style="margin:0; font-size:18px; font-weight:700; color:#0f172a; font-family:'Courier New',Courier,monospace; letter-spacing:0.5px;">%s</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:0 24px;">
                            <div style="height:1px; background-color:#f0e6f5;"></div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 24px 20px;">
                            <p style="margin:0 0 6px; font-size:12px; font-weight:700; color:#683b77; text-transform:uppercase; letter-spacing:1px;">Mot de passe provisoire</p>
                            <p style="margin:0; font-size:18px; font-weight:700; color:#0f172a; font-family:'Courier New',Courier,monospace; letter-spacing:0.5px;">%s</p>
                          </td>
                        </tr>
                      </table>

                      <!-- Warning -->
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:24px;">
                        <tr>
                          <td style="background-color:#fffbeb; border-left:4px solid #f59e0b; border-radius:4px; padding:14px 18px;">
                            <p style="margin:0; font-size:13px; color:#b45309; line-height:1.5; font-weight:500;">
                              &#9888;&#65039; <strong>Important :</strong> Pour des raisons de sécurité, veuillez modifier votre mot de passe dès votre première connexion.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- FOOTER -->
                  <tr>
                    <td style="background-color:#f8fafc; border-top:1px solid #f1f5f9; border-radius:0 0 15px 15px; padding:32px 40px; text-align:center;">
                      <p style="margin:0 0 8px; font-size:12px; color:#64748b; line-height:1.5;">
                        Cet email a été envoyé automatiquement par la plateforme
                      </p>
                      <p style="margin:0 0 16px; font-size:14px; font-weight:700; color:#683b77;">
                        Antigone RH
                      </p>
                      <div style="height:1px; background-color:#e2e8f0; margin:0 40px 16px;"></div>
                      <p style="margin:0; font-size:11px; color:#94a3b8;">
                        &copy; 2026 Antigone RH. Tous droits réservés.
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
        .formatted(employeNom, username, password);
  }

  private String buildResetPasswordHtml(String employeNom, String resetLink) {
    return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Antigone RH - Réinitialisation du mot de passe</title>
        </head>
        <body style="margin:0; padding:0; background-color:#f8fafc; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc; padding:40px 0;">
            <tr>
              <td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border:1px solid #e2e8f0; border-radius:16px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);">
                  
                  <!-- HEADER -->
                  <tr>
                    <td style="background-color:#683b77; background:linear-gradient(135deg, #683b77 0%%, #8a4f9e 100%%); border-radius:15px 15px 0 0; padding:40px 40px; text-align:center;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td align="center">
                            <div style="width:56px; height:56px; background-color:rgba(255,255,255,0.15); border-radius:14px; display:inline-block; line-height:56px; margin-bottom:16px; text-align:center;">
                              <span style="font-size:28px; color:#ffffff; vertical-align:middle; line-height:56px;">&#128274;</span>
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td align="center">
                            <h1 style="margin:0; font-size:26px; font-weight:800; color:#ffffff; letter-spacing:-0.5px;">Antigone RH</h1>
                            <p style="margin:6px 0 0; font-size:14px; color:#f3e8ff; font-weight:500;">Réinitialisation du mot de passe</p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- BODY -->
                  <tr>
                    <td style="padding:40px 40px 30px; background-color:#ffffff;">
                      <h2 style="margin:0 0 12px; font-size:20px; font-weight:700; color:#0f172a;">
                        Bonjour %s 👋
                      </h2>
                      <p style="margin:0 0 24px; font-size:15px; color:#475569; line-height:1.6;">
                        Vous avez demandé la réinitialisation de votre mot de passe sur la plateforme <strong style="color:#683b77;">Antigone RH</strong>. Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe.
                      </p>

                      <!-- CTA Button -->
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin:28px 0;">
                        <tr>
                          <td align="center">
                            <a href="%s" style="display:inline-block; padding:14px 36px; background-color:#683b77; background:linear-gradient(135deg, #683b77 0%%, #8a4f9e 100%%); color:#ffffff !important; font-size:15px; font-weight:700; text-decoration:none; border-radius:10px; box-shadow:0 4px 12px rgba(104,59,119,0.3); border:1px solid #683b77;">
                              Réinitialiser mon mot de passe
                            </a>
                          </td>
                        </tr>
                      </table>

                      <!-- Warning -->
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:24px;">
                        <tr>
                          <td style="background-color:#fffbeb; border-left:4px solid #f59e0b; border-radius:4px; padding:14px 18px;">
                            <p style="margin:0; font-size:13px; color:#b45309; line-height:1.5; font-weight:500;">
                              &#9888;&#65039; Ce lien est valable pendant <strong>30 minutes</strong>. Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email en toute sécurité.
                            </p>
                          </td>
                        </tr>
                      </table>

                      <!-- Fallback link -->
                      <p style="margin:28px 0 0; font-size:12px; color:#94a3b8; line-height:1.6;">
                        Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :<br/>
                        <a href="%s" style="color:#683b77; text-decoration:underline; word-break:break-all;">%s</a>
                      </p>
                    </td>
                  </tr>

                  <!-- FOOTER -->
                  <tr>
                    <td style="background-color:#f8fafc; border-top:1px solid #f1f5f9; border-radius:0 0 15px 15px; padding:32px 40px; text-align:center;">
                      <p style="margin:0 0 8px; font-size:12px; color:#64748b; line-height:1.5;">
                        Cet email a été envoyé automatiquement par la plateforme
                      </p>
                      <p style="margin:0 0 16px; font-size:14px; font-weight:700; color:#683b77;">
                        Antigone RH
                      </p>
                      <div style="height:1px; background-color:#e2e8f0; margin:0 40px 16px;"></div>
                      <p style="margin:0; font-size:11px; color:#94a3b8;">
                        &copy; 2026 Antigone RH. Tous droits réservés.
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
        .formatted(employeNom, resetLink, resetLink, resetLink);
  }
}
