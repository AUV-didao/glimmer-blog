#!/bin/sh
set -eu

# 首次启动从种子库初始化，后续启动继续使用数据卷中的数据库。
mkdir -p "$(dirname "$BLOG_DB_PATH")"
if [ ! -e "$BLOG_DB_PATH" ]; then
    cp /opt/glimmer/blog-seed.db "$BLOG_DB_PATH.tmp"
    mv "$BLOG_DB_PATH.tmp" "$BLOG_DB_PATH"
fi

exec "$@"
