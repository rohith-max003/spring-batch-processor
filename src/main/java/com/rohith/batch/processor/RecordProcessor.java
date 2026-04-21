package com.rohith.batch.processor;

import com.rohith.batch.model.StateRecord;
import com.rohith.batch.service.AzureOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordProcessor implements ItemProcessor<StateRecord, StateRecord> {

    private final AzureOpenAIService openAIService;

    @Override
    public StateRecord process(StateRecord record) throws Exception {
        log.debug("Processing record: {}", record.getDocumentId());

        try {
            // AI-powered extraction: structured data from unstructured state documents
            String extractedJson = openAIService.extractStructuredData(record.getRawContent());

            record.setExtractedData(extractedJson);
            record.setStatus("COMPLETED");
            record.setAiProcessed(true);
            record.setProcessedAt(LocalDateTime.now());

            log.debug("Successfully processed record: {}", record.getDocumentId());
            return record;

        } catch (Exception e) {
            log.error("Failed to process record: {} — {}", record.getDocumentId(), e.getMessage());
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setProcessedAt(LocalDateTime.now());
            return record;
        }
    }
}