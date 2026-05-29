# User Module

## Get All Users

### Endpoint

```http
GET /users
```

### Authorization

```text
ADMIN
```

### Header

```http
Authorization: Bearer <token>
```

### Request

### Success Response

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "123",
      "username": "admin",
      "fullname": "Administrator",
      "email": "admin@gmail.com",
      "roles": ["ADMIN"]
    }
  ]
}
```
