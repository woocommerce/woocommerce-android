package com.woocommerce.android.ui.orders.wooshippinglabels.upsdap

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.carriertos.CarrierTermsBottomSheetScaffold
import com.woocommerce.android.ui.orders.wooshippinglabels.carriertos.CheckboxWithTitle

@Composable
fun UPSDAPTermsOfServiceBottomSheet(
    viewModel: UPSDAPTermsOfServiceViewModel,
    snackbarHostState: SnackbarHostState
) {
    viewModel.viewState.observeAsState().value?.let {
        UPSDAPTermsOfServiceBottomSheet(it, snackbarHostState)
    }
}

@Composable
fun UPSDAPTermsOfServiceBottomSheet(
    viewState: UPSDAPTermsOfServiceViewModel.ViewState,
    snackbarHostState: SnackbarHostState
) {
    CarrierTermsBottomSheetScaffold(
        titleResId = R.string.wpp_shipping_ups_tos_title,
        descriptionResId = R.string.wpp_shipping_ups_tos_description,
        originAddress = viewState.originShippingAddress.format(singleLine = false),
        confirmEnabled = viewState.areAllConditionsAccepted,
        isLoading = viewState.isLoading,
        snackbarHostState = snackbarHostState,
        onContinueClicked = viewState.onContinueClicked
    ) {
        CheckboxWithTitle(
            checked = viewState.conditionsState.isTermsOfServiceChecked,
            title = clickableAnnotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_terms,
                onUrlClick = viewState.onUrlClicked
            ),
            onCheckedChange = viewState.conditionsState.onTermsOfServiceCheckedChanged
        )

        CheckboxWithTitle(
            checked = viewState.conditionsState.isProhibitedItemsChecked,
            title = clickableAnnotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_prohibited_items,
                onUrlClick = viewState.onUrlClicked
            ),
            onCheckedChange = viewState.conditionsState.onProhibitedItemsCheckedChanged
        )

        CheckboxWithTitle(
            checked = viewState.conditionsState.isTechnologyAgreementChecked,
            title = clickableAnnotatedStringRes(
                stringResId = R.string.wpp_shipping_ups_tos_condition_technology_agreement,
                onUrlClick = viewState.onUrlClicked
            ),
            onCheckedChange = viewState.conditionsState.onTechnologyAgreementCheckedChanged
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun UPSDAPTermsOfServiceBottomSheetPreview() {
    WooThemeWithBackground {
        UPSDAPTermsOfServiceBottomSheet(
            viewState = UPSDAPTermsOfServiceViewModel.ViewState(
                isLoading = false,
                originShippingAddress = ShippingLabelSampleData.getShipFrom(),
                conditionsState = UPSDAPTermsOfServiceViewModel.ConditionsState(
                    isTermsOfServiceChecked = true,
                    isProhibitedItemsChecked = true,
                    isTechnologyAgreementChecked = true,
                    onTermsOfServiceCheckedChanged = {},
                    onProhibitedItemsCheckedChanged = {},
                    onTechnologyAgreementCheckedChanged = {}
                ),
                onUrlClicked = {},
                onContinueClicked = {}
            ),
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
