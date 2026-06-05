## Upload Document

### Endpoint

```http
POST /documents/uploads
```
---
## Request

### Headers

```http
Content-Type: multipart/form-data
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---

### Request Body

```json
{
  "title": "Java Basic",
  "file": "<FILE>",
  "replaceExisting": false
}
```
---
## Success Response

### HTTP Status

```http
201 Created
```
---
### Response Body

```json
{
  "code": 1000,
  "message": "Upload document successfully",
  "result": {
    "documentId": "doc_123",
    "title": "Java Basic",
    "fileName": "java-basic.pdf",
    "fileExtension": "pdf",
    "mimeType": "application/pdf",
    "fileSize": 1048576,
    "storageUrl": "https://cloud-storage/documents/java-basic.pdf",
    "status": "COMPLETED"
  }
}
```
---
## Error Responses

### Unauthenticated

```http
401 Unauthorized
```

```json
{
  "code": 1022,
  "message": "Unauthenticated"
}
```

---

### Document Title Required

```http
400 Bad Request
```

```json
{
  "code": 1301,
  "message": "Document title is required"
}
```

---

### File Required

```http
400 Bad Request
```

```json
{
  "code": 1302,
  "message": "File is required"
}
```

---

### File Too Large

```http
400 Bad Request
```

```json
{
  "code": 1303,
  "message": "File exceeds maximum allowed size"
}
```

---

### File Type Not Supported

```http
400 Bad Request
```

```json
{
  "code": 1304,
  "message": "File type is not supported"
}
```

---

### Invalid Mime Type

```http
400 Bad Request
```

```json
{
  "code": 1305,
  "message": "Invalid mime type or fake file extension"
}
```

---

### Storage Not Enough

```http
403 Forbidden
```

```json
{
  "code": 1306,
  "message": "Cloud storage is not enough. Please upgrade your storage"
}
```

---

### File Already Exists

```http
409 Conflict
```

```json
{
  "code": 1307,
  "message": "File already exists"
}
```

---

### Upload Failed

```http
500 Internal Server Error
```

```json
{
  "code": 1308,
  "message": "Upload failed"
}
```

---

## Pause Upload

### Endpoint

```http
POST /documents/uploads/{uploadId}/pause
```
---
## Request

### Headers

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---
## Success Response

### HTTP Status

```http
200 OK
```
---
### Response Body

```json
{
  "code": 1000,
  "message": "Upload paused successfully",
  "result": {
    "uploadId": "upload_123",
    "status": "PAUSED",
    "uploadedBytes": 524288
  }
}
```
---
## Error Responses

### Upload Not Found

```http
404 Not Found
```

```json
{
  "code": 1309,
  "message": "Upload session not found"
}
```

---

### Upload Cannot Be Paused

```http
400 Bad Request
```

```json
{
  "code": 1310,
  "message": "Upload cannot be paused"
}
```

---

## Resume Upload

### Endpoint

```http
POST /documents/uploads/{uploadId}/resume
```
---
## Request

### Headers

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---
## Success Response

### HTTP Status

```http
200 OK
```
---
### Response Body

```json
{
  "code": 1000,
  "message": "Upload resumed successfully",
  "result": {
    "uploadId": "upload_123",
    "status": "UPLOADING",
    "uploadedBytes": 524288
  }
}
```
---
## Error Responses

### Upload Not Found

```http
404 Not Found
```

```json
{
  "code": 1309,
  "message": "Upload session not found"
}
```

---

### Upload Cannot Be Resumed

```http
400 Bad Request
```

```json
{
  "code": 1311,
  "message": "Upload cannot be resumed"
}
```

---

## Cancel Upload

### Endpoint

```http
DELETE /documents/uploads/{uploadId}
```
---
## Request

### Headers

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---
## Success Response

### HTTP Status

```http
200 OK
```
---
### Response Body

```json
{
  "code": 1000,
  "message": "Upload canceled successfully",
  "result": {
    "uploadId": "upload_123",
    "status": "CANCELED"
  }
}
```
---
## Error Responses

### Upload Not Found

```http
404 Not Found
```

```json
{
  "code": 1309,
  "message": "Upload session not found"
}
```

---

### Upload Cannot Be Canceled

```http
400 Bad Request
```

```json
{
  "code": 1312,
  "message": "Upload cannot be canceled"
}
```

---

## Get Upload Status

### Endpoint

```http
GET /documents/uploads/{uploadId}
```
---
## Request

### Headers

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---
## Success Response

### HTTP Status

```http
200 OK
```
---
### Response Body

```json
{
  "code": 1000,
  "message": "Get upload status successfully",
  "result": {
    "uploadId": "upload_123",
    "documentId": "doc_123",
    "status": "UPLOADING",
    "progress": 50,
    "uploadedBytes": 524288,
    "totalBytes": 1048576
  }
}
```
---
## Error Responses

### Upload Not Found

```http
404 Not Found
```

```json
{
  "code": 1309,
  "message": "Upload session not found"
}
```