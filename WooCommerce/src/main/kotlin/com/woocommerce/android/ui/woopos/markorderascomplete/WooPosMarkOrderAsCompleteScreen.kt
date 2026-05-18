package com.woocommerce.android.ui.woopos.markorderascomplete

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
fun WooPosMarkOrderAsCompleteScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val viewModel = hiltViewModel<WooPosMarkOrderAsCompleteViewModel>()
    val state = viewModel.state.collectAsState().value

    val onBackClicked = { viewModel.onBackClicked() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { onNavigationEvent(it) }
    }

    WooPosMarkOrderAsCompleteScreen(
        state = state,
        onNoteChanged = { viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.NoteChanged(it)) },
        onConfirmClicked = { viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked) },
        onBackClicked = onBackClicked,
    )
    BackHandler { onBackClicked() }
}

@Composable
private fun WooPosMarkOrderAsCompleteScreen(
    state: WooPosMarkOrderAsCompleteState,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_mark_order_as_complete_title),
            onBackClicked = onBackClicked,
        )
        when (state) {
            is WooPosMarkOrderAsCompleteState.Confirming -> Confirming(
                state = state,
                onNoteChanged = onNoteChanged,
                onConfirmClicked = onConfirmClicked,
            )

            WooPosMarkOrderAsCompleteState.Initiating -> Box(
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
    state: WooPosMarkOrderAsCompleteState.Confirming,
    onNoteChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(WooPosSpacing.Large.value),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        WooPosText(
            text = state.totalText,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        WooPosText(
            text = stringResource(R.string.woopos_mark_order_as_complete_body),
            style = WooPosTypography.BodyLarge,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_NOTE_FIELD),
            value = state.note,
            onValueChange = onNoteChanged,
            label = {
                WooPosText(
                    text = stringResource(R.string.woopos_mark_order_as_complete_note_label),
                    style = WooPosTypography.BodyLarge,
                )
            },
            placeholder = {
                WooPosText(
                    text = stringResource(R.string.woopos_mark_order_as_complete_note_hint),
                    style = WooPosTypography.BodyLarge,
                )
            },
            singleLine = false,
            maxLines = 3,
        )
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
            WooPosText(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = WooPosTypography.BodySmall,
                textAlign = TextAlign.Start,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        WooPosButton(
            text = state.button.text,
            onClick = onConfirmClicked,
            state = when (state.button.status) {
                WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED -> WooPosButtonState.ENABLED
                WooPosMarkOrderAsCompleteState.Confirming.Button.Status.LOADING -> WooPosButtonState.LOADING
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(WooPosTestTags.MARK_ORDER_AS_PAID_CONFIRM_BUTTON),
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosMarkOrderAsCompleteScreenPreview() {
    WooPosTheme {
        WooPosMarkOrderAsCompleteScreen(
            state = WooPosMarkOrderAsCompleteState.Confirming(
                totalText = "Order total: $42.00",
                note = "",
                errorMessage = null,
                button = WooPosMarkOrderAsCompleteState.Confirming.Button(
                    text = "Mark order as complete",
                    status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED,
                ),
            ),
            onNoteChanged = {},
            onConfirmClicked = {},
            onBackClicked = {},
        )
    }
}

@WooPosPreview
@Composable
private fun WooPosMarkOrderAsCompleteScreenErrorPreview() {
    WooPosTheme {
        WooPosMarkOrderAsCompleteScreen(
            state = WooPosMarkOrderAsCompleteState.Confirming(
                totalText = "Order total: $42.00",
                note = "Bank transfer",
                errorMessage = "Something went wrong. Please try again.",
                button = WooPosMarkOrderAsCompleteState.Confirming.Button(
                    text = "Mark order as complete",
                    status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED,
                ),
            ),
            onNoteChanged = {},
            onConfirmClicked = {},
            onBackClicked = {},
        )
    }
}
