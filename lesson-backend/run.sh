#!/usr/bin/env bash
# 编译并运行后端（优先使用 JAVA_HOME 指定的 JDK）
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(pwd)"

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

# 未显式指定 DATA_FILE 时，数据固定放在后端根目录的 data/ 下
export DATA_FILE="${DATA_FILE:-$ROOT/data/db.json}"
mkdir -p "$(dirname "$DATA_FILE")"

echo ">> 启动服务（Ctrl+C 停止）..."
exec "$JAVA" -cp "$OUT" com.lesson.Main
