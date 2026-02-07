package com.procuresense.backend.service;

import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.service.ai.OpenAiClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReorderExplanationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OpenAiClient openAiClient;
    private final Clock clock;

    public ReorderExplanationService(OpenAiClient openAiClient, Clock clock) {
        this.openAiClient = openAiClient;
        this.clock = clock;
    }

    public List<ReorderPrediction> enrich(List<ReorderPrediction> predictions) {
        return predictions.stream()
                .map(this::attachExplanation)
                .collect(Collectors.toList());
    }

    private ReorderPrediction attachExplanation(ReorderPrediction prediction) {
        long daysUntil = calculateDaysUntil(prediction.predictedReorderAt());
        String fallback = fallbackExplanation(prediction, daysUntil);
        String explanation = openAiClient.generateReorderExplanation(prediction, daysUntil)
                .orElse(fallback);
        return prediction.withExplanation(explanation);
    }

    private long calculateDaysUntil(OffsetDateTime predicted) {
        long days = Duration.between(OffsetDateTime.now(clock), predicted).toDays();
        return Math.max(0, days);
    }

    private String fallbackExplanation(ReorderPrediction prediction, long daysUntil) {
        String label = StringUtils.hasText(prediction.productName()) ? prediction.productName() : prediction.sku();
        String cadence = prediction.medianDaysBetween() == 1
                ? "daily"
                : "every " + prediction.medianDaysBetween() + " days";
        String daysText = daysUntil == 0 ? "now" : "in " + daysUntil + " days";
        String confidencePercent = String.format("%.0f%%", prediction.confidence() * 100);
        return String.format(
                "%s was last purchased on %s and typically replenished %s, so plan for the next order around %s (%s, %s confidence).",
                label,
                DATE_FORMATTER.format(prediction.lastPurchaseAt().toLocalDate()),
                cadence,
                DATE_FORMATTER.format(prediction.predictedReorderAt().toLocalDate()),
                daysText,
                confidencePercent
        );
    }
}
