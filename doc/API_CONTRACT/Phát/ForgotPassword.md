# Password Recovery (Forgot Password) API Specification

---

## 📌 STEP 1: Request Password Reset OTP

### Endpoint
POST /auth/forgot-password/send-otp

### Headers
Content-Type: application/json

### Request Body
{
"email": "user@example.com"
}

### Success Response
HTTP Status: 200 OK
{
"code": 1000,
"message": "Verification OTP has been successfully dispatched to your email address.",
"result": {
"email": "user@example.com"
}
}

### Error Responses

#### Email Invalid
HTTP Status: 400 Bad Request
{
"code": 1001,
"message": "Email is invalid"
}

#### Email Not Found
HTTP Status: 404 Not Found
{
"code": 1023,
"message": "Email does not exist in the system"
}

#### OTP Send Limit Exceeded
HTTP Status: 429 Too Many Requests
{
"code": 1008,
"message": "OTP send limit exceeded"
}

#### OTP Resend Too Soon
HTTP Status: 429 Too Many Requests
{
"code": 1009,
"message": "Please wait 60 seconds before requesting a new OTP"
}

---

## 📌 STEP 2: Verify OTP Code

### Endpoint
POST /auth/forgot-password/verify-otp

### Headers
Content-Type: application/json

### Request Body
{
"email": "user@example.com",
"otp": "123456"
}

### Success Response
HTTP Status: 200 OK
{
"code": 1000,
"message": "OTP verification successful. Use the provided reset token to update your password.",
"result": {
"resetToken": "b2f6d891-da73-490b-bc7d-1a8e9f2d3c4b"
}
}

### Error Responses

#### Invalid OTP Code
HTTP Status: 400 Bad Request
{
"code": 1024,
"message": "Invalid OTP verification code"
}

#### Expired OTP Code
HTTP Status: 400 Bad Request
{
"code": 1025,
"message": "OTP code has expired or does not exist"
}

---

## 📌 STEP 3: Reset to New Password

### Endpoint
POST /auth/forgot-password/reset

### Headers
Content-Type: application/json

### Request Body
{
"resetToken": "b2f6d891-da73-490b-bc7d-1a8e9f2d3c4b",
"newPassword": "NewSecurePassword@123",
"confirmPassword": "NewSecurePassword@123"
}

### Success Response
HTTP Status: 200 OK
{
"code": 1000,
"message": "Password reset successfully.",
"result": null
}

### Error Responses

#### Password Structure Invalid
HTTP Status: 400 Bad Request
{
"code": 1005,
"message": "Password must be at least 8 characters and contain at least 1 special character"
}

#### Confirm Password Mismatch
HTTP Status: 400 Bad Request
{
"code": 1027,
"message": "Confirm password does not match new password"
}

#### Invalid or Expired Reset Token
HTTP Status: 401 Unauthorized
{
"code": 1026,
"message": "Session has expired or password reset token is invalid"
}

---

## 💾 Unified Custom Error Codes Mapping

| Custom Code | HTTP Status | Message | Module Scope |
| :--- | :--- | :--- | :--- |
| 1000 | 200 OK | Success Operation | Global / All |
| 1001 | 400 Bad Request | Email is invalid | Register / Forgot Password |
| 1002 | 409 Conflict | Email already exists | Register |
| 1003 | 409 Conflict | Username already exists | Register |
| 1004 | 400 Bad Request | Username must be between 3 and 20 characters and contain no special characters | Register |
| 1005 | 400 Bad Request | Password must be at least 8 characters and contain at least 1 special character | Register / Forgot Password |
| 1006 | 400 Bad Request | Confirm password does not match | Register |
| 1008 | 429 Too Many Requests | OTP send limit exceeded | Register / Forgot Password |
| 1009 | 429 Too Many Requests | Please wait 60 seconds before requesting a new OTP | Register / Forgot Password |
| 1023 | 404 Not Found | Email does not exist in the system | Forgot Password |
| 1024 | 400 Bad Request | Invalid OTP verification code | Forgot Password |
| 1025 | 400 Bad Request | OTP code has expired or does not exist | Forgot Password |
| 1026 | 401 Unauthorized | Session has expired or password reset token is invalid | Forgot Password |
| 1027 | 400 Bad Request | Confirm password does not match new password | Forgot Password |
