# Low Battery Warning in Floating Toolbar

**Issue:** [WOOMOB-2093](https://linear.app/a8c/issue/WOOMOB-2093/woopos-low-battery-succeed-connect-hasnt-shown-any-warnings)

**Problem:** When a card reader connects successfully with low battery, merchants receive no warning. They only discover the issue when the battery becomes critical and operations fail.

**Solution:** Display a battery warning icon in the floating toolbar's card reader status button when battery is LOW or CRITICAL.

---

## Design Decisions

| Decision | Choice |
|----------|--------|
| Location | Floating toolbar, inside card reader status button |
| Visual | Battery icon next to the green "connected" dot |
| Threshold | Show for both LOW and CRITICAL states |
| Colors | Orange for LOW, red for CRITICAL |
| Interaction | No change - tapping still disconnects, icon is informational |

---

## State Model

**`WooPosHomeFloatingToolbarState.kt`**

Add `BatteryState` enum and include it in `Connected` status:

```kotlin
sealed class WooPosCardReaderStatus(@StringRes val title: Int) {
    data object NotConnected : WooPosCardReaderStatus(title = R.string.woopos_reader_disconnected)
    data class Connected(
        val batteryState: BatteryState = BatteryState.NOMINAL
    ) : WooPosCardReaderStatus(title = R.string.woopos_reader_connected)
    data object Reconnecting : WooPosCardReaderStatus(title = R.string.woopos_reader_reconnecting)
}

enum class BatteryState {
    NOMINAL,
    LOW,
    CRITICAL
}
```

---

## ViewModel Changes

**`WooPosHomeFloatingToolbarViewModel.kt`**

Combine `readerStatus` and `batteryStatus` flows:

```kotlin
init {
    viewModelScope.launch {
        combine(
            cardReaderFacade.readerStatus,
            cardReaderFacade.batteryStatus
        ) { readerStatus, batteryStatus ->
            mapToUiState(readerStatus, batteryStatus)
        }.collect { cardReaderStatus ->
            _state.value = _state.value.copy(cardReaderStatus = cardReaderStatus)
        }
    }
}

private fun mapToUiState(
    status: CardReaderStatus,
    batteryStatus: CardReaderBatteryStatus
): WooPosCardReaderStatus {
    return when (status) {
        is Connected -> WooPosCardReaderStatus.Connected(
            batteryState = mapBatteryState(batteryStatus)
        )
        is NotConnected, Connecting -> WooPosCardReaderStatus.NotConnected
        Reconnecting -> WooPosCardReaderStatus.Reconnecting
    }
}

private fun mapBatteryState(status: CardReaderBatteryStatus): BatteryState {
    return when (status) {
        is CardReaderBatteryStatus.StatusChanged -> when (status.batteryStatus) {
            BatteryStatus.CRITICAL -> BatteryState.CRITICAL
            BatteryStatus.LOW -> BatteryState.LOW
            BatteryStatus.NOMINAL, BatteryStatus.UNKNOWN -> BatteryState.NOMINAL
        }
        CardReaderBatteryStatus.Warning -> BatteryState.LOW
        CardReaderBatteryStatus.Unknown -> BatteryState.NOMINAL
    }
}
```

---

## UI Changes

**`WooPosHomeFloatingToolbar.kt`**

Add battery icon in the `CardReaderStatusButton`:

```kotlin
@Composable
private fun CardReaderStatusButton(
    modifier: Modifier,
    state: WooPosCardReaderStatus,
    menuCardDisabled: Boolean,
    onClick: () -> Unit
) {
    // ... existing code ...

    TextButton(...) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        Circle(size = 14.dp, color = illustrationColor)

        if (state is WooPosCardReaderStatus.Connected) {
            BatteryWarningIcon(batteryState = state.batteryState)
        }

        Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
        ReaderStatusText(...)
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    }
}

@Composable
private fun BatteryWarningIcon(batteryState: BatteryState) {
    when (batteryState) {
        BatteryState.NOMINAL -> { }
        BatteryState.LOW -> {
            Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_woo_pos_battery_low),
                contentDescription = stringResource(R.string.woopos_battery_low),
                tint = WooPosTheme.colors.warning,
                modifier = Modifier.size(16.dp)
            )
        }
        BatteryState.CRITICAL -> {
            Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_woo_pos_battery_critical),
                contentDescription = stringResource(R.string.woopos_battery_critical),
                tint = WooPosTheme.colors.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
```

---

## Resources

**Drawables:**
- `ic_woo_pos_battery_low.xml` — Battery outline ~25% filled
- `ic_woo_pos_battery_critical.xml` — Battery outline nearly empty

**Strings:**
```xml
<string name="woopos_battery_low">Card reader battery low</string>
<string name="woopos_battery_critical">Card reader battery critical</string>
```

---

## Files to Modify

1. `WooPosHomeFloatingToolbarState.kt` — Add `BatteryState` enum, modify `Connected`
2. `WooPosHomeFloatingToolbarViewModel.kt` — Combine flows, add mapping functions
3. `WooPosHomeFloatingToolbar.kt` — Add `BatteryWarningIcon` composable
4. `res/drawable/ic_woo_pos_battery_low.xml` — New icon
5. `res/drawable/ic_woo_pos_battery_critical.xml` — New icon
6. `res/values/strings.xml` — Add accessibility strings

---

## Testing

- Unit test `WooPosHomeFloatingToolbarViewModel` for battery state mapping
- Verify icon appears only when connected AND battery is LOW/CRITICAL
- Verify correct colors (orange for LOW, red for CRITICAL)
- Verify accessibility labels are announced