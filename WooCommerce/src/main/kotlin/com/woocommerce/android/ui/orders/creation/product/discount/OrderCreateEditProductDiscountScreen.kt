package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSelectableChip
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountType.Amount
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountType.Percentage
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.ViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

@Composable
fun OrderCreateEditProductDiscountScreen(
    viewState: StateFlow<ViewState>,
    onCloseClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    onRemoveDiscountClicked: () -> Unit,
    onDiscountAmountChange: (BigDecimal?) -> Unit,
    onPercentageDiscountSelected: () -> Unit,
    onAmountDiscountSelected: () -> Unit,
    discountInputFieldConfig: DiscountInputFieldConfig,
) {
    val state = viewState.collectAsState()
    Scaffold(topBar = { Toolbar(onCloseClicked, onDoneClicked, state.value.isDoneButtonEnabled) }) { padding ->
        val focusRequester = remember { FocusRequester() }
        Box(modifier = Modifier.padding(padding).background(MaterialTheme.colors.surface)) {
            Column(Modifier.padding(dimensionResource(id = R.dimen.minor_100))) {
                val discountValidationState = state.value.discountValidationState

                Row (
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WCOutlinedTypedTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .weight(1f),
                        value = state.value.discountAmount,
                        valueMapper = NullableCurrencyTextFieldValueMapper(
                            discountInputFieldConfig.decimalSeparator,
                            discountInputFieldConfig.numberOfDecimals
                        ),
                        onValueChange = onDiscountAmountChange,
                        label = stringResource(
                            R.string.order_creation_discount_amount_with_currency,
                            state.value.discountType.symbol
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = discountValidationState is Invalid,
                        trailingIcon = {
                            if (discountValidationState is Invalid) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = colorResource(id = R.color.woo_red_50)
                                )
                            }
                        },
                    )
                    Switch(state.value, onPercentageDiscountSelected, onAmountDiscountSelected)
                }
                if (discountValidationState is Invalid) {
                    Text(
                        text = discountValidationState.errorMessage,
                        color = colorResource(id = R.color.woo_red_50),
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                if (state.value.isRemoveButtonVisible) {
                    WCColoredButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensionResource(R.dimen.minor_100)),
                        onClick = onRemoveDiscountClicked
                    ) {
                        Text(stringResource(id = R.string.order_creation_remove_discount))
                    }
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    onCloseClicked: () -> Unit,
    onDoneClicked: () -> Unit,
    isDoneButtonEnabled: Boolean,
) {
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
        elevation = dimensionResource(id = R.dimen.appbar_elevation),
        actions = {
            WCTextButton(
                onClick = onDoneClicked,
                enabled = isDoneButtonEnabled,
                text = stringResource(id = R.string.done)
            )
        },
    )
}

@Composable
private fun Switch(
    state: ViewState,
    onPercentageDiscountClicked: () -> Unit,
    onManualDiscountClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.minor_100)),
        horizontalArrangement = Arrangement.Absolute.Center
    ) {
        WCSelectableChip(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.major_250))
                .height(dimensionResource(id = R.dimen.major_200)),
            onClick = onPercentageDiscountClicked,
            text = "%",
            isSelected = state.discountType is Percentage,
            contentPadding = PaddingValues(0.5.dp),
        )
        WCSelectableChip(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.major_250))
                .height(dimensionResource(id = R.dimen.major_200)),
            onClick = onManualDiscountClicked,
            text = state.currency,
            isSelected = state.discountType is Amount,
            contentPadding = PaddingValues(0.5.dp)
        )
    }
}

data class DiscountInputFieldConfig(
    val decimalSeparator: String,
    val numberOfDecimals: Int,
)

@Preview
@Composable
fun ToolbarPreview() = Toolbar({}, {}, true)

@Preview
@Composable
fun SwitchPreview() = Switch(ViewState("$", BigDecimal.ZERO, isRemoveButtonVisible = true), {}, {})

@Preview
@Composable
fun OrderCreateEditProductDiscountScreenPreview() =
    OrderCreateEditProductDiscountScreen(
        MutableStateFlow(ViewState("$", BigDecimal.ZERO, isRemoveButtonVisible = true)),
        {},
        {},
        {},
        {},
        {},
        {},
        DiscountInputFieldConfig(
            decimalSeparator = ".",
            numberOfDecimals = 2
        )
    )
