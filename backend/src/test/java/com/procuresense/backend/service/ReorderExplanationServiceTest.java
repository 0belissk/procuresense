package com.procuresense.backend.service;

import com.procuresense.backend.model.ReorderPrediction;
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
import static org.mockito.Mockito.when;

class ReorderExplanationServiceTest {

    private final OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
    private final ReorderExplanationService service = new ReorderExplanationService(openAiClient, fixedClock);

    @Test
    void enrichUsesGeneratedExplanationWhenAvailable() {
        ReorderPrediction basePrediction = samplePrediction();
        when(openAiClient.generateReorderExplanation(basePrediction, 9))
                .thenReturn(Optional.of("Assistant explanation"));

        List<ReorderPrediction> enriched = service.enrich(List.of(basePrediction));

        assertThat(enriched).hasSize(1);
        assertThat(enriched.get(0).explanation()).isEqualTo("Assistant explanation");
    }

    @Test
    void enrichFallsBackToDeterministicExplanation() {
        ReorderPrediction basePrediction = samplePrediction();
        when(openAiClient.generateReorderExplanation(basePrediction, 9)).thenReturn(Optional.empty());

        List<ReorderPrediction> enriched = service.enrich(List.of(basePrediction));

        assertThat(enriched.get(0).explanation())
                .contains("Eco Towels was last purchased on 2024-01-20")
                .contains("plan for the next order around 2024-02-10")
                .contains("in 9 days")
                .contains("80% confidence");
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
