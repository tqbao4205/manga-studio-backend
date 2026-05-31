# Tasks API Endpoints

## Quyền truy cập

| Role | Xem | Tạo/Sửa/Xoá Task | Nộp bài | Duyệt bài |
|------|-----|------------------|---------|-----------|
| MANAGAKA | ✅ | ✅ | ❌ | ✅ |
| ASSISTANT | ✅ (của mình) | ❌ | ✅ | ❌ |
| TANTOU_EDITOR | ✅ | ❌ | ❌ | ❌ |
| EDITORIAL_BOARD | ✅ | ❌ | ❌ | ❌ |

---

## 1. Task CRUD

### GET /api/tasks — Danh sách tasks

**Query params:** `status, assignedTo, assignedBy, priority, regionId, seriesId, page, size`

**Response:** `Page<TaskResponse>`

---

### GET /api/tasks/{id} — Chi tiết 1 task

**Response:** `TaskResponse` + submissions + attachments

---

### GET /api/regions/{regionId}/tasks — Tasks của region

**Response:** `TaskResponse[]`

---

### POST /api/regions/{regionId}/tasks — Tạo task mới

**Role:** MANAGAKA

**Body:**
```json
{
  "title": "Vẽ nhân vật chính",
  "regionType": "CHARACTER",
  "assistantId": 5,
  "priority": "HIGH",
  "description": "...",
  "notes": "...",
  "referenceImageUrl": "https://...",
  "dueDate": "2026-06-05T00:00:00"
}
```

**Response 201:** `TaskResponse`

---

### PUT /api/tasks/{id} — Cập nhật task

**Role:** MANAGAKA | **Chỉ sửa được khi TODO / REJECTED**

**Response 200:** `TaskResponse`

---

### PATCH /api/tasks/{id}/status — Đổi trạng thái

**Body:** `{ "status": "IN_PROGRESS" }`

| Chuyển | → | Ai được làm |
|--------|---|-------------|
| TODO → IN_PROGRESS | | ASSISTANT |
| IN_PROGRESS → REJECTED | | MANAGAKA |
| REJECTED → IN_PROGRESS | | ASSISTANT |

**Response 200:** `TaskResponse`

---

### DELETE /api/tasks/{id} — Xoá task

**Role:** MANAGAKA | **Chỉ xoá khi TODO**

**Response 204:** No content

---

## 2. Task Submission (Nộp bài)

### GET /api/tasks/{taskId}/submissions — Lịch sử nộp bài

**Response:** `TaskSubmissionResponse[]` (sort version DESC)

---

### POST /api/tasks/{taskId}/submissions — ASSISTANT nộp bài

**Role:** ASSISTANT | **Task phải đang IN_PROGRESS hoặc REJECTED**

**Body:**
```json
{
  "resultImageUrl": "https://...",
  "fileUrl": "https://...",
  "note": "Đã vẽ xong"
}
```

**Response 201:** `TaskSubmissionResponse`

---

### PATCH /api/submissions/{id}/status — MANAGAKA duyệt bài

**Role:** MANAGAKA

**Body:** `{ "status": "APPROVED" }`

| Status | Task sau duyệt |
|--------|----------------|
| APPROVED | → DONE |
| REVISION_REQUIRED | → IN_PROGRESS |

**Response 200:** `TaskSubmissionResponse`

---

## 3. Task Attachment (File đính kèm)

### POST /api/tasks/{taskId}/attachments — Thêm file

**Role:** MANAGAKA

**Body:** `{ "fileUrl": "https://..." }`

**Response 201:** `TaskAttachmentResponse`

---

### DELETE /api/attachments/{id} — Xoá file

**Role:** MANAGAKA

**Response 204:** No content

---

## Tổng kết

| # | Method | Endpoint | Role |
|---|--------|----------|------|
| 1 | GET | `/api/tasks` | Authenticated |
| 2 | GET | `/api/tasks/{id}` | Authenticated |
| 3 | GET | `/api/regions/{regionId}/tasks` | Authenticated |
| 4 | POST | `/api/regions/{regionId}/tasks` | MANAGAKA |
| 5 | PUT | `/api/tasks/{id}` | MANAGAKA |
| 6 | PATCH | `/api/tasks/{id}/status` | MANAGAKA / ASSISTANT |
| 7 | DELETE | `/api/tasks/{id}` | MANAGAKA |
| 8 | GET | `/api/tasks/{taskId}/submissions` | Authenticated |
| 9 | POST | `/api/tasks/{taskId}/submissions` | ASSISTANT |
| 10 | PATCH | `/api/submissions/{id}/status` | MANAGAKA |
| 11 | POST | `/api/tasks/{taskId}/attachments` | MANAGAKA |
| 12 | DELETE | `/api/attachments/{id}` | MANAGAKA |
