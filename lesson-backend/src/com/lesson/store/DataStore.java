package com.lesson.store;

import com.lesson.model.Chapter;
import com.lesson.model.Course;
import com.lesson.model.Lecture;
import com.lesson.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地 JSON 文件存储。
 * 所有数据保存在 data/db.json 一个文件中，读写均加锁；写操作后原子落盘。
 */
public class DataStore {

    private static final Comparator<Chapter> CHAPTER_ORDER =
            Comparator.comparingLong((Chapter c) -> c.sortOrder).thenComparingLong(c -> c.createdAt);
    private static final Comparator<Lecture> LECTURE_ORDER =
            Comparator.comparingLong((Lecture l) -> l.sortOrder).thenComparingLong(l -> l.createdAt);

    private final Path file;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final List<Course> courses = new ArrayList<>();
    private final List<Chapter> chapters = new ArrayList<>();
    private final List<Lecture> lectures = new ArrayList<>();

    public DataStore(Path file) {
        this.file = file;
        load();
    }

    // ====================== 加载 / 保存 ======================

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            if (Files.exists(file) && Files.size(file) > 0) {
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Map<String, Object> root = Json.parseObject(text);
                if (root != null) {
                    for (Object o : (List<Object>) root.getOrDefault("courses", new ArrayList<>())) {
                        courses.add(Course.fromMap((Map<String, Object>) o));
                    }
                    for (Object o : (List<Object>) root.getOrDefault("chapters", new ArrayList<>())) {
                        chapters.add(Chapter.fromMap((Map<String, Object>) o));
                    }
                    for (Object o : (List<Object>) root.getOrDefault("lectures", new ArrayList<>())) {
                        lectures.add(Lecture.fromMap((Map<String, Object>) o));
                    }
                    return;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取数据文件失败: " + file, e);
        }
        seed();
        save();
    }

