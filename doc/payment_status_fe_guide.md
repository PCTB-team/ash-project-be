# Payment Status FE Guide

## Root Cause To Fix On FE

Do not treat a PayOS return/callback URL as successful just because query params exist.
The callback query can contain `status=CANCELLED`, and that must render as a cancelled payment.

Bad patterns to avoid:

```ts
if (status) showSuccess();
if (code) showSuccess();
default: showSuccess();
```

## Callback Result API

After PayOS redirects back to FE, read the query params and call:

```http
GET /api/v1/payment/callback?orderCode={orderCode}&status={status}&code={code}
```

`status` and `code` are optional because some gateways may omit one of them.
`orderCode` is required.

Example cancelled callback:

```http
GET /api/v1/payment/callback?orderCode=123456789&status=CANCELLED
```

Response:

```json
{
  "result": {
    "transactionId": "uuid",
    "orderCode": 123456789,
    "status": "CANCELLED",
    "displayStatus": "Đã hủy",
    "title": "Đã hủy thanh toán",
    "message": "Giao dịch đã bị hủy. Không có khoản phí nào được tính vào tài khoản của bạn.",
    "planGranted": false
  }
}
```

FE should render `title` and `message` from this API instead of deriving success from URL presence.

## Status Mapping

Keep backend status values unchanged. Use this display mapping:

| Raw status | Display label | Result title |
| --- | --- | --- |
| `PENDING` | Chờ xử lý | Đang chờ thanh toán |
| `SUCCESS` | Thành công | Đã thanh toán |
| `PAID` | Thành công | Đã thanh toán |
| `CANCELLED` | Đã hủy | Đã hủy thanh toán |
| `CANCELED` | Đã hủy | Đã hủy thanh toán |
| `FAILED` | Thất bại | Thanh toán thất bại |
| `EXPIRED` | Hết hạn | Thanh toán đã hết hạn |
| `TIMEOUT` | Hết hạn | Thanh toán đã hết hạn |

## Required FE Behavior

- `status=CANCELLED` must show title `Đã hủy thanh toán`.
- `status=CANCELLED` must show message `Giao dịch đã bị hủy. Không có khoản phí nào được tính vào tài khoản của bạn.`
- Do not show `Đã thanh toán` unless normalized status is `SUCCESS` or `PAID`.
- Do not assume unknown/missing status is success. Missing status should stay pending or show a neutral verification state.
- Admin payment table should display the status returned by `/admin/payments`; do not map `CANCELLED` to `SUCCESS`.

## Backend Grant Rule

The backend grants or extends a storage plan only when callback/webhook confirms `SUCCESS` or `PAID`, and only if the transaction is still `PENDING`.

No plan is granted for:

- `PENDING`
- `CANCELLED`
- `CANCELED`
- `FAILED`
- `EXPIRED`
- `TIMEOUT`
- missing/unknown status

Repeated callback/webhook calls for the same `orderCode` are idempotent because non-`PENDING` transactions are not granted again.
