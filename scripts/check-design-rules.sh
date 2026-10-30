#!/usr/bin/env bash
# CI gate (FND-02): no Color literals outside the theme, no *Brand types, no Stripe secret keys.
# Phase 15 D-17 adds the insets and dialog rules (main sources only):
#   (a) a Scaffold whose content lambda padding is never applied
#   (b) a zero-inset Scaffold file with no nav-bar / IME handling
#   (c) a Screen or Sheet file with a text field and no imePadding / verticalScroll / LazyColumn
#   (R7) an AlertDialog whose arguments contain a text field (dialog windows do not follow the IME rules)
# A hit is opted out with `// insets-ok: <reason>` on the flagged line or the line directly above it.
# Usage: bash scripts/check-design-rules.sh [SRC_DIR]   (default: app/src)
set -euo pipefail

SRC_DIR="${1:-app/src}"
THEME_COLOR="ui/theme/Color.kt"
OPT_OUT='// insets-ok:'
PERL_BIN="${PERL_BIN:-perl}"
fail=0

# The perl rules must fail closed: a missing perl, a script error or an unreadable file is an internal error (exit 2),
# never an empty hit list that reads as "OK".
internal_error() {
    echo "Design rules: internal error while running $1 (is perl available?); refusing to report OK." >&2
    exit 2
}

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

