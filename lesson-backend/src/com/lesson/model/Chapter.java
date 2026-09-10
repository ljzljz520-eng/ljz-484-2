package com.lesson.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** 章节，归属于课程，sortOrder 决定在课程中的顺序 */
public class Chapter {
    public String id;
    public String courseId;
    public String title;
    public long sortOrder;
    public long createdAt;
    public long updatedAt;

    public Chapter() {
    }

    public Chapter(String id, String courseId, String title, long sortOrder, long now) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("courseId", courseId);
        m.put("title", title);
        m.put("sortOrder", sortOrder);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    public static Chapter fromMap(Map<String, Object> m) {
        Chapter c = new Chapter();
        c.id = (String) m.get("id");
        c.courseId = (String) m.get("courseId");
        c.title = (String) m.get("title");
        c.sortOrder = m.get("sortOrder") instanceof Number ? ((Number) m.get("sortOrder")).longValue() : 0L;
        c.createdAt = m.get("createdAt") instanceof Number ? ((Number) m.get("createdAt")).longValue() : 0L;
        c.updatedAt = m.get("updatedAt") instanceof Number ? ((Number) m.get("updatedAt")).longValue() : 0L;
        return c;
    }
}
