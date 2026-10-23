---
status: complete
phase: 06-booking-flow-free-bookings
source: [06-01-SUMMARY.md, 06-02-SUMMARY.md, 06-03-SUMMARY.md, 06-04-SUMMARY.md, 06-05-SUMMARY.md, 06-06-SUMMARY.md, 06-07-SUMMARY.md, 06-08-SUMMARY.md, 06-09-SUMMARY.md, 06-10-SUMMARY.md, 06-11-SUMMARY.md, 06-12-SUMMARY.md, 06-13-SUMMARY.md, 06-14-SUMMARY.md, 06-VERIFICATION.md, 06-REVIEW-FIX.md]
started: 2026-10-22T22:00:00Z
updated: 2026-10-22T23:40:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Open the booking form
expected: From Expert Profile, tap Book Session. Make a Booking opens with the bottom bar hidden: expert card, topic, calendar, time slots, meeting type, notes, Summary and Book Now. Book Now is disabled until a topic and a slot are chosen.
result: pass

### 2. Topic sheet
expected: Tapping the topic picker opens a bottom sheet listing the expert's topics; paid topics show their price. Picking a free topic closes the sheet and shows it in the picker. Picking a paid topic disables Book Now and shows the "coming soon" note. The sheet stays open across a rotation.
result: pass

### 3. Calendar
expected: Past days and months before the current one can't be selected. Days with open slots have a dot. The month arrows move between months and the dots update.
result: pass

### 4. Time slots and timezone note
expected: Picking a day lists its slots in your local time. The timezone note names the expert's city and zone abbreviation when their zone differs from yours. A slot held from iOS is dimmed and can't be selected.
result: pass

### 5. Cross-timezone slots near midnight (SC-2)
expected: With the iOS expert set to a far zone (e.g. Pacific/Auckland) and slots near midnight at a month edge, each slot appears under the correct local date, with no duplicates across neighbouring months.
result: skipped
reason: "Difficult to set up a far-zone expert; low priority since most users are in Canada and the US. Covered by golden-data unit tests (BookingSlotsTest)."

### 6. Meeting type and notes keyboard (CR-01 re-check)
expected: Meeting type can be switched. Tapping Notes opens the keyboard and it STAYS open while you type (the CR-01 fix). The counter counts up to 500 and stops there. The keyboard doesn't cover Book Now. Swiping the form with your finger dismisses the keyboard.
result: pass

### 7. Free booking end to end (SC-3)
expected: With a free topic, a slot, a meeting type and notes, tap Book Now. A spinner shows on the button and the form doesn't respond to taps (WR-04). Request Sent opens with the details and "Awaiting Approval". The iOS expert gets the booking_request push and sees the request.
result: pass

### 8. Request Sent navigation
expected: From Request Sent: Back lands on Expert Profile; View My Bookings opens the Bookings tab; Back to Home selects the Explore tab at its root (IN-10), including when started from another tab.
result: pass

### 9. Meeting link alert (D-08)
expected: Choosing Skype/Zoom with an expert who has no link for it shows the alert and the meeting type snaps back to In-app Voice.
result: pass

### 10. Slot taken by someone else (D-09)
expected: Take a slot from another account, then try to book it here. The alert shows, the slots reload and that slot turns dimmed.
result: pass

### 11. Passed slot (IN-09 / WR-02)
expected: Select a slot a minute or two before it starts, wait until it has started, then tap Book Now or leave and return to the app. The slot is cleared and a "Time slot unavailable" dialog says the time has passed. Tapping a different day or slot instead clears it silently, with no dialog.
result: pass

### 12. State survives rotation and "Don't keep activities"
expected: With topic, day, month, slot, meeting type and notes filled in, rotate and also background the app with "Don't keep activities" on. Everything comes back. Returning to Request Sent doesn't replay its animation.
result: pass

### 13. Booking finished in the background (WR-01 / WR-02 fix)
expected: With "Don't keep activities" on, tap Book Now and immediately leave the app; come back after a few seconds. You land on Request Sent for that booking, not on an editable form. (If the app came back to the form, you should get the "couldn't confirm your last booking request… check My Bookings" notice instead.)
result: pass

### 14. System Back while booking
expected: Pressing system Back while Book Now is spinning does nothing. When idle, Back leaves the form.
result: pass

### 15. Large font and animations off
expected: At 200% font scale both Make a Booking and Request Sent have no clipped or overlapping text. With animations off (Developer options), Request Sent shows its final state at once.
result: pass

### 16. Release build smoke (R8)
expected: A prodRelease build installs and the booking flow works as in debug. Needs the deferred Phase 2 keystore.
result: pass

## Summary

total: 16
passed: 15
issues: 0
pending: 0
skipped: 1
blocked: 0

## Gaps

[none yet]
