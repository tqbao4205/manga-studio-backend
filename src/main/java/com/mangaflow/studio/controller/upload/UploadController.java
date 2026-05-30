package com.mangaflow.studio.controller.upload;

import com.mangaflow.studio.common.security.CustomUserDetails;
import com.mangaflow.studio.service.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload avatar cho user hiện tại.
     *
     * Flow:
     *   1. Nếu user đã có avatar cũ → xoá khỏi Cloudinary
     *   2. Upload file mới lên Cloudinary
     *   3. Trả về URL của ảnh mới
     *
     * Frontend nhận URL này rồi gửi kèm trong PATCH /api/auth/profile
     * để lưu vào DB.
     *
     * @param file        File ảnh (multipart/form-data)
     * @param userDetails User hiện tại (từ JWT)
     * @return { "url": "https://res.cloudinary.com/.../avatar.jpg" }
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        String oldAvatarUrl = userDetails.getUser().getAvatarUrl();

        // Xoá avatar cũ trên Cloudinary nếu có
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            cloudinaryService.deleteImageByUrl(oldAvatarUrl);
        }

        // Upload ảnh mới
        String url = cloudinaryService.uploadAvatar(file, userId);

        return ResponseEntity.ok(Map.of("url", url));
    }
}
