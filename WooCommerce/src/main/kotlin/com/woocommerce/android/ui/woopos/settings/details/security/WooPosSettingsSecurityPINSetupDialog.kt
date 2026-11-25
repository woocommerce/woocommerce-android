package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
import com.woocommerce.android.ui.woopos.common.pin.PIN_DOT_CLEAR_MILLIS
import com.woocommerce.android.ui.woopos.common.pin.PIN_DOT_SIZE
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_PHONE_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_TABLET_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.PIN_PHONE_COMPACT_BODY_HEIGHT
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDotShake
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadLayout
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPhoneLayout
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.AccessUnavailable
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Entry
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Page
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Rejected
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import kotlinx.coroutines.delay

@Composable
fun WooPosSettingsSecurityPINSetupDialog(
    state: WooPosSettingsSecurityPINSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
    isFullScreen: Boolean = currentWooPosBreakpoint() == WooPosBreakpoint.Phone,
    chooseTitle: String? = null,
    confirmTitle: String? = null,
    savedActionLabel: String = stringResource(R.string.continue_button),
) {
    val shouldDismiss = isVisible && state == AccessUnavailable
    LaunchedEffect(shouldDismiss) {
        if (shouldDismiss) onClose()
    }
    // The flow resets when the dialog hides, so the closing animation keeps showing the last visible step.
    val shown = remember(isVisible, state.takeIf { isVisible }) { state }
    val resolvedChooseTitle = chooseTitle ?: stringResource(shown.role.pinSetupChooseTitleRes)
    val resolvedConfirmTitle = confirmTitle ?: stringResource(shown.role.pinSetupConfirmTitleRes)
    val sendEvent = { event: WooPosSettingsSecurityPINSetupUIEvent -> if (isVisible) onUIEvent(event) }
    val close = { if (isVisible && shown.canClose) onClose() }
    val goBack = { sendEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) }
    if (isFullScreen) {
        BackHandler(enabled = isVisible) {
            if (shown.canGoBack) goBack() else close()
        }
        AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
            val shownSavedActionLabel = remember { savedActionLabel }
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceBright) {
                PINSetupPhoneContent(
                    shown,
                    resolvedChooseTitle,
                    resolvedConfirmTitle,
                    shownSavedActionLabel,
                    sendEvent,
                    close,
                )
            }
        }
    } else {
        WooPosDialogWrapper(
            modifier = Modifier.widthIn(
                max = DIALOG_MAX_WIDTH * LocalDensity.current.fontScale.coerceAtLeast(1f),
            ),
            widthFraction = 1f,
            isVisible = isVisible,
            dialogBackgroundContentDescription = stringResource(R.string.woopos_security_setup),
            onDismissRequest = close,
        ) {
            val shownSavedActionLabel = remember { savedActionLabel }
            BackHandler(enabled = isVisible && shown.canGoBack, onBack = goBack)
            PINSetupDialogContent(
                shown,
                resolvedChooseTitle,
                resolvedConfirmTitle,
                shownSavedActionLabel,
                sendEvent,
                close,
            )
        }
    }
}

