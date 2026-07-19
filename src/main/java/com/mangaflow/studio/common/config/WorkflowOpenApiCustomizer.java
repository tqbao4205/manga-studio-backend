package com.mangaflow.studio.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.*;

@Component
public class WorkflowOpenApiCustomizer implements OpenApiCustomizer {

    private static final List<String> W1_PATTERNS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/me",
            "/api/auth/profile",
            "/api/upload/avatar",
            "/api/series",
            "/api/series/*",
            "/api/series/*/characters",
            "/api/series/*/characters/batch",
            "/api/series/*/characters/*",
            "/api/series/*/story-profile",
            "/api/series/*/reader-feedback",
            "/api/series/*/chapters",
            "/api/chapters/*",
            "/api/v1/chapters/*/pages",
            "/api/v1/chapters/*/pages/batch",
            "/api/v1/pages/*",
            "/api/v1/pages/*/order",
            "/api/v1/chapters/*/pages/reorder"
    );

    private static final List<String> W2_PATTERNS = Arrays.asList(
            "/api/series/*/tantou/invite",
            "/api/series/*/tantou/invitations",
            "/api/series/*/tantou/*",
            "/api/tantou/invitations",
            "/api/tantou/invitations/*",
            "/api/series/*/submit",
            "/api/series/*/tantou/approve",
            "/api/series/*/tantou/reject",
            "/api/v1/pages/*/comments",
            "/api/v1/comments/*/replies",
            "/api/v1/comments/*",
            "/api/v1/comments/*/status"
    );

    private static final List<String> W3_PATTERNS = Arrays.asList(
            "/api/meetings",
            "/api/meetings/*",
            "/api/series/*/meetings",
            "/api/criteria",
            "/api/schedules",
            "/api/schedules/*",
            "/api/series/*/schedule",
            "/api/series/*/status"
    );

    private static final List<String> W4_PATTERNS = Arrays.asList(
            "/api/users/assistants",
            "/api/series/*/assistants/invite",
            "/api/series/*/assistants",
            "/api/series/*/assistants/*",
            "/api/assistants/invitations",
            "/api/assistants/invitations/*",
            "/api/regions/*/tasks",
            "/api/tasks",
            "/api/tasks/*",
            "/api/tasks/*/submissions",
            "/api/tasks/*/attachments",
            "/api/submissions/*",
            "/api/attachments/*",
            "/api/v1/pages/*/merge",
            "/api/v1/pages/*/flatten",
            "/api/v1/pages/*/regions",
            "/api/v1/pages/*/regions/**",
            "/api/v1/regions/**",
            "/api/v1/pages/*/layers",
            "/api/v1/pages/*/layers/**",
            "/api/v1/layers/**"
    );

    private static final List<String> W5_PATTERNS = Arrays.asList(
            "/api/chapters/*/submit",
            "/api/chapters/*/tantou/approve",
            "/api/chapters/*/tantou/request-revision",
            "/api/v1/pages/*/status"
    );

    private static final List<String> W6_PATTERNS = Arrays.asList(
            "/api/chapters/*/publish",
            "/api/ranking/**",
            "/api/notifications/**"
    );

    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final Set<String> takenSet = new HashSet<>();

    private boolean taken(String path) {
        return takenSet.contains(path);
    }

    @Override
    public void customise(OpenAPI openApi) {
        Paths paths = openApi.getPaths();
        if (paths == null || paths.isEmpty()) return;

        Map<String, PathItem> w1Ordered = new LinkedHashMap<>();
        Map<String, PathItem> w2Ordered = new LinkedHashMap<>();
        Map<String, PathItem> w3Ordered = new LinkedHashMap<>();
        Map<String, PathItem> w4Ordered = new LinkedHashMap<>();
        Map<String, PathItem> w5Ordered = new LinkedHashMap<>();
        Map<String, PathItem> w6Ordered = new LinkedHashMap<>();
        Map<String, PathItem> others = new LinkedHashMap<>();

        for (String pattern : W1_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w1Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (String pattern : W2_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w2Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (String pattern : W3_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w3Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (String pattern : W4_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w4Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (String pattern : W5_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w5Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (String pattern : W6_PATTERNS) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                if (pathMatcher.match(pattern, path) && !taken(path)) {
                    w6Ordered.put(path, entry.getValue());
                    takenSet.add(path);
                }
            }
        }

        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            if (!taken(entry.getKey())) {
                others.put(entry.getKey(), entry.getValue());
            }
        }

        Paths sorted = new Paths();
        w1Ordered.forEach(sorted::addPathItem);
        w2Ordered.forEach(sorted::addPathItem);
        w3Ordered.forEach(sorted::addPathItem);
        w4Ordered.forEach(sorted::addPathItem);
        w5Ordered.forEach(sorted::addPathItem);
        w6Ordered.forEach(sorted::addPathItem);
        others.forEach(sorted::addPathItem);

        openApi.setPaths(sorted);

        // ── Sort tags by workflow order ──
        List<Tag> sortedTags = new ArrayList<>();
        List<String> workflowOrder = Arrays.asList(
                "1. Series Creation",
                "2. Series Review",
                "3. Board Review & Publication",
                "4. Task Collaboration",
                "5. Chapter Review",
                "6. Publication & Ranking"
        );
        Map<String, Tag> tagByName = new LinkedHashMap<>();
        if (openApi.getTags() != null) {
            openApi.getTags().forEach(t -> tagByName.put(t.getName(), t));
        }
        for (String name : workflowOrder) {
            Tag tag = tagByName.remove(name);
            if (tag != null) sortedTags.add(tag);
        }
        sortedTags.addAll(tagByName.values());
        openApi.setTags(sortedTags);
    }
}
