package com.woocommerce.android.ui.bookings.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.Render
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun BookingNoteScreen(
    viewModel: BookingNoteViewModel,
) {
    val viewState by viewModel.state.observeAsState()
    viewState?.let {
        BookingNoteScreen(
            viewState = it,
        )
    }
}

@Composable
fun BookingNoteScreen(
    viewState: BookingNoteViewState,
) {
    val focusRequester = remember { FocusRequester() }

    // Request focus only after the note is available in the composition
    LaunchedEffect(viewState.editedNote != null) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.booking_note_screen_title),
                onNavigationButtonClick = viewState.onBackPressed,
                actions = {
                    if (viewState.isSaveVisible) {
                        WCTextButton(
                            onClick = viewState.onSaveClicked,
                            enabled = viewState.isSaveEnabled,
                        ) {
                            when (viewState.noteSaveStatus) {
                                is NoteSaveStatus.Idle -> {
                                    Text(stringResource(R.string.booking_note_screen_done))
                                }

                                is NoteSaveStatus.InProgress -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            viewState.editedNote?.let {
                NoteTextField(
                    value = it,
                    onValueChange = viewState.onNoteChange,
                    enabled = viewState.noteEditable,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .focusRequester(focusRequester)
                )
            }
        }
    }

    viewState.dialogState?.Render()
}

@Composable
private fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var textFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    val textFieldValue = textFieldValueState.copy(text = value)

    val lastValue by rememberUpdatedState(value)

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValueState = it
            if (it.text != lastValue) {
                // Update external value when changed
                onValueChange(it.text)
            }
        },
        enabled = enabled,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    )
}
