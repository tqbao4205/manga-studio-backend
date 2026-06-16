package com.mangaflow.studio.controller.series;

import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.dto.series.request.CharacterRequest;
import com.mangaflow.studio.dto.series.response.CharacterResponse;
import com.mangaflow.studio.service.series.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/series/{seriesId}/characters")
@RequiredArgsConstructor
@Tag(name = "Character", description = "API quản lý nhân vật (character) trong series")
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "Danh sách characters",
               description = "Lấy danh sách tất cả characters của 1 series.")
    @GetMapping
    public ResponseEntity<List<CharacterResponse>> getAllBySeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(characterService.getAllBySeries(seriesId));
    }

    @Operation(summary = "Chi tiết character",
               description = "Xem thông tin chi tiết của 1 character.")
    @GetMapping("/{id}")
    public ResponseEntity<CharacterResponse> getById(@PathVariable Long seriesId,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(characterService.getById(id));
    }

    @Operation(summary = "Tạo character mới",
               description = "Mangaka tạo character mới. Gửi multipart/form-data với phần 'character' (JSON) + 'files' (ảnh sketch, tối đa 5 file).")
    @ApiResponse(responseCode = "201", description = "Tạo character thành công")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<CharacterResponse> create(
            @PathVariable Long seriesId,
            @RequestPart("character") @Valid CharacterRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(characterService.create(seriesId, request, files, user));
    }

    @Operation(summary = "Cập nhật character",
               description = "Mangaka cập nhật character. Files ảnh sketch là tuỳ chọn; nếu có sẽ thay thế toàn bộ ảnh cũ.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<CharacterResponse> update(
            @PathVariable Long seriesId,
            @PathVariable Long id,
            @RequestPart("character") @Valid CharacterRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(characterService.update(seriesId, id, request, files, user));
    }

    @Operation(summary = "Xoá character",
               description = "Mangaka xoá character khỏi series.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANGAKA')")
    public ResponseEntity<Void> delete(
            @PathVariable Long seriesId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        characterService.delete(seriesId, id, user);
        return ResponseEntity.noContent().build();
    }
}
