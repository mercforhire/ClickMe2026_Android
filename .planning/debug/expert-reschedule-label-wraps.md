---
status: diagnosed
trigger: "G-16.2-3: Expert Bookings upcoming card, Reschedule label wraps, final 'e' on second line (Samsung, default font scale)"
goal: find_root_cause_only
created: 2026-11-01
---

## Symptoms
expected: Reschedule action button label on one line in the expert upcoming card
actual: "Reschedul" / "e" (the final letter wraps)
errors: none
reproduction: UAT Test 3, SM-A075F, font scale 1.0
started: Phase 16.2 (added RescheduleActionButton plus ActionButtonPair on the card)

## Eliminated
- Font scale: `settings get system font_scale` = 1.0, so the 1.5x stacking path is not involved.
- Longer label: the visible text is only "Reschedule" (`reschedule_start_action`). The a11y string "Reschedule session with %1$s" is semantics only.
- M3 Button IconSpacing: a 76 dp slot predicts exactly "Reschedul" + "e" (75.9 dp fits, 84.5 dp does not). An extra 8 dp of spacing would predict "Reschedu" + "le", which was not observed.

## Evidence
- Device `adb`: SM-A075F, 720x1600 at 300 dpi = 384 dp wide.
- Card geometry: list `contentPadding` md=16 (`ExpertBookingsScreen.kt:336`) + card padding md=16 (`ExpertUpcomingCard.kt:138`) = inner width 384-64 = 320 dp.
- `ActionButtonPair` (`RescheduleActionButton.kt:101`) stacks when `maxWidth < 320.dp`. `320 < 320` is false, so a Row with weight(1f) each and a `Spacing.sm` gap gives 156 dp per button.
- Per-button chrome: `PressScaleButton` contentPadding horizontal 24 x 2 = 48 (`PressScale.kt:66`), icon 24, spacer 8, leaving a 76 dp text slot.
- Nunito SemiBold 600 at 16sp (fontTools on `res/font/nunito.ttf`): "Reschedule" 84.5 dp, "Reschedul" 75.9 dp, "Message" 64.5 dp, "Cancel Booking" 113.7 dp.
- iOS (`UpcomingBookingsSessionCard.swift:235-253`): the same pair has `frame(maxWidth: .infinity)` and no horizontal padding, 15pt text.
- `RescheduleActionButtonTest` uses `w411dp` and stub labels, so it never exercises this.
- Latent: client `UpcomingBookingCard.kt` has identical geometry (will wrap once G-16.2-1 shows the button); `UpcomingBookingScreen.kt:453` and `ExpertBookingSummaryScreen.kt:389` would wrap "Reschedule" at ~352-377 dp, and "Cancel Booking" (113.7 dp) likely wraps in its 88 dp slot at 384 dp.

## Resolution
root_cause: Stack decision is container-width-only (320 dp, exactly met at 384 dp); the half-width button's built-in 24 dp side padding leaves 76 dp for an 84.5 dp label; the Text has no wrap control, so it breaks mid-word.
fix: (not applied; diagnose-only)
