# Attendance Status Change - Design

## Summary

Update the attendance section UI to match Figma and wire the toggle to the existing network API with optimistic updates.

## UI Changes (BookingAttendanceSection composable)

Current: card-wrapped, vertical layout, large 80dp toggle buttons.
Target (Figma): no card, single row with title left + small pill toggles right, hint text below.

- Remove WooPosCard wrapper
- Row layout: "Attendance status" title (BodyLarge, bold) on left, two small pill buttons on right
- Small toggle buttons (~40dp height, BodySmall text):
  - Selected: inverseSurface background, inverseOnSurface text
  - Unselected: outlined border, transparent background, onSurface text
- Hint text below the row

New component: WooPosToggleButtonSmall in WooPosButtons.kt (same pattern as WooPosToggleButton but 40dp/BodySmall).

## ViewModel Logic

- Inject BookingsRepository into WooPosBookingsViewModel
- Handle AttendanceToggled event:
  1. Map boolean to AttendanceStatus (true -> Attended, false -> Unattended)
  2. Optimistically update the state (attendanceSection.selection + attendanceBadge + list item badge)
  3. Call BookingsRepository.updateAttendanceStatus() in background
  4. On failure: revert state to previous value

## No Changes Needed

- State models (AttendanceSection already has selection: AttendanceState?)
- Network layer (BookingsRepository.updateAttendanceStatus already exists)
- Section ordering (keep current order as-is)
