package com.procuresense.backend.service;

import com.procuresense.backend.model.ReorderInsight;
import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.repository.ReorderInsightRepository;
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
    private final ReorderInsightRepository reorderInsightRepository;

    public ReorderExplanationService(OpenAiClient openAiClient,
                                     Clock clock,
                                     ReorderInsightRepository reorderInsightRepository) {
        this.openAiClient = openAiClient;
        this.clock = clock;
        this.reorderInsightRepository = reorderInsightRepository;
    }

    public List<ReorderPrediction> enrich(List<ReorderPrediction> predictions) {
        return enrich(predictions, false);
    }

    public List<ReorderPrediction> enrich(List<ReorderPrediction> predictions, boolean cacheOnly) {
        return predictions.stream()
                .map(prediction -> attachExplanation(prediction, cacheOnly))
                .collect(Collectors.toList());
    }

    private ReorderPrediction attachExplanation(ReorderPrediction prediction, boolean cacheOnly) {
        long daysUntil = calculateDaysUntil(prediction.predictedReorderAt());
        String fingerprint = fingerprint(prediction);
        String cached = lookupCachedExplanation(prediction, fingerprint);
        if (StringUtils.hasText(cached)) {
            return prediction.withExplanation(cached);
        }
        String fallback = fallbackExplanation(prediction, daysUntil);
        if (cacheOnly) {
            return prediction.withExplanation(fallback);
        }
        String explanation = openAiClient.generateReorderExplanation(prediction, daysUntil)
                .orElse(fallback);
        persistInsight(prediction, explanation, fingerprint);
        return prediction.withExplanation(explanation);
    }

    private String lookupCachedExplanation(ReorderPrediction prediction, String fingerprint) {
        return reorderInsightRepository.findByOrgIdAndSku(prediction.orgId(), prediction.sku())
                .filter(insight -> fingerprint.equals(insight.getFingerprint()))
                .map(ReorderInsight::getExplanationText)
                .orElse(null);
    }

    private void persistInsight(ReorderPrediction prediction, String explanation, String fingerprint) {
        ReorderInsight insight = reorderInsightRepository.findByOrgIdAndSku(prediction.orgId(), prediction.sku())
                .orElseGet(ReorderInsight::new);
        insight.setOrgId(prediction.orgId());
        insight.setSku(prediction.sku());
        insight.setLastPurchaseAt(prediction.lastPurchaseAt());
        insight.setPredictedReorderAt(prediction.predictedReorderAt());
        insight.setMedianDaysBetween(prediction.medianDaysBetween());
        insight.setConfidence(prediction.confidence());
        insight.setExplanationText(explanation);
        insight.setFingerprint(fingerprint);
        reorderInsightRepository.save(insight);
    }

    private long calculateDaysUntil(OffsetDateTime predicted) {
        long days = Duration.between(OffsetDateTime.now(clock), predicted).toDays();
        return Math.max(0, days);
    }

    private String fingerprint(ReorderPrediction prediction) {
        return prediction.orgId() + "|" + prediction.sku() + "|" +
                prediction.lastPurchaseAt() + "|" + prediction.predictedReorderAt() + "|" +
                prediction.medianDaysBetween() + "|" + String.format("%.2f", prediction.confidence());
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