hits=$(grep -rnE '(sk_live|sk_test|rk_live|rk_test)_[A-Za-z0-9]+' app spikes gradle ./*.kts 2>/dev/null || true)
if [ -n "$hits" ]; then
    echo "Stripe secret key pattern:"
    echo "$hits"
    fail=1
fi

# Main sources only: tests and androidTest may do what they like with insets.
MAIN_FILES=$(find "$SRC_DIR" -name '*.kt' -not -path '*/src/test/*' -not -path '*/src/androidTest/*' | sort)

# True when line $2 of file $1, or the line above it, carries the opt-out comment.
opted_out() {
    local file="$1" line="$2" prev
    prev=$((line - 1))
    if sed -n "${line}p" "$file" | grep -qF "$OPT_OUT"; then
        return 0
    fi
    if [ "$prev" -ge 1 ] && sed -n "${prev}p" "$file" | grep -qF "$OPT_OUT"; then
        return 0
    fi
    return 1
}

# First line number in file $1 matching extended regex $2.
first_line() {
    grep -nE "$2" "$1" | head -1 | cut -d: -f1
}

read -r -d '' PERL_SCAFFOLD <<'PERL' || true
# Insets rule (a): for every Scaffold(...) call with a content lambda ({ name -> ... } after the arguments, or
# content = { name -> ... } inside them), the lambda parameter must exist, not be _, and be used INSIDE that lambda.
sub lambda_body {
    my ($src, $pos) = @_;
    my ($depth, $i, $len) = (1, $pos, length($src));
    while ($i < $len) {
        my $c = substr($src, $i, 1);
        if ($c eq '{') {
            $depth++;
        } elsif ($c eq '}') {
            $depth--;
            last if $depth == 0;
        }
        $i++;
    }
    return substr($src, $pos, $i - $pos);
}
for my $f (@ARGV) {
    open(my $fh, "<", $f) or die "cannot read $f: $!\n";
    local $/;
    my $src = <$fh>;
    close($fh);
    my @lines = split(/\n/, $src, -1);
    while ($src =~ /\bScaffold\s*(\((?:[^()]++|(?1))*\))/g) {
        my ($args, $argsStart, $start, $end) = ($1, $-[1], $-[0], $+[0]);
        my ($name, $bodyStart);
        if (substr($src, $end) =~ /\A\s*\{\s*(?:(\w+)\s*->)?/) {
            $name = $1;
            $bodyStart = $end + $+[0];
        } elsif ($args =~ /\bcontent\s*=\s*\{\s*(?:(\w+)\s*->)?/) {
            $name = $1;
            $bodyStart = $argsStart + $+[0];
        } else {
            next;
        }
        $name = defined($name) ? $name : "";
        my $ln = 1 + (substr($src, 0, $start) =~ tr/\n//);
        my $cur = $lines[$ln - 1];
        my $prev = $ln > 1 ? $lines[$ln - 2] : "";
        next if $cur =~ m{// insets-ok:} || $prev =~ m{// insets-ok:};
        my $bad = 0;
        if ($name eq "" || $name eq "_") {
            $bad = 1;
        } else {
            my $n = quotemeta($name);
            my $body = lambda_body($src, $bodyStart);
            $bad = 1 unless $body =~ /padding\(\s*$n\b|consumeWindowInsets\(\s*$n\b|windowInsetsPadding\(\s*$n\b|\b$n\.calculate/;
        }
        print "$f:$ln\n" if $bad;
    }
}
PERL

read -r -d '' PERL_DIALOG <<'PERL' || true
# Rule R7: an AlertDialog(...) argument span must not contain a text field.
for my $f (@ARGV) {
    open(my $fh, "<", $f) or next;
    local $/;
    my $src = <$fh>;
    close($fh);
    my @lines = split(/\n/, $src, -1);
    while ($src =~ /\bAlertDialog\s*(\((?:[^()]++|(?1))*\))/g) {
        my $args = $1;
        my $start = $-[0];
        my $ln = 1 + (substr($src, 0, $start) =~ tr/\n//);
        my $cur = $lines[$ln - 1];
        my $prev = $ln > 1 ? $lines[$ln - 2] : "";
        next if $cur =~ m{// insets-ok:} || $prev =~ m{// insets-ok:};
        print "$f:$ln\n" if $args =~ /TextField\(|ValidatedField\(/;
    }
}
PERL

# (a) Scaffold content padding applied.
hits=""
if [ -n "$MAIN_FILES" ]; then
    hits=$(printf '%s\n' "$MAIN_FILES" | tr '\n' '\0' | xargs -0 "$PERL_BIN" -e "$PERL_SCAFFOLD" --) ||
        internal_error "insets rule a"
fi
if [ -n "$hits" ]; then
    echo "Scaffold content padding not applied (insets rule a):"
    echo "$hits"
    fail=1
fi

# (b) Zero-inset Scaffold must handle the navigation bar and the IME itself.
hits=""
while IFS= read -r f; do
    [ -n "$f" ] || continue
    grep -qE 'contentWindowInsets = WindowInsets\(0' "$f" || continue
    if grep -qE 'navigationBarsPadding|imePadding|windowInsetsPadding|safeDrawing|WindowInsets\.navigationBars' "$f"; then
        continue
    fi
    ln=$(first_line "$f" 'contentWindowInsets = WindowInsets\(0')
    if ! opted_out "$f" "$ln"; then
        hits="${hits}${f}:${ln}"$'\n'
    fi
done <<EOF
$MAIN_FILES
EOF
if [ -n "$hits" ]; then
    echo "Zero-inset Scaffold without nav/IME handling (insets rule b):"
    printf '%s' "$hits"
    fail=1
fi

# (c) A Screen or Sheet with a text field must scroll or lift above the IME.
hits=""
while IFS= read -r f; do
    [ -n "$f" ] || continue
    case "$f" in
        *Screen.kt | *Sheet.kt) ;;
        *) continue ;;
    esac
    grep -qE 'TextField\(|ValidatedField\(' "$f" || continue
    if grep -qE 'imePadding|verticalScroll|LazyColumn' "$f"; then
        continue
    fi
    ln=$(first_line "$f" 'TextField\(|ValidatedField\(')
    if ! opted_out "$f" "$ln"; then
        hits="${hits}${f}:${ln}"$'\n'
    fi
done <<EOF
$MAIN_FILES
EOF
if [ -n "$hits" ]; then
    echo "Text field without IME handling (insets rule c):"
    printf '%s' "$hits"
    fail=1
fi

# (R7) No text field inside an AlertDialog.
hits=""
if [ -n "$MAIN_FILES" ]; then
    hits=$(printf '%s\n' "$MAIN_FILES" | tr '\n' '\0' | xargs -0 "$PERL_BIN" -e "$PERL_DIALOG" --) ||
        internal_error "rule R7"
fi
if [ -n "$hits" ]; then
    echo "Text field inside an AlertDialog (R7):"
    echo "$hits"
    fail=1
fi

if [ "$fail" -eq 0 ]; then
    echo "Design rules: OK"
fi
exit "$fail"
