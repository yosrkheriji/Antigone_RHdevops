import React from 'react';
import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AiChatClient } from '../api/client';
import { AiClientProvider } from '../hooks/useAiClient';
import { StructuredResultView } from '../components/results';
import { PayslipResult } from '../components/results/PayslipResult';
import { ReminderResult } from '../components/results/ReminderResult';
import type { PayslipResultPayload, ReminderResultPayload } from '../api/types';

/**
 * Payloads repris tels quels de `API_DOCUMENTATION.md` (§6 et §7) : tester avec
 * des donnees inventees validerait un contrat que le backend ne respecte pas.
 */
const REMINDER: ReminderResultPayload = {
  reminderId: 77,
  invoiceId: 314,
  invoiceNumero: 'FAC-2026-0314',
  clientNom: 'Alpha',
  clientEmail: 'compta@alpha.tn',
  subject: 'Rappel — facture FAC-2026-0314 échue le 12/07/2026',
  body: "Madame,\n\nSauf erreur de notre part, la facture FAC-2026-0314 d'un montant de 3 000,000 DT...\n\nL'équipe Antigone",
  tone: 'FORMAL',
  daysLate: 45,
  amountDue: 3000.0,
  sent: false,
};

const PAYSLIP: PayslipResultPayload = {
  explanation:
    'Votre net à payer passe de 2 310,000 DT en juin à 2 130,000 DT en juillet, soit 180,000 DT de moins.',
  comparison: {
    previousNet: 2310.0,
    currentNet: 2130.0,
    delta: -180.0,
    deltaReasons: [
      'Acompte de 150,000 DT déduit du net à payer',
      'IRPP mensuel passé de 240,000 à 300,000 DT',
    ],
  },
};

/**
 * ReminderResult declenche l'envoi lui-meme : il lui faut le client du widget.
 * Le rendre nu masquerait precisement le chemin que ces tests doivent couvrir.
 */
function renderWithClient(node: React.ReactElement, client?: Partial<AiChatClient>) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const aiClient = Object.assign(
    new AiChatClient({ baseUrl: 'http://localhost:8080', getAuthToken: () => 'jeton' }),
    client,
  );
  return render(
    <AiClientProvider client={aiClient} capability="REMINDER" queryClient={queryClient}>
      {node}
    </AiClientProvider>,
  );
}

