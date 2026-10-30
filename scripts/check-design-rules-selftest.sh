#!/usr/bin/env bash
# Self-test for the Phase 15 insets and dialog rules in check-design-rules.sh (D-17).
# Each rule gets a bad fixture that must fail with its heading, plus opt-out and good fixtures that must pass.
# Usage: bash scripts/check-design-rules-selftest.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RULES="$SCRIPT_DIR/check-design-rules.sh"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

mk() {
    # mk <case> <file name> : writes stdin to $tmp/<case>/<file name>
    mkdir -p "$tmp/$1"
    cat > "$tmp/$1/$2"
}

# --- fixtures -------------------------------------------------------------------------------------------------

mk a_bad FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(topBar = {}) { _ ->
        Text(text = stringResource(R.string.x))
    }
}
KT

mk a_unused FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(topBar = { Text(text = "t") }) { inner ->
        Text(text = stringResource(R.string.x))
    }
}
KT

mk a_optout FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    // insets-ok: fixture
    Scaffold(topBar = {}) { _ ->
        Text(text = stringResource(R.string.x))
    }
}
KT

mk a_two_scaffolds FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(topBar = {}) { innerPadding ->
        Text(text = "ignores the padding")
    }
}

@Composable
fun BarScreen() {
    Scaffold(topBar = {}) { innerPadding ->
        Column(Modifier.padding(innerPadding)) { Text(text = "uses it") }
    }
}
KT

mk a_named_content FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(
        topBar = {},
        content = { inner -> Text(text = "ignores the padding") },
    )
}
KT

mk a_named_content_ok FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(
        topBar = {},
        content = { inner -> Column(Modifier.padding(inner)) { Text(text = "x") } },
    )
}
KT

mk b_bad FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(contentWindowInsets = WindowInsets(0)) { inner ->
        Column(Modifier.padding(inner)) { Text(text = "x") }
    }
}
KT

mk b_optout FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(
        // insets-ok: fixture
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
        Column(Modifier.padding(inner)) { Text(text = "x") }
    }
}
KT

mk c_bad FooSheet.kt <<'KT'
package fixture

@Composable
fun FooSheet() {
    Column {
        OutlinedTextField(value = "v", onValueChange = {})
    }
}
KT

mk r7_bad FooDialog.kt <<'KT'
package fixture

@Composable
fun FooDialog() {
    AlertDialog(
        onDismissRequest = {},
        text = { OutlinedTextField(value = v, onValueChange = {}) },
        confirmButton = {},
    )
}
KT

mk r7_optout FooDialog.kt <<'KT'
package fixture

@Composable
fun FooDialog() {
    // insets-ok: fixture
    AlertDialog(
        onDismissRequest = {},
        text = { OutlinedTextField(value = v, onValueChange = {}) },
        confirmButton = {},
    )
}
KT

mk good FooScreen.kt <<'KT'
package fixture

@Composable
fun FooScreen() {
    Scaffold(topBar = {}) { inner ->
        Column(Modifier.padding(inner).imePadding()) {
            OutlinedTextField(value = "v", onValueChange = {})
        }
    }
}
KT

mk good_dialog FooDialog.kt <<'KT'
package fixture

@Composable
fun FooDialog() {
    AlertDialog(
        onDismissRequest = {},
        text = { Text(text = "no field here (parens)") },
        confirmButton = {},
    )
}
KT

# --- assertions -----------------------------------------------------------------------------------------------

failures=0

# The rules run from $tmp so the repo-wide Stripe key scan (app, spikes, gradle) finds nothing to walk.
expect_fail() {
    # expect_fail <case> <heading>
    local out status=0
    out=$(cd "$tmp" && bash "$RULES" "$tmp/$1" 2>&1) || status=$?
    if [ "$status" -ne 1 ]; then
        echo "FAIL: $1 expected exit 1, got $status"
        failures=$((failures + 1))
    elif ! printf '%s\n' "$out" | grep -qF "$2"; then
        echo "FAIL: $1 did not print heading: $2"
        failures=$((failures + 1))
    fi
}

expect_pass() {
    local out status=0
    out=$(cd "$tmp" && bash "$RULES" "$tmp/$1" 2>&1) || status=$?
    if [ "$status" -ne 0 ]; then
        echo "FAIL: $1 expected exit 0, got $status"
        printf '%s\n' "$out"
        failures=$((failures + 1))
    fi
}

expect_fail a_bad "Scaffold content padding not applied (insets rule a):"
expect_fail a_unused "Scaffold content padding not applied (insets rule a):"
expect_fail a_two_scaffolds "Scaffold content padding not applied (insets rule a):"
expect_fail a_named_content "Scaffold content padding not applied (insets rule a):"
expect_fail b_bad "Zero-inset Scaffold without nav/IME handling (insets rule b):"
expect_fail c_bad "Text field without IME handling (insets rule c):"
expect_fail r7_bad "Text field inside an AlertDialog (R7):"
expect_pass a_optout
expect_pass b_optout
expect_pass r7_optout
expect_pass a_named_content_ok
expect_pass good
expect_pass good_dialog

# Fail closed: with no usable perl the gate must exit 2 and never print "Design rules: OK".
status=0
out=$(cd "$tmp" && PERL_BIN=/nonexistent/perl bash "$RULES" "$tmp/good" 2>&1) || status=$?
if [ "$status" -ne 2 ] || printf '%s\n' "$out" | grep -qF "Design rules: OK"; then
    echo "FAIL: a missing perl must fail the gate with exit 2, got $status"
    failures=$((failures + 1))
fi

if [ "$failures" -ne 0 ]; then
    exit 1
fi
echo "Design rules self-test: OK"
