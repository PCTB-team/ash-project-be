## Login

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
  "password": "P@ssw0rd!"
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
    "refreshToken": "<REFRESH_TOKEN>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 12,
      "username": "johndoe",
      "email": "user@example.com",
      "fullname": "John Doe"
    }
  }
}
```

---

## Error Responses

### Identifier Is Required

```http
400 Bad Request
```

```json
{
  "code": 1017,
  "message": "Identifier is required"
}
```

---

### Password Is Required

```http
400 Bad Request
```

```json
{
  "code": 1018,
  "message": "Password is required"
}
```

---

### Invalid Credentials

```http
401 Unauthorized
```

```json
{
  "code": 1019,
  "message": "Invalid email/username or password"
}
```

---

### Account Not Verified

```http
403 Forbidden
```

```json
{
  "code": 1020,
  "message": "Account is not verified"
}
```

---

### Login Attempts Exceeded

```http
429 Too Many Requests
```

```json
{
  "code": 1021,
  "message": "Too many login attempts. Please try again later"
}
```
