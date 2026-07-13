# GetDoc API - Admin Documents

File nay mo ta API lay danh sach document cho man hinh Admin Page 3.

## Base Endpoint

```http
GET /admin/documents
```

Endpoint nay dung chung cho:

- Lay tat ca document
- Tim kiem document bang keyword
- Loc document theo file type
- Vua tim kiem keyword vua loc theo file type

## Authentication

Can dang nhap bang tai khoan co role `ADMIN`.

Header:

```http
Authorization: Bearer <access_token>
```

## Query Params

| Param | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `page` | number | No | `0` | So trang, bat dau tu `0` |
| `size` | number | No | `20` | So item moi trang |
| `keyword` | string | No | none | Tim theo `fileName` hoac `ownerUsername` |
| `fileType` | string | No | `ALL` | Loc theo loai file |

Gia tri hop le cua `fileType`:

| fileType | Extensions duoc tinh |
| --- | --- |
| `ALL` | Tat ca file |
| `DOCUMENT` | `PDF`, `DOC`, `DOCX`, `TXT`, `XLSX` |
| `IMAGE` | `PNG`, `JPG`, `JPEG`, `GIF`, `WEBP` |
| `AUDIO` | `MP3`, `WAV`, `FLAC` |
| `VIDEO` | `MP4`, `AVI`, `MKV`, `MOV` |

## 1. Lay Tat Ca Document

FE chi can truyen `page` va `size`.

```http
GET /admin/documents?page=0&size=20
```

Y nghia:

- Khong truyen `keyword`
- Khong truyen `fileType`
- Backend se lay tat ca document va sap xep theo `createdAt` giam dan

## 2. Lay Document Co Keyword

```http
GET /admin/documents?page=0&size=20&keyword=lesson
```

Y nghia:

- Tim document co `fileName` chua `lesson`
- Hoac owner co `username` chua `lesson`
- Khong loc theo file type

Vi du khac:

```http
GET /admin/documents?page=0&size=20&keyword=student01
```

## 3. Lay Document Theo Type

```http
GET /admin/documents?page=0&size=20&fileType=DOCUMENT
```

Y nghia:

- Khong tim keyword
- Chi lay cac file thuoc nhom document: `PDF`, `DOC`, `DOCX`, `TXT`, `XLSX`

Vi du lay image:

```http
GET /admin/documents?page=0&size=20&fileType=IMAGE
```

## 4. Lay Document Co Keyword Va Type

```http
GET /admin/documents?page=0&size=20&keyword=lesson&fileType=DOCUMENT
```

Y nghia:

- Tim document theo keyword `lesson`
- Sau do loc chi lay cac file thuoc nhom `DOCUMENT`

Vi du tim anh cua user:

```http
GET /admin/documents?page=0&size=20&keyword=student01&fileType=IMAGE
```

## Response

Response tra ve theo format chung `ApiResponse`.

```json
{
  "code": 1000,
  "message": null,
  "result": {
    "content": [
      {
        "id": "document-id",
        "fileName": "lesson.pdf",
        "fileExtension": "pdf",
        "fileSize": 1048576,
        "ownerUsername": "student01",
        "ownerEmail": "student@example.com",
        "deleted": false,
        "createdAt": "2026-06-30T14:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "number": 0,
    "size": 20,
    "numberOfElements": 1,
    "empty": false
  }
}
```

## FE Mapping

FE nen doc data tai:

```js
const documents = response.result.content;
const totalElements = response.result.totalElements;
const totalPages = response.result.totalPages;
const currentPage = response.result.number;
const pageSize = response.result.size;
```

## Goi API Mau Cho FE

```js
async function getAdminDocuments({ page = 0, size = 20, keyword, fileType } = {}) {
  const params = new URLSearchParams();
  params.set("page", page);
  params.set("size", size);

  if (keyword && keyword.trim()) {
    params.set("keyword", keyword.trim());
  }

  if (fileType && fileType !== "ALL") {
    params.set("fileType", fileType);
  }

  const response = await fetch(`/admin/documents?${params.toString()}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  return response.json();
}
```

## Luu Y

- `page` bat dau tu `0`, khong phai `1`.
- Muon get all thi chi can `page` va `size`.
- `keyword` va `fileType` la optional.
- Neu FE chon tab/filter `ALL`, co the khong gui `fileType`, hoac gui `fileType=ALL` deu duoc.
- Hien tai backend admin documents co tra ca document da bi delete qua field `deleted`. Neu FE chi muon hien active document thi nen filter `deleted === false` o FE, hoac backend can sua them dieu kien.
