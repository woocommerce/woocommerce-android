package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import com.woocommerce.android.ui.woopos.common.pin.PIN_DOT_CLEAR_MILLIS
import com.woocommerce.android.ui.woopos.common.pin.PIN_DOT_SIZE
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
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
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WooPosSettingsSecurityPINSetupDialog(
    state: WooPosSettingsSecurityPINSetupState,
    isVisible: Boolean,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
    isFullScreen: Boolean = currentWooPosBreakpoint() == WooPosBreakpoint.Phone,
    chooseTitle: String = stringResource(R.string.woopos_security_choose_pin),
    confirmTitle: String = stringResource(R.string.woopos_security_confirm_pin),
) {
    val shouldDismiss = isVisible && state == AccessUnavailable
    LaunchedEffect(shouldDismiss) {
        if (shouldDismiss) onClose()
    }
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
                PINSetupPhoneContent(shown, chooseTitle, confirmTitle, sendEvent, close)
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
            PINSetupDialogContent(shown, chooseTitle, confirmTitle, sendEvent, close)
        }
    }
}

@Composable
private fun PINSetupPhoneContent(
    state: WooPosSettingsSecurityPINSetupState,
    chooseTitle: String,
    confirmTitle: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val verticalPadding = if (maxHeight < PHONE_COMPACT_BODY_HEIGHT) {
            WooPosSpacing.None.value
        } else {
            WooPosSpacing.Medium.value
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WooPosSpacing.XLarge.value, vertical = verticalPadding),
        ) {
            PINSetupHeader(state, onUIEvent, onClose)
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val bodyHeight = maxHeight
                when (state) {
                    is Entry -> PINSetupPhoneEntry(state, chooseTitle, confirmTitle, onUIEvent)
                    Saving, AccessUnavailable -> PINSetupProgress()
                    Saved -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        PINSetupSaved(onContinue = onClose, minHeight = bodyHeight)
                    }
                }
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
    val titleHeight = pinSetupTitleHeight(maxWidth, chooseTitle, confirmTitle)
    val isShort = maxHeight < PHONE_COMPACT_BODY_HEIGHT
    val spacing = if (isShort) WooPosSpacing.Small.value else WooPosSpacing.Large.value
    val minStatusHeight = if (isShort) PHONE_COMPACT_STATUS_HEIGHT else PHONE_STATUS_HEIGHT
    val statusLayout = pinSetupStatusLayout(maxWidth, minStatusHeight)
    val keySpacing = WooPosSpacing.Small.value
    val keypadHeight = maxHeight - titleHeight - statusLayout.height - spacing * 2
    val keySize = ((keypadHeight - keySpacing * (KEYPAD_ROWS - 1)) / KEYPAD_ROWS)
        .coerceIn(PHONE_MIN_KEY_SIZE, PHONE_KEY_SIZE)
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Box(Modifier.heightIn(min = titleHeight), contentAlignment = Alignment.BottomCenter) {
            PINSetupTitle(if (state.page == Page.CONFIRM) confirmTitle else chooseTitle)
        }
        PINEntry(state, onUIEvent, keySize, statusLayout, spacing)
    }
}

@Composable
private fun PINSetupDialogContent(
    state: WooPosSettingsSecurityPINSetupState,
    chooseTitle: String,
    confirmTitle: String,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    // Loading and success keep the height the entry had, so the dialog does not resize between steps.
    var entryHeight by rememberSaveable { mutableFloatStateOf(0f) }
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val titleWidth = maxWidth - WooPosIconSize.XLarge.value * 2
        val titleHeight = pinSetupTitleHeight(titleWidth, chooseTitle, confirmTitle)
        val statusLayout = pinSetupStatusLayout(maxWidth, STATUS_MIN_HEIGHT)
        val title = when ((state as? Entry)?.page) {
            Page.CHOOSE -> chooseTitle
            Page.CONFIRM -> confirmTitle
            null -> null
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
        ) {
            PINSetupHeader(state, onUIEvent, onClose, title, titleHeight)
            when (state) {
                is Entry -> PINEntry(
                    entry = state,
                    onUIEvent = onUIEvent,
                    keySize = KEY_SIZE,
                    statusLayout = statusLayout,
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
    title: String? = null,
    minTitleHeight: Dp = 0.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(WooPosIconSize.XLarge.value, minTitleHeight)),
    ) {
        if ((state as? Entry)?.page == Page.CONFIRM) {
            WooPosBackButton(
                modifier = Modifier.align(Alignment.CenterStart),
                contentDescription = stringResource(R.string.back),
                onClick = { onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped) },
            )
        }
        if (title != null) {
            PINSetupTitle(
                title = title,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = WooPosIconSize.XLarge.value),
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
private fun PINEntry(
    entry: Entry,
    onUIEvent: (WooPosSettingsSecurityPINSetupUIEvent) -> Unit,
    keySize: Dp,
    statusLayout: PINSetupStatusLayout,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        PINSetupStatus(entry, statusLayout)
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
    val shake = remember { Animatable(0f) }
    LaunchedEffect(rejecting) {
        shake.snapTo(0f)
        if (rejecting) {
            shake.animateTo(1f, tween(REJECTION_SHAKE_MILLIS, easing = LinearEasing))
        }
    }
    WooPosPINDots(
        enteredDigits = entry.enteredDigits,
        isHidden = isOutgoing,
        modifier = Modifier.graphicsLayer {
            if (rejecting) {
                translationX = sin(shake.value * PI.toFloat() * SHAKE_COUNT) * SHAKE_TRAVEL.toPx()
            }
        }
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

private const val PAGE_MILLIS = 300
private const val PAGE_TRAVEL = 1.27f
private const val MESSAGE_FADE_MILLIS = 120
private const val MISMATCH_MESSAGE_DELAY_MILLIS = 500L
private const val REJECTION_SHAKE_MILLIS = 400
private const val SHAKE_COUNT = 3
private const val KEYPAD_ROWS = 4
private val DIALOG_MAX_WIDTH = 520.dp
private val STATUS_MIN_HEIGHT = 84.dp
private val KEY_SIZE = 90.dp
private val PHONE_KEY_SIZE = 72.dp
private val PHONE_MIN_KEY_SIZE = 48.dp
private val PHONE_COMPACT_BODY_HEIGHT = 380.dp
private val PHONE_COMPACT_STATUS_HEIGHT = 28.dp
private val PHONE_STATUS_HEIGHT = 48.dp
private val SHAKE_TRAVEL = 8.dp
