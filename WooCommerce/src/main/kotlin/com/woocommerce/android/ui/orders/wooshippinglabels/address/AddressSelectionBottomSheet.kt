package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.orders.wooshippinglabels.ShipmentDetailsSectionTitle
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionBottomSheet(
    shipFrom: OriginShippingAddress,
    originAddresses: List<OriginShippingAddress>,
    onDismiss: () -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    modifier: Modifier = Modifier
) {
    WCModalBottomSheet(
        modifier = modifier,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismiss,
    ) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.orderdetail_shipping_label_item_shipfrom),
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.minor_100)
            )
        )
        LazyColumn {
            items(originAddresses) { option ->
                val isSelected = option == shipFrom
                OriginAddressSelectionItem(
                    address = option,
                    isSelected = isSelected,
                    onEdit = onEditOriginAddress,
                    onClick = {
                        onOriginAddressSelected(option)
                    },
                    modifier = Modifier.padding(
                        top = dimensionResource(id = R.dimen.minor_100),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}
