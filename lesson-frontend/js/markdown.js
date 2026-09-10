/* ===== 极简 Markdown 渲染器（无第三方依赖） =====
 * 支持：标题、代码块、行内代码、粗体、斜体、删除线、链接、图片、
 *       有序/无序列表、引用、分割线、表格、段落。
 * 先做 HTML 转义，保证讲义内容安全渲染。
 */
(function (global) {
  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function inline(text) {
    let t = escapeHtml(text);
    const codes = [];
    // 行内代码先占位，避免内部被其他规则处理
    t = t.replace(/`([^`]+)`/g, (m, c) => {
      codes.push('<code>' + c + '</code>');
      return '\x00' + (codes.length - 1) + '\x00';
    });
    t = t.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, '<img alt="$1" src="$2">');
    t = t.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
    t = t.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    t = t.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>');
    t = t.replace(/~~([^~]+)~~/g, '<del>$1</del>');
    t = t.replace(/\x00(\d+)\x00/g, (m, i) => codes[+i]);
    return t;
  }

  function render(src) {
    const lines = String(src || '').replace(/\r\n?/g, '\n').split('\n');
    const html = [];
    let i = 0;
    let para = [];

    function flushPara() {
      if (para.length) {
        html.push('<p>' + para.map(inline).join('<br>') + '</p>');
        para = [];
      }
    }

    while (i < lines.length) {
      const line = lines[i];

      //  fenced code block
      const fence = line.match(/^```(\w*)\s*$/);
      if (fence) {
        flushPara();
        const buf = [];
        i++;
        while (i < lines.length && !/^```\s*$/.test(lines[i])) { buf.push(lines[i]); i++; }
        i++; // skip closing ```
        html.push('<pre><code>' + escapeHtml(buf.join('\n')) + '</code></pre>');
        continue;
      }

      // heading
      const h = line.match(/^(#{1,6})\s+(.*)$/);
      if (h) { flushPara(); html.push('<h' + h[1].length + '>' + inline(h[2]) + '</h' + h[1].length + '>'); i++; continue; }

      // hr
      if (/^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(line)) { flushPara(); html.push('<hr>'); i++; continue; }

      // blockquote（支持多行）
      if (/^>\s?/.test(line)) {
        flushPara();
        const buf = [];
        while (i < lines.length && /^>\s?/.test(lines[i])) { buf.push(lines[i].replace(/^>\s?/, '')); i++; }
        html.push('<blockquote>' + render(buf.join('\n')) + '</blockquote>');
        continue;
      }

      // table
      if (/^\|.*\|\s*$/.test(line) && i + 1 < lines.length && /^\|[\s:|-]+\|\s*$/.test(lines[i + 1])) {
        flushPara();
        const parseRow = (r) => r.trim().replace(/^\||\|$/g, '').split('|').map((c) => inline(c.trim()));
        const head = parseRow(line);
        i += 2;
        const rows = [];
        while (i < lines.length && /^\|.*\|\s*$/.test(lines[i])) { rows.push(parseRow(lines[i])); i++; }
        let t = '<table><thead><tr>' + head.map((c) => '<th>' + c + '</th>').join('') + '</tr></thead><tbody>';
        t += rows.map((r) => '<tr>' + r.map((c) => '<td>' + c + '</td>').join('') + '</tr>').join('');
        html.push(t + '</tbody></table>');
        continue;
      }

      // unordered list
      if (/^\s*[-*+]\s+/.test(line)) {
        flushPara();
        const items = [];
        while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
          items.push(lines[i].replace(/^\s*[-*+]\s+/, '')); i++;
        }
        html.push('<ul>' + items.map((it) => '<li>' + inline(it) + '</li>').join('') + '</ul>');
        continue;
      }

      // ordered list
      if (/^\s*\d+[.、]\s+/.test(line)) {
        flushPara();
        const items = [];
        while (i < lines.length && /^\s*\d+[.、]\s+/.test(lines[i])) {
          items.push(lines[i].replace(/^\s*\d+[.、]\s+/, '')); i++;
        }
        html.push('<ol>' + items.map((it) => '<li>' + inline(it) + '</li>').join('') + '</ol>');
        continue;
      }

      // blank
      if (/^\s*$/.test(line)) { flushPara(); i++; continue; }

      para.push(line);
      i++;
    }
    flushPara();
    return html.join('\n');
  }

  global.Markdown = { render };
})(window);
