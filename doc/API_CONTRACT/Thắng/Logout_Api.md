## Logout

### Endpoint
 
```http
POST /auth/logout
```

---

## Request

### Headers

```http
Content-Type: application/json
Authorization: Bearer <ACCESS_TOKEN>
```

---

### Request Body

```json
{
  "refreshToken": "<REFRESH_TOKEN>"
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
  "message": "Logout successfully",
  "result": {
    "loggedOut": true
  }
}
```

---

## Error Responses

### Unauthorized

```http
401 Unauthorized
```

```json
{
  "code": 1022,
  "message": "Unauthorized"
}
```

---

### Access Token Invalid

```http
401 Unauthorized
```

```json
{
  "code": 1023,
  "message": "Access token is invalid"
}
```

---

### Refresh Token Invalid

```http
400 Bad Request
```

```json
{
  "code": 1024,
  "message": "Refresh token is invalid"
}
```

---

### Token Already Logged Out

```http
409 Conflict
```

```json
{
  "code": 1025,
  "message": "Token already logged out"
}
```
