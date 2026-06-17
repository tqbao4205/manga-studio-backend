package com.mangaflow.studio.dto.series.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * ── CharacterRequest DTO ──
 * DTO nhận dữ liệu từ client khi tạo hoặc cập nhật character.
 *
 * 📌 @Data (Lombok): Tự sinh getter, setter.
 *
 * 📌 Validation:
 *    name → @NotBlank (bắt buộc, không được để trống)
 *    motivation → optional (nullable — HTML từ RichEditor)
 *
 * 📌 Dùng chung cho cả POST (create) và PUT (update):
 *    - Khi create: tất cả field đều được xử lý
 *    - Khi update: name nếu null → bỏ qua (null-safe update trong Service)
 *
 * 📌 Field sketch URLs không có ở đây vì file ảnh được gửi
 *    riêng qua multipart/form-data với key "files".
 *
 * 📌 preservedSketchUrls:
 *    Chỉ dùng khi UPDATE (PUT). Là danh sách URL sketch cũ muốn GIỮ LẠI
 *    sau khi edit. Các URL không nằm trong list này sẽ bị xoá khỏi Cloudinary.
 *    Khi CREATE (POST): field này bị ignore (null).
 */
@Data
public class CharacterRequest {

    @NotBlank(message = "Character name is required")
    private String name;

    private String motivation;

    private List<String> preservedSketchUrls;
}
