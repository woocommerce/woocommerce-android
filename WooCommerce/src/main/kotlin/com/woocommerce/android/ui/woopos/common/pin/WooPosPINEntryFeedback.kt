package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
private class WooPosPINFeedback {
    var heldDigits by mutableIntStateOf(0)
    val shake = WooPosPINDotShake()
}

@Composable
private fun rememberPINFeedback(status: Status, enteredDigits: Int): WooPosPINFeedback {
    val feedback = remember { WooPosPINFeedback() }
    val currentState by rememberUpdatedState(status to enteredDigits)
    LaunchedEffect(feedback) {
        var previousDigits = currentState.second
        var shakeJob: Job? = null
        snapshotFlow { currentState }.collect { (currentStatus, digits) ->
            val rejected = (currentStatus == Status.Incorrect || currentStatus is Status.Wait) &&
                digits == 0 && previousDigits > 0
            if (rejected) {
                shakeJob?.cancel()
                shakeJob = launch {
                    try {
                        feedback.heldDigits = WooPosPINHash.PIN_LENGTH
                        feedback.shake.play()
                    } finally {
                        feedback.heldDigits = 0
                    }
                }
            } else if (digits > 0) {
                shakeJob?.cancel()
            }
            previousDigits = digits
        }
    }
    return feedback
}

private enum class WooPosPINMessage { INCORRECT, WAIT, UNAVAILABLE }

@Composable
internal fun WooPosPINEntryFeedback(status: Status, enteredDigits: Int, statusHeight: Dp) {
    val feedback = rememberPINFeedback(status, enteredDigits)
    val message = if (feedback.heldDigits > 0) {
        null
    } else {
        when (status) {
            Status.Initializing, Status.Idle, Status.Checking -> null
            Status.Incorrect -> if (enteredDigits == 0) WooPosPINMessage.INCORRECT else null
            is Status.Wait -> WooPosPINMessage.WAIT
            Status.StorageUnavailable -> WooPosPINMessage.UNAVAILABLE
        }
    }
    val feedbackSpec = tween<Float>(FEEDBACK_MILLIS, easing = LinearOutSlowInEasing)
    val dotsAlpha by animateFloatAsState(if (message == null) 1f else 0f, feedbackSpec, label = "pinDots")
    Box(Modifier.heightIn(min = statusHeight), contentAlignment = Alignment.Center) {
        // The dots stay composed under messages so the next digit still pops in.
        WooPosPINDots(
            enteredDigits = feedback.heldDigits.takeIf { it > 0 } ?: enteredDigits,
            isHidden = message != null,
            modifier = Modifier.graphicsLayer {
                alpha = dotsAlpha
                translationX = if (feedback.heldDigits > 0) feedback.shake.offset.toPx() else 0f
            },
        )
        Crossfade(targetState = message, animationSpec = feedbackSpec, label = "pinStatus") { shown ->
            if (shown != null) PINMessageText(shown, (status as? Status.Wait)?.remainingSeconds ?: 0L)
        }
    }
}

@Composable
private fun PINMessageText(message: WooPosPINMessage, remainingSeconds: Long) {
    val locale = LocalConfiguration.current.locales[0]
    val text = when (message) {
        WooPosPINMessage.INCORRECT -> AnnotatedString(stringResource(R.string.woopos_lock_incorrect))
        WooPosPINMessage.UNAVAILABLE -> AnnotatedString(stringResource(R.string.woopos_lock_unavailable))
        WooPosPINMessage.WAIT -> {
            val minutes = remainingSeconds / SECONDS_PER_MINUTE
            val seconds = remainingSeconds % SECONDS_PER_MINUTE
            val duration = String.format(locale, "%d:%02d", minutes, seconds)
            val full = stringResource(R.string.woopos_lock_wait, duration)
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
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private const val FEEDBACK_MILLIS = 120
private const val SECONDS_PER_MINUTE = 60
