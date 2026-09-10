package com.lesson;

import com.lesson.store.DataStore;
import com.lesson.web.ApiHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * 课程讲义系统后端启动类。
 *
 * 环境变量：
 *   PORT          监听端口，默认 8080
 *   DATA_FILE     数据文件路径，默认 data/db.json（相对运行目录）
 *   TEACHER_TOKEN 老师令牌，默认 teacher-123456
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(getEnv("PORT", "8080"));
        Path dataFile = Paths.get(getEnv("DATA_FILE", "data/db.json"));
        String teacherToken = getEnv("TEACHER_TOKEN", "teacher-123456");

        DataStore store = new DataStore(dataFile);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api", new ApiHandler(store, teacherToken));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("==============================================");
        System.out.println(" 课程讲义系统后端已启动");
        System.out.println(" 监听地址 : http://localhost:" + port);
        System.out.println(" 数据文件 : " + dataFile.toAbsolutePath());
        System.out.println(" 老师令牌 : " + teacherToken + "（请求头 X-Teacher-Token）");
        System.out.println(" 健康检查 : GET /api/health");
        System.out.println("==============================================");
    }

    private static String getEnv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }
}
