package com.woocommerce.android.ui.products.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail

@Composable
fun StockManagementBottomSheet(
    state: ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Text(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
            text = stringResource(id = R.string.scan_to_update_inventory_product_label),
            fontWeight = FontWeight(590),
        )
        Divider()
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)))
        ProductThumbnail(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp)),
            imageUrl = state.product.imageUrl
        )
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)))
        Text(
            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            text = state.product.name,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h5
        )
        Text(
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.major_200),
                top = dimensionResource(id = R.dimen.minor_50),
            ),
            text = state.product.sku
        )
        Divider()

    }
}
