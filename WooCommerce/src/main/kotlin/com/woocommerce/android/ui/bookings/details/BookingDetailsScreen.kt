package com.woocommerce.android.ui.bookings.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewModel: BookingDetailsViewModel,
    onBack: () -> Unit
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()

    BookingDetailsScreen(
        viewState = viewState, onBack = onBack
    )
}

@Composable
fun BookingDetailsScreen(
    viewState: BookingDetailsViewState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.toolbarTitle,
                onNavigationButtonClick = onBack,
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding))
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingDetailsPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState(
                toolbarTitle = "Booking #12345",
            ),
            onBack = {}
        )
    }
}
