package com.woocommerce.android.ui.products.inventory

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ProductInfo

@Composable
fun QuickInventoryUpdateBottomSheet(
    state: State<ScanToUpdateInventoryViewModel.ViewState>,
    onIncrementQuantityClicked: () -> Unit,
    onManualQuantityEntered: (String) -> Unit,
    onUpdateQuantityClicked: () -> Unit,
) {
    if (state.value !is ScanToUpdateInventoryViewModel.ViewState.ProductLoaded) return

    val productState = state.value as ScanToUpdateInventoryViewModel.ViewState.ProductLoaded
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp))
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.scan_to_update_inventory_product_label),
                fontSize = 17.sp,
                fontWeight = FontWeight(590)
            )
            Divider()
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)))
            ProductThumbnail(
                modifier = Modifier
                    .size(160.dp)
                    .clip(
                        RoundedCornerShape(8.dp)
                    ),
                imageUrl = productState.product.imageUrl,
            )
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)))
            Text(
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                text = productState.product.name,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight(700),
                )
            )
            Text(
                modifier = Modifier.padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_200),
                    top = dimensionResource(id = R.dimen.minor_50),
                ),
                text = productState.product.sku
            )
            Divider()
            Row(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.scan_to_update_inventory_quantity_label),
                )
                BasicTextField(
                    value = TextFieldValue(
                        text = productState.product.quantity.toString(),
                        selection = TextRange(productState.product.quantity.toString().length)
                    ),
                    onValueChange = {
                        onManualQuantityEntered(it.text)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.body1.copy(
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.onSurface,
                    ),
                )
            }
            Divider()
            if (productState.isPendingUpdate) {
                Row(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.scan_to_update_inventory_original_quantity_label),
                        style = MaterialTheme.typography.caption,
                    )
                    Text(
                        text = productState.originalQuantity,
                        style = MaterialTheme.typography.caption
                    )
                }
                WCColoredButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.major_100)),
                    onClick = onUpdateQuantityClicked,
                    text = stringResource(R.string.scan_to_update_inventory_update_quantity_button)
                )
            } else {
                WCColoredButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.major_100)),
                    onClick = onIncrementQuantityClicked,
                    text = stringResource(R.string.scan_to_update_inventory_increment_quantity_button)
                )
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun QuickInventoryUpdateBottomSheetPreview() {
    val product = ProductInfo(
        id = 12,
        name = "Product Name",
        imageUrl = "https://woocommerce.com/wp-content/uploads/2017/03/woocommerce-logo.png",
        sku = "123-SKU-456",
        quantity = 10,
    )
    val state = rememberSaveable {
        mutableStateOf(
            ScanToUpdateInventoryViewModel.ViewState.ProductLoaded(
                product,
                true,
                "8"
            )
        )
    }
    WooTheme {
        QuickInventoryUpdateBottomSheet(state, {}, {}, {})
    }
}
