## Login Account

### Endpoint

```http
POST /auth/login
```
---
## Request

### Headers

```http
Content-Type: application/json
```
---

### Request Body

```json
{
  "identifier": "user@example.com",
  "password": "Password@123"
}
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
  "message": "Login successfully",
  "result": {
    "accessToken": "<JWT_ACCESS_TOKEN>",
    "refreshToken": "<JWT_REFRESH_TOKEN>",
    "authenticated": true
  }
}
```
---
## Error Responses

### Identifier Required

```http
400 Bad Request
```

```json
{
  "code": 1013,
  "message": "Email or username is required"
}
```

---

### Password Required

```http
400 Bad Request
```

```json
{
  "code": 1014,
  "message": "Password is required"
}
```

---

### Account Not Verified

```http
403 Forbidden
```

```json
{
  "code": 1015,
  "message": "Account is not verified"
}
```

---

### Username Or Password Incorrect

```http
401 Unauthorized
```

```json
{
  "code": 1016,
  "message": "Username or password is incorrect"
}
```

---

### Login Attempts Exceeded

```http
429 Too Many Requests
```

```json
{
  "code": 1017,
  "message": "Login attempts exceeded. Please try again later"
}
```
