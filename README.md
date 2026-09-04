# Ask-My-Doc

Ask-My-Doc is a Java Spring Boot application that allows users to upload documents and query them using a simple API. The application provides endpoints for document upload and question-answering from those documents.

---

## Setup

### Prerequisites

- Java and Maven installed
- A MongoDB Atlas cluster (free tier is fine)
- A Gemini API key
- A Cohere API key

### 1. Environment variables

The application expects the following environment variables to be set:

| Variable          | Description                                  |
|-------------------|-----------------------------------------------|
| `GEMINI_API_KEY`  | API key for Google Gemini (chat + embeddings) |
| `MONGODB_URI`     | Your MongoDB Atlas connection string          |
| `COHERE_API_KEY`  | API key for Cohere Rerank                     |

### 2. Create the database and collection in MongoDB Atlas

The application is configured to use:

- **Database:** `askmydoc`
- **Collection:** `chunks`

You can create these ahead of time from the Atlas UI (Database → Browse Collections → Create Database), or let MongoDB create them automatically the first time a document is uploaded. Either way, the names must match exactly, since they're set in `application.properties`:

```properties
spring.data.mongodb.database=askmydoc
spring.ai.vectorstore.mongodb.collection-name=chunks
```

### 3. Create the Atlas Vector Search index

Vector search requires an explicit index — it is **not** created automatically just by inserting data.

1. In Atlas, go to your cluster → **Search** → **Create Search Index**.
2. Choose **JSON Editor** (not the visual builder).
3. Select database `askmydoc` and collection `chunks`.
4. Name the index `vector_index`.
5. Paste the following definition:

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 3072,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "metadata.docId"
    }
  ]
}
```

- `numDimensions: 3072` matches the default output size of `gemini-embedding-001`. If you change the embedding model or configure a smaller output dimension, update this value to match.
- The `filter` field on `metadata.docId` is required because queries filter results down to specific uploaded documents — Atlas requires any field used this way to be explicitly indexed as filterable.

6. Click **Create** and wait for the index status to show **Active** before running queries against it.

> **Note:** Setting `spring.ai.vectorstore.mongodb.initialize-schema=true` in `application.properties` lets Spring AI create this index automatically on startup *if it's missing*. It won't fix or replace an existing index with the wrong configuration, so the manual steps above are still the reliable way to get it right the first time.

---

## API Endpoints

### 1. Upload Documents

**Endpoint:**
`POST /api/v1/upload`

**Description:**
Uploads one or more documents for processing. Returns a list of document IDs upon success.

**Headers:**
- `Content-Type: multipart/form-data`

**Request Parameters:**
- `file` (required): One or more files to be uploaded. Use the field name `file` for all uploads.

**Sample Request (cURL):**
```sh
curl -X POST "http://localhost:8000/api/v1/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/document1.pdf" \
  -F "file=@/path/to/document2.pdf"
```

**Success Response:**
```json
[
  "document_id_1",
  "document_id_2"
]
```

**Failure Response:**
```json
["File is empty"]
```

---

### 2. Query Documents

**Endpoint:**
`POST /api/v1/query`

**Description:**
Submit a question with a list of document IDs to retrieve an answer.

**Headers:**
- `Content-Type: application/json`

**Request Body:**
- `userQuery` (string, required): The question to ask about the documents.
- `docIds` (array of strings, required): The list of document IDs to query against.

**Sample Request:**
```json
{
  "userQuery": "What is the policy on medical leave?",
  "docIds": ["document_id_1", "document_id_2"]
}
```

**Sample HTTP Request (cURL):**
```sh
curl -X POST "http://localhost:8000/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{
    "userQuery": "What is the policy on medical leave?",
    "docIds": ["document_id_1", "document_id_2"]
  }'
```

**Sample Response:**
```json
"The policy on medical leave states that..."
```

---

## Running the Project

1. Set the required environment variables (see [Setup](#setup)).
2. Make sure the MongoDB Atlas database, collection, and vector search index are created (see [Setup](#setup)).
3. Build and run the Spring Boot application:
   ```sh
   ./mvnw spring-boot:run
   ```
4. The app runs on `http://localhost:8000` by default (configurable via `server.port`).
5. Use the endpoints above to upload documents and query them.
