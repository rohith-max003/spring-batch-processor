package com.rohith.batch.writer;

import com.rohith.batch.model.StateRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordWriter implements ItemWriter<StateRecord> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(Chunk<? extends StateRecord> chunk) throws Exception {
        log.info("Writing batch of {} records", chunk.size());

        jdbcTemplate.batchUpdate(
            "UPDATE state_records SET extracted_data = ?::jsonb, status = ?, ai_processed = ?, error_message = ?, processed_at = ? WHERE id = ?",
            chunk.getItems(),
            chunk.size(),
            (ps, record) -> {
                ps.setString(1, record.getExtractedData());
                ps.setString(2, record.getStatus());
                ps.setBoolean(3, record.isAiProcessed());
                ps.setString(4, record.getErrorMessage());
                ps.setObject(5, record.getProcessedAt());
                ps.setLong(6, record.getId());
            }
        );

        log.info("Successfully persisted {} records", chunk.size());
    }
}