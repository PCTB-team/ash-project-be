## Register Account

### Endpoint

```http
POST /auth/register
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
  "username": "bin123",
  "email": "user@example.com",
  "password": "Password@123",
  "confirmPassword": "Password@123"
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
  "message": "Register successfully. Please verify your email",
  "result": {
    "email": "user@example.com",
    "username": "bin123"
  }
}
```
---
## Error Responses

### Email Invalid

```http
400 Bad Request
```

```json
{
  "code": 1001,
  "message": "Email is invalid"
}
```

---

### Email Already Exists

```http
409 Conflict
```

```json
{
  "code": 1002,
  "message": "Email already exists"
}
```

---

### Username Already Exists

```http
409 Conflict
```

```json
{
  "code": 1003,
  "message": "Username already exists"
}
```

---

### Username Invalid

```http
400 Bad Request
```

```json
{
  "code": 1004,
  "message": "Username must be between 3 and 20 characters and contain no special characters"
}
```

---

### Password Invalid

```http
400 Bad Request
```

```json
{
  "code": 1005,
  "message": "Password must be at least 8 characters and contain at least 1 special character"
}
```

---

### Confirm Password Not Match

```http
400 Bad Request
```

```json
{
  "code": 1006,
  "message": "Confirm password does not match"
}
```
