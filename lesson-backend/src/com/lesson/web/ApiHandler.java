package com.lesson.web;

import com.lesson.model.Chapter;
import com.lesson.model.Course;
import com.lesson.model.Lecture;
import com.lesson.store.DataStore;
import com.lesson.store.NotFoundException;
import com.lesson.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API 处理器。
 * /api/teacher/** 需要请求头 X-Teacher-Token；/api/student/** 公开。
 */
public class ApiHandler implements HttpHandler {

    private final DataStore store;
    private final String teacherToken;

    public ApiHandler(DataStore store, String teacherToken) {
        this.store = store;
        this.teacherToken = teacherToken;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            addCorsHeaders(ex);
            String method = ex.getRequestMethod();
            if ("OPTIONS".equals(method)) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String path = ex.getRequestURI().getPath();
            String[] seg = splitPath(path);

            if (seg.length == 2 && seg[1].equals("health") && "GET".equals(method)) {
                Map<String, Object> ok = new LinkedHashMap<>();
                ok.put("status", "ok");
                sendJson(ex, 200, ok);
                return;
            }

            if (seg.length >= 2 && seg[1].equals("student")) {
                handleStudent(ex, method, seg);
                return;
            }

            if (seg.length >= 2 && seg[1].equals("teacher")) {
                requireTeacher(ex);
                handleTeacher(ex, method, seg);
                return;
            }

            throw new ApiException(404, "接口不存在: " + path);
        } catch (ApiException e) {
            sendError(ex, e.getStatus(), e.getMessage());
        } catch (NotFoundException e) {
            sendError(ex, 404, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(ex, 500, "服务器内部错误: " + e.getMessage());
        } finally {
            ex.close();
        }
    }

    // ====================== 学生端（公开） ======================

    private void handleStudent(HttpExchange ex, String method, String[] seg) throws IOException {
        // GET /api/student/courses （只列出含有已发布讲义的课程）
        if (seg.length == 3 && seg[2].equals("courses")) {
            requireGet(method);
            List<Object> result = new ArrayList<>();
            for (Course c : store.listCourses()) {
                Map<String, Object> detail = store.courseDetailStudent(c.id);
                if (!((List<?>) detail.get("chapters")).isEmpty()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", c.id);
                    item.put("title", c.title);
                    item.put("description", c.description);
                    item.put("updatedAt", c.updatedAt);
                    result.add(item);
                }
            }
            sendJson(ex, 200, result);
            return;
        }

        // GET /api/student/courses/{id}
        if (seg.length == 4 && seg[2].equals("courses")) {
            requireGet(method);
            sendJson(ex, 200, store.courseDetailStudent(seg[3]));
            return;
        }

        // GET /api/student/lectures/{id}
        if (seg.length == 4 && seg[2].equals("lectures")) {
            requireGet(method);
            sendJson(ex, 200, store.getPublishedLecture(seg[3]).toMap());
            return;
        }

        throw new ApiException(404, "接口不存在");
    }

    // ====================== 老师端（需 Token） ======================

    private void handleTeacher(HttpExchange ex, String method, String[] seg) throws IOException {
        if (seg.length >= 3 && seg[2].equals("courses")) {
            handleTeacherCourses(ex, method, seg);
            return;
        }

        // /api/teacher/chapters/{id}
        if (seg.length == 4 && seg[2].equals("chapters")) {
            String id = seg[3];
            if ("PUT".equals(method)) {
                Map<String, Object> body = readBody(ex);
                String title = Json.strReq(body, "title").trim();
                if (title.isEmpty()) {
                    throw new IllegalArgumentException("章节标题不能为空");
                }
                sendJson(ex, 200, store.renameChapter(id, title).toMap());
                return;
            }
            if ("DELETE".equals(method)) {
                store.deleteChapter(id);
                sendJson(ex, 200, deleted(id));
                return;
            }
            throw new ApiException(405, "不支持的方法: " + method);
        }

        if (seg.length >= 3 && seg[2].equals("lectures")) {
            handleTeacherLectures(ex, method, seg);
            return;
        }

        throw new ApiException(404, "接口不存在");
    }

