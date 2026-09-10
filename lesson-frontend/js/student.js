/* ===== 学生端逻辑：只读已发布讲义 ===== */
(function () {
  const treeEl = document.getElementById('course-tree');
  const readerEl = document.getElementById('reader');
  const apiBaseInput = document.getElementById('api-base');

  apiBaseInput.value = API.getBase();
  apiBaseInput.addEventListener('change', () => {
    API.setBase(apiBaseInput.value);
    apiBaseInput.value = API.getBase();
    load();
    toast('后端地址已更新为 ' + API.getBase());
  });

  function toast(msg, isErr) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.className = 'show' + (isErr ? ' err' : '');
    setTimeout(() => { t.className = ''; }, 2200);
  }

  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  async function load() {
    treeEl.innerHTML = '<div class="empty">加载中…</div>';
    try {
      const courses = await API.student.listCourses();
      if (!courses.length) {
        treeEl.innerHTML = '<div class="empty">暂无已发布的讲义</div>';
        return;
      }
      // 逐个课程拉取章节/讲义目录（不含正文）
      const details = await Promise.all(courses.map((c) => API.student.courseDetail(c.id)));
      renderTree(details);
    } catch (e) {
      treeEl.innerHTML = '<div class="empty">加载失败<br>' + esc(e.message) + '</div>';
    }
  }

  function renderTree(details) {
    treeEl.innerHTML = '';
    for (const d of details) {
      const group = document.createElement('div');
      const gt = document.createElement('div');
      gt.className = 'group-title';
      gt.textContent = d.course.title;
      gt.title = d.course.description || '';
      group.appendChild(gt);

      for (const ch of d.chapters) {
        const chEl = document.createElement('div');
        chEl.className = 'item';
        chEl.innerHTML = '<span class="title">📂 ' + esc(ch.title) + '</span>';
        chEl.style.cursor = 'default';
        group.appendChild(chEl);

        for (const le of d.lectures.filter((l) => l.chapterId === ch.id)) {
          const leEl = document.createElement('div');
          leEl.className = 'item';
          leEl.style.paddingLeft = '26px';
          leEl.innerHTML = '<span class="title">📄 ' + esc(le.title) + '</span>';
          leEl.addEventListener('click', () => {
            document.querySelectorAll('.sidebar .item.active')
              .forEach((x) => x.classList.remove('active'));
            leEl.classList.add('active');
            openLecture(le.id, d.course.title, ch.title);
          });
          group.appendChild(leEl);
        }
      }
      treeEl.appendChild(group);
    }
  }

  async function openLecture(id, courseTitle, chapterTitle) {
    readerEl.innerHTML = '<div class="empty">加载中…</div>';
    try {
      const le = await API.student.lecture(id);
      readerEl.innerHTML =
        '<div class="crumb">' + esc(courseTitle) + ' / ' + esc(chapterTitle) + '</div>' +
        '<div class="md">' + Markdown.render(le.content) + '</div>';
    } catch (e) {
      readerEl.innerHTML = '<div class="empty">加载失败：' + esc(e.message) + '</div>';
      toast(e.message, true);
    }
  }

  load();
})();
