package com.woocommerce.android.ui.woopos.roles.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.modifier.wooPosUserInteraction
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_PHONE_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.PIN_PAD_TABLET_KEY_SIZE
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINEntryFeedback
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPhoneLayout
import com.woocommerce.android.ui.woopos.common.pin.measureWooPosPINStatusHeight
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus

internal val LocalWooPosRequestApproval =
    staticCompositionLocalOf<(WooPosManagerApprovalAction, () -> Unit) -> Unit> {
        error("POS manager approval requires a POS root")
    }

@Composable
internal fun WooPosManagerApprovalDialog(
    state: WooPosManagerApprovalState,
    onUIEvent: (WooPosManagerApprovalUIEvent) -> Unit,
) {
    val fullScreen = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    Dialog(
        onDismissRequest = { if (state.canDismiss) onUIEvent(WooPosManagerApprovalUIEvent.DismissRequested) },
        properties = DialogProperties(
            dismissOnClickOutside = !fullScreen && state.canDismiss,
            dismissOnBackPress = state.canDismiss,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = !fullScreen,
        ),
    ) {
        ApprovalContent(state, fullScreen, onUIEvent)
    }
}

@Composable
private fun ApprovalContent(
    state: WooPosManagerApprovalState,
    fullScreen: Boolean,
    onUIEvent: (WooPosManagerApprovalUIEvent) -> Unit,
) {
    val onDismiss = { onUIEvent(WooPosManagerApprovalUIEvent.DismissRequested) }
    Surface(
        modifier = if (fullScreen) {
            Modifier.wooPosUserInteraction().fillMaxSize()
        } else {
            Modifier.wooPosUserInteraction()
                .padding(WooPosSpacing.Large.value)
                .widthIn(max = APPROVAL_CARD_MAX_WIDTH.toAdaptiveComponentSize())
                .fillMaxWidth()
        },
        shape = if (fullScreen) RectangleShape else RoundedCornerShape(WooPosCornerRadius.Large.value),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        val bodyModifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        if (fullScreen) {
            WooPosPINPhoneLayout(header = { ApprovalCloseHeader(onDismiss, state.canDismiss) }) {
                ApprovalBody(state, onUIEvent, keySize = PIN_PAD_PHONE_KEY_SIZE, modifier = bodyModifier)
            }
        } else {
            Column(Modifier.padding(WooPosSpacing.Large.value)) {
                ApprovalCloseHeader(onDismiss, state.canDismiss)
                ApprovalBody(
                    state,
                    onUIEvent,
                    keySize = PIN_PAD_TABLET_KEY_SIZE,
                    modifier = Modifier.weight(1f, fill = false).then(bodyModifier),
                )
            }
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
    state: WooPosManagerApprovalState,
    onUIEvent: (WooPosManagerApprovalUIEvent) -> Unit,
    keySize: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_approval_required),
            modifier = Modifier.semantics { heading() },
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        val action = state.action
        WooPosText(
            text = when (action) {
                is WooPosManagerApprovalAction.IssueRefund -> stringResource(action.reason, action.orderNumber)
                else -> stringResource(action.reason)
            },
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val statusHeight = measureWooPosPINStatusHeight(
                rememberTextMeasurer(),
                constraints.maxWidth,
                APPROVAL_STATUS_MIN_HEIGHT,
            )
            WooPosPINEntryFeedback(state.status, state.enteredDigits, statusHeight)
        }
        WooPosPINPad(
            enteredDigits = state.enteredDigits,
            enabled = state.status.canEnterPIN,
            onDigit = { onUIEvent(WooPosManagerApprovalUIEvent.DigitTapped(it)) },
            onDelete = { onUIEvent(WooPosManagerApprovalUIEvent.DeleteTapped) },
            keySize = keySize,
            style = if (state.status.showsBlockedPINPad) WooPosPINPadStyle.BLOCKED else WooPosPINPadStyle.STANDARD,
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosManagerApprovalPreview() {
    WooPosTheme {
        ApprovalContent(
            WooPosManagerApprovalState(WooPosManagerApprovalAction.CreateCoupon, WooPosPINEntryStatus.Idle),
            fullScreen = false,
            onUIEvent = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosManagerApprovalWaitPreview() {
    WooPosTheme {
        ApprovalContent(
            WooPosManagerApprovalState(WooPosManagerApprovalAction.IssueRefund("#1043"), WooPosPINEntryStatus.Wait(30)),
            fullScreen = true,
            onUIEvent = {},
        )
    }
}

private val APPROVAL_CARD_MAX_WIDTH = 448.dp
private val APPROVAL_STATUS_MIN_HEIGHT = 44.dp