    private void handleTeacherCourses(HttpExchange ex, String method, String[] seg) throws IOException {
        if (seg.length == 3) {
            if ("GET".equals(method)) {
                List<Object> list = new ArrayList<>();
                for (Course c : store.listCourses()) {
                    list.add(c.toMap());
                }
                sendJson(ex, 200, list);
                return;
            }
            if ("POST".equals(method)) {
                Map<String, Object> body = readBody(ex);
                String title = Json.strReq(body, "title").trim();
                if (title.isEmpty()) {
                    throw new IllegalArgumentException("课程标题不能为空");
                }
                String desc = Json.str(body, "description");
                sendJson(ex, 201, store.createCourse(title, desc == null ? "" : desc).toMap());
                return;
            }
            throw new ApiException(405, "不支持的方法: " + method);
        }

        String courseId = seg[3];

        if (seg.length == 4) {
            if ("GET".equals(method)) {
                sendJson(ex, 200, store.courseDetailTeacher(courseId));
                return;
            }
            if ("PUT".equals(method)) {
                Map<String, Object> body = readBody(ex);
                String title = Json.str(body, "title");
                if (title != null && title.trim().isEmpty()) {
                    throw new IllegalArgumentException("课程标题不能为空");
                }
                String desc = Json.str(body, "description");
                Course c = store.updateCourse(courseId, title == null ? null : title.trim(), desc);
                sendJson(ex, 200, c.toMap());
                return;
            }
            if ("DELETE".equals(method)) {
                store.deleteCourse(courseId);
                sendJson(ex, 200, deleted(courseId));
                return;
            }
            throw new ApiException(405, "不支持的方法: " + method);
        }

        if (seg.length == 5 && seg[4].equals("chapters") && "POST".equals(method)) {
            Map<String, Object> body = readBody(ex);
            String title = Json.strReq(body, "title").trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("章节标题不能为空");
            }
            sendJson(ex, 201, store.createChapter(courseId, title).toMap());
            return;
        }

        if (seg.length == 5 && seg[4].equals("lectures") && "POST".equals(method)) {
            Map<String, Object> body = readBody(ex);
            String chapterId = Json.strReq(body, "chapterId").trim();
            String title = Json.strReq(body, "title").trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("讲义标题不能为空");
            }
            String content = Json.str(body, "content");
            Lecture l = store.createLecture(courseId, chapterId, title, content == null ? "" : content);
            sendJson(ex, 201, l.toMap());
            return;
        }

        throw new ApiException(404, "接口不存在");
    }

    private void handleTeacherLectures(HttpExchange ex, String method, String[] seg) throws IOException {
        String id = seg[3];

        if (seg.length == 4) {
            if ("GET".equals(method)) {
                sendJson(ex, 200, store.getLecture(id).toMap());
                return;
            }
            if ("PUT".equals(method)) {
                Map<String, Object> body = readBody(ex);
                String title = Json.str(body, "title");
                if (title != null && title.trim().isEmpty()) {
                    throw new IllegalArgumentException("讲义标题不能为空");
                }
                String content = Json.str(body, "content");
                Lecture l = store.updateLecture(id, title == null ? null : title.trim(), content);
                sendJson(ex, 200, l.toMap());
                return;
            }
            if ("DELETE".equals(method)) {
                store.deleteLecture(id);
                sendJson(ex, 200, deleted(id));
                return;
            }
            throw new ApiException(405, "不支持的方法: " + method);
        }

        // PATCH /api/teacher/lectures/{id}/publish
        if (seg.length == 5 && seg[4].equals("publish") && "PATCH".equals(method)) {
            Map<String, Object> body = readBody(ex);
            boolean published = Json.bool(body, "published", true);
            sendJson(ex, 200, store.setPublished(id, published).toMap());
            return;
        }

        throw new ApiException(404, "接口不存在");
    }

    // ====================== 工具方法 ======================

    private void requireTeacher(HttpExchange ex) {
        String token = ex.getRequestHeaders().getFirst("X-Teacher-Token");
        if (token == null || !constantEquals(token, teacherToken)) {
            throw new ApiException(401, "老师令牌无效或缺失（请在请求头设置 X-Teacher-Token）");
        }
    }

    private static boolean constantEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private void requireGet(String method) {
        if (!"GET".equals(method)) {
            throw new ApiException(405, "不支持的方法: " + method);
        }
    }

    private static String[] splitPath(String path) {
        String[] parts = path.split("/");
        List<String> seg = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) {
                seg.add(p);
            }
        }
        return seg.toArray(new String[0]);
    }

    private Map<String, Object> readBody(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return new LinkedHashMap<>();
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        Map<String, Object> m = Json.parseObject(text);
        return m == null ? new LinkedHashMap<>() : m;
    }

    private Map<String, Object> deleted(String id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("deleted", true);
        return m;
    }

    private void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers",
                "Content-Type, X-Teacher-Token");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    }

    private void sendJson(HttpExchange ex, int status, Object data) throws IOException {
        byte[] body = Json.stringify(data).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private void sendError(HttpExchange ex, int status, String message) throws IOException {
        if (status == 401) {
            ex.getResponseHeaders().add("WWW-Authenticate", "TeacherToken");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", message);
        m.put("status", status);
        byte[] body = Json.stringify(m).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
