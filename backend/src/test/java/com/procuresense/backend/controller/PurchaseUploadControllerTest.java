package com.procuresense.backend.controller;

import com.procuresense.backend.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @BeforeEach
    void clearData() {
        purchaseRepository.deleteAll();
    }

    @Test
    void uploadValidCsvReturnsSummary() throws Exception {
        String csv = "order_id,sku,product_name,category,quantity,unit_price,purchased_at\n" +
                "ORD-001,SKU-100,Lab Gloves,Safety,5,12.50,2024-01-01T10:00:00Z\n" +
                "ORD-002,SKU-200,Boxes,Logistics,3,5.30,2024-01-05T09:30:00Z";
        MockMultipartFile file = new MockMultipartFile("file", "purchases.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/purchases/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("X-Org-Id", "test-org")
                        .header("X-Role", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRows").value(2))
                .andExpect(jsonPath("$.rejectedRows").value(0));
    }

    @Test
    void uploadWithInvalidRowCollectsError() throws Exception {
        String csv = "order_id,sku,product_name,category,quantity,unit_price,purchased_at\n" +
                "ORD-003,SKU-300,Tape,Cleaning,-1,3.10,2024-02-01T10:00:00Z\n" +
                "ORD-004,SKU-400,Labels,Logistics,2,4.50,2024-02-03T10:00:00Z";
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/purchases/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("X-Org-Id", "test-org")
                        .header("X-Role", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRows").value(1))
                .andExpect(jsonPath("$.rejectedRows").value(1))
                .andExpect(jsonPath("$.sampleErrors[0]").value(containsString("quantity must be greater than zero")));
    }

    @Test
    void missingColumnsReturnsHelpfulError() throws Exception {
        String csv = "order_id,sku,product_name,category,quantity,purchased_at\n" +
                "ORD-005,SKU-500,Paper,Office,4,2024-02-01T10:00:00Z";
        MockMultipartFile file = new MockMultipartFile("file", "missing.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/purchases/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("X-Org-Id", "test-org")
                        .header("X-Role", "admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Missing required columns")));
    }
}
