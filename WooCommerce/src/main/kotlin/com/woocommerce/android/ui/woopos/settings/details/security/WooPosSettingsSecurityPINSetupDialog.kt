package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import kotlinx.coroutines.delay

@Composable
fun WooPosSettingsSecurityPINSetupDialog(
    state: WooPosSettingsSecurityPINSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    // The flow resets when the dialog hides, so the closing animation keeps showing the last visible step.
    val shown = remember(isVisible, state.takeIf { isVisible }) { state }
    val close = { if (shown.canClose) onClose() }
    WooPosDialogWrapper(
        modifier = Modifier.widthIn(max = DIALOG_MAX_WIDTH),
        widthFraction = 1f,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(R.string.woopos_security_setup),
        onDismissRequest = close,
    ) {
        BackHandler(enabled = isVisible && shown.canGoBack) {
            onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped)
        }
        PINSetupContent(state = shown, onUIEvent = onUIEvent, onClose = close)
    }
}

@Composable
private fun PINSetupContent(
    state: WooPosSettingsSecurityPINSetupState,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    var entryHeight by remember { mutableStateOf(0.dp) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
    ) {
        PINSetupHeader(state, onUIEvent, onClose)
        Box(contentAlignment = Alignment.Center) {
            // The entry keeps the dialog's size while loading and success replace it.
            val entry = state as? Choose ?: state as? Confirm ?: Confirm(WooPosPINHash.PIN_LENGTH)
            val showsEntry = state is Choose || state is Confirm
            PINEntry(
                entry = entry,
                enabled = showsEntry,
                onUIEvent = onUIEvent,
                modifier = Modifier
                    .onSizeChanged { entryHeight = with(density) { it.height.toDp() } }
                    .alpha(if (showsEntry) 1f else 0f)
                    .then(if (showsEntry) Modifier else Modifier.clearAndSetSemantics {}),
            )
            when (state) {
                Saving -> WooPosCircularLoadingIndicator(
                    Modifier
                        .size(WooPosIconSize.XLarge.value)
                        .progressSemantics()
                )
                Saved -> PINSetupSaved(onContinue = onClose, minHeight = entryHeight)
                is Choose, is Confirm -> Unit
            }
        }
    }
}

@Composable
private fun PINSetupHeader(
    state: WooPosSettingsSecurityPINSetupState,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    val title = when (state) {
        is Choose -> stringResource(R.string.woopos_security_choose_pin)
        is Confirm -> stringResource(R.string.woopos_security_confirm_pin)
        Saving, Saved -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WooPosIconSize.XLarge.value),
    ) {
        if (state is Confirm) {
            WooPosBackButton(
                modifier = Modifier.align(Alignment.CenterStart),
                contentDescription = stringResource(R.string.back),
                onClick = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) },
            )
        }
        if (title != null) {
            WooPosText(
                text = title,
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = WooPosIconSize.XLarge.value)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
        if (state != Saving) {
            WooPosBackButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                iconRes = R.drawable.ic_close_24dp,
                contentDescription = stringResource(R.string.close),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun PINEntry(
    entry: WooPosSettingsSecurityPINSetupState,
    enabled: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enteredDigits = (entry as? Choose)?.enteredDigits ?: (entry as? Confirm)?.enteredDigits ?: 0
    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
    ) {
        Box(Modifier.heightIn(min = STATUS_MIN_HEIGHT), contentAlignment = Alignment.Center) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            AnimatedContent(
                targetState = entry,
                contentKey = { it is Confirm },
                transitionSpec = { pageTransition(isRtl) },
                label = "pinSetupPage",
            ) { page ->
                PINStatus(
                    enteredDigits = (page as? Choose)?.enteredDigits ?: (page as? Confirm)?.enteredDigits ?: 0,
                    error = (page as? Choose)?.error,
                )
            }
        }
        WooPosPINPad(
            enteredDigits = enteredDigits,
            enabled = enabled && entry.canEnterDigits,
            onDigit = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(it)) },
            onDelete = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped) },
            style = WooPosPINPadStyle(
                keySize = if (isPhone) PHONE_KEY_SIZE else KEY_SIZE,
                keySpacing = WooPosSpacing.Small.value,
            ),
        )
    }
}

