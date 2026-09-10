/* ===== 老师端逻辑：课程 / 章节 / 讲义维护与发布 ===== */
(function () {
  const courseListEl = document.getElementById('course-list');
  const treeEl = document.getElementById('chapter-tree');
  const courseNameEl = document.getElementById('current-course-name');
  const btnNewCourse = document.getElementById('btn-new-course');
  const btnNewChapter = document.getElementById('btn-new-chapter');
  const emptyEl = document.getElementById('editor-empty');
  const editorEl = document.getElementById('editor');
  const titleEl = document.getElementById('editor-title');
  const contentEl = document.getElementById('editor-content');
  const previewEl = document.getElementById('editor-preview');
  const statusEl = document.getElementById('editor-status');
  const btnSave = document.getElementById('btn-save');
  const btnPublish = document.getElementById('btn-publish');
  const btnDelete = document.getElementById('btn-delete');
  const tokenEl = document.getElementById('teacher-token');
  const apiBaseEl = document.getElementById('api-base');

  let courses = [];
  let currentCourseId = null;
  let detail = null;          // { course, chapters, lectures }
  let currentLecture = null;  // 正在编辑的讲义（含正文）

  // ---------- 基础交互 ----------

  function toast(msg, isErr) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.className = 'show' + (isErr ? ' err' : '');
    setTimeout(() => { t.className = ''; }, 2200);
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function fmtTime(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    const p = (n) => (n < 10 ? '0' + n : '' + n);
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
      ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
  }

  // ---------- 顶栏配置 ----------

  tokenEl.value = localStorage.getItem('lesson_teacher_token') || 'teacher-123456';
  apiBaseEl.value = API.getBase();
  API.setToken(tokenEl.value);

  tokenEl.addEventListener('change', () => {
    API.setToken(tokenEl.value.trim());
    toast('令牌已保存');
    loadCourses();
  });
  apiBaseEl.addEventListener('change', () => {
    API.setBase(apiBaseEl.value);
    apiBaseEl.value = API.getBase();
    toast('后端地址已更新为 ' + API.getBase());
    loadCourses();
  });

  // ---------- 课程列表 ----------

  async function loadCourses(keepSelection) {
    try {
      courses = await API.teacher.listCourses();
    } catch (e) {
      courseListEl.innerHTML = '<div class="empty">加载失败<br>' + esc(e.message) + '</div>';
      return;
    }
    renderCourses();

    if (currentCourseId && courses.some((c) => c.id === currentCourseId)) {
      await openCourse(currentCourseId, keepSelection);
      return;
    }
    currentCourseId = courses.length ? courses[0].id : null;
    detail = null;
    if (currentCourseId) {
      await openCourse(currentCourseId, false);
    } else {
      courseNameEl.textContent = '章节与讲义';
      treeEl.innerHTML = '<div class="empty">还没有课程，先新建一门吧</div>';
      showEmptyEditor();
    }
  }

  function renderCourses() {
    courseListEl.innerHTML = '';
    if (!courses.length) {
      courseListEl.innerHTML = '<div class="empty">还没有课程<br><span class="hint">点右上角「+ 新建」</span></div>';
      return;
    }
    courses.forEach((course) => {
      const el = document.createElement('div');
      el.className = 'item' + (course.id === currentCourseId ? ' active' : '');
      el.innerHTML =
        '<span class="title">' + esc(course.title) + '</span>' +
        '<span class="ops">' +
          '<button title="编辑课程" data-act="edit">✎</button>' +
          '<button title="删除课程" data-act="del">🗑</button>' +
        '</span>';
      el.addEventListener('click', (e) => {
        const act = e.target.closest('button') && e.target.closest('button').dataset.act;
        if (act === 'edit') { e.stopPropagation(); editCourse(course); return; }
        if (act === 'del') { e.stopPropagation(); deleteCourse(course); return; }
        openCourse(course.id, false);
      });
      courseListEl.appendChild(el);
    });
  }

  async function editCourse(course) {
    const title = window.prompt('课程标题：', course.title);
    if (title === null) return;
    const t = title.trim();
    if (!t) { toast('课程标题不能为空', true); return; }
    const desc = window.prompt('课程简介：', course.description || '');
    if (desc === null) return;
    try {
      await API.teacher.updateCourse(course.id, { title: t, description: desc.trim() });
      toast('课程信息已更新');
      await loadCourses(true);
    } catch (e) {
      toast(e.message, true);
    }
  }

  async function deleteCourse(course) {
    if (!window.confirm('确定删除课程「' + course.title + '」吗？\n该课程下的章节与讲义会一并删除。')) {
      return;
    }
    try {
      await API.teacher.deleteCourse(course.id);
      toast('课程已删除');
      if (currentCourseId === course.id) {
        currentCourseId = null;
        currentLecture = null;
      }
      await loadCourses(false);
    } catch (e) {
      toast(e.message, true);
    }
  }

  btnNewCourse.addEventListener('click', async () => {
    const title = window.prompt('新课程标题：', '');
    if (title === null) return;
    const t = title.trim();
    if (!t) { toast('课程标题不能为空', true); return; }
    const desc = window.prompt('课程简介（可留空）：', '') || '';
    try {
      const created = await API.teacher.createCourse(t, desc.trim());
      toast('课程已创建');
      currentCourseId = created.id;
      currentLecture = null;
      await loadCourses(false);
    } catch (e) {
      toast(e.message, true);
    }
  });

  // ---------- 章节 / 讲义树 ----------

  async function openCourse(courseId, keepSelection) {
    try {
      detail = await API.teacher.courseDetail(courseId);
    } catch (e) {
      toast(e.message, true);
      return;
    }
    currentCourseId = courseId;
    courseNameEl.textContent = detail.course.title;
    btnNewChapter.disabled = false;
    renderCourses();
    renderTree();

    if (keepSelection && currentLecture &&
        detail.lectures.some((l) => l.id === currentLecture.id)) {
      await openLecture(currentLecture.id);
    } else if (!keepSelection) {
      currentLecture = null;
      showEmptyEditor();
    }
  }

  function renderTree() {
    treeEl.innerHTML = '';
    if (!detail.chapters.length) {
      treeEl.innerHTML = '<div class="empty">还没有章节<br><span class="hint">点右上角「+ 章节」</span></div>';
      return;
    }
    detail.chapters.forEach((ch) => {
      const group = document.createElement('div');

      const head = document.createElement('div');
      head.className = 'item';
      head.style.cursor = 'default';
      head.innerHTML =
        '<span class="title">📂 ' + esc(ch.title) + '</span>' +
        '<span class="ops">' +
          '<button title="重命名" data-act="rename">✎</button>' +
          '<button title="新建讲义" data-act="add-le">＋</button>' +
          '<button title="删除章节" data-act="del-ch">🗑</button>' +
        '</span>';
      head.addEventListener('click', (e) => {
        const btn = e.target.closest('button');
        if (!btn) return;
        e.stopPropagation();
        const act = btn.dataset.act;
        if (act === 'rename') renameChapter(ch);
        if (act === 'add-le') addLecture(ch);
        if (act === 'del-ch') deleteChapter(ch);
      });
      group.appendChild(head);

      const lectures = detail.lectures.filter((l) => l.chapterId === ch.id);
      if (!lectures.length) {
        const tip = document.createElement('div');
        tip.className = 'hint';
        tip.style.padding = '2px 10px 6px 26px';
        tip.textContent = '（暂无讲义）';
        group.appendChild(tip);
      }
      lectures.forEach((le) => {
        const leEl = document.createElement('div');
        leEl.className = 'item' + (currentLecture && le.id === currentLecture.id ? ' active' : '');
        leEl.style.paddingLeft = '26px';
        leEl.innerHTML =
          '<span class="title">📄 ' + esc(le.title) + '</span>' +
          '<span class="badge ' + (le.published ? 'published' : 'draft') + '">' +
            (le.published ? '已发布' : '草稿') + '</span>';
        leEl.addEventListener('click', () => openLecture(le.id));
        group.appendChild(leEl);
      });

      treeEl.appendChild(group);
    });
  }

  btnNewChapter.addEventListener('click', async () => {
    if (!currentCourseId) { toast('请先选择课程', true); return; }
    const name = window.prompt('新章节名称：',
      '第 ' + (detail ? detail.chapters.length + 1 : 1) + ' 章');
    if (name === null) return;
    const title = name.trim();
    if (!title) { toast('章节名称不能为空', true); return; }
    try {
      await API.teacher.createChapter(currentCourseId, title);
      toast('章节已创建');
      await openCourse(currentCourseId, true);
    } catch (e) {
      toast(e.message, true);
    }
  });

  async function renameChapter(ch) {
    const name = window.prompt('修改章节名称：', ch.title);
    if (name === null) return;
    const title = name.trim();
    if (!title) { toast('章节名称不能为空', true); return; }
    try {
      await API.teacher.renameChapter(ch.id, title);
      toast('章节已重命名');
      await openCourse(currentCourseId, true);
    } catch (e) {
      toast(e.message, true);
    }
  }

  async function deleteChapter(ch) {
    if (!window.confirm('确定删除章节「' + ch.title + '」吗？\n该章节下的讲义也会被删除。')) {
      return;
    }
    try {
      await API.teacher.deleteChapter(ch.id);
      toast('章节已删除');
      if (currentLecture && currentLecture.chapterId === ch.id) {
        currentLecture = null;
      }
      await openCourse(currentCourseId, true);
    } catch (e) {
      toast(e.message, true);
    }
  }

  async function addLecture(ch) {
    const name = window.prompt('新讲义标题（章节：' + ch.title + '）：', '新讲义');
    if (name === null) return;
    const title = name.trim();
    if (!title) { toast('讲义标题不能为空', true); return; }
    try {
      const created = await API.teacher.createLecture(currentCourseId, ch.id, title, '');
      toast('讲义已创建，开始编辑吧');
      await openCourse(currentCourseId, false);
      await openLecture(created.id);
    } catch (e) {
      toast(e.message, true);
    }
  }

  // ---------- 讲义编辑器 ----------

  function showEmptyEditor() {
    emptyEl.style.display = '';
    editorEl.style.display = 'none';
  }

  function renderPreview() {
    previewEl.innerHTML = Markdown.render(contentEl.value);
  }

  async function openLecture(id) {
    try {
      currentLecture = await API.teacher.getLecture(id);
    } catch (e) {
      toast(e.message, true);
      return;
    }
    emptyEl.style.display = 'none';
    editorEl.style.display = '';
    titleEl.value = currentLecture.title;
    contentEl.value = currentLecture.content || '';
    renderStatus();
    renderPreview();
    renderTree();
  }

  function renderStatus() {
    if (currentLecture.published) {
      statusEl.textContent = '已发布 · 发布于 ' + fmtTime(currentLecture.publishedAt);
      statusEl.className = 'badge published';
      btnPublish.textContent = '撤销发布';
    } else {
      statusEl.textContent = '草稿（学生不可见）';
      statusEl.className = 'badge draft';
      btnPublish.textContent = '发布';
    }
  }

  contentEl.addEventListener('input', renderPreview);

  async function saveLecture() {
    if (!currentLecture) return;
    const title = titleEl.value.trim();
    if (!title) { toast('讲义标题不能为空', true); return; }
    btnSave.disabled = true;
    btnSave.textContent = '保存中…';
    try {
      const updated = await API.teacher.updateLecture(currentLecture.id, {
        title: title,
        content: contentEl.value
      });
      currentLecture = updated;
      renderStatus();
      toast('已保存');
      await openCourse(currentCourseId, true);
    } catch (e) {
      toast(e.message, true);
    } finally {
      btnSave.disabled = false;
      btnSave.textContent = '保存 (Ctrl+S)';
    }
  }

  btnSave.addEventListener('click', saveLecture);
  document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.keyCode === 83)) {
      if (editorEl.style.display !== 'none') {
        e.preventDefault();
        saveLecture();
      }
    }
  });

  btnPublish.addEventListener('click', async () => {
    if (!currentLecture) return;
    const next = !currentLecture.published;
    try {
      const updated = await API.teacher.setPublished(currentLecture.id, next);
      currentLecture = updated;
      renderStatus();
      renderTree();
      toast(next ? '已发布，学生端现在可以阅读' : '已撤销发布，学生端将不可见');
    } catch (e) {
      toast(e.message, true);
    }
  });

  btnDelete.addEventListener('click', async () => {
    if (!currentLecture) return;
    if (!window.confirm('确定删除讲义「' + currentLecture.title + '」吗？')) return;
    try {
      await API.teacher.deleteLecture(currentLecture.id);
      toast('讲义已删除');
      currentLecture = null;
      await openCourse(currentCourseId, false);
    } catch (e) {
      toast(e.message, true);
    }
  });

  // ---------- 启动 ----------
  loadCourses(false);
})();
