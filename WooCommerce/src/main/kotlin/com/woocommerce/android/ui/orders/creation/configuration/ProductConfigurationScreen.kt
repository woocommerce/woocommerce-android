package com.woocommerce.android.ui.orders.creation.configuration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

internal const val OUTLINED_BORDER_OPACITY = 0.14f

@Composable
fun ProductConfigurationScreen(viewModel: ProductConfigurationViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    BackHandler(onBack = viewModel::onCancel)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(id = R.string.product_configuration_title)) },
            navigationIcon = {
                IconButton(viewModel::onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            elevation = 0.dp,
        )
    }) { padding ->
        when (val state = viewState) {
            is ProductConfigurationViewModel.ViewState.Error -> Text(text = state.message)
            is ProductConfigurationViewModel.ViewState.Loading -> Text(text = "Loading")
            is ProductConfigurationViewModel.ViewState.DisplayConfiguration -> {
                ProductConfigurationScreen(
                    productRules = state.productRules,
                    productConfiguration = state.productConfiguration,
                    onUpdateChildrenConfiguration = viewModel::onUpdateChildrenConfiguration,
                    onSaveConfigurationClick = {},
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun ProductConfigurationScreen(
    productRules: ProductRules,
    productConfiguration: ProductConfiguration,
    onUpdateChildrenConfiguration: (Long, String, String) -> Unit,
    onSaveConfigurationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface {
        Column(modifier.fillMaxSize()) {
            productRules.isConfigurable()
            onSaveConfigurationClick()
            productConfiguration.childrenConfiguration?.entries?.forEach { childMapEntry ->

                val item = childMapEntry.key

                val hasQuantityRule = childMapEntry.value.containsKey(QuantityRule.KEY)
                val hasOptionalRule = childMapEntry.value.containsKey(OptionalRule.KEY)
                val hasQuantityAndOptionalRules = hasQuantityRule && hasOptionalRule

                if (hasQuantityAndOptionalRules) {
                    OptionalQuantityProductItem(
                        "item: $item",
                        imageUrl = null,
                        info = null,
                        quantity = childMapEntry.value[QuantityRule.KEY]?.toInt() ?: 0,
                        onQuantityChanged = { value ->
                            onUpdateChildrenConfiguration(item, QuantityRule.KEY, value.toString())
                        },
                        isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false,
                        onSwitchChanged = { value ->
                            onUpdateChildrenConfiguration(item, OptionalRule.KEY, value.toString())
                        }
                    )
                } else {
                    if (hasQuantityRule) {
                        QuantityProductItem(
                            "item: $item",
                            imageUrl = null,
                            info = null,
                            quantity = childMapEntry.value[QuantityRule.KEY]?.toInt() ?: 0,
                            onQuantityChanged = { value ->
                                onUpdateChildrenConfiguration(item, QuantityRule.KEY, value.toString())
                            }
                        )
                    }
                    if (hasOptionalRule) {
                        OptionalProductItem(
                            title = "item: $item",
                            imageUrl = null,
                            info = null,
                            isIncluded = childMapEntry.value[OptionalRule.KEY]?.toBoolean() ?: false,
                            onSwitchChanged = { value ->
                                onUpdateChildrenConfiguration(item, OptionalRule.KEY, value.toString())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OptionalQuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Int,
    onQuantityChanged: (Int) -> Unit,
    isIncluded: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        configurableControlStart = {
            SelectionCheck(
                isSelected = isIncluded,
                onSelectionChange = onSwitchChanged,
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
            )
        },
        configurableControlEnd = {
            Stepper(
                value = quantity,
                onStepUp = { value -> onQuantityChanged(value) },
                onStepDown = { value -> onQuantityChanged(value) },
                isStepDownEnabled = isIncluded,
                isStepUpEnabled = isIncluded
            )
        }
    )
}

@Composable
fun QuantityProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    quantity: Int,
    onQuantityChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Stepper(
            value = quantity,
            onStepUp = { value -> onQuantityChanged(value) },
            onStepDown = { value -> onQuantityChanged(value) }
        )
    }
}

@Preview
@Composable
fun QuantityProductItemPreview() {
    WooThemeWithBackground {
        QuantityProductItem(
            title = "This is an optional item with a very very very long title that should wrap into two columns",
            imageUrl = null,
            info = null,
            quantity = 1,
            onQuantityChanged = {}
        )
    }
}

@Composable
fun OptionalProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    isIncluded: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfigurableListItem(
        title = title,
        imageUrl = imageUrl,
        info = info,
        modifier = modifier.padding(start = 8.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
        configurableControlEnd = {
            SelectionCheck(
                isSelected = isIncluded,
                onSelectionChange = onSwitchChanged,
                modifier = Modifier.size(dimensionResource(id = R.dimen.min_tap_target))
            )
        }
    )
}

@Preview
@Composable
fun OptionalChildrenPreview() {
    WooThemeWithBackground {
        OptionalProductItem(
            title = "This is an optional item with a very very very long title that should wrap into two columns",
            imageUrl = null,
            info = null,
            isIncluded = true,
            onSwitchChanged = {}
        )
    }
}

@Composable
fun SelectionCheck(
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionDrawable = if (isSelected) {
        R.drawable.ic_rounded_chcekbox_checked
    } else {
        R.drawable.ic_rounded_chcekbox_unchecked
    }
    Box(
        modifier = modifier.clickable { onSelectionChange(!isSelected) },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selectionDrawable,
            modifier = modifier.wrapContentSize(),
            label = "itemSelection"
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                contentDescription = "imageContentDescription"
            )
        }
    }
}

@Preview
@Composable
fun SelectionCheckPreview() {
    var value: Boolean by rememberSaveable { mutableStateOf(false) }
    WooThemeWithBackground {
        SelectionCheck(
            isSelected = value,
            onSelectionChange = { newValue -> value = newValue }
        )
    }
}

@Composable
fun ConfigurableListItem(
    title: String,
    imageUrl: String?,
    info: String?,
    modifier: Modifier = Modifier,
    configurableControlStart: @Composable () -> Unit = {},
    configurableControlEnd: @Composable () -> Unit = {}
) {
    Column {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.wrapContentSize()) {
                configurableControlStart()
            }
            OrderProductItem(
                title = title,
                imageUrl = imageUrl,
                info = info,
                modifier = Modifier.weight(2f)
            )
            Box(modifier = Modifier.wrapContentSize()) {
                configurableControlEnd()
            }
        }
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = OUTLINED_BORDER_OPACITY), thickness = 1.dp)
    }
}

@Preview
@Composable
fun ConfigurableListItemPreview() {
    WooThemeWithBackground {
        ConfigurableListItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = "this is the product description"
        ) {
            Button(onClick = { }) {
                Text(text = "Configure Item")
            }
        }
    }
}

@Composable
fun OrderProductItem(
    title: String,
    imageUrl: String?,
    info: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_product),
            error = painterResource(R.drawable.ic_product),
            contentDescription = stringResource(R.string.product_image_content_description),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .size(dimensionResource(R.dimen.major_300))
                .clip(RoundedCornerShape(3.dp))
                .padding(end = 8.dp)
        )

        Column(
            modifier = Modifier
                .wrapContentHeight()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1
            )
            if (!info.isNullOrEmpty()) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Preview
