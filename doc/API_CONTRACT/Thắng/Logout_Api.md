## Logout Account

### Endpoint

```http
POST /auth/logout
```
---
## Request

### Headers

```http
Content-Type: application/json
Authorization: Bearer <JWT_ACCESS_TOKEN>
```
---

### Request Body

```json
{
  "refreshToken": "<JWT_REFRESH_TOKEN>"
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

### Refresh Token Missing

```http
400 Bad Request
```

```json
{
  "code": 1018,
  "message": "Refresh token is required"
}
```

---

### Refresh Token Invalid

```http
401 Unauthorized
```

```json
{
  "code": 1019,
  "message": "Refresh token is invalid"
}
```

---

### Token Expired

```http
401 Unauthorized
```

```json
{
  "code": 1020,
  "message": "Token has expired"
}
```

---

### Account Already Logged Out

```http
409 Conflict
```

```json
{
  "code": 1021,
  "message": "Account already logged out"
}
```

---

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
