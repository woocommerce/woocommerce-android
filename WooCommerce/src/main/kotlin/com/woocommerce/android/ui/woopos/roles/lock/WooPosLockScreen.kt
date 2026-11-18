package com.woocommerce.android.ui.woopos.roles.lock

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINDots
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WooPosLockScreenDialog(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
) {
    if (state.isLocked) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                // The opaque lock covers POS from its first frame instead of fading in over dimmed content.
                window?.setWindowAnimations(0)
                window?.setDimAmount(0f)
            }
            WooPosLockScreen(state = state, onUIEvent = onUIEvent)
        }
    }
}

@Composable
fun WooPosLockScreen(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright)
            .safeDrawingPadding()
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            LockContent(state, onUIEvent)
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.LockContent(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
) {
    val isCompactHeight = maxHeight < COMPACT_HEIGHT && maxWidth > maxHeight
    val metrics = lockMetrics(isCompactHeight)
    val layout = calculateLockLayout(metrics, isCompactHeight)
    val intro = rememberLockIntro(animates = layout.showsIllustration)
    val feedback = rememberLockPINFeedback(state)
    val contentModifier = Modifier.graphicsLayer {
        alpha = intro.contentAlpha
        translationY = intro.travel().y
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { intro.animation.viewport = it }
            .then(if (intro.isActive) Modifier.clearAndSetSemantics {} else Modifier)
            .verticalScroll(rememberScrollState(), enabled = !intro.isActive)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = this@LockContent.maxHeight)
                .padding(horizontal = metrics.horizontalPadding, vertical = layout.verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing, Alignment.CenterVertically),
        ) {
            Column(Modifier.widthIn(max = CONTENT_MAX_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
                if (layout.showsIllustration) {
                    Box(
                        Modifier
                            .size(metrics.illustrationSize)
                            .onGloballyPositioned { intro.animation.slot = it },
                        contentAlignment = Alignment.Center,
                    ) {
                        LockIllustration(
                            shackleAngle = { intro.shackleAngle },
                            modifier = Modifier.graphicsLayer {
                                alpha = intro.illustrationAlpha
                                val travel = intro.travel()
                                translationX = travel.x
                                translationY = travel.y
                            },
                        )
                    }
                    Spacer(Modifier.height(metrics.sectionSpacing))
                }
                WooPosText(
                    text = stringResource(R.string.woopos_lock_enter_pin),
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .semantics { heading() }
                        .then(contentModifier),
                )
            }
            Column(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH)
                    .then(contentModifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
            ) {
                LockStatus(state, feedback, statusHeight = layout.statusHeight)
                // TODO WOOMOB-4606: Show Forgot PIN once PIN recovery is available.
                WooPosPINPad(
                    enteredDigits = state.enteredDigits,
                    enabled = state.canEnterPIN && !intro.isActive,
                    onDigit = { onUIEvent(WooPosLockScreenUIEvent.DigitTapped(it)) },
                    onDelete = { onUIEvent(WooPosLockScreenUIEvent.DeleteTapped) },
                    style = WooPosPINPadStyle(
                        keySize = layout.keySize,
                        keySpacing = metrics.keySpacing,
                        showsDisabledKeys = state.isLocked && when (state.status) {
                            is Status.Wait, Status.StorageUnavailable -> true
                            else -> false
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun lockMetrics(isCompactHeight: Boolean) = WooPosLockScreenLayout.Metrics(
    horizontalPadding = WooPosSpacing.XXLarge.value,
    verticalPadding = if (isCompactHeight) WooPosSpacing.Small.value else WooPosSpacing.XXLarge.value,
    reducedVerticalPadding = if (isCompactHeight) WooPosSpacing.Small.value else WooPosSpacing.Medium.value,
    sectionSpacing = if (isCompactHeight) WooPosSpacing.Medium.value else WooPosSpacing.XLarge.value,
    keySpacing = WooPosSpacing.Small.value,
    illustrationSize = ILLUSTRATION_SIZE,
)

@Composable
private fun BoxWithConstraintsScope.calculateLockLayout(
    metrics: WooPosLockScreenLayout.Metrics,
    isCompactHeight: Boolean,
): WooPosLockScreenLayout {
    val density = LocalDensity.current
    val title = stringResource(R.string.woopos_lock_enter_pin)
    val titleStyle = WooPosTypography.BodyXLarge.style.copy(fontWeight = FontWeight.Bold)
    val statusStyle = WooPosTypography.BodySmall.style
    val statusMessages = listOf(
        stringResource(R.string.woopos_lock_wait, LONGEST_WAIT_MINUTES, LONGEST_WAIT_SECONDS),
        stringResource(R.string.woopos_lock_incorrect),
        stringResource(R.string.woopos_lock_unavailable),
    )
    val textMeasurer = rememberTextMeasurer()
    val contentWidth = maxWidth - metrics.horizontalPadding * 2
    val textWidth = min(CONTENT_MAX_WIDTH, contentWidth)
    val textConstraints = Constraints(maxWidth = with(density) { textWidth.roundToPx() }.coerceAtLeast(0))
    fun measure(text: String, style: TextStyle) =
        with(density) { textMeasurer.measure(text, style, constraints = textConstraints).size.height.toDp() }
    val titleHeight = measure(title, titleStyle)
    // Reserves the longest status message so the keys keep their size when it replaces the dots.
    val statusHeight = statusMessages.maxOf { measure(it, statusStyle) }.coerceAtLeast(STATUS_ROW_HEIGHT)
    val preferredKeySize = if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) PHONE_KEY_SIZE else KEY_SIZE
    return WooPosLockScreenLayout.calculate(
        viewport = DpSize(maxWidth, maxHeight),
        preferredKeySize = preferredKeySize,
        titleHeight = titleHeight,
        statusHeight = statusHeight,
        isCompactHeight = isCompactHeight,
        metrics = metrics,
    )
}

@Stable
private class WooPosLockIntroAnimation {
    val shackle = Animatable(LOCK_OPEN_ANGLE)
    val reveal = Animatable(0f)
    var viewport: LayoutCoordinates? by mutableStateOf(null)
    var slot: LayoutCoordinates? by mutableStateOf(null)
}

/** Plays the lock intro over the opaque lock background; protection and enforcement never wait for it. */
private class WooPosLockIntro(val isActive: Boolean, val animation: WooPosLockIntroAnimation) {
    val shackleAngle: Float get() = if (isActive) animation.shackle.value else 0f
    val contentAlpha: Float get() = if (isActive) animation.reveal.value else 1f
    val illustrationAlpha: Float get() = if (isActive && slotTravel() == null) 0f else 1f

    /** Moves the lock from the screen center to its slot; the title and keypad rise by the same distance. */
    fun travel(): Offset = if (isActive) (slotTravel() ?: Offset.Zero) * (1f - animation.reveal.value) else Offset.Zero

    private fun slotTravel(): Offset? {
        val viewport = animation.viewport?.takeIf { it.isAttached } ?: return null
        val slot = animation.slot?.takeIf { it.isAttached } ?: return null
        val viewportCenter = Offset(viewport.size.width / 2f, viewport.size.height / 2f)
        return viewportCenter - viewport.localPositionOf(slot, Offset(slot.size.width / 2f, slot.size.height / 2f))
    }
}

private enum class WooPosLockIntroProgress { PENDING, RUNNING, DONE }

@Composable
private fun rememberLockIntro(animates: Boolean): WooPosLockIntro {
    val canAnimate = animates && !LocalInspectionMode.current && ValueAnimator.areAnimatorsEnabled()
    var progress by rememberSaveable {
        mutableStateOf(if (canAnimate) WooPosLockIntroProgress.PENDING else WooPosLockIntroProgress.DONE)
    }
    // A recreated screen, for example after rotation, does not replay an interrupted intro.
    val wasInterrupted = remember { progress == WooPosLockIntroProgress.RUNNING }
    val isActive = canAnimate && !wasInterrupted && progress != WooPosLockIntroProgress.DONE
    SideEffect { if (!isActive) progress = WooPosLockIntroProgress.DONE }
    val animation = remember { WooPosLockIntroAnimation() }
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) {
            progress = WooPosLockIntroProgress.DONE
            return@LaunchedEffect
        }
        progress = WooPosLockIntroProgress.RUNNING
        animation.shackle.animateTo(0f, tween(INTRO_CLOSE_MILLIS, delayMillis = INTRO_HOLD_MILLIS))
        animation.reveal.animateTo(1f, tween(INTRO_REVEAL_MILLIS, easing = FastOutSlowInEasing))
        progress = WooPosLockIntroProgress.DONE
    }
    return remember(isActive) { WooPosLockIntro(isActive, animation) }
}

@Composable
private fun LockIllustration(shackleAngle: () -> Float, modifier: Modifier = Modifier) {
    Box(modifier.size(LOCK_ARTWORK_WIDTH, LOCK_ARTWORK_HEIGHT)) {
        // The shackle turns around its right hinge behind the fixed lock body.
        Image(
            painter = painterResource(R.drawable.woopos_lock_shackle),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = shackleAngle()
                    transformOrigin = LOCK_HINGE
                },
        )
        Image(
            painter = painterResource(R.drawable.woopos_lock_body),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Keeps a rejected PIN's dots in place while they shake, then lets the error replace them. */
@Stable
private class WooPosLockPINFeedback {
    var heldDigits by mutableIntStateOf(0)
    val shake = Animatable(0f)

    val shakeOffset: Float
        get() = if (heldDigits > 0) sin(shake.value * PI.toFloat() * SHAKE_COUNT) else 0f
}

@Composable
private fun rememberLockPINFeedback(state: WooPosLockScreenState): WooPosLockPINFeedback {
    val feedback = remember { WooPosLockPINFeedback() }
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(feedback) {
        var previous = currentState
        var shakeJob: Job? = null
        snapshotFlow { currentState }.collect { current ->
            val rejected = (current.status == Status.Incorrect || current.status is Status.Wait) &&
                current.enteredDigits == 0 && previous.enteredDigits > 0
            if (rejected) {
                shakeJob?.cancel()
                shakeJob = launch {
                    try {
                        feedback.heldDigits = WooPosPINHash.PIN_LENGTH
                        feedback.shake.snapTo(0f)
                        feedback.shake.animateTo(1f, tween(SHAKE_MILLIS, easing = LinearEasing))
                    } finally {
                        feedback.heldDigits = 0
                    }
                }
            } else if (current.enteredDigits > 0 || !current.isLocked) {
                shakeJob?.cancel()
            }
            previous = current
        }
    }
    return feedback
}

private enum class WooPosLockMessage { INCORRECT, WAIT, UNAVAILABLE }

@Composable
private fun LockStatus(state: WooPosLockScreenState, feedback: WooPosLockPINFeedback, statusHeight: Dp) {
    val message = if (feedback.heldDigits > 0) {
        null
    } else {
        when (state.status) {
            Status.Idle, Status.Checking -> null
            Status.Incorrect -> if (state.enteredDigits == 0) WooPosLockMessage.INCORRECT else null
            is Status.Wait -> WooPosLockMessage.WAIT
            Status.StorageUnavailable -> WooPosLockMessage.UNAVAILABLE
        }
    }
    val feedbackSpec = tween<Float>(FEEDBACK_MILLIS, easing = LinearOutSlowInEasing)
    val dotsAlpha by animateFloatAsState(if (message == null) 1f else 0f, feedbackSpec, label = "lockDots")
    Box(Modifier.heightIn(min = statusHeight), contentAlignment = Alignment.Center) {
        // The dots stay composed under messages so the next digit still pops in.
        WooPosPINDots(
            enteredDigits = feedback.heldDigits.takeIf { it > 0 } ?: state.enteredDigits,
            isHidden = message != null,
            modifier = Modifier.graphicsLayer {
                alpha = dotsAlpha
                translationX = feedback.shakeOffset * SHAKE_TRAVEL.toPx()
            },
        )
        Crossfade(targetState = message, animationSpec = feedbackSpec, label = "lockStatus") { shown ->
            if (shown != null) LockMessageText(shown, (state.status as? Status.Wait)?.remainingSeconds ?: 0L)
        }
    }
}

@Composable
private fun LockMessageText(message: WooPosLockMessage, remainingSeconds: Long) {
    val locale = LocalConfiguration.current.locales[0]
    val text = when (message) {
        WooPosLockMessage.INCORRECT -> AnnotatedString(stringResource(R.string.woopos_lock_incorrect))
        WooPosLockMessage.UNAVAILABLE -> AnnotatedString(stringResource(R.string.woopos_lock_unavailable))
        WooPosLockMessage.WAIT -> {
            val minutes = remainingSeconds / SECONDS_PER_MINUTE
            val seconds = remainingSeconds % SECONDS_PER_MINUTE
            val full = stringResource(R.string.woopos_lock_wait, minutes, seconds)
            val duration = String.format(locale, "%d:%02d", minutes, seconds)
            buildAnnotatedString {
                append(full)
                val start = full.indexOf(duration)
                if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, start + duration.length)
            }
        }
    }
    WooPosText(
        text = text,
        style = WooPosTypography.BodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(max = CONTENT_MAX_WIDTH)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@WooPosPreview
@Composable
fun WooPosLockScreenPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true, enteredDigits = 2), onUIEvent = {})
    }
}

@WooPosPreview
@Composable
fun WooPosLockScreenIncorrectPreview() {
    WooPosTheme {
        WooPosLockScreen(
            state = WooPosLockScreenState(isLocked = true, status = Status.Incorrect),
            onUIEvent = {},
        )
    }
}

@WooPosPreview
@Composable
fun WooPosLockScreenWaitPreview() {
    WooPosTheme {
        WooPosLockScreen(state = WooPosLockScreenState(isLocked = true, status = Status.Wait(30)), onUIEvent = {})
    }
}

@WooPosPreview
@Composable
fun WooPosLockScreenUnavailablePreview() {
    WooPosTheme {
        WooPosLockScreen(
            state = WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable),
            onUIEvent = {},
        )
    }
}

private const val INTRO_HOLD_MILLIS = 400
private const val INTRO_CLOSE_MILLIS = 200
private const val INTRO_REVEAL_MILLIS = 280
private const val LOCK_OPEN_ANGLE = 30f
private const val SHAKE_MILLIS = 400
private const val SHAKE_COUNT = 3
private const val FEEDBACK_MILLIS = 120
private const val LONGEST_WAIT_MINUTES = 59L
private const val LONGEST_WAIT_SECONDS = 59L
private const val SECONDS_PER_MINUTE = 60

private val LOCK_ARTWORK_WIDTH = 60.456.dp
private val LOCK_ARTWORK_HEIGHT = 80.dp
private val LOCK_HINGE = TransformOrigin(pivotFractionX = 45.7003f / 60.456f, pivotFractionY = 27.785f / 80f)
private val ILLUSTRATION_SIZE = 88.dp
private val KEY_SIZE = 90.dp
private val PHONE_KEY_SIZE = 72.dp
private val CONTENT_MAX_WIDTH = 420.dp
private val COMPACT_HEIGHT = 480.dp
private val STATUS_ROW_HEIGHT = 24.dp
private val SHAKE_TRAVEL = 8.dp
