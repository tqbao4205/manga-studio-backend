# Tasks Feature — Kế hoạch phát triển

## Tổng quan

Tính năng **Tasks** cho phép:

- **MANGAKA** tạo vùng vẽ (region) trên page → giao task cho ASSISTANT
- **ASSISTANT** nhận task → làm → nộp bài
- **MANGAKA** duyệt bài nộp (approve / yêu cầu sửa lại)

---

## Các bảng trong database

### task

| Column | Type | Ghi chú |
|--------|------|---------|
| id | BIGINT IDENTITY | PK |
| region_id | BIGINT NOT NULL | FK → region(id) ON DELETE CASCADE |
| title | NVARCHAR(255) | |
| region_type | NVARCHAR(50) | CHECK: BACKGROUND, CHARACTER, TEXT, EFFECT, TONE, OTHER |
| assistant_id | BIGINT | FK → user(id) |
| assigned_by | BIGINT | FK → user(id) |
| status | NVARCHAR(50) | DEFAULT 'TODO', CHECK: TODO, IN_PROGRESS, DONE, REJECTED |
| priority | NVARCHAR(20) | DEFAULT 'MEDIUM', CHECK: LOW, MEDIUM, HIGH, URGENT |
| description | NVARCHAR(MAX) | |
| notes | NVARCHAR(MAX) | |
| reference_image_url | NVARCHAR(255) | |
| page_image_url | NVARCHAR(255) | |
| assigned_at | DATETIME | DEFAULT GETDATE() |
| due_date | DATETIME | |
| created_at | DATETIME | DEFAULT GETDATE() |

### task_submission

| Column | Type | Ghi chú |
|--------|------|---------|
| id | BIGINT IDENTITY | PK |
| task_id | BIGINT NOT NULL | FK → task(id) ON DELETE CASCADE |
| result_image_url | NVARCHAR(255) | |
| file_url | NVARCHAR(255) | |
| note | NVARCHAR(MAX) | |
| version | INT | DEFAULT 1 |
| status | NVARCHAR(50) | DEFAULT 'SUBMITTED', CHECK: SUBMITTED, APPROVED, REVISION_REQUIRED |
| submitted_at | DATETIME | DEFAULT GETDATE() |

### task_attachment

| Column | Type | Ghi chú |
|--------|------|---------|
| id | BIGINT IDENTITY | PK |
| task_id | BIGINT NOT NULL | FK → task(id) ON DELETE CASCADE |
| file_url | NVARCHAR(255) NOT NULL | |
| uploaded_at | DATETIME | DEFAULT GETDATE() |

---

## Công việc cần làm

### Phase 1: Backend — Entity Layer

**Mục tiêu:** Tạo các class Java map với 3 tables trên.

| # | File cần tạo | package | Nội dung |
|---|-------------|---------|----------|
| ✅ | `TaskStatus.java` | `model/task` | Enum: TODO, IN_PROGRESS, DONE, REJECTED |
| ✅ | `Priority.java` | `model/task` | Enum: LOW, MEDIUM, HIGH, URGENT |
| ✅ | `Task.java` | `model/task` | JPA Entity map table task |
| ✅ | `TaskSubmissionStatus.java` | `model/task` | Enum: SUBMITTED, APPROVED, REVISION_REQUIRED |
| ✅ | `TaskSubmission.java` | `model/task` | JPA Entity map table task_submission |
| ✅ | `TaskAttachment.java` | `model/task` | JPA Entity map table task_attachment |

**Chi tiết `Task.java`:**

```java
@Entity
@Table(name = "task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_type")
    private RegionType regionType;  // BACKGROUND, CHARACTER, TEXT, EFFECT, TONE, OTHER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_id")
    private User assistant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "reference_image_url")
    private String referenceImageUrl;

    @Column(name = "page_image_url")
    private String pageImageUrl;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.assignedAt == null) this.assignedAt = LocalDateTime.now();
    }
}
```

> **Lưu ý:** Entity `Region` hiện chưa có trong codebase. Cần tạo trước hoặc dùng `@Column(name = "region_id")` kiểu Long thay vì `@ManyToOne`.

---

### Phase 2: Backend — Repository Layer

**Mục tiêu:** Tạo Spring Data JPA repositories.

| # | File | package | Methods |
|---|------|---------|---------|
| ✅ | `TaskRepository.java` | `repository/task` | CRUD + `findByAssistantId`, `findByRegionId`, `findByStatus`, `findByAssistantIdAndStatus`, `findByAssignedById` |
| ✅ | `TaskSubmissionRepository.java` | `repository/task` | CRUD + `findByTaskIdOrderByVersionDesc` |
| ✅ | `TaskAttachmentRepository.java` | `repository/task` | CRUD + `findByTaskId` |

---

### Phase 3: Backend — DTO Layer

**Mục tiêu:** Tạo request/response DTOs.

