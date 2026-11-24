package com.woocommerce.android.ui.woopos.roles.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINEntryFeedback
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPad
import com.woocommerce.android.ui.woopos.common.pin.WooPosPINPadStyle
import com.woocommerce.android.ui.woopos.common.pin.measureWooPosPINStatusHeight
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status

internal val LocalWooPosRequestApproval = staticCompositionLocalOf<(WooPosApprovalAction, () -> Unit) -> Unit> {
    error("POS manager approval requires a POS root")
}

@Composable
internal fun WooPosManagerApprovalDialog(
    state: WooPosManagerApprovalState,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fullScreen = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = !fullScreen,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = !fullScreen,
        ),
    ) {
        ApprovalContent(state, fullScreen, onDigit, onDelete, onDismiss)
    }
}

@Composable
private fun ApprovalContent(
    state: WooPosManagerApprovalState,
    fullScreen: Boolean,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = if (fullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .padding(WooPosSpacing.Large.value)
                .widthIn(max = 520.dp.toAdaptiveComponentSize())
                .fillMaxWidth()
        },
        shape = if (fullScreen) RectangleShape else RoundedCornerShape(WooPosCornerRadius.Large.value),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier = Modifier
                .then(if (fullScreen) Modifier.safeDrawingPadding() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(WooPosSpacing.Large.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value, Alignment.CenterVertically),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WooPosText(
                    text = stringResource(R.string.woopos_approval_required),
                    modifier = Modifier.padding(horizontal = WooPosSpacing.XXXLarge.value).semantics { heading() },
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
            val action = state.action
            WooPosText(
                text = when (action) {
                    is WooPosApprovalAction.IssueRefund -> stringResource(action.reason, action.orderNumber)
                    else -> stringResource(action.reason)
                },
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val statusHeight = measureWooPosPINStatusHeight(rememberTextMeasurer(), constraints.maxWidth, 44.dp)
                WooPosPINEntryFeedback(state.status, state.enteredDigits, statusHeight)
            }
            WooPosPINPad(
                enteredDigits = state.enteredDigits,
                enabled = state.status.canEnterPIN,
                onDigit = onDigit,
                onDelete = onDelete,
                keySize = if (fullScreen) 72.dp else 90.dp,
                style = if (state.status.showsBlockedPINPad) WooPosPINPadStyle.BLOCKED else WooPosPINPadStyle.STANDARD,
            )
        }
    }
}

@WooPosPreview
@Composable
private fun WooPosManagerApprovalPreview() {
    WooPosTheme {
        ApprovalContent(
            WooPosManagerApprovalState(WooPosApprovalAction.CreateCoupon, Status.Idle),
            fullScreen = false,
            onDigit = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosManagerApprovalWaitPreview() {
    WooPosTheme {
        ApprovalContent(
            WooPosManagerApprovalState(WooPosApprovalAction.IssueRefund("#1043"), Status.Wait(30)),
            fullScreen = true,
            onDigit = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
