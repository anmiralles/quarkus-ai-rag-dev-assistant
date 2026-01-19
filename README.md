# Quarkus AI RAG Development Assistant

An AI-powered development assistant built with Quarkus that provides intelligent answers to Quarkus Java framework questions using the Retrieval-Augmented Generation (RAG) pattern. The application leverages local LLMs via Ollama, vector similarity search with pgvector, and advanced document processing through Docling.

## Architecture Overview

This application implements a complete RAG pipeline with the following components:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DOCUMENT INGESTION (Startup)                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
    ┌──────────────────────────────────▼──────────────────────────────────┐
    │  Documents (PDF/DOCX/HTML)                                           │
    │  src/main/resources/documents/                                       │
    └──────────────────────────────────┬──────────────────────────────────┘
                                       │
                                       ▼
                            ┌──────────────────┐
                            │  Docling Server  │──── Parse & Chunk by Page
                            │  (Port 5001)     │
                            └──────────────────┘
                                       │
                                       ▼
                            ┌──────────────────┐
                            │  Ollama Granite  │──── Generate 384-dim
                            │  Embedding Model │     Vector Embeddings
                            └──────────────────┘
                                       │
                                       ▼
                            ┌──────────────────┐
                            │  PostgreSQL      │
                            │  + pgvector      │──── Store Text + Embeddings
                            │  (embeddings)    │
                            └──────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         QUERY PROCESSING (Runtime)                          │
└─────────────────────────────────────────────────────────────────────────────┘

    User Question ──────► REST API (/copilot?q=...)
                              │
                              ▼
                    ┌──────────────────┐
                    │  Ollama Granite  │──── Convert Query
                    │  Embedding Model │     to Embeddings
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  DocumentRetriever│──── Vector Similarity Search
                    │  + pgvector       │     (Top 5, Min Score: 0.7)
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Content Injector │──── Format Context with
                    │                  │     Metadata & Citations
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Ollama Granite4 │──── Generate Answer with
                    │  Chat Model      │     Retrieved Context
                    │  (temp: 0.2)     │
                    └──────────────────┘
                              │
                              ▼
                    AI Response with Citations
```

### Technology Stack

- **Quarkus 3.30.6** - Supersonic Subatomic Java Framework
- **Java 21** - Modern Java runtime
- **LangChain4j** - AI orchestration and RAG implementation
- **Ollama** - Local LLM inference engine (Granite models)
- **Docling Server** - Document parsing and intelligent chunking
- **PostgreSQL + pgvector** - Vector embeddings storage and similarity search
- **Hibernate ORM with Panache** - Simplified database operations

### RAG Pipeline Flow

1. **Document Ingestion** (Startup)
   - Scans `src/main/resources/documents/` for PDF, DOCX, HTML files
   - Uses Docling server to parse and chunk documents by page
   - Generates embeddings using Ollama's `granite-embedding:latest` model
   - Stores text segments and 384-dimensional embeddings in pgvector

2. **Query Processing** (Runtime)
   - REST API accepts user questions at `/copilot?q=<question>`
   - Query text is converted to embeddings
   - Vector similarity search finds top 5 relevant chunks (minimum similarity: 0.7)
   - Retrieved context is injected into the prompt with metadata

3. **Response Generation**
   - Augmented prompt sent to `granite4:latest` LLM via Ollama
   - Model generates answer based on retrieved context
   - Response includes citations and metadata about sources

### Key Components

- `DocumentLoader` - Ingests documents at application startup
- `DoclingConverter` - Interfaces with Docling server for document processing
- `DocumentRetriever` - Performs vector similarity search
- `RetrievalAugmentorSupplier` - Intercepts queries and injects retrieved context
- `CustomContentInjector` - Formats retrieved content with structured metadata
- `DevAssistant` - LangChain4j AI Service interface with system prompt

## Prerequisites

### 1. Docling Server

The application requires Docling server for document processing. Start it using Podman or Docker:

```bash
podman run -p 5001:5001 -e DOCLING_SERVE_ENABLE_UI=1 quay.io/docling-project/docling-serve
```

Or with Docker:

```bash
docker run -p 5001:5001 -e DOCLING_SERVE_ENABLE_UI=1 quay.io/docling-project/docling-serve
```

Docling UI will be available at http://localhost:5001

### 2. Ollama with Granite Models

Install Ollama and pull the required models:

```bash
# Install Ollama from https://ollama.ai

