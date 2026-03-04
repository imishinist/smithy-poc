#!/usr/bin/env bash
set -euo pipefail

OPENAPI_JSON="build/smithyprojections/smithy-poc/openapi/openapi/MyService.openapi.json"
OUTPUT="docs/index.html"

if [ ! -f "$OPENAPI_JSON" ]; then
  echo "Error: $OPENAPI_JSON not found. Run 'make smithy' first." >&2
  exit 1
fi

mkdir -p docs

SPEC=$(cat "$OPENAPI_JSON" | tr -d '\n' | sed "s/'/\\\\'/g")

cat > "$OUTPUT" <<EOF
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>MyService API Documentation</title>
  <link rel="stylesheet" href="https://unpkg.com/@stoplight/elements/styles.min.css">
  <script src="https://unpkg.com/@stoplight/elements/web-components.min.js"></script>
  <style>body { margin: 0; height: 100vh; } elements-api { height: 100%; }</style>
</head>
<body>
  <elements-api id="docs" router="hash" layout="sidebar"></elements-api>
  <script>
    const spec = ${SPEC};
    document.getElementById('docs').apiDescriptionDocument = JSON.stringify(spec);
  </script>
</body>
</html>
EOF

echo "Generated: $OUTPUT"