| # | File | package | Fields |
|---|------|---------|--------|
| ✅ | `TaskRequest.java` | `dto/task/request` | `title, regionType, assistantId, priority, description, notes, referenceImageUrl, dueDate` |
| ✅ | `TaskResponse.java` | `dto/task/response` | `id, regionId, regionType, title, assistant(UserBrief), assignedBy(UserBrief), status, priority, description, notes, referenceImageUrl, pageImageUrl, assignedAt, dueDate, createdAt, submissions[], attachments[]` |
| ✅ | `TaskSubmissionRequest.java` | `dto/task/request` | `resultImageUrl, fileUrl, note` |
| ✅ | `TaskSubmissionResponse.java` | `dto/task/response` | `id, taskId, resultImageUrl, fileUrl, note, version, status, submittedAt` |
| ✅ | `TaskAttachmentResponse.java` | `dto/task/response` | `id, taskId, fileUrl, uploadedAt` |
| ✅ | `TaskStatusRequest.java` | `dto/task/request` | `status` |

---

### Phase 4: Backend — Service Layer

**Mục tiêu:** Xử lý business logic.

| # | File | Methods | Mô tả |
|---|------|---------|-------|
| ✅ | `TaskService.java` | `getTasks(filters)` | List tasks với filter + phân trang. ASSISTANT chỉ thấy task của mình, MANGAKA chỉ thấy task mình giao |
| | | `getTaskById(id)` | Chi tiết 1 task kèm submissions + attachments |
| | | `getTasksByRegion(regionId)` | Tasks của 1 region |
| | | `createTask(request)` | Tạo task. Validate region tồn tại, assistant có role ASSISTANT |
| | | `updateTask(id, request)` | Cập nhật task. Chỉ được sửa khi TODO / REJECTED |
| | | `updateTaskStatus(id, status)` | Đổi status theo workflow: TODO→IN_PROGRESS, IN_PROGRESS→REJECTED, REJECTED→IN_PROGRESS |
| | | `deleteTask(id)` | Xoá task. Chỉ xoá khi TODO |
| ✅ | `TaskSubmissionService.java` | `getSubmissions(taskId)` | Lịch sử nộp bài theo version giảm dần |
| | | `submitTask(taskId, request, user)` | Nộp bài. Auto increment version. Set submittedAt = now |
| | | `reviewSubmission(submissionId, status)` | Duyệt: APPROVED → task DONE, REVISION_REQUIRED → task IN_PROGRESS |
| ✅ | `TaskAttachmentService.java` | `addAttachment(taskId, fileUrl)` | Thêm file đính kèm |
| | | `deleteAttachment(id)` | Xoá file đính kèm |

**Chi tiết `TaskService.createTask()`:**

```java
public TaskResponse createTask(Long regionId, TaskRequest request, User currentUser) {
    Region region = regionRepository.findById(regionId)
        .orElseThrow(() -> new ResourceNotFoundException("Region not found"));

    User assistant = userRepository.findById(request.getAssistantId())
        .orElseThrow(() -> new ResourceNotFoundException("Assistant not found"));

    if (assistant.getRole() != Role.ASSISTANT) {
        throw new BadRequestException("User is not an ASSISTANT");
    }

    Task task = Task.builder()
        .region(region)
        .title(request.getTitle())
        .regionType(request.getRegionType())
        .assistant(assistant)
        .assignedBy(currentUser)
        .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
        .description(request.getDescription())
        .notes(request.getNotes())
        .referenceImageUrl(request.getReferenceImageUrl())
        .pageImageUrl(region.getPage().getWebImageUrl())
        .dueDate(request.getDueDate())
        .build();

    task = taskRepository.save(task);
    return taskMapper.toResponse(task);
}
```

---

### Phase 5: Backend — Controller Layer

**Mục tiêu:** REST API endpoints.

| # | Method | Endpoint | Role | Request | Response |
|---|--------|----------|------|---------|----------|
| ✅ | GET | `/api/tasks` | Authenticated | Query: status, assignedTo, priority, page, size | `Page<TaskResponse>` |
| ✅ | GET | `/api/tasks/{id}` | Authenticated | Path: id | `TaskResponse` |
| ✅ | GET | `/api/regions/{regionId}/tasks` | Authenticated | Path: regionId | `List<TaskResponse>` |
| ✅ | POST | `/api/regions/{regionId}/tasks` | MANGAKA | Path: regionId + Body: TaskRequest | `TaskResponse` 201 |
| ✅ | PUT | `/api/tasks/{id}` | MANGAKA | Path: id + Body: TaskRequest | `TaskResponse` |
| ✅ | PATCH | `/api/tasks/{id}/status` | MANGAKA/ASSISTANT | Path: id + Body: TaskStatusRequest | `TaskResponse` |
| ✅ | DELETE | `/api/tasks/{id}` | MANGAKA | Path: id | 204 No Content |
| ✅ | GET | `/api/tasks/{taskId}/submissions` | Authenticated | Path: taskId | `List<TaskSubmissionResponse>` |
| ✅ | POST | `/api/tasks/{taskId}/submissions` | ASSISTANT | Path: taskId + Body: TaskSubmissionRequest | `TaskSubmissionResponse` 201 |
| ✅ | PATCH | `/api/submissions/{id}/status` | MANGAKA | Path: id + Body: status | `TaskSubmissionResponse` |
| ✅ | POST | `/api/tasks/{taskId}/attachments` | MANGAKA | Path: taskId + Body: fileUrl | `TaskAttachmentResponse` 201 |
| ✅ | DELETE | `/api/attachments/{id}` | MANGAKA | Path: id | 204 No Content |