describe('ReminderResult', () => {
  it('affiche le numero de facture, le retard et le reste du', () => {
    renderWithClient(<ReminderResult result={REMINDER} />);

    // Le numero apparait dans le titre, dans l'objet et dans le corps : on vise
    // explicitement le titre de la carte.
    expect(screen.getByText('Relance — FAC-2026-0314')).toBeInTheDocument();
    expect(screen.getByText('45 j')).toBeInTheDocument();
    expect(screen.getByText('3 000,000 DT')).toBeInTheDocument();
  });

  it('signale le palier de ton', () => {
    renderWithClient(<ReminderResult result={REMINDER} />);

    expect(screen.getByText('Ton formel')).toBeInTheDocument();
  });

  it('propose l’envoi et invite a relire avant', () => {
    renderWithClient(<ReminderResult result={REMINDER} />);

    expect(screen.getByRole('button', { name: /Envoyer au client/ })).toBeInTheDocument();
    expect(screen.getByText(/Relisez le message avant de l’envoyer/)).toBeInTheDocument();
  });

  it('expedie le brouillon et confirme l’envoi', async () => {
    const sendReminder = vi.fn().mockResolvedValue({ ...REMINDER, sent: true });
    renderWithClient(<ReminderResult result={REMINDER} />, { sendReminder });

    await userEvent.click(screen.getByRole('button', { name: /Envoyer au client/ }));

    // C'est la reference du brouillon persiste qui part, pas une regeneration.
    await waitFor(() => expect(sendReminder).toHaveBeenCalledWith(77));
    await waitFor(() => expect(screen.getByText('Envoyé')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: /Envoyer au client/ })).not.toBeInTheDocument();
  });

  it('affiche l’echec d’envoi sans perdre le brouillon', async () => {
    const sendReminder = vi.fn().mockRejectedValue(new Error('reseau'));
    renderWithClient(<ReminderResult result={REMINDER} />, { sendReminder });

    await userEvent.click(screen.getByRole('button', { name: /Envoyer au client/ }));

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    // Le texte reste a l'ecran : l'utilisateur peut reessayer ou le copier.
    expect(screen.getByText(/Sauf erreur de notre part/)).toBeInTheDocument();
  });

  it('n’offre pas l’envoi sans adresse e-mail connue', () => {
    renderWithClient(<ReminderResult result={{ ...REMINDER, clientEmail: null }} />);

    expect(screen.queryByRole('button', { name: /Envoyer au client/ })).not.toBeInTheDocument();
    expect(screen.getByText(/Aucune adresse e-mail n’est enregistrée/)).toBeInTheDocument();
  });

  it('propose de copier et d’ouvrir le client mail', () => {
    renderWithClient(<ReminderResult result={REMINDER} />);

    expect(screen.getByRole('button', { name: /copier le message/i })).toBeInTheDocument();

    const mailto = screen.getByRole('link', { name: /client mail/i });
    expect(mailto).toHaveAttribute('href', expect.stringContaining('mailto:compta%40alpha.tn'));
  });

  it('affiche le badge Envoyé uniquement quand le backend le confirme', () => {
    const { rerender } = renderWithClient(<ReminderResult result={REMINDER} />);
    expect(screen.queryByText('Envoyé')).not.toBeInTheDocument();

    rerender(
      <AiClientProvider
        client={new AiChatClient({ baseUrl: 'http://localhost:8080', getAuthToken: () => 'jeton' })}
        capability="REMINDER"
        queryClient={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      >
        <ReminderResult result={{ ...REMINDER, sent: true }} />
      </AiClientProvider>,
    );
    expect(screen.getByText('Envoyé')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Envoyer au client/ })).not.toBeInTheDocument();
  });

  it('signale une adresse manquante plutot que d’afficher un lien inerte', () => {
    renderWithClient(<ReminderResult result={{ ...REMINDER, clientEmail: null }} />);

    expect(screen.queryByRole('link', { name: /client mail/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Aucune adresse e-mail n’est enregistrée/)).toBeInTheDocument();
  });

  it('affiche le logo du client destinataire quand l’email en propose un', () => {
    renderWithClient(
      <ReminderResult
        result={{ ...REMINDER, clientLogoUrl: 'http://localhost:8080/api/clients/9/logo' }}
      />,
    );

    const logo = screen.getByRole('img', { name: 'Alpha' });
    expect(logo).toHaveAttribute('src', 'http://localhost:8080/api/clients/9/logo');
  });

  it('reste personnalise au nom du client sans logo, sans image cassee', () => {
    renderWithClient(<ReminderResult result={{ ...REMINDER, clientLogoUrl: null }} />);

    expect(screen.queryByRole('img', { name: 'Alpha' })).not.toBeInTheDocument();
    // Le nom du client apparait plusieurs fois (aperçu + email) : au moins une fois suffit.
    expect(screen.getAllByText('Alpha').length).toBeGreaterThan(0);
  });
});

describe('PayslipResult', () => {
  it('affiche le comparatif et l’ecart', () => {
    render(<PayslipResult result={PAYSLIP} />);

    expect(screen.getByText('2 310,000 DT')).toBeInTheDocument();
    expect(screen.getByText('2 130,000 DT')).toBeInTheDocument();
    expect(screen.getByText('-180,000 DT')).toBeInTheDocument();
  });

  it('liste les causes de l’ecart', () => {
    render(<PayslipResult result={PAYSLIP} />);

    expect(screen.getByText(/Acompte de 150,000 DT/)).toBeInTheDocument();
    expect(screen.getByText(/IRPP mensuel passé/)).toBeInTheDocument();
  });

  it('annonce l’absence de comparaison plutot qu’un tableau a moitie vide', () => {
    render(
      <PayslipResult
        result={{
          explanation: 'Premier bulletin disponible.',
          comparison: { previousNet: null, currentNet: 1600, delta: null, deltaReasons: [] },
        }}
      />,
    );

    expect(screen.getByText(/Aucune comparaison disponible/)).toBeInTheDocument();
    expect(screen.getByText('1 600,000 DT')).toBeInTheDocument();
  });

  it('affiche l’explication en clair', () => {
    render(<PayslipResult result={PAYSLIP} />);

    expect(screen.getByText(/passe de 2 310,000 DT en juin/)).toBeInTheDocument();
  });
});

describe('StructuredResultView', () => {
  it('reconnait une relance a sa forme', () => {
    renderWithClient(<StructuredResultView result={REMINDER} />);

    expect(screen.getByText(/Relance — FAC-2026-0314/)).toBeInTheDocument();
  });

  it('reconnait un bulletin de paie a sa forme', () => {
    render(<StructuredResultView result={PAYSLIP} />);

    expect(screen.getByText('Votre bulletin de paie')).toBeInTheDocument();
  });

  it('n’affiche rien plutot que du JSON brut sur une forme inconnue', () => {
    const { container } = render(<StructuredResultView result={{ inattendu: true }} />);

    expect(container).toBeEmptyDOMElement();
  });
});
