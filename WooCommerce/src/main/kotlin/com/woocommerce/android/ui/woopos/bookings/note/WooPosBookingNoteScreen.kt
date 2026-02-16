package com.woocommerce.android.ui.woopos.bookings.note

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosFullScreenInputLayout
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosBookingNoteScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosBookingNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            onNavigationEvent(event)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect {
            Toast.makeText(
                context,
                context.getString(R.string.woopos_bookings_note_screen_save_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    WooPosBookingNoteScreen(
        state = state,
        onNoteChanged = viewModel::onNoteChanged,
        onSaveClicked = viewModel::onSaveClicked,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
    )
}

@Composable
private fun WooPosBookingNoteScreen(
    state: WooPosBookingNoteState,
    onNoteChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    WooPosFullScreenInputLayout(
        titleText = stringResource(R.string.woopos_bookings_note_screen_title),
        onBackClicked = onBackClicked,
        centerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WooPosInputField(
                    value = state.noteText,
                    onValueChange = onNoteChanged,
                    label = stringResource(R.string.woopos_bookings_note_screen_hint),
                    contentAlignment = Alignment.Center,
                    textStyle = WooPosTypography.Heading,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = WooPosSpacing.Medium.value)
                )
            }
        },
        buttonText = stringResource(R.string.woopos_bookings_note_screen_button_save),
        buttonState = state.saveButtonState,
        onButtonClicked = { onSaveClicked() },
    )
}
