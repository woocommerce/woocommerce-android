package com.woocommerce.android.ui.orders.wooshippinglabels.fedex

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.carriertos.CarrierTermsBottomSheetScaffold
import com.woocommerce.android.ui.orders.wooshippinglabels.carriertos.CheckboxWithTitle

@Composable
fun FedExTermsOfServiceBottomSheet(
    viewModel: FedExTermsOfServiceViewModel,
    snackbarHostState: SnackbarHostState
) {
    viewModel.viewState.observeAsState().value?.let {
        FedExTermsOfServiceBottomSheet(it, snackbarHostState)
    }
}

@Composable
fun FedExTermsOfServiceBottomSheet(
    viewState: FedExTermsOfServiceViewModel.ViewState,
    snackbarHostState: SnackbarHostState
) {
    CarrierTermsBottomSheetScaffold(
        titleResId = R.string.wpp_shipping_fedex_tos_title,
        descriptionResId = R.string.wpp_shipping_fedex_tos_description,
        originAddress = null,
        confirmEnabled = viewState.isTermsOfServiceAccepted,
        isLoading = viewState.isLoading,
        snackbarHostState = snackbarHostState,
        onContinueClicked = viewState.onContinueClicked
    ) {
        CheckboxWithTitle(
            checked = viewState.isTermsOfServiceAccepted,
            title = clickableAnnotatedStringRes(
                stringResId = R.string.wpp_shipping_fedex_tos_condition_terms,
                onUrlClick = viewState.onUrlClicked
            ),
            onCheckedChange = viewState.onTermsOfServiceCheckedChanged
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun FedExTermsOfServiceBottomSheetPreview() {
    WooThemeWithBackground {
        FedExTermsOfServiceBottomSheet(
            viewState = FedExTermsOfServiceViewModel.ViewState(
                isLoading = false,
                isTermsOfServiceAccepted = true,
                onUrlClicked = {},
                onTermsOfServiceCheckedChanged = {},
                onContinueClicked = {}
            ),
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
