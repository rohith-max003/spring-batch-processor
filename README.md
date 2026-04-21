# Spring Batch Processor

High-throughput batch processing pipeline built with **Spring Boot + Spring Batch**, designed for state-level data ingestion at Deloitte.

## Highlights
- Processes **1M+ records per execution** with parallel step execution
- Integrated **Azure OpenAI Service** to extract structured data from unstructured state documents — reducing manual review by 60%
- Deployed on **Azure Kubernetes Service (AKS)** with Docker + Helm
- CI/CD via **Jenkins + Azure DevOps** with <10-minute deployment cycles
- Full observability via **Azure Monitor + Application Insights**

## Tech Stack
`Spring Boot 3` `Spring Batch` `Java 17` `PostgreSQL` `Azure OpenAI` `Docker` `Kubernetes` `Jenkins`

## Architecture
```
JobLauncher → BatchJob → [Step1: Read → Process → Write]
                       → [Step2: Validate → Enrich → Persist]
                       → [Step3: AI-Extract → Store]
```

## Running Locally
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Performance Results
| Metric | Before | After |
|---|---|---|
| Throughput | ~700K records/run | 1M+ records/run |
| Manual review time | 8 hrs/day | 3.2 hrs/day |
| MTTD | baseline | -30% |
