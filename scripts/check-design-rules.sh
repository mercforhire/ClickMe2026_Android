#!/usr/bin/env bash
# CI gate (FND-02): no Color literals outside the theme, no *Brand types, no Stripe secret keys.
# Usage: bash scripts/check-design-rules.sh [SRC_DIR]   (default: app/src)
set -euo pipefail

SRC_DIR="${1:-app/src}"
THEME_COLOR="ui/theme/Color.kt"
fail=0

hits=$(grep -rnE --include='*.kt' 'Color\((0x|red *=|[0-9])' "$SRC_DIR" | grep -v "$THEME_COLOR" || true)
if [ -n "$hits" ]; then
    echo "Color literals outside $THEME_COLOR:"
    echo "$hits"
    fail=1
fi

hits=$(grep -rnE --include='*.kt' '(object|class|enum class|interface) +[A-Za-z0-9_]*Brand\b' "$SRC_DIR" || true)
if [ -n "$hits" ]; then
    echo "Forbidden *Brand types:"
    echo "$hits"
    fail=1
fi

hits=$(grep -rnE '(sk_live|sk_test|rk_live|rk_test)_[A-Za-z0-9]+' app gradle ./*.kts 2>/dev/null || true)
if [ -n "$hits" ]; then
    echo "Stripe secret key pattern:"
    echo "$hits"
    fail=1
fi

if [ "$fail" -eq 0 ]; then
    echo "Design rules: OK"
fi
exit "$fail"
