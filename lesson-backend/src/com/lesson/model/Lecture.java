package com.lesson.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** 讲义正文，归属于章节；published=false 时仅老师可见 */
public class Lecture {
    public String id;
    public String chapterId;
    public String courseId;
    public String title;
    public String content;
    public boolean published;
    public long sortOrder;
    public long createdAt;
    public long updatedAt;
    public long publishedAt;

    public Lecture() {
    }

    public Lecture(String id, String courseId, String chapterId, String title,
                   String content, long sortOrder, long now) {
        this.id = id;
        this.courseId = courseId;
        this.chapterId = chapterId;
        this.title = title;
        this.content = content;
        this.published = false;
        this.sortOrder = sortOrder;
        this.createdAt = now;
        this.updatedAt = now;
        this.publishedAt = 0L;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("chapterId", chapterId);
        m.put("courseId", courseId);
        m.put("title", title);
        m.put("content", content);
        m.put("published", published);
        m.put("sortOrder", sortOrder);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        m.put("publishedAt", publishedAt);
        return m;
    }

    public static Lecture fromMap(Map<String, Object> m) {
        Lecture l = new Lecture();
        l.id = (String) m.get("id");
        l.chapterId = (String) m.get("chapterId");
        l.courseId = (String) m.get("courseId");
        l.title = (String) m.get("title");
        l.content = m.get("content") == null ? "" : (String) m.get("content");
        l.published = Boolean.TRUE.equals(m.get("published"));
        l.sortOrder = m.get("sortOrder") instanceof Number ? ((Number) m.get("sortOrder")).longValue() : 0L;
        l.createdAt = m.get("createdAt") instanceof Number ? ((Number) m.get("createdAt")).longValue() : 0L;
        l.updatedAt = m.get("updatedAt") instanceof Number ? ((Number) m.get("updatedAt")).longValue() : 0L;
        l.publishedAt = m.get("publishedAt") instanceof Number ? ((Number) m.get("publishedAt")).longValue() : 0L;
        return l;
    }
}
