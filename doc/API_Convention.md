AI Study Hub - API Convention
0. # Environment Variables
 Phải cấu hình biến môi trường
```env

```
1. ## Base URL
   Local Environment:http://localhost:8080/api/v1
2. ## Response Format
     Tất cả API phải trả về format:
    {
    "code": 1000,
    "message": "Success",
    "result": {}
    }
3. ## Success Code
      1000 : success
4. ## Error Code Convention
    Ví dụ :    

| Code | Description |
|------|-------------|
| 2001 | User already exists |
| 2002 | User not found |

# 5. Endpoint Naming Convention

Use RESTful API.

## Correct

```text
GET /users
GET /users/{id}
POST /users
PUT /users/{id}
DELETE /users/{id}
```

## Incorrect

```text
GET /getAllUsers
POST /createUser
DELETE /deleteUser
```

## Rule

```text
URL = Resource
HTTP Method = Action
```

---

# 6. HTTP Status Convention

| Status | Meaning |
|---------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthenticated |
| 403 | Forbidden |
| 404 | Not Found |
| 500 | Internal Server Error |

---
# 7. Authentication Convention

Protected APIs require:

```http
Authorization: Bearer <access_token>
```

Public APIs:

```text
/auth/login
/auth/register
/swagger-ui/**
/v3/api-docs/**
```

---

# 8. Request DTO Naming

```text
LoginRequest
RegisterRequest
CreateDocumentRequest
UpdateProfileRequest
```

---

# 9. Response DTO Naming

```text
LoginResponse
RegisterResponse
UserResponse
DocumentResponse
StorageResponse
```

---

# 10. JSON Convention

Use camelCase.

Example:

```json
{
  "firstName": "Bin",
  "lastName": "Tran"
}
```

---

# 11. Security Convention

## Authentication

- JWT
- Stateless

## Authorization

- Role-based access control

---

# 12. Swagger

Local URL:

```text
http://localhost:8080/api/v1/swagger-ui/index.html
```

---

# 13. CORS

Allowed Origins:

```text
http://localhost:3000
http://localhost:5173
```


