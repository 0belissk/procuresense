package com.procuresense.backend.service;

import com.procuresense.backend.model.ReorderInsight;
import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.repository.ReorderInsightRepository;
import com.procuresense.backend.service.ai.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReorderExplanationServiceTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
    private final ReorderInsightRepository reorderInsightRepository = Mockito.mock(ReorderInsightRepository.class);
    private final ReorderExplanationService service = new ReorderExplanationService(openAiClient, fixedClock, reorderInsightRepository);

    @Test
    void enrichUsesGeneratedExplanationWhenAvailable() {
        ReorderPrediction basePrediction = samplePrediction();
        when(openAiClient.generateReorderExplanation(basePrediction, 9))
                .thenReturn(Optional.of("Assistant explanation"));
        when(reorderInsightRepository.findByOrgIdAndSku("demo-org-a", "SKU-42"))
                .thenReturn(Optional.empty());

        List<ReorderPrediction> enriched = service.enrich(List.of(basePrediction));

        assertThat(enriched).hasSize(1);
        assertThat(enriched.get(0).explanation()).isEqualTo("Assistant explanation");
        verify(reorderInsightRepository).save(Mockito.any(ReorderInsight.class));
    }

    @Test
    void enrichFallsBackToDeterministicExplanation() {
        ReorderPrediction basePrediction = samplePrediction();
        when(openAiClient.generateReorderExplanation(basePrediction, 9)).thenReturn(Optional.empty());
        when(reorderInsightRepository.findByOrgIdAndSku("demo-org-a", "SKU-42"))
                .thenReturn(Optional.empty());

        List<ReorderPrediction> enriched = service.enrich(List.of(basePrediction));

        assertThat(enriched.get(0).explanation())
                .contains("Eco Towels was last purchased on 2024-01-20")
                .contains("plan for the next order around 2024-02-10")
                .contains("in 9 days")
                .contains("80% confidence");
    }

    @Test
    void enrichUsesCachedExplanationWhenFingerprintMatches() {
        ReorderPrediction basePrediction = samplePrediction();
        ReorderInsight cached = new ReorderInsight();
        cached.setOrgId("demo-org-a");
        cached.setSku("SKU-42");
        cached.setFingerprint(buildFingerprint(basePrediction));
        cached.setExplanationText("Cached insight");
        when(reorderInsightRepository.findByOrgIdAndSku("demo-org-a", "SKU-42"))
                .thenReturn(Optional.of(cached));

        List<ReorderPrediction> enriched = service.enrich(List.of(basePrediction));

        assertThat(enriched.get(0).explanation()).isEqualTo("Cached insight");
        verify(openAiClient, never()).generateReorderExplanation(Mockito.any(), Mockito.anyLong());
        verify(reorderInsightRepository, never()).save(Mockito.any());
    }

    private String buildFingerprint(ReorderPrediction prediction) {
        return prediction.orgId() + "|" + prediction.sku() + "|" +
                prediction.lastPurchaseAt() + "|" + prediction.predictedReorderAt() + "|" +
                prediction.medianDaysBetween() + "|" + String.format("%.2f", prediction.confidence());
    }

    private ReorderPrediction samplePrediction() {
        return new ReorderPrediction(
                "demo-org-a",
                "SKU-42",
                "Eco Towels",
                OffsetDateTime.parse("2024-01-20T12:00:00Z"),
                10,
                OffsetDateTime.parse("2024-02-10T12:00:00Z"),
                0.8,
                null
        );
    }
}
