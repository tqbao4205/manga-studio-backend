package com.mangaflow.studio.model.series;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ── Character Entity ──
 * Ánh xạ tới bảng "characters" trong database.
 *
 * 📌 Mỗi Character thuộc về 1 Series (@ManyToOne).
 * Khi xoá Series → xoá luôn Character (cascade = ALL).
 *
 * 📌 sketchUrls là danh sách URL ảnh phác thảo,
 * lưu trong bảng phụ "character_sketch_urls"
 * (character_id, sketch_url).
 *
 * 📌 motivation là HTML string từ RichEditor trên FE
 * (core motivation của nhân vật).
 */
@Entity
@Table(name = "characters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    /**
     * id: Khoá chính, tự động tăng (IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * series: Series chứa character này (N:1).
     * LAZY fetch → chỉ load khi cần.
     * Không cho phép null — mỗi character phải thuộc 1 series.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    /**
     * name: Tên nhân vật.
     * NOT NULL — bắt buộc nhập.
     */
    @Column(nullable = false)
    private String name;

    /**
     * motivation: Core motivation của nhân vật.
     * Lưu HTML string từ RichEditor (Tiptap).
     * columnDefinition = "TEXT" → không giới hạn độ dài.
     */
    @Column(columnDefinition = "TEXT")
    private String motivation;

    /**
     * sketchUrls: Danh sách URL ảnh phác thảo từ Cloudinary.
     * @ElementCollection → tạo bảng phụ "character_sketch_urls"
     * (character_id, sketch_url) để lưu nhiều URL cho 1 character.
     * được upload với key "files" từ FE (multipart).
     */
    @ElementCollection
    @CollectionTable(
        name = "character_sketch_urls",
        joinColumns = @JoinColumn(name = "character_id")
    )
    @Column(name = "sketch_url", nullable = false)
    private List<String> sketchUrls = new ArrayList<>();

    /**
     * createdAt: Thời điểm tạo character.
     * @Column(updatable = false) → chỉ set 1 lần khi insert.
     */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * updatedAt: Thời điểm cập nhật gần nhất.
     * Set tự động trong @PrePersist và @PreUpdate.
     */
    private LocalDateTime updatedAt;

    /**
     * ── @PrePersist ──
     * JPA lifecycle callback — tự động chạy TRƯỚC KHI insert.
     * Set createdAt + updatedAt lần đầu.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ── @PreUpdate ──
     * JPA lifecycle callback — tự động chạy TRƯỚC KHI update.
     * Chỉ set updatedAt (createdAt giữ nguyên).
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