@Composable
private fun PINSetupPhoneContent(
    state: WooPosSettingsSecurityPINSetupState,
    chooseTitle: String,
    confirmTitle: String,
    savedActionLabel: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    WooPosPINPhoneLayout(header = { PINSetupHeader(state, onUIEvent, onClose) }) {
        val bodyHeight = maxHeight
        when (state) {
            is Entry -> PINSetupPhoneEntry(state, chooseTitle, confirmTitle, onUIEvent)
            is Saving, AccessUnavailable -> PINSetupProgress()
            is Saved -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PINSetupSaved(state.role, savedActionLabel, onContinue = onClose, minHeight = bodyHeight)
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.PINSetupPhoneEntry(
    state: Entry,
    chooseTitle: String,
    confirmTitle: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
) {
    val isShort = maxHeight < PIN_PHONE_COMPACT_BODY_HEIGHT
    PINSetupEntryLayout(
        state,
        chooseTitle,
        confirmTitle,
        PIN_PAD_PHONE_KEY_SIZE,
        if (isShort) PHONE_COMPACT_STATUS_HEIGHT else PHONE_STATUS_HEIGHT,
        onUIEvent,
    )
}

@Composable
private fun PINSetupEntryLayout(
    entry: Entry,
    chooseTitle: String,
    confirmTitle: String,
    preferredKeySize: Dp,
    minimumStatusHeight: Dp,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosPINPadLayout(
        preferredKeySize = preferredKeySize,
        preferredSpacing = WooPosSpacing.Large.value,
        modifier = modifier.fillMaxWidth(),
        messageHeight = { width, spacing ->
            pinSetupTitleHeight(width, chooseTitle, confirmTitle) +
                pinSetupStatusLayout(width, minimumStatusHeight).height + spacing
        },
        message = { spacing ->
            val titleHeight = pinSetupTitleHeight(maxWidth, chooseTitle, confirmTitle)
            val statusLayout = pinSetupStatusLayout(maxWidth, minimumStatusHeight)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                Box(Modifier.heightIn(min = titleHeight), contentAlignment = Alignment.BottomCenter) {
                    PINSetupTitle(if (entry.page == Page.CONFIRM) confirmTitle else chooseTitle)
                }
                PINSetupStatus(entry, statusLayout)
            }
        },
        keypad = { keySize -> PINSetupKeypad(entry, onUIEvent, keySize) },
    )
}

@Composable
private fun PINSetupDialogContent(
    state: WooPosSettingsSecurityPINSetupState,
    chooseTitle: String,
    confirmTitle: String,
    savedActionLabel: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    // Loading and success keep the height the entry had, so the dialog does not resize between steps.
    var entryHeight by rememberSaveable { mutableFloatStateOf(0f) }
    Box(contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().then(
                if (state is Entry) Modifier else Modifier.verticalScroll(rememberScrollState()),
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PINSetupHeader(state, onUIEvent, onClose)
            when (state) {
                is Entry -> PINSetupEntryLayout(
                    entry = state,
                    chooseTitle = chooseTitle,
                    confirmTitle = confirmTitle,
                    preferredKeySize = PIN_PAD_TABLET_KEY_SIZE,
                    minimumStatusHeight = STATUS_MIN_HEIGHT,
                    onUIEvent = onUIEvent,
                    modifier = Modifier.weight(1f, fill = false).onSizeChanged {
                        entryHeight = with(density) { it.height.toDp().value }
                    },
                )
                is Saving, AccessUnavailable -> Spacer(Modifier.height(entryHeight.dp))
                is Saved -> PINSetupSaved(
                    state.role,
                    savedActionLabel,
                    onContinue = onClose,
                    minHeight = entryHeight.dp,
                )
            }
        }
        if (state is Saving || state == AccessUnavailable) PINSetupProgress()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WooPosIconSize.XLarge.value),
    ) {
        if ((state as? Entry)?.page == Page.CONFIRM) {
            WooPosBackButton(
                modifier = Modifier.align(Alignment.CenterStart),
                contentDescription = stringResource(R.string.back),
                enabled = state.canGoBack,
                onClick = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) },
            )
        }
        if (state is Entry) {
            WooPosBackButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                iconRes = R.drawable.ic_close_24dp,
                contentDescription = stringResource(R.string.close),
                enabled = state.canClose,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun PINSetupTitle(title: String, modifier: Modifier = Modifier) {
    WooPosText(
        text = title,
        style = WooPosTypography.BodyXLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
    )
}

@Composable
private fun PINSetupKeypad(
    entry: Entry,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    keySize: Dp,
) {
    WooPosPINPad(
        enteredDigits = entry.enteredDigits,
        enabled = entry.canEnterDigits,
        onDigit = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(it)) },
        onDelete = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped) },
        keySize = keySize,
    )
}

