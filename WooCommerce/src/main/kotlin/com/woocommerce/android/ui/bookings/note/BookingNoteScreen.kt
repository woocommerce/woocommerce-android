package com.woocommerce.android.ui.bookings.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun BookingNoteScreen(
    viewModel: BookingNoteViewModel,
    onBack: () -> Unit,
) {
    val viewState by viewModel.state.observeAsState()
    viewState?.let {
        BookingNoteScreen(
            viewState = it,
            onBack = onBack,
        )
    }
}

@Composable
fun BookingNoteScreen(
    viewState: BookingNoteViewState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.booking_note_screen_title),
                onNavigationButtonClick = onBack,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}
