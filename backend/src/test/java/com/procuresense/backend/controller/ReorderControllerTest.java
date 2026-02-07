package com.procuresense.backend.controller;

import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.service.BundleRecommendationService;
import com.procuresense.backend.service.DemoDataService;
import com.procuresense.backend.service.PurchaseImportService;
import com.procuresense.backend.service.ReorderExplanationService;
import com.procuresense.backend.service.ReorderInsightService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseController.class)
class ReorderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReorderInsightService reorderInsightService;

    @MockBean
    private DemoDataService demoDataService;

    @MockBean
    private PurchaseImportService purchaseImportService;

    @MockBean
    private BundleRecommendationService bundleRecommendationService;

    @MockBean
    private ReorderExplanationService reorderExplanationService;

    @Test
    void returnsPredictionsSortedAndLimited() throws Exception {
        List<ReorderPrediction> predictions = List.of(
                new ReorderPrediction("demo-org-a", "SKU-2", "Item 2", OffsetDateTime.parse("2024-01-10T00:00:00Z"), 5,
                        OffsetDateTime.parse("2024-01-15T00:00:00Z"), 0.8, 12, ""),
                new ReorderPrediction("demo-org-a", "SKU-1", "Item 1", OffsetDateTime.parse("2024-01-05T00:00:00Z"), 10,
                        OffsetDateTime.parse("2024-01-15T00:00:00Z"), 0.9, 6, "")
        );
        when(reorderInsightService.computePredictions(anyString())).thenReturn(predictions);
        when(reorderExplanationService.enrich(Mockito.anyList(), Mockito.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(get("/api/purchases/insights/reorders")
                        .header("X-Org-Id", "demo-org-a")
                        .header("X-Role", "BUYER")
                        .param("limit", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-2"))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        Mockito.verify(reorderInsightService).computePredictions("demo-org-a");
        Mockito.verify(reorderExplanationService).enrich(Mockito.anyList(), Mockito.eq(false));
    }
}
