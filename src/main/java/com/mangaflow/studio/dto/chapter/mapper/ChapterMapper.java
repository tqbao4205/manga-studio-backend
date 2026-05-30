package com.mangaflow.studio.dto.chapter.mapper;

import com.mangaflow.studio.dto.chapter.request.ChapterRequest;
import com.mangaflow.studio.dto.chapter.response.ChapterResponse;
import com.mangaflow.studio.model.chapter.Chapter;
import com.mangaflow.studio.model.series.Series;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ChapterMapper {

    @Mapping(target = "seriesId", source = "series.id")
    @Mapping(target = "seriesTitle", source = "series.title")
    ChapterResponse toResponse(Chapter chapter); //map series.id → seriesId, series.title → seriesTitle

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "series", source = "series")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "pageCount", constant = "0")
    @Mapping(target = "progressPercent", constant = "0")
    @Mapping(target = "publishDate", ignore = true)
    @Mapping(target = "deadline", source = "request.deadline")
    @Mapping(target = "chapterNumber", source = "request.chapterNumber")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Chapter toEntity(ChapterRequest request, Series series); //dùng cho create

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "series", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pageCount", ignore = true)
    @Mapping(target = "progressPercent", ignore = true)
    @Mapping(target = "publishDate", ignore = true)
    @Mapping(target = "deadline", source = "request.deadline")
    @Mapping(target = "chapterNumber", source = "request.chapterNumber")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Chapter chapter, ChapterRequest request); //dùng sau này cho put
}
