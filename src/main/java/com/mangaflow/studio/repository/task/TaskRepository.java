package com.mangaflow.studio.repository.task;

import com.mangaflow.studio.model.task.Task;
import com.mangaflow.studio.model.task.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Lấy danh sách tasks có chứa 1 region cụ thể.
     * Dùng JOIN qua quan hệ @OneToMany (task.regions).
     */
    @EntityGraph(attributePaths = "submissions")
    @Query("SELECT t FROM Task t JOIN t.regions r WHERE r.id = :regionId ORDER BY t.assignedAt DESC")
    List<Task> findByRegionId(@Param("regionId") Long regionId);

    // ═══════════════════════════════════════════════════════════
    //  DASHBOARD STATISTICS — Thêm cho Series Statistics Feature
    // ═══════════════════════════════════════════════════════════

    /**
     * Đếm số task của 1 assistant trong 1 series.
     * Join qua: Task → Region → Page → Chapter → Series.
     */
    @Query("SELECT COUNT(t) FROM Task t JOIN t.regions r " +
           "WHERE t.assistant.id = :assistantId " +
           "AND EXISTS (SELECT 1 FROM Chapter ch WHERE ch.series.id = :seriesId " +
           "    AND EXISTS (SELECT 1 FROM Page p WHERE p.chapterId = ch.id AND p.id = r.pageId))")
    long countByAssistantIdAndSeriesId(@Param("assistantId") Long assistantId,
                                        @Param("seriesId") Long seriesId);

    /**
     * Đếm số task của 1 assistant trong 1 series theo trạng thái.
     * Join qua: Task → Region → Page → Chapter → Series.
     */
    @Query("SELECT COUNT(t) FROM Task t JOIN t.regions r " +
           "WHERE t.assistant.id = :assistantId " +
           "AND t.status = :status " +
           "AND EXISTS (SELECT 1 FROM Chapter ch WHERE ch.series.id = :seriesId " +
           "    AND EXISTS (SELECT 1 FROM Page p WHERE p.chapterId = ch.id AND p.id = r.pageId))")
    long countByAssistantIdAndSeriesIdAndStatus(@Param("assistantId") Long assistantId,
                                                 @Param("seriesId") Long seriesId,
                                                 @Param("status") TaskStatus status);
}
