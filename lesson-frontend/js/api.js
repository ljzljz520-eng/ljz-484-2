/* ===== API 客户端封装 =====
 * 后端地址优先级：localStorage.lesson_api_base > 同源(8080 端口托管时) > http://localhost:8080
 * 老师令牌存于 localStorage.lesson_teacher_token，请求头 X-Teacher-Token。
 */
(function (global) {
  const DEFAULT_BASE = 'http://localhost:8080';

  function getBase() {
    const saved = localStorage.getItem('lesson_api_base');
    if (saved && saved.trim()) return saved.trim().replace(/\/+$/, '');
    // 页面若由后端同源托管（例如直接 8080 端口），则走同源
    if (location.protocol.startsWith('http') && location.port === '8080') {
      return location.origin;
    }
    return DEFAULT_BASE;
  }

  function getToken() {
    return localStorage.getItem('lesson_teacher_token') || '';
  }

  async function request(path, options = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    const token = getToken();
    if (token) headers['X-Teacher-Token'] = token;

    let resp;
    try {
      resp = await fetch(getBase() + path, Object.assign({}, options, { headers }));
    } catch (e) {
      throw new Error('无法连接后端 ' + getBase() + ' ，请确认 lesson-backend 已启动');
    }
    if (!resp.ok) {
      let msg = resp.status + ' ' + resp.statusText;
      try {
        const data = await resp.json();
        if (data && data.error) msg = data.error;
      } catch (e) { /* ignore */ }
      const err = new Error(msg);
      err.status = resp.status;
      throw err;
    }
    return resp.json();
  }

  global.API = {
    getBase,
    setBase(url) {
      if (url && url.trim()) localStorage.setItem('lesson_api_base', url.trim());
      else localStorage.removeItem('lesson_api_base');
    },
    getToken,
    setToken(t) { localStorage.setItem('lesson_teacher_token', t || ''); },

    // ---- 学生端（公开） ----
    student: {
      listCourses: () => request('/api/student/courses'),
      courseDetail: (id) => request('/api/student/courses/' + encodeURIComponent(id)),
      lecture: (id) => request('/api/student/lectures/' + encodeURIComponent(id)),
    },

    // ---- 老师端（需令牌） ----
    teacher: {
      listCourses: () => request('/api/teacher/courses'),
      courseDetail: (id) => request('/api/teacher/courses/' + encodeURIComponent(id)),
      createCourse: (title, description) =>
        request('/api/teacher/courses', { method: 'POST', body: JSON.stringify({ title, description }) }),
      updateCourse: (id, patch) =>
        request('/api/teacher/courses/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(patch) }),
      deleteCourse: (id) =>
        request('/api/teacher/courses/' + encodeURIComponent(id), { method: 'DELETE' }),

      createChapter: (courseId, title) =>
        request('/api/teacher/courses/' + encodeURIComponent(courseId) + '/chapters',
          { method: 'POST', body: JSON.stringify({ title }) }),
      renameChapter: (id, title) =>
        request('/api/teacher/chapters/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify({ title }) }),
      deleteChapter: (id) =>
        request('/api/teacher/chapters/' + encodeURIComponent(id), { method: 'DELETE' }),

      createLecture: (courseId, chapterId, title, content) =>
        request('/api/teacher/courses/' + encodeURIComponent(courseId) + '/lectures',
          { method: 'POST', body: JSON.stringify({ chapterId, title, content }) }),
      getLecture: (id) => request('/api/teacher/lectures/' + encodeURIComponent(id)),
      updateLecture: (id, patch) =>
        request('/api/teacher/lectures/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(patch) }),
      setPublished: (id, published) =>
        request('/api/teacher/lectures/' + encodeURIComponent(id) + '/publish',
          { method: 'PATCH', body: JSON.stringify({ published }) }),
      deleteLecture: (id) =>
        request('/api/teacher/lectures/' + encodeURIComponent(id), { method: 'DELETE' }),
    },
  };
})(window);
