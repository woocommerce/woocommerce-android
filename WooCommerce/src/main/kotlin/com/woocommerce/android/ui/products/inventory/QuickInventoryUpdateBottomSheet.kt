package com.woocommerce.android.ui.products.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun QuickInventoryUpdateBottomSheet(product: ScanToUpdateInventoryViewModel.ProductInfo) {
    Surface(
        Modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(24.dp)
            )
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hello World")
            ProductThumbnail(
                modifier = Modifier.size(160.dp),
                imageUrl = product.imageUrl,
            )
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /*TODO*/ },
                text = "Quantity + 1"
            )
            WCTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /*TODO*/ },
                text = "View Product Details"
            )
        }
    }
}

@Preview
@Composable
fun QuickInventoryUpdateBottomSheetPreview() {
    val product = ScanToUpdateInventoryViewModel.ProductInfo(
        name = "Product Name",
        imageUrl = "https://woocommerce.com/wp-content/uploads/2017/03/woocommerce-logo.png",
        sku = "SKU",
        quantity = 10,
    )
    QuickInventoryUpdateBottomSheet(product)
}
