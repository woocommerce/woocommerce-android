package com.woocommerce.android.ui.woopos.roles.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.rememberRetained
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_PHONE_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_TABLET_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINEntryFeedback
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadLayout
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPhoneLayout
import com.woocommerce.android.ui.woopos.common.pin.measureWooPosPINStatusHeight
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus

internal val LocalWooPosRequestApproval =
    staticCompositionLocalOf<(WooPosApprovalAction, () -> Unit) -> Unit> {
        error("POS approval requires a POS root")
    }

@Composable
internal fun WooPosApprovalDialog(
    state: WooPosApprovalState?,
    onUIEvent: (WooPosApprovalUIEvent) -> Unit,
) {
    val onDismiss = {
        if (state?.canDismiss == true) onUIEvent(WooPosApprovalUIEvent.DismissRequested)
    }
    if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
        if (state == null) return
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = state.canDismiss,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            WooPosApprovalPhoneContent(state, onUIEvent, onDismiss)
        }
    } else {
        WooPosApprovalTabletContent(state, onUIEvent, onDismiss)
    }
}

@Composable
internal fun WooPosApprovalPhoneContent(
    state: WooPosApprovalState,
    onUIEvent: (WooPosApprovalUIEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceBright) {
        WooPosPINPhoneLayout(header = { ApprovalCloseHeader(onDismiss, state.canDismiss) }) {
            ApprovalBody(
                state,
                onUIEvent,
                keySize = PIN_PAD_PHONE_KEY_SIZE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun WooPosApprovalTabletContent(
    state: WooPosApprovalState?,
    onUIEvent: (WooPosApprovalUIEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val isVisible = state != null
    val shownState = rememberRetained(state)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    WooPosDialogWrapper(
        modifier = Modifier
            .padding(WooPosSpacing.Large.value)
            .widthIn(
                max = APPROVAL_CARD_MAX_WIDTH.toAdaptiveComponentSize() * fontScale,
            ),
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(R.string.dismiss),
        widthFraction = 1f,
        onDismissRequest = { if (isVisible) onDismiss() },
    ) {
        if (shownState == null) return@WooPosDialogWrapper
        Column {
            ApprovalCloseHeader({ if (isVisible) onDismiss() }, isVisible && shownState.canDismiss)
            ApprovalBody(
                shownState,
                onUIEvent = { if (isVisible) onUIEvent(it) },
                keySize = PIN_PAD_TABLET_KEY_SIZE,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ApprovalCloseHeader(onDismiss: () -> Unit, enabled: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        WooPosBackButton(
            iconRes = R.drawable.ic_close_24dp,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
            enabled = enabled,
        )
    }
}

@Composable
private fun ApprovalBody(
    state: WooPosApprovalState,
    onUIEvent: (WooPosApprovalUIEvent) -> Unit,
    keySize: Dp,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.woopos_approval_required)
    val reason = when (val action = state.action) {
        is WooPosApprovalAction.IssueRefund -> stringResource(action.reason, action.orderNumber)
        else -> stringResource(action.reason)
    }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val headingStyle = WooPosTypography.Heading.style.copy(fontWeight = FontWeight.Bold)
    WooPosPINPadLayout(
        preferredKeySize = keySize,
        preferredSpacing = WooPosSpacing.Medium.value,
        modifier = modifier,
        messageHeight = { width, spacing ->
            val textConstraints = Constraints(maxWidth = with(density) { width.roundToPx() })
            val titleHeight = measurer.measure(title, headingStyle, constraints = textConstraints).size.height
            val reasonHeight =
                measurer.measure(reason, WooPosTypography.BodyMedium.style, constraints = textConstraints)
                    .size.height
            with(density) { (titleHeight + reasonHeight).toDp() } + spacing * 2 +
                measureWooPosPINStatusHeight(measurer, textConstraints.maxWidth, APPROVAL_STATUS_MIN_HEIGHT)
        },
        message = { spacing ->
            val statusHeight = measureWooPosPINStatusHeight(measurer, constraints.maxWidth, APPROVAL_STATUS_MIN_HEIGHT)
            ApprovalMessage(title, reason, state, statusHeight, spacing)
        },
        keypad = { ApprovalKeypad(state, it, onUIEvent) },
    )
}

@Composable
private fun ApprovalMessage(
    title: String,
    reason: String,
    state: WooPosApprovalState,
    statusHeight: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        WooPosText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        WooPosText(
            text = reason,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        WooPosPINEntryFeedback(state.status, state.enteredDigits, statusHeight)
    }
}

@Composable
private fun ApprovalKeypad(
    state: WooPosApprovalState,
    keySize: Dp,
    onUIEvent: (WooPosApprovalUIEvent) -> Unit,
) {
    WooPosPINPad(
        enteredDigits = state.enteredDigits,
        enabled = state.status.canEnterPIN,
        onDigit = { onUIEvent(WooPosApprovalUIEvent.DigitTapped(it)) },
        onDelete = { onUIEvent(WooPosApprovalUIEvent.DeleteTapped) },
        keySize = keySize,
        style = if (state.status.showsBlockedPINPad) WooPosPINPadStyle.BLOCKED else WooPosPINPadStyle.STANDARD,
    )
}

@WooPosPreview
@Composable
private fun WooPosApprovalPreview() {
    WooPosTheme {
        WooPosApprovalTabletContent(
            WooPosApprovalState(WooPosApprovalAction.CreateCoupon, WooPosPINEntryStatus.Idle),
            onUIEvent = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosApprovalWaitPreview() {
    WooPosTheme {
        WooPosApprovalPhoneContent(
            WooPosApprovalState(WooPosApprovalAction.IssueRefund("#1043"), WooPosPINEntryStatus.Wait(30)),
            onUIEvent = {},
            onDismiss = {},
        )
    }
}

private val APPROVAL_CARD_MAX_WIDTH = 448.dp
private val APPROVAL_STATUS_MIN_HEIGHT = 44.dp