@Composable
private fun PINSetupStatus(entry: Entry, layout: PINSetupStatusLayout) {
    val error = (entry as? Choose)?.error
    var visibleError by remember(error) { mutableStateOf<Error?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
            val wait = when (error) {
                Error.MISMATCH -> MISMATCH_MESSAGE_DELAY_MILLIS
                Error.PIN_TAKEN -> PIN_DOT_CLEAR_MILLIS.toLong()
                Error.SAVE_FAILED -> 0L
            }
            delay((wait * scale).toLong())
            visibleError = error
        }
    }
    Box(Modifier.fillMaxWidth().height(layout.height)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(layout.dotsHeight),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = entry,
                modifier = Modifier.drawWithContent {
                    // The slide stays in a narrow track, with room for the stationary dots to pop and shake.
                    val padding = DOT_TRACK_PADDING.toPx()
                    clipRect(-padding, -padding, size.width + padding, size.height + padding) {
                        this@drawWithContent.drawContent()
                    }
                },
                contentKey = { it.page },
                transitionSpec = { pageTransition() },
                label = "pinSetupPage",
            ) { page ->
                PINSetupDots(page, isOutgoing = page.page != entry.page)
            }
        }
        AnimatedVisibility(
            visible = visibleError != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(MESSAGE_FADE_MILLIS)),
            exit = fadeOut(tween(MESSAGE_FADE_MILLIS)),
        ) {
            visibleError?.let {
                WooPosText(
                    text = stringResource(it.messageRes),
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Entry>.pageTransition(): ContentTransform {
    val towards = if (targetState.page == Page.CONFIRM) SlideDirection.Start else SlideDirection.End
    val spec = tween<IntOffset>(PAGE_MILLIS, easing = LinearOutSlowInEasing)
    val fade = tween<Float>(PAGE_MILLIS, easing = LinearOutSlowInEasing)
    val travel = { fullSlide: Int -> (fullSlide * PAGE_TRAVEL).toInt() }
    return (slideIntoContainer(towards, spec, travel) + fadeIn(fade)) togetherWith
        (slideOutOfContainer(towards, spec, travel) + fadeOut(fade)) using
        SizeTransform(clip = false)
}

@Composable
private fun PINSetupDots(entry: Entry, isOutgoing: Boolean) {
    val rejecting = entry is Rejected
    val shake = remember { WooPosPINDotShake() }
    LaunchedEffect(rejecting) { if (rejecting) shake.play() }
    WooPosPINDots(
        enteredDigits = entry.enteredDigits,
        isHidden = isOutgoing,
        modifier = if (rejecting) Modifier.graphicsLayer { translationX = shake.offset.toPx() } else Modifier,
    )
}

private data class PINSetupStatusLayout(val height: Dp, val dotsHeight: Dp)

@Composable
private fun pinSetupStatusLayout(width: Dp, minimumHeight: Dp): PINSetupStatusLayout {
    val messages = Error.entries.map { stringResource(it.messageRes) }
    val errorHeight = pinSetupTextHeight(width, WooPosTypography.BodySmall.style, messages)
    val messageSpace = errorHeight + WooPosSpacing.XSmall.value
    val height = maxOf(minimumHeight, PIN_DOT_SIZE + messageSpace)
    return PINSetupStatusLayout(height, height - messageSpace)
}

@Composable
private fun pinSetupTitleHeight(width: Dp, chooseTitle: String, confirmTitle: String): Dp {
    val style = WooPosTypography.BodyXLarge.style.copy(fontWeight = FontWeight.Bold)
    return pinSetupTextHeight(width, style, listOf(chooseTitle, confirmTitle))
}

@Composable
private fun pinSetupTextHeight(width: Dp, style: TextStyle, texts: List<String>): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val constraints = Constraints(maxWidth = with(density) { width.roundToPx() }.coerceAtLeast(0))
    return with(density) { texts.maxOf { measurer.measure(it, style, constraints = constraints).size.height }.toDp() }
}

private val Error.messageRes: Int
    get() = when (this) {
        Error.MISMATCH -> R.string.woopos_security_mismatch
        Error.PIN_TAKEN -> R.string.woopos_security_pin_taken
        Error.SAVE_FAILED -> R.string.woopos_security_save_failed
    }

@Composable
private fun PINSetupSaved(role: WooPosLocalRole, actionLabel: String, onContinue: () -> Unit, minHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier)
        WooPosSettingsSecurityProtectedMessage(
            role = role,
            modifier = Modifier
                .padding(vertical = WooPosSpacing.Large.value)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        WooPosButton(
            text = actionLabel,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupChoosePreview() {
    PINSetupDialogPreview(Choose(WooPosLocalRole.ADMIN))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupPartialPreview() {
    PINSetupDialogPreview(Choose(WooPosLocalRole.ADMIN, enteredDigits = 2))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupConfirmPreview() {
    PINSetupDialogPreview(Confirm(WooPosLocalRole.CASHIER))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupMismatchPreview() {
    PINSetupDialogPreview(Choose(WooPosLocalRole.ADMIN, error = Error.MISMATCH))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupFailurePreview() {
    PINSetupDialogPreview(Choose(WooPosLocalRole.ADMIN, error = Error.SAVE_FAILED))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupTakenPreview() {
    PINSetupDialogPreview(Choose(WooPosLocalRole.CASHIER, error = Error.PIN_TAKEN))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupCashierSavedPreview() {
    PINSetupDialogPreview(Saved(WooPosLocalRole.CASHIER))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupSavingPreview() {
    PINSetupDialogPreview(Saving(WooPosLocalRole.ADMIN))
}

@WooPosPreview
@Composable
fun WooPosSettingsSecurityPINSetupSavedPreview() {
    PINSetupDialogPreview(Saved(WooPosLocalRole.ADMIN))
}

@Composable
private fun PINSetupDialogPreview(state: WooPosSettingsSecurityPINSetupState) {
    WooPosTheme {
        WooPosSettingsSecurityPINSetupDialog(
            state = state,
            isVisible = true,
            onUIEvent = {},
            onClose = {},
        )
    }
}

private val WooPosLocalRole.pinSetupChooseTitleRes: Int
    get() = when (this) {
        WooPosLocalRole.ADMIN -> R.string.woopos_security_choose_pin
        WooPosLocalRole.CASHIER -> R.string.woopos_security_choose_cashier_pin
    }

private val WooPosLocalRole.pinSetupConfirmTitleRes: Int
    get() = when (this) {
        WooPosLocalRole.ADMIN -> R.string.woopos_security_confirm_pin
        WooPosLocalRole.CASHIER -> R.string.woopos_security_confirm_cashier_pin
    }

private const val PAGE_MILLIS = 300
private const val PAGE_TRAVEL = 1.27f
private const val MESSAGE_FADE_MILLIS = 120
private const val MISMATCH_MESSAGE_DELAY_MILLIS = 500L
private val DIALOG_MAX_WIDTH = 520.dp
private val STATUS_MIN_HEIGHT = 84.dp
private val PHONE_COMPACT_STATUS_HEIGHT = 28.dp
private val PHONE_STATUS_HEIGHT = 48.dp
private val DOT_TRACK_PADDING = 10.dp