@Composable
fun OrderProductItemWithInfoPreview() {
    WooThemeWithBackground {
        OrderProductItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = "this is the product description"
        )
    }
}

@Preview
@Composable
fun OrderProductItemWithoutInfoPreview() {
    WooThemeWithBackground {
        OrderProductItem(
            title = "This the product title",
            imageUrl = "not valid url",
            info = null
        )
    }
}

@Composable
fun Stepper(
    value: Int,
    onStepUp: (Int) -> Unit,
    onStepDown: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isStepDownEnabled: Boolean = true,
    isStepUpEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = OUTLINED_BORDER_OPACITY),
                shape = RoundedCornerShape(8.dp)
            )

    ) {
        OutlinedButton(
            onClick = { onStepDown(value - 1) },
            enabled = isStepDownEnabled,
            modifier = Modifier
                .sizeIn(
                    minWidth = dimensionResource(id = R.dimen.min_tap_target),
                    minHeight = dimensionResource(id = R.dimen.min_tap_target)
                )
                .padding(top = 1.dp, bottom = 1.dp, start = 2.dp),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_minus),
                contentDescription = stringResource(
                    id = R.string.order_creation_change_product_quantity,
                    value,
                    value - 1
                )
            )
        }
        BasicTextField(
            value = value.toString(),
            readOnly = true,
            onValueChange = {},
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.subtitle1.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface
            ),
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
                .width(dimensionResource(id = R.dimen.min_tap_target))

        )
        OutlinedButton(
            onClick = { onStepUp(value + 1) },
            enabled = isStepUpEnabled,
            modifier = Modifier
                .sizeIn(
                    minWidth = dimensionResource(id = R.dimen.min_tap_target),
                    minHeight = dimensionResource(id = R.dimen.min_tap_target)
                )
                .padding(top = 1.dp, bottom = 1.dp, end = 2.dp),
            border = BorderStroke(0.dp, Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = stringResource(
                    id = R.string.order_creation_change_product_quantity,
                    value,
                    value + 1
                )
            )
        }
    }
}

@Preview
@Composable
fun StepperPreview() {
    var value: Int by rememberSaveable { mutableStateOf(100) }
    WooThemeWithBackground {
        Stepper(
            value = value,
            onStepDown = { newValue -> value = newValue },
            onStepUp = { newValue -> value = newValue },
            modifier = Modifier.padding(16.dp)
        )
    }
}