/** Confirmation pushes the dots in from the trailing edge; returning to Choose pushes them back. */
private fun AnimatedContentTransitionScope<WooPosSettingsSecurityPINSetupState>.pageTransition(
    isRtl: Boolean,
): ContentTransform {
    val direction = (if (targetState is Confirm) 1 else -1) * (if (isRtl) -1 else 1)
    val spec = tween<IntOffset>(PAGE_MILLIS, easing = FastOutSlowInEasing)
    val fade = tween<Float>(PAGE_MILLIS, easing = FastOutSlowInEasing)
    return (slideInHorizontally(spec) { it * direction } + fadeIn(fade)) togetherWith
        (slideOutHorizontally(spec) { -it * direction } + fadeOut(fade)) using
        SizeTransform(clip = true)
}

@Composable
private fun PINStatus(enteredDigits: Int, error: Error?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        WooPosPINDots(enteredDigits)
        if (error != null) PINError(error)
    }
}

/** Reserves the message's space at once but reveals and announces it after the dots slide back into place. */
@Composable
private fun PINError(error: Error) {
    val isPreview = LocalInspectionMode.current
    var isShown by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) {
        delay((PAGE_MILLIS * (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f)).toLong())
        isShown = true
    }
    val alpha by animateFloatAsState(if (isShown) 1f else 0f, tween(MESSAGE_FADE_MILLIS), label = "pinSetupError")
    WooPosText(
        text = stringResource(
            when (error) {
                Error.MISMATCH -> R.string.woopos_security_mismatch
                Error.SAVE_FAILED -> R.string.woopos_security_save_failed
            }
        ),
        style = WooPosTypography.BodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (isShown) {
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                } else {
                    Modifier.clearAndSetSemantics {}
                }
            ),
    )
}

@Composable
private fun PINSetupSaved(onContinue: () -> Unit, minHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        // The empty first item centers the message above the bottom-aligned button.
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier)
        WooPosSettingsSecurityProtectedMessage(Modifier.padding(vertical = WooPosSpacing.Large.value))
        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Confirms that the Admin PIN protects POS; Security reuses it until PIN management is available. */
@Composable
fun WooPosSettingsSecurityProtectedMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        Image(
            painter = painterResource(R.drawable.woopos_check_circle),
            contentDescription = null,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value),
        )
        WooPosText(
            text = stringResource(R.string.woopos_security_success),
            style = WooPosTypography.BodyXLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        WooPosText(
            text = stringResource(R.string.woopos_security_success_message),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MESSAGE_MAX_WIDTH),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupChoosePreview() {
    PINSetupDialogPreview(Choose())
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupPartialPreview() {
    PINSetupDialogPreview(Choose(enteredDigits = 2))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupConfirmPreview() {
    PINSetupDialogPreview(Confirm())
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupMismatchPreview() {
    PINSetupDialogPreview(Choose(error = Error.MISMATCH))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupFailurePreview() {
    PINSetupDialogPreview(Choose(error = Error.SAVE_FAILED))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupSavingPreview() {
    PINSetupDialogPreview(Saving)
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupSavedPreview() {
    PINSetupDialogPreview(Saved)
}

@Composable
private fun PINSetupDialogPreview(state: WooPosSettingsSecurityPINSetupState) {
    WooPosTheme {
        WooPosSettingsSecurityPINSetupDialog(state = state, isVisible = true, onUIEvent = {}, onClose = {})
    }
}

private const val PAGE_MILLIS = 240
private const val MESSAGE_FADE_MILLIS = 120
private val DIALOG_MAX_WIDTH = 520.dp
private val MESSAGE_MAX_WIDTH = 470.dp
private val STATUS_MIN_HEIGHT = 56.dp
private val KEY_SIZE = 90.dp
private val PHONE_KEY_SIZE = 72.dp
