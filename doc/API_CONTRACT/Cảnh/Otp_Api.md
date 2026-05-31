## Send OTP

### Endpoint
 
```http
POST /auth/send-otp
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
  "email": "user@example.com"
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
  "message": "OTP sent successfully",
  "result": {
    "email": "user@example.com"
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

### Email Not Found

```http
404 Not Found
```

```json
{
  "code": 1007,
  "message": "Email not found"
}
```

---

### OTP Send Limit Exceeded

```http
429 Too Many Requests
```

```json
{
  "code": 1008,
  "message": "OTP send limit exceeded"
}
```

---

## Resend OTP

### Endpoint

```http
POST /auth/resend-otp
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
  "email": "user@example.com"
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
  "message": "OTP resent successfully",
  "result": {
    "email": "user@example.com"
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

### Email Not Found

```http
404 Not Found
```

```json
{
  "code": 1007,
  "message": "Email not found"
}
```

---

### OTP Resend Too Soon

```http
429 Too Many Requests
```

```json
{
  "code": 1009,
  "message": "Please wait 60 seconds before requesting a new OTP"
}
```

---

### OTP Send Limit Exceeded

```http
429 Too Many Requests
```

```json
{
  "code": 1008,
  "message": "OTP send limit exceeded"
}
```

---

## Verify OTP

### Endpoint

```http
POST /auth/verify-otp
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
  "email": "user@example.com",
  "otp": "123456"
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
  "message": "Account verified successfully",
  "result": {
    "email": "user@example.com",
    "verified": true
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

### Email Not Found

```http
404 Not Found
```

```json
{
  "code": 1007,
  "message": "Email not found"
}
```

---

### OTP Invalid

```http
400 Bad Request
```

```json
{
  "code": 1010,
  "message": "OTP is invalid"
}
```

---

### OTP Expired

```http
400 Bad Request
```

```json
{
  "code": 1011,
  "message": "OTP has expired"
}
```

---

### Account Already Verified

```http
409 Conflict
```

```json
{
  "code": 1012,
  "message": "Account already verified"
}
```