# Pull chat model
ollama pull granite4:latest

# Pull embedding model
ollama pull granite-embedding:latest

# Serve the models
ollama serve
```

### 3. PostgreSQL with pgvector

Ensure PostgreSQL is running with pgvector extension enabled. Quarkus Dev Services can automatically provision this in development mode.

## Getting Started

### Running in Development Mode

```bash
./mvnw quarkus:dev
```

The application will:
- Start on http://localhost:8080
- Automatically ingest documents from `src/main/resources/documents/`
- Connect to Docling server at http://localhost:5001
- Connect to Ollama at http://localhost:11434
- Provide Dev UI at http://localhost:8080/q/dev/

### Adding Documents

Place PDF, DOCX, or HTML files in `src/main/resources/documents/`. The application processes these automatically at startup.

### Querying the Assistant

```bash
curl "http://localhost:8080/copilot?q=How%20do%20I%20configure%20quarkus?"
```

Or visit in browser:
```
http://localhost:8080/copilot?q=How do I configure quarkus?
```

## Configuration

Key settings in `application.properties`:

```properties
# Ollama Models
quarkus.langchain4j.ollama.chat-model.model-id=granite4:latest
quarkus.langchain4j.ollama.embedding-model.model-id=granite-embedding:latest
quarkus.langchain4j.ollama.chat-model.temperature=0.2

# pgvector Configuration
quarkus.langchain4j.pgvector.table=embeddings
quarkus.langchain4j.pgvector.dimension=384

# Docling Server
quarkus.docling.service.url=http://localhost:5001
```

### Retrieval Parameters

Adjust in `DocumentRetriever.java`:
- `MAX_RESULTS = 5` - Documents retrieved per query
- `MIN_SCORE = 0.7` - Minimum similarity threshold

### Model Temperature

Set to 0.2 for consistent, factual responses. Increase for more creative outputs (up to 1.0).

## Building and Packaging

### Standard JAR

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Uber JAR

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

### Native Executable

```bash
# With local GraalVM
./mvnw package -Dnative

# Using container build (no GraalVM required)
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Run native executable
./target/regulatory-change-copilot-1.0.0-SNAPSHOT-runner
```

## Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Test specific class
./mvnw test -Dtest=DocumentRetrieverTest
```

## Project Structure

```
src/main/java/me/amiralles/devassistant/
├── ai/
│   ├── DevAssistant.java              # AI Service interface
│   ├── RetrievalAugmentorSupplier.java # RAG integration
│   └── CustomContentInjector.java     # Context formatter
├── api/
│   └── DevAssistantResource.java      # REST endpoint
├── ingest/
│   ├── DocumentLoader.java            # Startup ingestion
│   └── DoclingConverter.java          # Docling integration
└── retrieval/
    └── DocumentRetriever.java         # Vector search
```

## Customization

### Change System Prompt

Edit `@SystemMessage` in `DevAssistant.java` to modify the assistant's role and expertise domain.

### Adjust Chunking Strategy

Modify `DoclingConverter.java` to change how Docling processes documents. Current implementation uses hybrid chunking by page.

### Tune Retrieval Quality

- Increase `MIN_SCORE` for stricter matching
- Increase `MAX_RESULTS` for more context
- Adjust embedding model dimension if changing models

## Documentation

- [Quarkus](https://quarkus.io/)
- [LangChain4j Quarkus](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Docling](https://github.com/DS4SD/docling)
- [Ollama](https://ollama.ai/)
- [pgvector](https://github.com/pgvector/pgvector)

