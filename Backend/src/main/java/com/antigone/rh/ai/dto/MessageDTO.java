package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message rendu au frontend.
 *
 * <p>{@code structuredResult} rejoue le contenu de l'evenement SSE du meme nom :
 * un media plan genere reste donc affichable a l'identique apres rechargement de
 * la page, y compris si le flux avait ete interrompu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private String role;
    private String content;
    private String toolCalls;
    private String structuredResult;
    private String capability;
    private Long sequence;
    private LocalDateTime createdAt;
}
