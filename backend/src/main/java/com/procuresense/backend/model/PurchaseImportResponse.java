package com.procuresense.backend.model;

import java.util.List;

public record PurchaseImportResponse(int importedRows,
                                     int rejectedRows,
                                     List<String> sampleErrors) {
}
