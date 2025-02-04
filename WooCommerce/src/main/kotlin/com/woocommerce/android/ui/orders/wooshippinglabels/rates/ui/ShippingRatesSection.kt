package com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel

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

        WooShippingLabelCreationViewModel.ShippingRatesState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 300.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Error")
                WCColoredButton(onClick = { onRefreshShippingRates() }) {
                    Text(text = "Retry")
                }
            }
        }

        is WooShippingLabelCreationViewModel.ShippingRatesState.Loading -> {
            ShippingRatesLoading(
                selectedSortOption = shippingRatesState.selectedRatesSortOrder,
                onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged
            )
        }

        WooShippingLabelCreationViewModel.ShippingRatesState.NoAvailable -> {}
    }
}
