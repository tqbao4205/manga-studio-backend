package com.mangaflow.studio.service.series;

import com.mangaflow.studio.common.exception.AppException;
import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.dto.series.request.CharacterRequest;
import com.mangaflow.studio.dto.series.request.CharactersBatchRequest;
import com.mangaflow.studio.dto.series.response.CharacterResponse;
import com.mangaflow.studio.model.series.Character;
import com.mangaflow.studio.model.series.Series;
import com.mangaflow.studio.repository.series.CharacterRepository;
import com.mangaflow.studio.repository.series.SeriesRepository;
import com.mangaflow.studio.service.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ── CharacterService ──
 * Service chịu trách nhiệm CRUD cho Character entity.
 *
 * 📌 @Service: Spring Bean — chứa business logic Character.
 * 📌 @RequiredArgsConstructor: Lombok — DI constructor (final fields).
 *
 * ══════════════════════════════════════════════════
 *  Scope:
 * ══════════════════════════════════════════════════
 *  ✅ getAllBySeries()   — Query: danh sách characters của 1 series
 *  ✅ getById()          — Query: chi tiết 1 character
 *  ✅ create()           — Command: tạo mới character (MANGAKA)
 *  ✅ update()           — Command: cập nhật character (MANGAKA)
 *  ✅ delete()           — Command: xoá character (MANGAKA)
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final SeriesRepository seriesRepository;
    private final CloudinaryService cloudinaryService;
    private final ExecutorService uploadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ════════════════════════════════════════════════════════════
    // 1. GET ALL BY SERIES — Danh sách characters của 1 series
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CharacterResponse> getAllBySeries(Long seriesId) {
        List<Character> characters = characterRepository.findBySeriesId(seriesId);
        return characters.stream().map(this::toResponse).toList();
    }

    // ════════════════════════════════════════════════════════════
    // 2. GET BY ID — Chi tiết 1 character
    // ════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public CharacterResponse getById(Long id) {
        Character character = characterRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Character not found"));
        return toResponse(character);
    }

    // ════════════════════════════════════════════════════════════
    // 3. CREATE — Tạo character mới (kèm upload sketches)
    // ════════════════════════════════════════════════════════════

    @Transactional
    public CharacterResponse create(Long seriesId, CharacterRequest request,
                                     List<MultipartFile> files, CustomUserDetails user) {
        if (!user.getRole().equals("MANGAKA")) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only MANGAKA can create characters");
        }

        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        Character character = Character.builder()
                .name(request.getName())
                .motivation(request.getMotivation())
                .series(series)
                .sketchUrls(new ArrayList<>())
                .build();

        character = characterRepository.save(character);
        final Long savedCharacterId = character.getId();

        if (files != null && !files.isEmpty()) {
            List<String> sketchUrls = new ArrayList<>();
            int maxIndex = Math.min(files.size(), 5);
            List<CompletableFuture<String>> futures = new ArrayList<>();
            for (int i = 0; i < maxIndex; i++) {
                final int idx = i;
                final MultipartFile f = files.get(i);
                if (!f.isEmpty()) {
                    futures.add(CompletableFuture.supplyAsync(() ->
                            cloudinaryService.uploadCharacterSketch(f, seriesId, savedCharacterId, idx),
                            uploadExecutor));
                }
            }
            sketchUrls = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            character.setSketchUrls(sketchUrls);
        }

        return toResponse(character);
    }

    // ════════════════════════════════════════════════════════════
    // 3b. CREATE BATCH — Tạo nhiều characters trong 1 request
    // ════════════════════════════════════════════════════════════

    @Transactional
    public List<CharacterResponse> createBatch(Long seriesId, CharactersBatchRequest batchRequest,
                                                List<MultipartFile> allFiles, CustomUserDetails user) {
        if (!user.getRole().equals("MANGAKA")) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only MANGAKA can create characters");
        }

        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        List<CharacterResponse> responses = new ArrayList<>();
        int fileOffset = 0;

        for (CharactersBatchRequest.CharacterEntry entry : batchRequest.getCharacters()) {
            Character character = Character.builder()
                    .name(entry.getName())
                    .motivation(entry.getMotivation())
                    .series(series)
                    .sketchUrls(new ArrayList<>())
                    .build();

            character = characterRepository.save(character);
            final Long savedCharacterId = character.getId();

            if (entry.getFileCount() > 0 && allFiles != null) {
                int end = Math.min(fileOffset + entry.getFileCount(), allFiles.size());
                List<MultipartFile> charFiles = allFiles.subList(fileOffset, end);
                fileOffset = end;

                List<String> sketchUrls = new ArrayList<>();
                int maxIndex = Math.min(charFiles.size(), 5);
                List<CompletableFuture<String>> futures = new ArrayList<>();
                for (int i = 0; i < maxIndex; i++) {
                    final int idx = i;
                    final MultipartFile f = charFiles.get(i);
                    if (!f.isEmpty()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                cloudinaryService.uploadCharacterSketch(f, seriesId, savedCharacterId, idx),
                                uploadExecutor));
                    }
                }
                sketchUrls = futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                character.setSketchUrls(sketchUrls);
            }

            responses.add(toResponse(character));
        }

        return responses;
    }

    // ════════════════════════════════════════════════════════════
    // 4. UPDATE — Cập nhật character (kèm upload sketches mới)
    // ════════════════════════════════════════════════════════════

    @Transactional
    public CharacterResponse update(Long seriesId, Long characterId,
                                     CharacterRequest request,
                                     List<MultipartFile> files, CustomUserDetails user) {
        if (!user.getRole().equals("MANGAKA")) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only MANGAKA can update characters");
        }

        seriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Series not found"));

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Character not found"));

        if (!character.getSeries().getId().equals(seriesId)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Character does not belong to this series");
        }

        if (request.getName() != null) {
            character.setName(request.getName());
        }
        if (request.getMotivation() != null) {
            character.setMotivation(request.getMotivation());
        }

        // ── Xử lý sketches: merge preservedUrls + new files ──
        // preservedSketchUrls != null → FE đã gửi danh sách URL muốn giữ lại
        // → merge với files mới, xoá ảnh bị remove khỏi Cloudinary
        List<String> finalSketchUrls = new ArrayList<>();

        if (request.getPreservedSketchUrls() != null) {
            // Giữ lại các URL cũ theo danh sách FE gửi
            finalSketchUrls.addAll(request.getPreservedSketchUrls());

            // Xoá khỏi Cloudinary các ảnh cũ không còn trong preserved list
            List<String> removedUrls = character.getSketchUrls().stream()
                    .filter(url -> !request.getPreservedSketchUrls().contains(url))
                    .toList();
            List<CompletableFuture<Void>> deleteFutures = new ArrayList<>();
            for (String url : removedUrls) {
                deleteFutures.add(CompletableFuture.runAsync(
                        () -> cloudinaryService.deleteImageByUrl(url), uploadExecutor));
            }
            deleteFutures.forEach(CompletableFuture::join);
        }

        // Upload files mới (nếu có) — append vào cuối danh sách
        if (files != null && !files.isEmpty()) {
            int maxNew = Math.min(files.size(), Math.max(0, 5 - finalSketchUrls.size()));
            List<CompletableFuture<String>> uploadFutures = new ArrayList<>();
            for (int i = 0; i < maxNew; i++) {
                final int idx = i;
                MultipartFile file = files.get(i);
                if (!file.isEmpty()) {
                    uploadFutures.add(CompletableFuture.supplyAsync(() ->
                            cloudinaryService.uploadCharacterSketch(
                                    file, seriesId, characterId, finalSketchUrls.size() + idx),
                            uploadExecutor));
                }
            }
            List<String> newUrls = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            finalSketchUrls.addAll(newUrls);
        }

        // Nếu có thay đổi sketches → ghi đè
        if (!finalSketchUrls.isEmpty()
                || (request.getPreservedSketchUrls() != null && request.getPreservedSketchUrls().isEmpty())
                || (files != null && !files.isEmpty())) {
            character.setSketchUrls(finalSketchUrls);
        }

        return toResponse(character);
    }

    // ════════════════════════════════════════════════════════════
    // 5. DELETE — Xoá character
    // ════════════════════════════════════════════════════════════

    @Transactional
    public void delete(Long seriesId, Long characterId, CustomUserDetails user) {
        if (!user.getRole().equals("MANGAKA")) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only MANGAKA can delete characters");
        }

        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Character not found"));

        if (!character.getSeries().getId().equals(seriesId)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Character does not belong to this series");
        }

        cloudinaryService.deleteCharacterFolder(seriesId, characterId);
        characterRepository.delete(character);
    }

    // ════════════════════════════════════════════════════════════
    // PRIVATE — Map Entity → Response DTO
    // ════════════════════════════════════════════════════════════

    private CharacterResponse toResponse(Character character) {
        return CharacterResponse.builder()
                .id(character.getId())
                .name(character.getName())
                .motivation(character.getMotivation())
                .sketchUrls(character.getSketchUrls())
                .seriesId(character.getSeries().getId())
                .createdAt(character.getCreatedAt())
                .updatedAt(character.getUpdatedAt())
                .build();
    }
}
