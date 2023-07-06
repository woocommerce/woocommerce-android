package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun OrderCreateEditProductDiscountScreen(
    viewState: StateFlow<OrderCreateEditProductDiscountViewModel.ViewState>,
    onCloseClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onRemoveDiscountClicked: () -> Unit,
    onDiscountAmountChange: (String) -> Unit,
) {
    Scaffold(topBar = { Toolbar(onCloseClicked, onDoneClicked) }) { padding ->
        val state = viewState.collectAsState()
        Box(modifier = Modifier.padding(padding)) {
            Column(Modifier.padding(dimensionResource(id = R.dimen.minor_100))) {
                WCOutlinedTextField(
                    value = state.value.discountAmount,
                    onValueChange = onDiscountAmountChange,
                    label = stringResource(
                        R.string.order_creation_discount_amount_with_currency,
                        state.value.currency
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                WCColoredButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRemoveDiscountClicked
                ) {
                    Text(stringResource(id = R.string.order_creation_remove_discount))
                }
            }
        }
    }
}

@Composable
private fun Toolbar(onCloseClicked: () -> Unit, onDoneClicked: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.discount)) },
        navigationIcon = {
            IconButton(onClick = onCloseClicked) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        },
        backgroundColor = colorResource(id = R.color.color_toolbar),
        elevation = 0.dp,
        actions = {
            WCTextButton(onClick = onDoneClicked, text = stringResource(id = R.string.done))
        },
    )
}

@Preview
@Composable
fun ToolbarPreview() = Toolbar({}, {})

@Preview
@Composable
fun OrderCreateEditProductDiscountScreenPreview() =
    OrderCreateEditProductDiscountScreen(
        MutableStateFlow(OrderCreateEditProductDiscountViewModel.ViewState("$", "0")),
        {},
        {},
        {},
        {_->}
    )