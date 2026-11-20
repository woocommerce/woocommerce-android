package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.AccessUnavailable
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Entry
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving

@Composable
fun WooPosSettingsSecurityPINSetupDialog(
    state: WooPosSettingsSecurityPINSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
    isFullScreen: Boolean = currentWooPosBreakpoint() == WooPosBreakpoint.Phone,
) {
    // The flow resets when the dialog hides, so the closing animation keeps showing the last visible step.
    val shown = remember(isVisible, state.takeIf { isVisible }) { state }
    val sendEvent = { event: WooPosSettingsSecurityPINSetupUIEvent -> if (isVisible) onUIEvent(event) }
    val close = { if (isVisible && shown.canClose) onClose() }
    val goBack = { sendEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) }
    if (isFullScreen) {
        BackHandler(enabled = isVisible) {
            if (shown.canGoBack) goBack() else close()
        }
        AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceBright) {
                PINSetupPhoneContent(state = shown, onUIEvent = sendEvent, onClose = close)
            }
        }
    } else {
        WooPosDialogWrapper(
            modifier = Modifier.widthIn(max = DIALOG_MAX_WIDTH),
            widthFraction = 1f,
            isVisible = isVisible,
            dialogBackgroundContentDescription = stringResource(R.string.woopos_security_setup),
            onDismissRequest = close,
        ) {
            BackHandler(enabled = isVisible && shown.canGoBack, onBack = goBack)
            PINSetupDialogContent(state = shown, onUIEvent = sendEvent, onClose = close)
        }
    }
}

@Composable
private fun PINSetupPhoneContent(
    state: WooPosSettingsSecurityPINSetupState,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = WooPosSpacing.XLarge.value, vertical = WooPosSpacing.Medium.value),
    ) {
        PINSetupHeader(state, onUIEvent, onClose)
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val bodyHeight = maxHeight
            val isShort = bodyHeight < PHONE_COMPACT_BODY_HEIGHT
            val spacing = if (isShort) WooPosSpacing.Medium.value else WooPosSpacing.Large.value
            val statusHeight = if (isShort) PHONE_COMPACT_STATUS_HEIGHT else STATUS_MIN_HEIGHT
            val keySpacing = WooPosSpacing.Small.value
            val keySize = ((bodyHeight - statusHeight - spacing - keySpacing * (KEYPAD_ROWS - 1)) / KEYPAD_ROWS)
                .coerceIn(PHONE_MIN_KEY_SIZE, PHONE_KEY_SIZE)
            when (state) {
                is Entry -> Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PINEntry(state, onUIEvent, keySize, statusHeight, spacing)
                }
                Saving, AccessUnavailable -> PINSetupProgress()
                Saved -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    PINSetupSaved(onContinue = onClose, minHeight = bodyHeight)
                }
            }
        }
    }
}

@Composable
private fun PINSetupDialogContent(
    state: WooPosSettingsSecurityPINSetupState,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    // Loading and success keep the height the entry had, so the dialog does not resize between steps.
    var entryHeight by rememberSaveable { mutableFloatStateOf(0f) }
    Box(contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
        ) {
            PINSetupHeader(state, onUIEvent, onClose)
            when (state) {
                is Entry -> PINEntry(
                    entry = state,
                    onUIEvent = onUIEvent,
                    keySize = KEY_SIZE,
                    statusHeight = STATUS_MIN_HEIGHT,
                    spacing = WooPosSpacing.Large.value,
                    modifier = Modifier.onSizeChanged { entryHeight = with(density) { it.height.toDp().value } },
                )
                Saving, AccessUnavailable -> Spacer(Modifier.height(entryHeight.dp))
                Saved -> PINSetupSaved(onContinue = onClose, minHeight = entryHeight.dp)
            }
        }
        if (state == Saving || state == AccessUnavailable) PINSetupProgress()
    }
}

@Composable
private fun PINSetupProgress() {
    WooPosCircularLoadingIndicator(
        Modifier
            .size(WooPosComponentSize.Medium.value)
            .progressSemantics()
    )
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
        Saving, Saved, AccessUnavailable -> null
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
        if (state is Entry || state == Saved) {
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
    entry: Entry,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    keySize: Dp,
    statusHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Box(Modifier.heightIn(min = statusHeight), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = entry,
                contentKey = { it is Confirm },
                transitionSpec = { pageTransition() },
                label = "pinSetupPage",
            ) { page ->
                PINStatus(
                    enteredDigits = page.enteredDigits,
                    error = (page as? Choose)?.error,
                    isSettled = transition.currentState == EnterExitState.Visible,
                )
            }
        }
        WooPosPINPad(
            enteredDigits = entry.enteredDigits,
            enabled = entry.canEnterDigits,
            onDigit = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(it)) },
            onDelete = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped) },
            style = WooPosPINPadStyle(
                keySize = keySize,
                keySpacing = WooPosSpacing.Small.value,
            ),
        )
    }
}

private fun AnimatedContentTransitionScope<Entry>.pageTransition(): ContentTransform {
    val towards = if (targetState is Confirm) SlideDirection.Start else SlideDirection.End
    val spec = tween<IntOffset>(PAGE_MILLIS, easing = LinearOutSlowInEasing)
    val fade = tween<Float>(PAGE_MILLIS, easing = LinearOutSlowInEasing)
    val travel = { fullSlide: Int -> (fullSlide * PAGE_TRAVEL).toInt() }
    return (slideIntoContainer(towards, spec, travel) + fadeIn(fade)) togetherWith
        (slideOutOfContainer(towards, spec, travel) + fadeOut(fade)) using
        SizeTransform(clip = true)
}

/** The error's space is reserved at once; it appears and is announced after the dots settle. */
@Composable
private fun PINStatus(enteredDigits: Int, error: Error?, isSettled: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        WooPosPINDots(enteredDigits)
        if (error != null) {
            val alpha by animateFloatAsState(
                targetValue = if (isSettled) 1f else 0f,
                animationSpec = tween(MESSAGE_FADE_MILLIS),
                label = "pinSetupError",
            )
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
                        if (isSettled) {
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        } else {
                            Modifier.clearAndSetSemantics {}
                        }
                    ),
            )
        }
    }
}

@Composable
private fun PINSetupSaved(onContinue: () -> Unit, minHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier)
        WooPosSettingsSecurityProtectedMessage(
            Modifier
                .padding(vertical = WooPosSpacing.Large.value)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
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

private const val PAGE_MILLIS = 290
private const val PAGE_TRAVEL = 1.27f
private const val MESSAGE_FADE_MILLIS = 120
private const val KEYPAD_ROWS = 4
private val DIALOG_MAX_WIDTH = 520.dp
private val STATUS_MIN_HEIGHT = 84.dp
private val KEY_SIZE = 90.dp
private val PHONE_KEY_SIZE = 72.dp
private val PHONE_MIN_KEY_SIZE = 48.dp
private val PHONE_COMPACT_BODY_HEIGHT = 380.dp
private val PHONE_COMPACT_STATUS_HEIGHT = 48.dp
