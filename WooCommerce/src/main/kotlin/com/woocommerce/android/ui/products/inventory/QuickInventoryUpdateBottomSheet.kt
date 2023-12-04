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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ProductInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickInventoryUpdateBottomSheet(
    viewState: StateFlow<ScanToUpdateInventoryViewModel.ViewState>,
    onIncrementQuantityClicked: () -> Unit,
    onManualQuantityEntered: (String) -> Unit,
    onUpdateQuantityClicked: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (val state = viewState.collectAsState().value) {
        is ScanToUpdateInventoryViewModel.ViewState.ProductLoaded -> {
            ModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onDismissRequest = onDismiss,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                        text = stringResource(id = R.string.scan_to_update_inventory_product_label),
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
                        imageUrl = state.product.imageUrl,
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
                    Row(
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.scan_to_update_inventory_quantity_label),
                        )
                        OutlinedTextField(
                            value = state.product.quantity.toString(),
                            onValueChange = {
                                onManualQuantityEntered(it)
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
                    if (state.isPendingUpdate) {
                        Row(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(id = R.string.scan_to_update_inventory_original_quantity_label),
                                style = MaterialTheme.typography.caption,
                            )
                            Text(
                                text = state.originalQuantity,
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
        else -> Unit
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
    val state = MutableStateFlow(ScanToUpdateInventoryViewModel.ViewState.ProductLoaded(product))
    WooTheme {
        QuickInventoryUpdateBottomSheet(state, {}, {}, {}, {})
    }
}
