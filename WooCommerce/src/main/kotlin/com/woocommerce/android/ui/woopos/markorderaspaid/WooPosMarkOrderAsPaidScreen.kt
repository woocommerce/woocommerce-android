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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

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
        onCloseClicked = onBackClicked,
    )
}

@Composable
private fun WooPosMarkOrderAsPaidScreen(
    state: WooPosMarkOrderAsPaidState,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onCloseClicked: () -> Unit,
) {
    val isProcessing = (state as? WooPosMarkOrderAsPaidState.Confirming)?.isProcessing == true
    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_mark_order_as_paid_title),
            onBackClicked = onCloseClicked.takeUnless { isProcessing },
            navigationIconRes = R.drawable.ic_close_24dp,
        )
        when (state) {
            is WooPosMarkOrderAsPaidState.Confirming -> Confirming(
                state = state,
                onNoteChanged = onNoteChanged,
                onConfirmClicked = onConfirmClicked,
            )

            WooPosMarkOrderAsPaidState.Initiating -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                WooPosCircularLoadingIndicator()
            }
        }
    }
}

@Composable
private fun Confirming(
    state: WooPosMarkOrderAsPaidState.Confirming,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_mark_order_as_paid_message, state.formattedTotal),
            style = WooPosTypography.BodyLarge,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )

        WooPosText(
            text = stringResource(R.string.woopos_mark_order_as_paid_note_label),
            style = WooPosTypography.BodySmall,
            fontWeight = FontWeight.Bold,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )

        WooPosInputField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = stringResource(R.string.woopos_mark_order_as_paid_note_hint),
            textColor = MaterialTheme.colorScheme.onSurface,
            textStyle = WooPosTypography.BodyLarge,
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_NOTE_FIELD),
        )

        if (state.errorMessage != null) {
            WooPosText(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = WooPosTypography.BodySmall,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        WooPosButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WooPosSpacing.Medium.value)
                .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_CONFIRM_BUTTON),
            text = stringResource(R.string.woopos_mark_order_as_paid_confirm_button),
            onClick = onConfirmClicked,
            state = when {
                state.isProcessing -> WooPosButtonState.LOADING
                !state.canConfirm -> WooPosButtonState.DISABLED
                else -> WooPosButtonState.ENABLED
            },
        )
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
            onCloseClicked = {},
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
            onCloseClicked = {},
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
            onCloseClicked = {},
        )
    }
}