    private void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> cs = new ArrayList<>();
        List<Object> chs = new ArrayList<>();
        List<Object> ls = new ArrayList<>();
        for (Course c : courses) {
            cs.add(c.toMap());
        }
        for (Chapter c : chapters) {
            chs.add(c.toMap());
        }
        for (Lecture l : lectures) {
            ls.add(l.toMap());
        }
        root.put("courses", cs);
        root.put("chapters", chs);
        root.put("lectures", ls);

        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.write(tmp, Json.stringify(root).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("写入数据文件失败: " + file, e);
        }
    }

    /** 首次启动的示例数据 */
    private void seed() {
        long t = 1_720_000_000_000L;

        Course cs101 = new Course("c-cs101", "Java 程序设计入门",
                "面向零基础同学的 Java 课程，覆盖环境搭建、基础语法与面向对象思想。", t);
        Course math = new Course("c-math101", "高等数学（上）",
                "函数、极限、导数与微分的入门讲义，配套课堂例题。", t + 1_000L);
        courses.add(cs101);
        courses.add(math);

        Chapter chIntro = new Chapter("ch-intro", cs101.id, "第一章 课程导论", 1, t + 2_000L);
        Chapter chEnv = new Chapter("ch-env", cs101.id, "第二章 开发环境", 2, t + 3_000L);
        Chapter chOop = new Chapter("ch-oop", cs101.id, "第三章 面向对象基础", 3, t + 4_000L);
        Chapter chLimit = new Chapter("ch-limit", math.id, "第一章 极限与连续", 1, t + 5_000L);
        chapters.add(chIntro);
        chapters.add(chEnv);
        chapters.add(chOop);
        chapters.add(chLimit);

        Lecture welcome = new Lecture("le-welcome", cs101.id, chIntro.id,
                "1.1 为什么学习 Java",
                "# 为什么学习 Java\n\n"
                        + "Java 是一门**面向对象**的编程语言，具有以下特点：\n\n"
                        + "- 一次编写，到处运行（JVM 屏蔽了操作系统差异）\n"
                        + "- 强类型、静态类型，编译期就能发现大量问题\n"
                        + "- 生态成熟，服务端、Android、大数据领域广泛使用\n\n"
                        + "## 学习路线\n\n"
                        + "1. 搭建 JDK 开发环境\n"
                        + "2. 掌握变量、流程控制与数组\n"
                        + "3. 理解类与对象、封装继承多态\n\n"
                        + "> 本讲义支持 Markdown，老师可以在编辑器里直接编写。\n",
                1, t + 6_000L);
        welcome.published = true;
        welcome.publishedAt = t + 7_000L;
        welcome.updatedAt = t + 7_000L;

        Lecture jdk = new Lecture("le-jdk", cs101.id, chEnv.id,
                "2.1 安装 JDK 与第一个程序",
                "# 安装 JDK\n\n"
                        + "1. 下载 JDK 17（Temurin / Oracle 均可）\n"
                        + "2. 配置 `JAVA_HOME` 与 `PATH`\n"
                        + "3. 在终端执行 `java -version` 验证\n\n"
                        + "## Hello World\n\n"
                        + "```java\n"
                        + "public class Hello {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"你好，Java！\");\n"
                        + "    }\n"
                        + "}\n"
                        + "```\n",
                1, t + 8_000L);
        jdk.published = true;
        jdk.publishedAt = t + 9_000L;
        jdk.updatedAt = t + 9_000L;

        Lecture oopDraft = new Lecture("le-oop-draft", cs101.id, chOop.id,
                "3.1 类与对象（草稿）",
                "# 类与对象\n\n"
                        + "本节内容正在编写中：类的定义、构造方法、`this` 关键字……\n",
                1, t + 10_000L);

        Lecture limit = new Lecture("le-limit", math.id, chLimit.id,
                "1.1 数列的极限",
                "# 数列的极限\n\n"
                        + "设 {x_n} 为一数列，如果存在常数 a，对于任意给定的正数 ε……\n\n"
                        + "- ε-N 语言\n"
                        + "- 收敛数列的性质：唯一性、有界性、保号性\n",
                1, t + 11_000L);
        limit.published = true;
        limit.publishedAt = t + 12_000L;
        limit.updatedAt = t + 12_000L;

        lectures.add(welcome);
        lectures.add(jdk);
        lectures.add(oopDraft);
        lectures.add(limit);
    }

    // ====================== 课程 ======================

    public List<Course> listCourses() {
        lock.readLock().lock();
        try {
            List<Course> copy = new ArrayList<>(courses);
            copy.sort(Comparator.comparingLong(c -> c.createdAt));
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Course getCourse(String id) {
        lock.readLock().lock();
        try {
            return findCourse(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Course createCourse(String title, String description) {
        lock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            Course c = new Course(newId("c-"), title, description, now);
            courses.add(c);
            save();
            return c;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Course updateCourse(String id, String title, String description) {
        lock.writeLock().lock();
        try {
            Course c = findCourse(id);
            if (title != null) {
                c.title = title;
            }
            if (description != null) {
                c.description = description;
            }
            c.updatedAt = System.currentTimeMillis();
            save();
            return c;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteCourse(String id) {
        lock.writeLock().lock();
        try {
            findCourse(id);
            lectures.removeIf(l -> l.courseId.equals(id));
            chapters.removeIf(ch -> ch.courseId.equals(id));
            courses.removeIf(c -> c.id.equals(id));
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ====================== 章节 ======================

    public List<Chapter> listChapters(String courseId) {
        lock.readLock().lock();
        try {
            findCourse(courseId);
            List<Chapter> list = new ArrayList<>();
            for (Chapter ch : chapters) {
                if (ch.courseId.equals(courseId)) {
                    list.add(ch);
                }
            }
            list.sort(CHAPTER_ORDER);
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Chapter createChapter(String courseId, String title) {
        lock.writeLock().lock();
        try {
            findCourse(courseId);
            long order = 1;
            for (Chapter ch : chapters) {
                if (ch.courseId.equals(courseId)) {
                    order = Math.max(order, ch.sortOrder + 1);
                }
            }
            long now = System.currentTimeMillis();
            Chapter ch = new Chapter(newId("ch-"), courseId, title, order, now);
            chapters.add(ch);
            save();
            return ch;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Chapter renameChapter(String id, String title) {
        lock.writeLock().lock();
        try {
            Chapter ch = findChapter(id);
            ch.title = title;
            ch.updatedAt = System.currentTimeMillis();
            save();
            return ch;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteChapter(String id) {
        lock.writeLock().lock();
        try {
            Chapter ch = findChapter(id);
            lectures.removeIf(l -> l.chapterId.equals(ch.id));
            chapters.remove(ch);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ====================== 讲义 ======================

    public Lecture getLecture(String id) {
        lock.readLock().lock();
        try {
            return findLecture(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Lecture createLecture(String courseId, String chapterId, String title, String content) {
        lock.writeLock().lock();
        try {
            findCourse(courseId);
            Chapter ch = findChapter(chapterId);
            if (!ch.courseId.equals(courseId)) {
                throw new IllegalArgumentException("章节不属于该课程");
            }
            long order = 1;
            for (Lecture l : lectures) {
                if (l.chapterId.equals(chapterId)) {
                    order = Math.max(order, l.sortOrder + 1);
                }
            }
            long now = System.currentTimeMillis();
            Lecture l = new Lecture(newId("le-"), courseId, chapterId, title,
                    content == null ? "" : content, order, now);
            lectures.add(l);
            save();
            return l;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Lecture updateLecture(String id, String title, String content) {
        lock.writeLock().lock();
        try {
            Lecture l = findLecture(id);
            if (title != null) {
                l.title = title;
            }
            if (content != null) {
                l.content = content;
            }
            l.updatedAt = System.currentTimeMillis();
            save();
            return l;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Lecture setPublished(String id, boolean published) {
        lock.writeLock().lock();
        try {
            Lecture l = findLecture(id);
            long now = System.currentTimeMillis();
            if (published && !l.published) {
                l.publishedAt = now;
            }
            if (!published) {
                l.publishedAt = 0L;
            }
            l.published = published;
            l.updatedAt = now;
            save();
            return l;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteLecture(String id) {
        lock.writeLock().lock();
        try {
            Lecture l = findLecture(id);
            lectures.remove(l);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ====================== 聚合视图 ======================

    /** 老师端：课程 + 全部章节 + 全部讲义（含草稿） */
    public Map<String, Object> courseDetailTeacher(String courseId) {
        lock.readLock().lock();
        try {
            Course course = findCourse(courseId);
            List<Chapter> chs = new ArrayList<>();
            List<Lecture> ls = new ArrayList<>();
            for (Chapter ch : chapters) {
                if (ch.courseId.equals(courseId)) {
                    chs.add(ch);
                }
            }
            for (Lecture l : lectures) {
                if (l.courseId.equals(courseId)) {
                    ls.add(l);
                }
            }
            chs.sort(CHAPTER_ORDER);
            ls.sort(LECTURE_ORDER);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("course", course.toMap());
            List<Object> chMaps = new ArrayList<>();
            for (Chapter ch : chs) {
                chMaps.add(ch.toMap());
            }
            List<Object> lMaps = new ArrayList<>();
            for (Lecture l : ls) {
                lMaps.add(l.toMap());
            }
            root.put("chapters", chMaps);
            root.put("lectures", lMaps);
            return root;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 学生端：课程 + 章节 + 仅已发布讲义（不含 content 正文，正文走单独接口） */
    public Map<String, Object> courseDetailStudent(String courseId) {
        lock.readLock().lock();
        try {
            Course course = findCourse(courseId);
            List<Chapter> chs = new ArrayList<>();
            List<Lecture> ls = new ArrayList<>();
            for (Chapter ch : chapters) {
                if (ch.courseId.equals(courseId)) {
                    chs.add(ch);
                }
            }
            for (Lecture l : lectures) {
                if (l.courseId.equals(courseId) && l.published) {
                    ls.add(l);
                }
            }
            chs.sort(CHAPTER_ORDER);
            ls.sort(LECTURE_ORDER);

            // 只保留含有已发布讲义的章节
            List<Object> chMaps = new ArrayList<>();
            for (Chapter ch : chs) {
                boolean hasPublished = false;
                for (Lecture l : ls) {
                    if (l.chapterId.equals(ch.id)) {
                        hasPublished = true;
                        break;
                    }
                }
                if (hasPublished) {
                    chMaps.add(ch.toMap());
                }
            }
            List<Object> lMaps = new ArrayList<>();
            for (Lecture l : ls) {
                Map<String, Object> m = l.toMap();
                m.remove("content");
                lMaps.add(m);
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("course", course.toMap());
            root.put("chapters", chMaps);
            root.put("lectures", lMaps);
            return root;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 学生端读取已发布讲义正文 */
    public Lecture getPublishedLecture(String id) {
        lock.readLock().lock();
        try {
            Lecture l = findLecture(id);
            if (!l.published) {
                throw new NotFoundException("讲义不存在或尚未发布");
            }
            return l;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ====================== 内部查找（调用前需持锁） ======================

    private Course findCourse(String id) {
        for (Course c : courses) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        throw new NotFoundException("课程不存在: " + id);
    }

    private Chapter findChapter(String id) {
        for (Chapter ch : chapters) {
            if (ch.id.equals(id)) {
                return ch;
            }
        }
        throw new NotFoundException("章节不存在: " + id);
    }

    private Lecture findLecture(String id) {
        for (Lecture l : lectures) {
            if (l.id.equals(id)) {
                return l;
            }
        }
        throw new NotFoundException("讲义不存在: " + id);
    }

    private static String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
