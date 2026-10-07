package com.antigone.rh.ai.controller;

import com.antigone.rh.ai.dto.ExplainPayslipRequest;
import com.antigone.rh.ai.dto.PayslipExplanation;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.AiRateLimiter;
import com.antigone.rh.ai.service.PayslipExplanationService;
import com.antigone.rh.ai.sse.AiSseFactory;
import com.antigone.rh.ai.sse.AiSseSession;
import com.antigone.rh.ai.sse.AiTaskStreamer;
import com.antigone.rh.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Explication de bulletin de paie.
 *
 * <p>Pas de {@code @PreAuthorize} par permission ici : le controle est plus fin que
 * « qui peut appeler ». Un administrateur interroge n'importe quel employe, tout
 * autre compte est ramene au sien, {@code employeeId} du corps etant alors ignore.
 * Cette resolution appartient au service, qui seul connait l'identite du JWT.
 */
@RestController
@RequestMapping("/api/v1/payslip")
@RequiredArgsConstructor
@Tag(name = "Assistant - Bulletin de paie", description = "Explication d'un bulletin de paie")
public class AiPayslipController {

    private final PayslipExplanationService payslipService;
    private final AiRateLimiter rateLimiter;
    private final AiSseFactory sseFactory;
    private final AiTaskStreamer streamer;
    private final AiAccessScope accessScope;

    @PostMapping(value = "/explain",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Explique un bulletin de paie (streaming SSE)")
    public SseEmitter explainStreaming(@RequestBody @Valid ExplainPayslipRequest request) {
        AuthPrincipal principal = accessScope.current();
        rateLimiter.checkAndRecord(principal.getAccountId());

        AiSseSession session = sseFactory.open();
        return streamer.run(session, sse -> {
            sse.toolCallStart("PayrollLookupTool", java.util.Map.of("month", request.getMonth()));
            PayslipExplanation explanation =
                    payslipService.explain(principal, request.getEmployeeId(), request.getMonth());
            sse.toolCallEnd("PayrollLookupTool", true, null);
            return explanation;
        });
    }

    @PostMapping(value = "/explain",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Explique un bulletin de paie (reponse JSON bloquante)")
    public ResponseEntity<PayslipExplanation> explainBlocking(
            @RequestBody @Valid ExplainPayslipRequest request) {
        AuthPrincipal principal = accessScope.current();
        rateLimiter.checkAndRecord(principal.getAccountId());

        return ResponseEntity.ok(
                payslipService.explain(principal, request.getEmployeeId(), request.getMonth()));
    }
}
