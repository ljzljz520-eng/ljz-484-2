#!/usr/bin/env bash
# 编译并运行后端（优先使用 JAVA_HOME 指定的 JDK）
set -euo pipefail
cd "$(dirname "$0")"

OUT=out
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
  JAVAC="$JAVA_HOME/bin/javac"
else
  JAVA=java
  JAVAC=javac
fi

echo ">> 编译源码到 $OUT/ ..."
$JAVAC -encoding UTF-8 -d "$OUT" $(find src -name '*.java')
echo ">> 启动服务（Ctrl+C 停止）..."
cd "$OUT"
exec "$JAVA" com.lesson.Main
