package com.rohith.batch.config;

import com.rohith.batch.model.StateRecord;
import com.rohith.batch.processor.RecordProcessor;
import com.rohith.batch.writer.RecordWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final DataSource dataSource;
    private final RecordProcessor recordProcessor;
    private final RecordWriter recordWriter;

    @Value("${batch.chunk-size:500}")
    private int chunkSize;

    @Value("${batch.thread-pool-size:10}")
    private int threadPoolSize;

    @Bean
    public Job stateDataProcessingJob(JobRepository jobRepository, Step processStep) {
        return new JobBuilder("stateDataProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .start(processStep)
                .build();
    }

    @Bean
    public Step processStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processStep", jobRepository)
                .<StateRecord, StateRecord>chunk(chunkSize, transactionManager)
                .reader(stateRecordReader())
                .processor(recordProcessor)
                .writer(recordWriter)
                .taskExecutor(taskExecutor())
                .throttleLimit(threadPoolSize)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean
    public JdbcCursorItemReader<StateRecord> stateRecordReader() {
        return new JdbcCursorItemReaderBuilder<StateRecord>()
                .name("stateRecordReader")
                .dataSource(dataSource)
                .sql("SELECT id, document_id, raw_content, status, created_at FROM state_records WHERE status = 'PENDING' ORDER BY id")
                .rowMapper((rs, rowNum) -> StateRecord.builder()
                        .id(rs.getLong("id"))
                        .documentId(rs.getString("document_id"))
                        .rawContent(rs.getString("raw_content"))
                        .status(rs.getString("status"))
                        .build())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("batch-worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("Starting batch job: {} at {}", jobExecution.getJobInstance().getJobName(), jobExecution.getStartTime());
            }
            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("Batch job completed. Status: {} | Read: {} | Written: {} | Skipped: {}",
                        jobExecution.getStatus(),
                        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum(),
                        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum(),
                        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum());
            }
        };
    }
}