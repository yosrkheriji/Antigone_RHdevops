package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.dto.ConversationDetailDTO;
import com.antigone.rh.ai.dto.ConversationSummaryDTO;
import com.antigone.rh.ai.dto.CreateConversationRequest;
import com.antigone.rh.ai.dto.UpdateConversationRequest;
import com.antigone.rh.ai.entity.AiMessageRole;
import com.antigone.rh.ai.service.ConversationService;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scenario 10 : CRUD des conversations et cloisonnement entre comptes.
 *
 * <p>Le point sensible est le dernier test : acceder a la conversation d'autrui
 * repond 404 et non 403, un 403 confirmant l'existence de la ressource.
 */
class ConversationCrudIT extends AbstractAiIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private TestFixtures fixtures;

    private AuthPrincipal alice;
    private AuthPrincipal bob;

    @BeforeEach
    void setUp() {
        alice = fixtures.employeePrincipal(fixtures.employe("Haddad", "Alice"));
        bob = fixtures.employeePrincipal(fixtures.employe("Mansour", "Bob"));
    }

    private ConversationSummaryDTO createFor(AuthPrincipal principal, String title) {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setTitle(title);
        return conversationService.create(principal, request);
    }

    @Test
    @DisplayName("Creation, lecture, renommage et suppression s'enchainent")
    void fullLifecycle() {
        ConversationSummaryDTO created = createFor(alice, "Preparation juillet");
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Preparation juillet");
        assertThat(created.getPinned()).isFalse();

        UpdateConversationRequest rename = new UpdateConversationRequest();
        rename.setTitle("Media plan juillet");
        rename.setPinned(true);
        ConversationSummaryDTO renamed = conversationService.update(alice, created.getId(), rename);
        assertThat(renamed.getTitle()).isEqualTo("Media plan juillet");
        assertThat(renamed.getPinned()).isTrue();

        assertThat(conversationService.list(alice, PageRequest.of(0, 10)).getContent())
                .extracting(ConversationSummaryDTO::getId)
                .contains(created.getId());

        conversationService.delete(alice, created.getId());

        assertThat(conversationService.list(alice, PageRequest.of(0, 10)).getContent())
                .extracting(ConversationSummaryDTO::getId)
                .doesNotContain(created.getId());
    }

    @Test
    @DisplayName("Sans titre, la conversation part sur un libelle par defaut")
    void defaultTitleWhenNoneProvided() {
        ConversationSummaryDTO created = conversationService.create(alice, new CreateConversationRequest());

        assertThat(created.getTitle()).isEqualTo("Nouvelle conversation");
    }

    @Test
    @DisplayName("Le titre par defaut est remplace par le premier message")
    void firstMessageDrivesTheTitle() {
        ConversationSummaryDTO created = conversationService.create(alice, new CreateConversationRequest());

        // Sans modele configure, le repli tronque le message : le comportement doit
        // rester correct quand le titrage automatique n'est pas disponible.
        conversationService.ensureTitle(created.getId(), "Genere le media plan de juillet pour Alpha");

        ConversationDetailDTO detail = conversationService.get(alice, created.getId());
        assertThat(detail.getTitle()).isEqualTo("Genere le media plan de juillet pour Alpha");
    }

    @Test
    @DisplayName("Un titre deja choisi n'est jamais ecrase")
    void explicitTitleIsNeverOverwritten() {
        ConversationSummaryDTO created = createFor(alice, "Mon titre");

        conversationService.ensureTitle(created.getId(), "Un tout autre sujet");

        assertThat(conversationService.get(alice, created.getId()).getTitle()).isEqualTo("Mon titre");
    }

    @Test
    @DisplayName("Les messages sont rendus dans leur ordre d'arrivee")
    void messagesKeepTheirOrder() {
        ConversationSummaryDTO created = createFor(alice, "Echange");

        conversationService.append(created.getId(), AiMessageRole.USER, "Premiere question", null, null, null);
        conversationService.append(created.getId(), AiMessageRole.ASSISTANT, "Premiere reponse",
                null, null, "GENERAL");
        conversationService.append(created.getId(), AiMessageRole.USER, "Seconde question", null, null, null);

        ConversationDetailDTO detail = conversationService.get(alice, created.getId());

        assertThat(detail.getMessages()).extracting("content").containsExactly(
                "Premiere question", "Premiere reponse", "Seconde question");
        assertThat(detail.getMessages()).extracting("sequence").containsExactly(0L, 1L, 2L);
    }

    @Test
    @DisplayName("Le compteur de messages remonte dans la liste")
    void messageCountIsExposedInTheListing() {
        ConversationSummaryDTO created = createFor(alice, "Compte");
        conversationService.append(created.getId(), AiMessageRole.USER, "un", null, null, null);
        conversationService.append(created.getId(), AiMessageRole.ASSISTANT, "deux", null, null, null);

        ConversationSummaryDTO listed = conversationService.list(alice, PageRequest.of(0, 10))
                .getContent().stream()
                .filter(row -> row.getId().equals(created.getId()))
                .findFirst().orElseThrow();

        assertThat(listed.getMessageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("La liste est triable par date de mise a jour")
    void listingIsSortableByUpdatedAt() throws InterruptedException {
        ConversationSummaryDTO first = createFor(alice, "Ancienne");
        Thread.sleep(20);
        ConversationSummaryDTO second = createFor(alice, "Recente");

        var descending = conversationService.list(alice,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();

        assertThat(descending).extracting(ConversationSummaryDTO::getId)
                .containsSubsequence(second.getId(), first.getId());
    }

    @Test
    @DisplayName("La conversation d'un autre compte est invisible et inaccessible")
    void anotherAccountsConversationIsInvisible() {
        ConversationSummaryDTO aliceConversation = createFor(alice, "Prive Alice");

        assertThat(conversationService.list(bob, PageRequest.of(0, 10)).getContent())
                .extracting(ConversationSummaryDTO::getId)
                .doesNotContain(aliceConversation.getId());

        // 404 et non 403 : un 403 confirmerait que la conversation existe.
        assertThatThrownBy(() -> conversationService.get(bob, aliceConversation.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        UpdateConversationRequest rename = new UpdateConversationRequest();
        rename.setTitle("Pirate");
        assertThatThrownBy(() -> conversationService.update(bob, aliceConversation.getId(), rename))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> conversationService.delete(bob, aliceConversation.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // Et la conversation d'Alice est restee intacte.
        assertThat(conversationService.get(alice, aliceConversation.getId()).getTitle())
                .isEqualTo("Prive Alice");
    }

    @Test
    @DisplayName("Une conversation supprimee n'est plus lisible par son proprietaire")
    void deletedConversationIsNoLongerReadable() {
        ConversationSummaryDTO created = createFor(alice, "A supprimer");
        conversationService.delete(alice, created.getId());

        assertThatThrownBy(() -> conversationService.get(alice, created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
