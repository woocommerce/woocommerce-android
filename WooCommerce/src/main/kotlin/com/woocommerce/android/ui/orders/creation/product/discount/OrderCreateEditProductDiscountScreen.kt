package com.woocommerce.android.ui.orders.creation.product.discount

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSelectableChip
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountAmountValidationState.Invalid
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountType.Amount
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.DiscountType.Percentage
import com.woocommerce.android.ui.orders.creation.product.discount.OrderCreateEditProductDiscountViewModel.ViewState
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

@SuppressLint("StateFlowValueCalledInComposition")
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
    productItem: MutableStateFlow<OrderCreationProduct>,
) {
    val state = viewState.collectAsState()
    Scaffold(topBar = { Toolbar(onCloseClicked, onDoneClicked, state.value.isDoneButtonEnabled) }) { padding ->
        val focusRequester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .padding(padding)
                .background(MaterialTheme.colors.surface)
        ) {
            Column(
                Modifier
                    .padding(dimensionResource(id = R.dimen.minor_100))
                    .verticalScroll(rememberScrollState())
            ) {
                val discountValidationState = state.value.discountValidationState

                ProductCard(
                    imageUrl = viewState.value.productDetailsState?.imageUrl,
                    productName = productItem.value.item.name,
                    productPrice = productItem.value.item.pricePreDiscount,
                    productQuantity = productItem.value.item.quantity,
                    subTotalPerProduct = productItem.value.item.subtotal,
                    state = state.value
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.minor_100)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WCOutlinedTypedTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .weight(1f),
                        value = state.value.discountAmount,
                        valueMapper = NullableCurrencyTextFieldValueMapper.create(
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

                CalculatedAmount(viewState.value)

                PriceAfterDiscount(viewState.value)

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
private fun ProductCard(
    imageUrl: String?,
    productName: String,
    productPrice: BigDecimal,
    productQuantity: Float,
    subTotalPerProduct: BigDecimal,
    state: ViewState
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.major_400))
            .padding(dimensionResource(id = R.dimen.minor_100))
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(id = R.color.woo_gray_5),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
    ) {
        val (asyncImage, productNameText, productDetailsText, totalText) = createRefs()

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.product_image_content_description),
            contentScale = ContentScale.FillWidth,
            placeholder = painterResource(R.drawable.ic_product),
            error = painterResource(R.drawable.ic_product),
            modifier = Modifier
                .constrainAs(asyncImage) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .size(dimensionResource(R.dimen.major_300))
                .clip(RoundedCornerShape(3.dp))
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                )
        )

        Text(
            text = productName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .constrainAs(productNameText) {
                    start.linkTo(asyncImage.end)
                    top.linkTo(asyncImage.top)
                    bottom.linkTo(productDetailsText.top)
                    end.linkTo(totalText.start)
                    width = Dimension.fillToConstraints
                }
                .padding(start = dimensionResource(id = R.dimen.major_100))
        )

        Text(
            text = "$productQuantity x ${state.currency}$productPrice",
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.woo_gray_40),
            modifier = Modifier
                .constrainAs(productDetailsText) {
                    start.linkTo(asyncImage.end)
                    top.linkTo(productNameText.bottom)
                    bottom.linkTo(asyncImage.bottom)
                    width = Dimension.fillToConstraints
                }
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                )
        )

        Text(
            text = "${state.currency}$subTotalPerProduct",
            modifier = Modifier
                .constrainAs(totalText) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(dimensionResource(id = R.dimen.major_100)),
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun Switch(
    state: ViewState,
    onPercentageDiscountClicked: () -> Unit,
    onManualDiscountClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = dimensionResource(id = R.dimen.minor_100)),
        horizontalArrangement = Arrangement.Absolute.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WCSelectableChip(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.major_250))
                .height(dimensionResource(id = R.dimen.major_250)),
            onClick = onPercentageDiscountClicked,
            text = "%",
            isSelected = state.discountType is Percentage,
            contentPadding = PaddingValues(0.5.dp),
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
        )
        WCSelectableChip(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.major_250))
                .height(dimensionResource(id = R.dimen.major_250)),
            onClick = onManualDiscountClicked,
            text = state.currency,
            isSelected = state.discountType is Amount,
            contentPadding = PaddingValues(0.5.dp),
            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
        )
    }
}

data class DiscountInputFieldConfig(
    val decimalSeparator: String,
    val numberOfDecimals: Int,
)

@Composable
fun CalculatedAmount(
    state: ViewState,
) {
    val discountAmount = when (state.discountType) {
        is Percentage -> "${state.currency}${state.calculatedPriceAfterDiscount}"
        is Amount -> "${state.calculatedPriceAfterDiscount}%"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.minor_100),
                end = dimensionResource(id = R.dimen.minor_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (state.discountType) {
                is Percentage -> stringResource(id = R.string.order_creation_discount_amount_label)
                is Amount -> stringResource(id = R.string.order_creation_discount_percentage_label)
            },
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.woo_gray_40)
        )
        Text(
            text = discountAmount,
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.woo_gray_40)
        )
    }
}

@Composable
private fun PriceAfterDiscount(
    state: ViewState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.minor_100),
                end = dimensionResource(id = R.dimen.minor_100),
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.order_creation_price_after_discount_label),
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = "${state.currency}${state.priceAfterDiscount}",
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.woo_gray_40)
        )
    }
}

@Preview
@Composable
fun ToolbarPreview() = Toolbar({}, {}, true)

@Preview
@Composable
fun SwitchPreview() =
    Switch(ViewState("$", BigDecimal.ZERO, isRemoveButtonVisible = true), {}, {})

@Preview
@Composable
fun OrderCreateEditProductDiscountScreenPreview() =
    OrderCreateEditProductDiscountScreen(
        MutableStateFlow(
            ViewState(
                "$",
                BigDecimal.ZERO,
                isRemoveButtonVisible = true,
            )
        ),
        {},
        {},
        {},
        {},
        {},
        {},
        DiscountInputFieldConfig(
            decimalSeparator = ".",
            numberOfDecimals = 2
        ),
        productItem = MutableStateFlow(
            OrderCreationProduct.ProductItem(
                Order.Item(
                    name = "Product Name",
                    quantity = 1f,
                    price = BigDecimal.ZERO,
                    total = BigDecimal.ZERO,
                    productId = 1,
                    variationId = 1,
                    subtotal = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO,
                    sku = "",
                    itemId = 1L,
                    attributesList = emptyList(),
                ),
                ProductInfo(
                    imageUrl = "",
                    isStockManaged = false,
                    stockQuantity = 0.0,
                    stockStatus = ProductStockStatus.InStock,
                    productType = ProductType.SIMPLE,
                    isConfigurable = false,
                    pricePreDiscount = "$10",
                    priceTotal = "$30",
                    priceSubtotal = "$30",
                    discountAmount = "$5",
                    priceAfterDiscount = "$25",
                    hasDiscount = false
                )
            )
        )
    )

@Preview(showBackground = true)
@Composable
fun ProductCardPreview() {
    ProductCard(
        imageUrl = "",
        productName = "Product Name",
        productPrice = BigDecimal.ZERO,
        productQuantity = 1f,
        subTotalPerProduct = BigDecimal.ZERO,
        state = ViewState(
            "$",
            BigDecimal.ZERO,
            isRemoveButtonVisible = true,
        )
    )
}
