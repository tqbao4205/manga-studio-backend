package com.mangaflow.studio.dto.series.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CharactersBatchRequest {
    @Valid
    @NotEmpty
    private List<CharacterEntry> characters;

    @Data
    public static class CharacterEntry {
        @NotBlank
        private String name;
        private String motivation;
        private int fileCount;
    }
}
