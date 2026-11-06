#!/usr/bin/env bash
# Play Store phone screenshots from the Samsung SM-A075F (Phase 17, D-16).
#
# Google rejects screenshots whose long side is more than twice the short side; the Samsung's native
# 720x1600 is 2.22:1. `prep` therefore overrides the display to 1080x1920 (density 420) and puts SystemUI
# into demo mode (clean status bar). `restore` ALWAYS has to run afterwards: a display override left on a
# real phone is a denial of service for its owner.
#
# Usage:
#   bash scripts/play-store-screenshot.sh prep
#   bash scripts/play-store-screenshot.sh capture <name> [--native]
#   bash scripts/play-store-screenshot.sh check <file>
#   bash scripts/play-store-screenshot.sh restore
#   bash scripts/play-store-screenshot.sh session <cmd...>   # runs prep, then <cmd...>, restore on exit (trap)
#
# Environment:
#   ASSETS_DIR  output folder, outside the repo (default /Users/LeonChen/ClickMe-release/store-assets)
#   ADB         adb binary (default /Users/LeonChen/Library/Android/sdk/platform-tools/adb)
#   MODEL       device model token on the `adb devices -l` line (default SM_A075F)
#
# Interactive use must always end with `restore`. The transport id is used (-t), never -s: -s hangs on
# wireless ADB (Phase 16 runbook H1).
#
# `capture <name> --native` is the fallback when the wm override is not honoured: the native capture is
# cropped to a 2:1 frame (removes the status bar at the top and the navigation bar at the bottom), which
# meets Google's minimum but not the 9:16 recommendation for featuring.
set -euo pipefail

ADB="${ADB:-/Users/LeonChen/Library/Android/sdk/platform-tools/adb}"
ASSETS_DIR="${ASSETS_DIR:-/Users/LeonChen/ClickMe-release/store-assets}"
MODEL="${MODEL:-SM_A075F}"
TARGET_SIZE="1080x1920"
TARGET_DENSITY="420"

usage() {
    sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'
    exit "${1:-2}"
}

transport() {
    local id
    id=$("$ADB" devices -l | sed -n "s/.*model:${MODEL}.*transport_id:\([0-9]*\).*/\1/p" | head -1)
    if [ -z "$id" ]; then
        echo "FAIL no device with model:${MODEL} on '$ADB devices -l' (wireless debugging on?)" >&2
        exit 3
    fi
    printf '%s' "$id"
}

adb_t() {
    "$ADB" -t "$(transport)" "$@"
}

demo() {
    adb_t shell am broadcast -a com.android.systemui.demo "$@" >/dev/null
}

cmd_prep() {
    mkdir -p "$ASSETS_DIR"
    echo "before: $(adb_t shell wm size | tr '\n' ' ') | $(adb_t shell wm density | tr '\n' ' ')"
    adb_t shell wm size "$TARGET_SIZE"
    adb_t shell wm density "$TARGET_DENSITY"
    adb_t shell settings put global sysui_demo_allowed 1
    demo -e command enter
    demo -e command clock -e hhmm 1000
    demo -e command battery -e level 100 -e plugged false
    demo -e command network -e wifi show -e level 4 -e mobile show -e level 4 -e datatype none
    demo -e command notifications -e visible false
    echo "prep done: $(adb_t shell wm size | tr '\n' ' ')"
}

cmd_restore() {
    # Every step is attempted even if an earlier one fails.
    demo -e command exit || echo "warn: demo exit failed" >&2
    adb_t shell settings put global sysui_demo_allowed 0 || true
    adb_t shell wm size reset || echo "warn: wm size reset failed" >&2
    adb_t shell wm density reset || echo "warn: wm density reset failed" >&2
    echo "restored: $(adb_t shell wm size | tr '\n' ' ') | $(adb_t shell wm density | tr '\n' ' ')"
}

cmd_check() {
    local f="${1:-}"
    [ -n "$f" ] || usage
    if [ ! -f "$f" ]; then
        echo "FAIL $f missing"
        exit 1
    fi
    local info w h a
    info=$(sips -g pixelWidth -g pixelHeight -g hasAlpha "$f")
    w=$(printf '%s\n' "$info" | sed -n 's/.*pixelWidth: *//p')
    h=$(printf '%s\n' "$info" | sed -n 's/.*pixelHeight: *//p')
    a=$(printf '%s\n' "$info" | sed -n 's/.*hasAlpha: *//p')
    if [ -z "$w" ] || [ -z "$h" ]; then
        echo "FAIL $f unreadable by sips"
        exit 1
    fi
    local short=$w long=$h
    if [ "$h" -lt "$w" ]; then short=$h; long=$w; fi
    if [ "$short" -lt 320 ] || [ "$long" -gt 3840 ]; then
        echo "FAIL $f ${w}x${h} outside 320-3840 per side"
        exit 1
    fi
    if [ "$long" -gt $((short * 2)) ]; then
        echo "FAIL $f ${w}x${h} long side is more than twice the short side"
        exit 1
    fi
    case "$f" in
        *.jpg | *.jpeg)
            if [ "$a" != "no" ]; then
                echo "FAIL $f ${w}x${h} JPEG carries alpha=$a"
                exit 1
            fi
            ;;
    esac
    echo "OK $f ${w}x${h} alpha=$a"
}

cmd_capture() {
    local name="${1:-}" native=0
    [ -n "$name" ] || usage
    shift || true
    for arg in "$@"; do
        case "$arg" in
            --native) native=1 ;;
            *) usage ;;
        esac
    done
    mkdir -p "$ASSETS_DIR"
    local png="$ASSETS_DIR/$name.png" jpg="$ASSETS_DIR/$name.jpg"
    adb_t exec-out screencap -p >"$png"
    if [ "$native" -eq 1 ]; then
        local w h
        w=$(sips -g pixelWidth "$png" | sed -n 's/.*pixelWidth: *//p')
        h=$(sips -g pixelHeight "$png" | sed -n 's/.*pixelHeight: *//p')
        local target=$((w * 2))
        if [ "$h" -gt "$target" ]; then
            # Remove the status bar share at the top (~4% of the height) and the rest from the bottom
            # (navigation bar), keeping height = 2 x width.
            local top=$((h * 4 / 100))
            if [ $((h - top)) -lt "$target" ]; then top=$((h - target)); fi
            sips -c "$target" "$w" --cropOffset "$top" 0 "$png" >/dev/null
        fi
    fi
    sips -s format jpeg -s formatOptions 92 "$png" --out "$jpg" >/dev/null
    rm -f "$png"
    cmd_check "$jpg"
}

cmd_session() {
    [ "$#" -gt 0 ] || usage
    trap cmd_restore EXIT
    cmd_prep
    "$@"
}

sub="${1:-}"
[ -n "$sub" ] || usage
shift
case "$sub" in
    prep) cmd_prep ;;
    capture) cmd_capture "$@" ;;
    check) cmd_check "$@" ;;
    restore) cmd_restore ;;
    session) cmd_session "$@" ;;
    -h | --help | help) usage 0 ;;
    *) usage ;;
esac
