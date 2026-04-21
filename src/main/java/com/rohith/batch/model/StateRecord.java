package com.rohith.batch.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "state_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "extracted_data", columnDefinition = "JSONB")
    private String extractedData;

    @Column(name = "status")
    private String status;  // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "ai_processed")
    private boolean aiProcessed;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}