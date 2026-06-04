## Redis API

### Endpoint

```http
POST /redis/set
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
  "key": "otp:user@example.com",
  "value": "123456"
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
  "message": "Set key successfully",
  "result": "otp:user@example.com"
}
```
---
## Error Responses

### Key Required

```http
400 Bad Request
```

```json
{
  "code": 1101,
  "message": "Key is required"
}
```

---

### Value Required

```http
400 Bad Request
```

```json
{
  "code": 1102,
  "message": "Value is required"
}
```

---

## Redis Set With TTL

### Endpoint

```http
POST /redis/set-with-ttl
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
  "key": "otp:user@example.com",
  "value": "123456",
  "ttlSeconds": 60
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
  "message": "Set redis key with ttl successfully",
  "result": "otp:user@example.com"
}
```
---
## Error Responses

### Key Required

```http
400 Bad Request
```

```json
{
  "code": 1101,
  "message": "Key is required"
}
```

---

### Value Required

```http
400 Bad Request
```

```json
{
  "code": 1102,
  "message": "Value is required"
}
```

---

### TTL Invalid

```http
400 Bad Request
```

```json
{
  "code": 1103,
  "message": "TTL must be greater than 0"
}
```

---

## Redis Get

### Endpoint

```http
GET /redis/get?key=otp:user@example.com
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
  "message": "Get redis key successfully",
  "result": "123456"
}
```
---
## Error Responses

### Key Required

```http
400 Bad Request
```

```json
{
  "code": 1101,
  "message": "Key is required"
}
```

---

## Redis Delete

### Endpoint

```http
DELETE /redis/delete?key=otp:user@example.com
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
  "message": "Delete redis key successfully",
  "result": true
}
```

---

## Redis Increment

### Endpoint

```http
POST /redis/increment?key=login:fail:user@example.com
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
  "message": "Increment redis key successfully",
  "result": 1
}
```

---

## Redis Expire

### Endpoint

```http
POST /redis/expire?key=login:fail:user@example.com&ttlSeconds=300
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
  "message": "Set expire successfully",
  "result": true
}
```

---

## Redis TTL

### Endpoint

```http
GET /redis/ttl?key=login:fail:user@example.com
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
  "message": "Get redis ttl successfully",
  "result": 287
}
```