---

### Phase 6: Frontend — API Service

**Mục tiêu:** Kết nối frontend với API thật.

| # | Công việc | File | Mô tả |
|---|-----------|------|-------|
| ✅ | Tạo service | `src/services/taskService.js` | 11 hàm gọi API: `getTasks`, `getTaskById`, `createTask`, `updateTask`, `updateTaskStatus`, `deleteTask`, `getSubmissions`, `submitTask`, `reviewSubmission`, `addAttachment`, `deleteAttachment` |
| ✅ | Update store | `src/app/stores/taskStore.js` | Thay `mockTasks` bằng API calls |
| ✅ | Update hooks | `src/shared/hooks/useMockData.js` | Bỏ `useTasks()`, `useTaskSubmissions()`, `useTaskAttachments()` |

---

### Phase 7: Frontend — UI Tích hợp

**Mục tiêu:** Các trang dùng API thật thay vì mock data.

| # | Trang/Component | Việc cần làm |
|---|----------------|-------------|
| ✅ | `TasksPage.jsx` | Thay `useTasks()` bằng `useTaskStore()`, thêm loading/error state |
| ✅ | `TaskPanel.jsx` | `handleAssign` → `createTask`, `handleSubmit` → `submitTask`, `handleReview` → `reviewSubmission` |
| ✅ | `ReviewsPage.jsx` | Gọi API thật cho submissions, approve/revision |
| ✅ | `DashboardPage.jsx` | MangakaDashboard + AssistantDashboard dùng API thật |

---

### Phase 8: Kiểm thử

| # | Công việc | Mô tả |
|---|-----------|-------|
| ✅ | Test backend | Swagger: create task → assign → submit → approve / revision |
| ✅ | Test frontend | `docker compose up -d --build`, verify luồng từ TasksPage |
| ✅ | Fix bugs | Sửa lỗi phát sinh trong quá trình test |
| ✅ | Merge | Merge `feature/tasks` vào `main` |

---

## Luồng hoạt động

```
┌─────────────────────────────────────────────────────┐
│  MANGAKA                                            │
│  1. Upload page vào chapter                         │
│  2. Vẽ region trên page                             │
│  3. Tạo task → chọn region → gán ASSISTANT          │
│  4. Xem bài nộp → duyệt                             │
│     ├─ APPROVED → task hoàn thành                   │
│     └─ REVISION_REQUIRED → gửi lại ASSISTANT sửa    │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│  ASSISTANT                                          │
│  1. Xem danh sách task được gán                     │
│  2. Nhận task (TODO → IN_PROGRESS)                  │
│  3. Làm → nộp bài (kèm file ảnh kết quả)            │
│  4. Nếu bị yêu cầu sửa → làm lại → nộp lại          │
└─────────────────────────────────────────────────────┘
```

---

## Workflow trạng thái

```
                     ┌─────────┐
                     │  TODO   │ ← Task mới được tạo
                     └────┬────┘
                          │ ASSISTANT nhận
                          ▼
                   ┌──────────────┐
              ┌───│ IN_PROGRESS  │ ← Đang làm
              │   └──────┬───────┘
              │          │ ASSISTANT nộp bài
              │          ▼
              │   ┌──────────────┐
              │   │  SUBMITTED   │ ← Chờ duyệt (task_submission)
              │   └──────┬───────┘
              │          │ MANGAKA duyệt
              │     ┌────┴────┐
              │     │         │
              │     ▼         ▼
              │  APPROVED  REVISION_REQUIRED
              │     │         │
              │     ▼         └──────────┐
              │  ┌────────┐             │
              │  │  DONE  │             │
              │  └────────┘             ▼
              │                  ┌──────────────┐
              └────── REJECTED──│ IN_PROGRESS   │ (làm lại)
                                └──────────────┘
```

---

## Các branch dự kiến

| Branch | Nội dung | Phụ thuộc |
|--------|----------|-----------|
| `feature/task-entities` | Phase 1 + 2: Entity + Repository | — |
| `feature/task-backend` | Phase 3 + 4 + 5: DTO + Service + Controller | feature/task-entities |
| `feature/task-frontend` | Phase 6 + 7: Frontend API + UI | — (có thể làm song song) |
| `feature/tasks` | Merge tất cả vào 1 branch | 3 branch trên |

## Gợi ý phân công

| Người | Phase | Công việc | Thời gian |
|-------|-------|-----------|-----------|
| A | 1 + 2 | Entity + Repository | ~2.5 ngày |
| B | 3 + 4 + 5 | DTO + Service + Controller | ~4.5 ngày (chờ A) |
| C | 6 + 7 | Frontend API Service + UI | ~3 ngày (song song với B) |
| A/B/C | 8 | Kiểm thử + fix bug | ~1 ngày |
