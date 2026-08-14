#!/bin/bash

find . -type f \( -name "*.java" -o -name "application.properties" \) \
  -not -path "*/target/*" \
  -not -path "*/build/*" \
  -not -path "*/.git/*" \
  -print0 | while IFS= read -r -d '' file; do
    echo -e "\n========================================"
    echo "PATH: $file"
    echo -e "========================================\n"
    cat "$file"
done > project_dump.txt

echo "Codebase successfully dumped to project_dump.txt"
