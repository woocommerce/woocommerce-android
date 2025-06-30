package com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton

@Composable
internal fun ShippingRatesSection(
    shippingRatesState: WooShippingLabelCreationViewModel.ShippingRatesState,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
) {
    when (shippingRatesState) {
        is WooShippingLabelCreationViewModel.ShippingRatesState.DataState -> {
            ShippingRatesCard(
                selectedRate = shippingRatesState.selectedRate,
                shippingRates = shippingRatesState.shippingRates,
                selectedSortOption = shippingRatesState.selectedRatesSortOrder,
                onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                onSelectedSippingRateChanged = onSelectedSippingRateChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is WooShippingLabelCreationViewModel.ShippingRatesState.MissingInfo -> {
            ShippingRatesSectionMissingInfo(
                modifier = Modifier.padding(horizontal = 16.dp),
                missingInfo = shippingRatesState
            )
        }

        WooShippingLabelCreationViewModel.ShippingRatesState.Error -> ErrorMessageWithButton(
            message = R.string.woo_shipping_labels_package_creation_shipping_rates_loading_error,
            modifier = Modifier.sizeIn(minHeight = 300.dp),
            onRetryClick = { onRefreshShippingRates() }
        )

        is WooShippingLabelCreationViewModel.ShippingRatesState.Loading -> {
            ShippingRatesLoading(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedSortOption = shippingRatesState.selectedRatesSortOrder,
                onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged
            )
        }

        WooShippingLabelCreationViewModel.ShippingRatesState.NoAvailable -> {}
    }
}
