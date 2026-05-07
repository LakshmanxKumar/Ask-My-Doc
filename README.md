# Ask-My-Doc

Ask-My-Doc is a Java Spring Boot application that allows users to upload documents and query them using a simple API. The application provides endpoints for document upload and question-answering from those documents.

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
curl -X POST "http://localhost:8080/api/v1/upload" \
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
curl -X POST "http://localhost:8080/api/v1/query" \
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

1. Build and run the Spring Boot application.
2. Use the provided endpoints to upload documents and query them.
