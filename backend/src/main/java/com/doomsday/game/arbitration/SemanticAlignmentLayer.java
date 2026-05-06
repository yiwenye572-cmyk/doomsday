package com.doomsday.game.arbitration;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.arbitration.dto.ArbitrationRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SemanticAlignmentLayer {

    public LayerOutcome evaluate(TurnContext ctx) {
        if (ctx.plot == null || ctx.plot.text() == null || ctx.plot.text().isBlank()) {
            return LayerOutcome.fail("SEMANTIC_FAIL", "plot missing");
        }
        double best = ctx.retrievedContexts.stream()
                .mapToDouble(rc -> jaccard(ctx.plot.text(), rc.text()))
                .max()
                .orElse(0.0);
        if (best < 0.12) {
            return LayerOutcome.fail("SEMANTIC_FAIL", "low semantic alignment");
        }
        return LayerOutcome.pass("SEMANTIC_PASS");
    }

    public LayerOutcome evaluate(ArbitrationRequest request) {
        String candidate = request.candidateEventId() == null ? "" : request.candidateEventId();
        if (candidate.isBlank()) {
            return LayerOutcome.fail("SEMANTIC_FAIL", "candidateEventId is required");
        }
        return LayerOutcome.pass("SEMANTIC_PASS");
    }

    private double jaccard(String a, String b) {
        Set<String> sa = tokens(a);
        Set<String> sb = tokens(b);
        if (sa.isEmpty() || sb.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0.0 : inter.size() * 1.0 / union.size();
    }

    private Set<String> tokens(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+"))
                .filter(s -> !s.isBlank())
                .limit(128)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
}
