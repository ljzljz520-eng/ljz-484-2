package com.lesson.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** 课程 */
public class Course {
    public String id;
    public String title;
    public String description;
    public long createdAt;
    public long updatedAt;

    public Course() {
    }

    public Course(String id, String title, String description, long now) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", description);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    public static Course fromMap(Map<String, Object> m) {
        Course c = new Course();
        c.id = (String) m.get("id");
        c.title = (String) m.get("title");
        c.description = m.get("description") == null ? "" : (String) m.get("description");
        c.createdAt = num(m.get("createdAt"));
        c.updatedAt = num(m.get("updatedAt"));
        return c;
    }

    private static long num(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }
}
