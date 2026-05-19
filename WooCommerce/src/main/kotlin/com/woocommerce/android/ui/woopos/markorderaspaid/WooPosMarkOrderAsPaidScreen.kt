package com.woocommerce.android.ui.woopos.markorderaspaid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

private val MAX_CONTENT_WIDTH = 480.dp
private val ICON_SIZE = 36.dp

@Composable
fun WooPosMarkOrderAsPaidScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel = hiltViewModel<WooPosMarkOrderAsPaidViewModel>()
    val state = viewModel.state.collectAsState().value

    val onBackClicked = { viewModel.onBackClicked() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { onNavigationEvent(it) }
    }

    val isProcessing = (state as? WooPosMarkOrderAsPaidState.Confirming)?.isProcessing == true
    BackHandler(enabled = !isProcessing) { onBackClicked() }

    WooPosMarkOrderAsPaidScreen(
        state = state,
        onNoteChanged = { viewModel.onUIEvent(WooPosMarkOrderAsPaidUIEvent.NoteChanged(it)) },
        onConfirmClicked = { viewModel.onUIEvent(WooPosMarkOrderAsPaidUIEvent.ConfirmClicked) },
        onCancelClicked = onBackClicked,
    )
}

@Composable
private fun WooPosMarkOrderAsPaidScreen(
    state: WooPosMarkOrderAsPaidState,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is WooPosMarkOrderAsPaidState.Confirming -> Confirming(
                state = state,
                onNoteChanged = onNoteChanged,
                onConfirmClicked = onConfirmClicked,
                onCancelClicked = onCancelClicked,
            )

            WooPosMarkOrderAsPaidState.Initiating -> WooPosCircularLoadingIndicator()
        }
    }
}

@Composable
private fun Confirming(
    state: WooPosMarkOrderAsPaidState.Confirming,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MAX_CONTENT_WIDTH)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(WooPosSpacing.Large.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(ICON_SIZE),
        )

        WooPosText(
            text = stringResource(R.string.woopos_mark_order_as_paid_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        WooPosText(
            text = stringResource(R.string.woopos_mark_order_as_paid_message, state.formattedTotal),
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_mark_order_as_paid_note_label),
                style = WooPosTypography.BodySmall,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_NOTE_FIELD),
                value = state.note,
                onValueChange = onNoteChanged,
                placeholder = {
                    WooPosText(
                        text = stringResource(R.string.woopos_mark_order_as_paid_note_hint),
                        style = WooPosTypography.BodyMedium,
                    )
                },
                singleLine = false,
                maxLines = 4,
                minLines = 2,
                enabled = !state.isProcessing,
            )
        }

        if (state.errorMessage != null) {
            WooPosText(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = WooPosTypography.BodySmall,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            WooPosButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_CONFIRM_BUTTON),
                text = stringResource(R.string.woopos_mark_order_as_paid_confirm_button),
                onClick = onConfirmClicked,
                state = when {
                    state.isProcessing -> WooPosButtonState.LOADING
                    !state.canConfirm -> WooPosButtonState.DISABLED
                    else -> WooPosButtonState.ENABLED
                },
            )
            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.woopos_mark_order_as_paid_cancel_button),
                onClick = onCancelClicked,
            )
        }
    }
}

@WooPosPreview
@Composable
private fun WooPosMarkOrderAsPaidScreenPreview() {
    WooPosTheme {
        WooPosMarkOrderAsPaidScreen(
            state = WooPosMarkOrderAsPaidState.Confirming(
                formattedTotal = "$24.99",
                note = "",
                errorMessage = null,
                isProcessing = false,
                canConfirm = true,
            ),
            onNoteChanged = {},
            onConfirmClicked = {},
            onCancelClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosMarkOrderAsPaidScreenProcessingPreview() {
    WooPosTheme {
        WooPosMarkOrderAsPaidScreen(
            state = WooPosMarkOrderAsPaidState.Confirming(
                formattedTotal = "$24.99",
                note = "Bank transfer from Maria",
                errorMessage = null,
                isProcessing = true,
                canConfirm = true,
            ),
            onNoteChanged = {},
            onConfirmClicked = {},
            onCancelClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosMarkOrderAsPaidScreenErrorPreview() {
    WooPosTheme {
        WooPosMarkOrderAsPaidScreen(
            state = WooPosMarkOrderAsPaidState.Confirming(
                formattedTotal = "$24.99",
                note = "Bank transfer",
                errorMessage = "Couldn't update the order. Try again.",
                isProcessing = false,
                canConfirm = true,
            ),
            onNoteChanged = {},
            onConfirmClicked = {},
            onCancelClicked = {},
        )
    }
}
