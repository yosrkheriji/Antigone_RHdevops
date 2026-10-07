package com.antigone.rh.ai.tools;

import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.security.AiAccessScope;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Set;

/**
 * Recherche dans le reglement interieur de l'entreprise.
 *
 * <p>Document transverse (indexe par {@code EmbeddingIndexService#indexPolicy}),
 * lisible par tout employe quelle que soit son affectation a une marque :
 * contrairement aux outils de media plan, aucune verification de perimetre client
 * n'a de sens ici. La recherche s'effectue donc avec {@link
 * AiAccessScope.ClientScope#all()}, pas avec le perimetre de l'appelant.
 */
@lombok.AllArgsConstructor
public class PolicyTools {

    private final HybridRetriever hybridRetriever;
    private final ToolAuditService audit;
    /** Appelant de cette instance d'outil. Une instance = un utilisateur. */
    private final AiCallContext context;

    @Tool(name = "InternalPolicyLookupTool", value = """
            Recherche dans le reglement interieur de l'entreprise (horaires de travail, conges, \
            confidentialite, securite, sanctions et procedure disciplinaire, usage du materiel, \
            formation, etc). Retourne les articles les plus pertinents pour la question posee, a \
            citer tels quels. N'invente jamais une regle qui n'y figure pas.""")
    public String lookup(
            @P("Termes de recherche, reformules a partir de la question de l'utilisateur") String query) {
        return audit.execute(context, "InternalPolicyLookupTool", "query=" + query, () -> {
            List<RagHit> hits = hybridRetriever.search(RagQuery.of(
                    query, AiAccessScope.ClientScope.all(), Set.of(AiSourceType.POLICY)));

            if (hits.isEmpty()) {
                return "Aucun article du reglement interieur ne correspond a cette question. "
                        + "Dis-le clairement a l'utilisateur plutot que d'inventer une regle, "
                        + "et invite-le a contacter le service RH.";
            }

            StringBuilder sb = new StringBuilder("Extraits du reglement interieur :\n\n");
            for (RagHit hit : hits) {
                sb.append(hit.chunk().getContent()).append("\n\n---\n\n");
            }
            return sb.toString();
        });
    }
}
